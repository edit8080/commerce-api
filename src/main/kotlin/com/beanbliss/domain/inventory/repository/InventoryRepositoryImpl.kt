package com.beanbliss.domain.inventory.repository

import com.beanbliss.domain.inventory.domain.Inventory
import com.beanbliss.domain.inventory.entity.InventoryEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import com.beanbliss.domain.product.entity.ProductEntity
import com.beanbliss.domain.product.entity.ProductOptionEntity
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * [책임]: Spring Data JPA를 활용한 Inventory 영속성 처리
 * Infrastructure Layer에 속하며, JPA 기술에 종속적
 */
interface InventoryJpaRepository : JpaRepository<InventoryEntity, Long> {
    /**
     * 상품 옵션 ID로 재고 조회
     */
    fun findByProductOptionId(productOptionId: Long): InventoryEntity?

    /**
     * 여러 상품 옵션 ID로 재고 일괄 조회 (Bulk 조회)
     */
    fun findByProductOptionIdIn(productOptionIds: List<Long>): List<InventoryEntity>

    /**
     * 전체 재고 개수 조회
     */
    override fun count(): Long

    /**
     * 재고 목록 조회 (PRODUCT_OPTION, PRODUCT와 JOIN)
     * N+1 문제 방지를 위한 단일 쿼리
     */
    @Query("""
        SELECT i, po, p
        FROM InventoryEntity i
        INNER JOIN ProductOptionEntity po ON i.productOptionId = po.id
        INNER JOIN ProductEntity p ON po.productId = p.id
        WHERE po.isActive = true
    """)
    fun findAllWithProductInfo(): List<Array<Any>>
}

/**
 * [책임]: InventoryRepository 인터페이스 구현체
 * - InventoryJpaRepository를 활용하여 실제 DB 접근
 * - INVENTORY_RESERVATION을 고려하여 가용 재고 계산
 */
@Repository
class InventoryRepositoryImpl(
    private val inventoryJpaRepository: InventoryJpaRepository,
    private val inventoryReservationRepository: InventoryReservationRepository
) : InventoryRepository {

    override fun calculateAvailableStock(productOptionId: Long): Int {
        // 실제 재고 조회
        val actualStock = inventoryJpaRepository.findByProductOptionId(productOptionId)?.stockQuantity ?: 0

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
    ): List<InventoryDetail> {
        // 1. 모든 재고 조회 (PRODUCT_OPTION, PRODUCT와 JOIN)
        val results = inventoryJpaRepository.findAllWithProductInfo()

        // 2. InventoryDetail로 변환
        val inventoryDetails = results.map { row ->
            val inventory = row[0] as InventoryEntity
            val productOption = row[1] as ProductOptionEntity
            val product = row[2] as ProductEntity

            InventoryDetail(
                inventoryId = inventory.id,
                productId = product.id,
                productName = product.name,
                productOptionId = productOption.id,
                optionCode = productOption.optionCode,
                optionName = "${productOption.grindType} ${productOption.weightGrams}g",
                price = productOption.price.toInt(),
                stockQuantity = inventory.stockQuantity,
                createdAt = inventory.createdAt
            )
        }

        // 3. 정렬 적용
        val sorted = when (sortBy) {
            "created_at" -> {
                if (sortDirection == "DESC") {
                    inventoryDetails.sortedByDescending { it.createdAt }
                } else {
                    inventoryDetails.sortedBy { it.createdAt }
                }
            }
            else -> inventoryDetails
        }

        // 4. 페이징 적용 (1-based index)
        val offset = (page - 1) * size
        return sorted.drop(offset).take(size)
    }

    override fun count(): Long {
        return inventoryJpaRepository.count()
    }

    override fun findByProductOptionId(productOptionId: Long): Inventory? {
        val entity = inventoryJpaRepository.findByProductOptionId(productOptionId) ?: return null
        return Inventory(
            productOptionId = entity.productOptionId,
            stockQuantity = entity.stockQuantity
        )
    }

    override fun findAllByProductOptionIds(productOptionIds: List<Long>): List<Inventory> {
        if (productOptionIds.isEmpty()) {
            return emptyList()
        }

        return inventoryJpaRepository.findByProductOptionIdIn(productOptionIds).map { entity ->
            Inventory(
                productOptionId = entity.productOptionId,
                stockQuantity = entity.stockQuantity
            )
        }
    }

    override fun save(inventory: Inventory): Inventory {
        // 기존 entity가 있으면 수정, 없으면 새로 생성
        val existingEntity = inventoryJpaRepository.findByProductOptionId(inventory.productOptionId)

        val entity = if (existingEntity != null) {
            // 기존 entity 업데이트
            InventoryEntity(
                id = existingEntity.id,
                productOptionId = existingEntity.productOptionId,
                stockQuantity = inventory.stockQuantity,
                createdAt = existingEntity.createdAt,
                updatedAt = java.time.LocalDateTime.now()
            )
        } else {
            // 새로운 entity 생성
            InventoryEntity(
                id = 0L,
                productOptionId = inventory.productOptionId,
                stockQuantity = inventory.stockQuantity
            )
        }

        val savedEntity = inventoryJpaRepository.save(entity)

        return Inventory(
            productOptionId = savedEntity.productOptionId,
            stockQuantity = savedEntity.stockQuantity
        )
    }
}
