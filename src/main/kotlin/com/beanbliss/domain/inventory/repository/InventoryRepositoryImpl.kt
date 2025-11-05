package com.beanbliss.domain.inventory.repository

import com.beanbliss.domain.inventory.domain.Inventory
import com.beanbliss.domain.inventory.dto.InventoryResponse
import com.beanbliss.domain.inventory.entity.InventoryEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import com.beanbliss.domain.product.repository.ProductOptionRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * [책임]: 재고 In-memory 저장소 구현
 * - ConcurrentHashMap을 사용하여 Thread-safe 보장
 * - product_option_id를 키로 사용하여 재고 정보 관리
 * - INVENTORY_RESERVATION을 고려하여 가용 재고 계산
 * - INVENTORY + PRODUCT_OPTION + PRODUCT를 JOIN하여 재고 목록 조회
 *
 * [주의사항]:
 * - 애플리케이션 재시작 시 데이터 소실 (In-memory 특성)
 * - 실제 DB 사용 시 JPA 기반 구현체로 교체 필요
 */
@Repository
class InventoryRepositoryImpl(
    private val inventoryReservationRepository: InventoryReservationRepository,
    private val productOptionRepository: ProductOptionRepository
) : InventoryRepository {

    // Thread-safe한 In-memory 저장소 (product_option_id -> InventoryEntity)
    private val inventories = ConcurrentHashMap<Long, InventoryEntity>()

    override fun calculateAvailableStock(productOptionId: Long): Int {
        // 실제 재고 조회
        val actualStock = inventories[productOptionId]?.stockQuantity ?: 0

        // 예약된 수량 합계 조회 (RESERVED, CONFIRMED 상태)
        val reservedQuantity = inventoryReservationRepository.sumQuantityByProductOptionIdAndStatus(
            productOptionId,
            InventoryReservationStatus.activeStatuses()
        )

        // 가용 재고 = 실제 재고 - 예약된 수량
        return actualStock - reservedQuantity
    }

    override fun calculateAvailableStockBatch(productOptionIds: List<Long>): Map<Long, Int> {
        return productOptionIds.associateWith { optionId ->
            calculateAvailableStock(optionId)
        }
    }

    override fun findAllWithProductInfo(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): List<InventoryResponse> {
        // 1. 모든 재고를 조회하고, ProductOption 정보와 JOIN
        val inventoryResponses = inventories.values.mapNotNull { inventory ->
            // ProductOption 정보 조회 (PRODUCT와 JOIN된 정보 포함)
            val optionDetail = productOptionRepository.findActiveOptionWithProduct(inventory.productOptionId)
                ?: return@mapNotNull null // 활성 옵션이 아니면 제외

            // InventoryResponse 생성
            InventoryResponse(
                inventoryId = inventory.id,
                productId = optionDetail.productId,
                productName = optionDetail.productName,
                productOptionId = optionDetail.optionId,
                optionCode = optionDetail.optionCode,
                optionName = "${optionDetail.grindType} ${optionDetail.weightGrams}g", // 옵션명 생성
                price = optionDetail.price,
                stockQuantity = inventory.stockQuantity,
                createdAt = inventory.createdAt
            )
        }

        // 2. 정렬 (sortBy, sortDirection 적용)
        val sorted = when (sortBy) {
            "created_at" -> {
                if (sortDirection == "DESC") {
                    inventoryResponses.sortedByDescending { it.createdAt }
                } else {
                    inventoryResponses.sortedBy { it.createdAt }
                }
            }
            else -> inventoryResponses // 기본 정렬 (정렬 기준 없음)
        }

        // 3. 페이징 적용 (offset, limit)
        val offset = (page - 1) * size
        return sorted.drop(offset).take(size)
    }

    override fun count(): Long {
        return inventories.size.toLong()
    }

    override fun findByProductOptionId(productOptionId: Long): Inventory? {
        // InventoryEntity를 조회하여 Inventory 도메인 모델로 변환
        val entity = inventories[productOptionId] ?: return null
        return Inventory(
            productOptionId = entity.productOptionId,
            stockQuantity = entity.stockQuantity
        )
    }

    override fun save(inventory: Inventory): Inventory {
        // 기존 entity가 있으면 수정, 없으면 새로 생성
        val existingEntity = inventories[inventory.productOptionId]

        val entity = if (existingEntity != null) {
            // 기존 entity 업데이트
            existingEntity.copy(
                stockQuantity = inventory.stockQuantity,
                updatedAt = LocalDateTime.now()
            )
        } else {
            // 새로운 entity 생성
            InventoryEntity(
                id = inventories.size.toLong() + 1, // 간단한 ID 생성
                productOptionId = inventory.productOptionId,
                stockQuantity = inventory.stockQuantity,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        }

        // 저장
        inventories[inventory.productOptionId] = entity

        // 저장된 entity를 domain model로 변환하여 반환
        return Inventory(
            productOptionId = entity.productOptionId,
            stockQuantity = entity.stockQuantity
        )
    }

    /**
     * 테스트용 헬퍼 메서드: 재고 정보 추가
     */
    fun add(entity: InventoryEntity) {
        inventories[entity.productOptionId] = entity
    }

    /**
     * 테스트용 헬퍼 메서드: 모든 데이터 삭제
     */
    fun clear() {
        inventories.clear()
    }
}
