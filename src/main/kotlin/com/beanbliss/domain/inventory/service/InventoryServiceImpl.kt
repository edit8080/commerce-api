package com.beanbliss.domain.inventory.service

import com.beanbliss.common.dto.PageableResponse
import com.beanbliss.common.exception.InvalidPageNumberException
import com.beanbliss.common.exception.InvalidPageSizeException
import com.beanbliss.common.exception.ResourceNotFoundException
import com.beanbliss.common.pagination.PageCalculator
import com.beanbliss.domain.cart.dto.CartItemResponse
import com.beanbliss.domain.inventory.dto.InventoryListResponse
import com.beanbliss.domain.inventory.entity.InventoryReservationEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import com.beanbliss.domain.inventory.repository.InventoryRepository
import com.beanbliss.domain.inventory.repository.InventoryReservationRepository
import com.beanbliss.domain.order.dto.InventoryReservationItemResponse
import com.beanbliss.domain.order.exception.DuplicateReservationException
import com.beanbliss.domain.order.exception.InsufficientAvailableStockException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * [책임]: 재고 관리 비즈니스 로직 구현
 *
 * [주요 기능]:
 * 1. 파라미터 유효성 검증
 * 2. Repository 호출 및 결과 조립
 * 3. 페이지 정보 계산
 * 4. 재고 예약 생성 및 관리
 *
 * [DIP 준수]:
 * - InventoryRepository Interface에만 의존
 */
@Service
class InventoryServiceImpl(
    private val inventoryRepository: InventoryRepository,
    private val inventoryReservationRepository: InventoryReservationRepository
) : InventoryService {

    override fun getInventories(page: Int, size: Int): InventoryListResponse {
        // 1. 파라미터 유효성 검증
        validatePageNumber(page)
        validatePageSize(size)

        // 2. 재고 목록 조회 (created_at DESC 정렬)
        val inventories = inventoryRepository.findAllWithProductInfo(
            page = page,
            size = size,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // 3. 전체 재고 개수 조회
        val totalElements = inventoryRepository.count()

        // 4. 전체 페이지 수 계산 (공통 유틸리티 사용)
        val totalPages = PageCalculator.calculateTotalPages(totalElements, size)

        // 5. 응답 데이터 조립
        return InventoryListResponse(
            content = inventories,
            pageable = PageableResponse(
                pageNumber = page,
                pageSize = size,
                totalElements = totalElements,
                totalPages = totalPages
            )
        )
    }

    /**
     * 페이지 번호 유효성 검증
     *
     * @throws InvalidPageNumberException 페이지 번호가 1 미만인 경우
     */
    private fun validatePageNumber(page: Int) {
        if (page < 1) {
            throw InvalidPageNumberException("페이지 번호는 1 이상이어야 합니다.")
        }
    }

    /**
     * 페이지 크기 유효성 검증
     *
     * @throws InvalidPageSizeException 페이지 크기가 1 미만이거나 100 초과인 경우
     */
    private fun validatePageSize(size: Int) {
        if (size < 1 || size > 100) {
            throw InvalidPageSizeException("페이지 크기는 1 이상 100 이하여야 합니다.")
        }
    }

    @Transactional
    override fun addStock(productOptionId: Long, quantity: Int): Int {
        // 1. 재고 조회 (비관적 락 - TODO: Repository 구현 시 적용)
        val inventory = inventoryRepository.findByProductOptionId(productOptionId)
            ?: throw ResourceNotFoundException("상품 옵션 ID: $productOptionId 의 재고 정보를 찾을 수 없습니다.")

        // 2. 도메인 모델에 비즈니스 로직 위임 (최대 재고 수량 검증 포함)
        val currentStock = inventory.addStock(quantity)

        // 3. 변경 사항 저장
        inventoryRepository.save(inventory)

        // 4. 현재 재고 수량 반환
        return currentStock
    }

    @Transactional(readOnly = true)
    override fun calculateAvailableStockBatch(optionIds: List<Long>): Map<Long, Int> {
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
     * 4. 예약 정보 응답 DTO 변환
     *
     * [트랜잭션]:
     * - @Transactional로 원자성 보장
     * - 재고 부족 시 롤백
     *
     * @param userId 사용자 ID
     * @param cartItems 장바구니 아이템 목록 (상품 정보 포함)
     * @return 생성된 재고 예약 정보 목록
     * @throws DuplicateReservationException 이미 활성 예약이 존재하는 경우
     * @throws InsufficientAvailableStockException 가용 재고가 부족한 경우
     */
    @Transactional
    override fun reserveInventory(userId: Long, cartItems: List<CartItemResponse>): List<InventoryReservationItemResponse> {
        // 1. 중복 예약 방지
        val activeReservationCount = inventoryReservationRepository.countActiveReservations(userId)
        if (activeReservationCount > 0) {
            throw DuplicateReservationException("이미 진행 중인 주문 예약이 있습니다.")
        }

        // 2. 가용 재고 계산 및 예약 생성
        val now = LocalDateTime.now()
        val expiresAt = now.plusMinutes(30)
        val reservations = mutableListOf<InventoryReservationItemResponse>()

        cartItems.forEach { cartItem ->
            // 가용 재고 조회
            val availableStock = inventoryRepository.calculateAvailableStock(cartItem.productOptionId)

            // 재고 충분성 검증
            if (availableStock < cartItem.quantity) {
                throw InsufficientAvailableStockException(
                    "가용 재고가 부족합니다. 상품 옵션 ID: ${cartItem.productOptionId}"
                )
            }

            // 예약 생성
            val reservation = InventoryReservationEntity(
                id = 0L,
                productOptionId = cartItem.productOptionId,
                userId = userId,
                quantity = cartItem.quantity,
                status = InventoryReservationStatus.RESERVED,
                reservedAt = now,
                expiresAt = expiresAt,
                updatedAt = now
            )

            val savedReservation = inventoryReservationRepository.save(reservation)

            // 응답 DTO 변환
            val reservationResponse = InventoryReservationItemResponse(
                reservationId = savedReservation.id,
                productOptionId = savedReservation.productOptionId,
                productName = cartItem.productName,
                optionCode = cartItem.optionCode,
                quantity = savedReservation.quantity,
                status = savedReservation.status,
                availableStock = availableStock - cartItem.quantity,
                reservedAt = savedReservation.reservedAt,
                expiresAt = savedReservation.expiresAt
            )

            reservations.add(reservationResponse)
        }

        return reservations
    }
}
