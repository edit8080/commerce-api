package com.beanbliss.domain.inventory.repository

import com.beanbliss.common.util.SortUtils
import com.beanbliss.domain.inventory.domain.Inventory
import com.beanbliss.domain.inventory.entity.InventoryEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
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
     * 특정 상품 옵션의 가용 재고 계산 (INVENTORY_RESERVATION과 LEFT JOIN)
     *
     * 계산식: INVENTORY.stock_quantity - COALESCE(SUM(INVENTORY_RESERVATION.quantity), 0)
     * WHERE INVENTORY_RESERVATION.status IN ('RESERVED', 'CONFIRMED')
     *
     * @param productOptionId 상품 옵션 ID
     * @return List<Array<Any>> = [[stockQuantity: Int, reservedQuantity: Long]]
     */
    @Query("""
        SELECT i.stockQuantity, COALESCE(SUM(ir.quantity), 0)
        FROM InventoryEntity i
        LEFT JOIN InventoryReservationEntity ir ON ir.productOptionId = i.productOptionId
            AND ir.status IN ('RESERVED', 'CONFIRMED')
        WHERE i.productOptionId = :productOptionId
        GROUP BY i.productOptionId
    """)
    fun calculateAvailableStockForOption(@Param("productOptionId") productOptionId: Long): List<Array<Any>>

    /**
     * 여러 상품 옵션의 가용 재고를 한 번에 계산 (Batch 조회)
     *
     * N+1 문제 방지: INVENTORY_RESERVATION과 LEFT JOIN하여 단일 쿼리로 처리
     *
     * @param productOptionIds 상품 옵션 ID 리스트
     * @return List<Array<Any>> = [[productOptionId: Long, stockQuantity: Int, reservedQuantity: Long], ...]
     */
    @Query("""
        SELECT i.productOptionId, i.stockQuantity, COALESCE(SUM(ir.quantity), 0)
        FROM InventoryEntity i
        LEFT JOIN InventoryReservationEntity ir ON ir.productOptionId = i.productOptionId
            AND ir.status IN ('RESERVED', 'CONFIRMED')
        WHERE i.productOptionId IN :productOptionIds
        GROUP BY i.productOptionId
    """)
    fun calculateAvailableStockBatchForOptions(@Param("productOptionIds") productOptionIds: List<Long>): List<Array<Any>>
}

/**
 * [책임]: InventoryRepository 인터페이스 구현체
 * - InventoryJpaRepository를 활용하여 실제 DB 접근
 * - INVENTORY_RESERVATION과 LEFT JOIN하여 가용 재고 계산 (단일 쿼리)
 */
@Repository
class InventoryRepositoryImpl(
    private val inventoryJpaRepository: InventoryJpaRepository
) : InventoryRepository {

    override fun calculateAvailableStock(productOptionId: Long): Int {
        // INVENTORY와 INVENTORY_RESERVATION을 LEFT JOIN하여 단일 쿼리로 조회
        val results = inventoryJpaRepository.calculateAvailableStockForOption(productOptionId)

        // 빈 결과 체크
        if (results.isEmpty()) {
            return 0
        }

        // result = [stockQuantity: Int, reservedQuantity: Number]
        val result = results[0]
        val stockQuantity = result[0] as Int
        val reservedQuantity = (result[1] as Number).toInt()

        // 가용 재고 = 실제 재고 - 예약된 수량
        return stockQuantity - reservedQuantity
    }

    override fun calculateAvailableStockBatch(productOptionIds: List<Long>): Map<Long, Int> {
        if (productOptionIds.isEmpty()) {
            return emptyMap()
        }

        // INVENTORY와 INVENTORY_RESERVATION을 LEFT JOIN하여 단일 쿼리로 Batch 조회
        val results = inventoryJpaRepository.calculateAvailableStockBatchForOptions(productOptionIds)

        // Map<productOptionId, availableStock> 변환
        return results.associate { row ->
            // row = [productOptionId: Long, stockQuantity: Int, reservedQuantity: Number]
            val productOptionId = row[0] as Long
            val stockQuantity = row[1] as Int
            val reservedQuantity = (row[2] as Number).toInt()
            val availableStock = stockQuantity - reservedQuantity

            productOptionId to availableStock
        }
    }

    override fun findAll(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): List<Inventory> {
        // 1. Create Sort object using SortUtils
        val sort = SortUtils.createSort(sortBy, sortDirection)

        // 2. DB 레벨에서 정렬 및 페이징 적용하여 재고 조회 (INVENTORY만)
        val pageRequest = PageRequest.of(page - 1, size, sort)
        val results = inventoryJpaRepository.findAll(pageRequest).content

        // 3. Inventory 도메인 모델로 변환
        return results.map { entity ->
            Inventory(
                productOptionId = entity.productOptionId,
                stockQuantity = entity.stockQuantity
            )
        }
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
