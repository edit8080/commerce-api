package com.beanbliss.domain.inventory.service

import com.beanbliss.common.exception.ResourceNotFoundException
import com.beanbliss.domain.cart.domain.CartItemDetail
import com.beanbliss.domain.inventory.domain.Inventory
import com.beanbliss.domain.inventory.entity.InventoryReservationEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import com.beanbliss.domain.inventory.repository.InventoryRepository
import com.beanbliss.domain.inventory.repository.InventoryReservationRepository
import com.beanbliss.domain.order.exception.DuplicateReservationException
import com.beanbliss.domain.order.exception.InsufficientAvailableStockException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * [책임]: 재고 관리 비즈니스 로직 구현
 *
 * [주요 기능]:
 * 1. Repository 호출 및 결과 조립
 * 2. 페이지 정보 계산
 * 3. 재고 예약 생성 및 관리
 *
 * [DIP 준수]:
 * - InventoryRepository Interface에만 의존
 *
 * [참고]:
 * - 파라미터 유효성 검증은 Controller에서 Jakarta Validator로 수행됨
 */
@Service
class InventoryService(
    private val inventoryRepository: InventoryRepository,
    private val inventoryReservationRepository: InventoryReservationRepository
) {

    /**
     * 재고 목록 조회 결과 (INVENTORY 도메인만)
     *
     * [설계 변경]:
     * - PRODUCT 정보 제거
     * - INVENTORY 도메인만 반환
     * - UseCase에서 PRODUCT 정보와 조합
     */
    data class InventoriesResult(
        val inventories: List<Inventory>,
        val totalElements: Long
    )

    /**
     * 재고 예약 정보 (도메인 데이터)
     */
    data class ReservationItem(
        val reservationEntity: InventoryReservationEntity,
        val productName: String,
        val optionCode: String,
        val availableStockAfterReservation: Int
    )

    /**
     * 재고 목록 조회 (INVENTORY 도메인만)
     *
     * [비즈니스 로직]:
     * 1. INVENTORY 테이블에서 재고 목록 조회
     * 2. 전체 재고 개수 조회 (페이징용)
     * 3. INVENTORY 도메인 데이터 반환
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 재고 목록 + 전체 개수
     */
    fun getInventories(page: Int, size: Int): InventoriesResult {
        // 1. 재고 목록 조회 (product_option_id DESC 정렬)
        val inventories = inventoryRepository.findAll(
            page = page,
            size = size,
            sortBy = "product_option_id",
            sortDirection = "DESC"
        )

        // 2. 전체 재고 개수 조회
        val totalElements = inventoryRepository.count()

        // 3. INVENTORY 도메인 데이터 반환
        return InventoriesResult(
            inventories = inventories,
            totalElements = totalElements
        )
    }

    @Transactional
    fun addStock(productOptionId: Long, quantity: Int): Int {
        // 1. 재고 조회 (비관적 락 적용)
        val inventory = inventoryRepository.findByProductOptionIdWithLock(productOptionId)
            ?: throw ResourceNotFoundException("상품 옵션 ID: $productOptionId 의 재고 정보를 찾을 수 없습니다.")

        // 2. 도메인 모델에 비즈니스 로직 위임 (최대 재고 수량 검증 포함)
        val currentStock = inventory.addStock(quantity)

        // 3. 변경 사항 저장
        inventoryRepository.save(inventory)

        // 4. 현재 재고 수량 반환
        return currentStock
    }

    @Transactional(readOnly = true)
    fun calculateAvailableStockBatch(optionIds: List<Long>): Map<Long, Int> {
        // 빈 목록인 경우 조기 반환
        if (optionIds.isEmpty()) {
            return emptyMap()
        }

        // Repository에 위임: Batch 쿼리로 모든 옵션의 가용 재고 조회
        // 계산식: 총 재고 - 예약 재고 (INVENTORY_RESERVATION)
        return inventoryRepository.calculateAvailableStockBatch(optionIds)
    }

    /**
     * 재고 예약 생성
     *
     * [비즈니스 로직]:
     * 1. 중복 예약 방지: 사용자의 활성 예약 존재 여부 확인
     * 2. 가용 재고 계산 및 충분성 검증
     * 3. 예약 엔티티 생성 (30분 만료)
     *
     * [트랜잭션]:
     * - @Transactional로 원자성 보장
     * - 재고 부족 시 롤백
     *
     * @param userId 사용자 ID
     * @param cartItems 장바구니 아이템 목록 (상품 정보 포함)
     * @return 생성된 재고 예약 정보 목록 (도메인 데이터)
     * @throws DuplicateReservationException 이미 활성 예약이 존재하는 경우
     * @throws InsufficientAvailableStockException 가용 재고가 부족한 경우
     */
    @Transactional
    fun reserveInventory(userId: Long, cartItems: List<CartItemDetail>): List<ReservationItem> {
        // 1. 재고 일괄 조회 (Bulk 조회 + 비관적 락)
        // [동시성 제어]: FOR UPDATE 락으로 재고 조회부터 예약 생성까지 원자성 보장
        val productOptionIds = cartItems.map { it.productOptionId }
        val inventories = inventoryRepository.findAllByProductOptionIdsWithLock(productOptionIds)

        // 1-1. 재고가 모든 상품에 대해 존재하는지 확인
        if (inventories.size != productOptionIds.size) {
            val foundIds = inventories.map { it.productOptionId }.toSet()
            val missingIds = productOptionIds.filterNot { it in foundIds }
            throw ResourceNotFoundException("재고 정보를 찾을 수 없습니다. 상품 옵션 ID: ${missingIds.joinToString()}")
        }

        // 2. 중복 예약 방지 (재고 락 범위 내에서 확인)
        // [중요]: 재고 락이 걸린 상태에서 예약 확인 → 동시 예약 방지
        val activeReservationCount = inventoryReservationRepository.countActiveReservations(userId)
        if (activeReservationCount > 0) {
            throw DuplicateReservationException("이미 진행 중인 주문 예약이 있습니다.")
        }

        // 3. 가용 재고 계산 (재고 락 범위 내)
        val inventoryMap = inventories.associateBy { it.productOptionId }
        val availableStocks = productOptionIds.associateWith { optionId ->
            val inventory = inventoryMap[optionId]!!
            val reservedQuantity = inventoryReservationRepository.sumQuantityByProductOptionIdAndStatus(
                optionId,
                listOf(InventoryReservationStatus.RESERVED, InventoryReservationStatus.CONFIRMED)
            )
            inventory.stockQuantity - reservedQuantity
        }

        // 4. 재고 충분성 검증
        cartItems.forEach { cartItem ->
            val availableStock = availableStocks[cartItem.productOptionId] ?: 0
            if (availableStock < cartItem.quantity) {
                throw InsufficientAvailableStockException(
                    "가용 재고가 부족합니다. 상품 옵션 ID: ${cartItem.productOptionId}, 가용 재고: $availableStock, 요청 수량: ${cartItem.quantity}"
                )
            }
        }

        // 5. 예약 엔티티 생성 (Bulk Insert 준비)
        val now = LocalDateTime.now()
        val expiresAt = now.plusMinutes(30)
        val reservationEntities = cartItems.map { cartItem ->
            InventoryReservationEntity(
                id = 0L,
                productOptionId = cartItem.productOptionId,
                userId = userId,
                quantity = cartItem.quantity,
                status = InventoryReservationStatus.RESERVED,
                reservedAt = now,
                expiresAt = expiresAt,
                updatedAt = now
            )
        }

        // 6. 예약 일괄 저장 (Batch Insert - 단일 트랜잭션)
        // [성능 최적화]: N번의 INSERT를 단일 Batch INSERT로 처리
        val savedReservations = inventoryReservationRepository.saveAll(reservationEntities)

        // 7. 도메인 데이터 반환
        return savedReservations.mapIndexed { index, savedReservation ->
            val cartItem = cartItems[index]
            val availableStock = availableStocks[cartItem.productOptionId]!!

            ReservationItem(
                reservationEntity = savedReservation,
                productName = cartItem.productName,
                optionCode = cartItem.optionCode,
                availableStockAfterReservation = availableStock - cartItem.quantity
            )
        }
    }

    @Transactional
    fun reduceStockForOrder(cartItems: List<CartItemDetail>) {
        // 1. 장바구니 아이템의 모든 상품 옵션 ID 추출
        val productOptionIds = cartItems.map { it.productOptionId }

        // 2. 재고 일괄 조회 (Bulk 조회 + 비관적 락 - 단일 쿼리)
        // [성능 최적화]: WHERE product_option_id IN (...) 사용으로 N+1 문제 방지
        // [동시성 제어]: FOR UPDATE + ORDER BY로 Deadlock 방지
        val inventories = inventoryRepository.findAllByProductOptionIdsWithLock(productOptionIds)

        // 2-1. 조회된 재고가 모든 상품 옵션에 대해 존재하는지 확인
        if (inventories.size != productOptionIds.size) {
            val foundIds = inventories.map { it.productOptionId }.toSet()
            val missingIds = productOptionIds.filterNot { it in foundIds }
            throw ResourceNotFoundException("재고 정보를 찾을 수 없습니다. 상품 옵션 ID: ${missingIds.joinToString()}")
        }

        // 3. 재고 검증 (Map으로 변환하여 O(1) 조회)
        val inventoryMap = inventories.associateBy { it.productOptionId }
        cartItems.forEach { cartItem ->
            val inventory = inventoryMap[cartItem.productOptionId]!!

            if (inventory.stockQuantity < cartItem.quantity) {
                throw com.beanbliss.domain.inventory.exception.InsufficientStockException(
                    "재고가 부족합니다. 상품 옵션 ID: ${cartItem.productOptionId}, 현재 재고: ${inventory.stockQuantity}, 요청 수량: ${cartItem.quantity}"
                )
            }
        }

        // 4. 재고 일괄 차감
        cartItems.forEach { cartItem ->
            val inventory = inventoryMap[cartItem.productOptionId]!!
            inventory.stockQuantity = inventory.stockQuantity - cartItem.quantity
            inventoryRepository.save(inventory)
        }
    }
}
