package com.beanbliss.domain.inventory.repository

import com.beanbliss.domain.inventory.entity.InventoryEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * [책임]: 재고 In-memory 저장소 구현
 * - ConcurrentHashMap을 사용하여 Thread-safe 보장
 * - product_option_id를 키로 사용하여 재고 정보 관리
 * - INVENTORY_RESERVATION을 고려하여 가용 재고 계산
 *
 * [주의사항]:
 * - 애플리케이션 재시작 시 데이터 소실 (In-memory 특성)
 * - 실제 DB 사용 시 JPA 기반 구현체로 교체 필요
 */
@Repository
class InventoryRepositoryImpl(
    private val inventoryReservationRepository: InventoryReservationRepository
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
