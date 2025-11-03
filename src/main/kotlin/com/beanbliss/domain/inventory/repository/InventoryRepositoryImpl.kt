package com.beanbliss.domain.inventory.repository

import com.beanbliss.domain.inventory.entity.InventoryEntity
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * [책임]: 재고 In-memory 저장소 구현
 * - ConcurrentHashMap을 사용하여 Thread-safe 보장
 * - product_option_id를 키로 사용하여 재고 정보 관리
 *
 * [주의사항]:
 * - 애플리케이션 재시작 시 데이터 소실 (In-memory 특성)
 * - 실제 DB 사용 시 JPA 기반 구현체로 교체 필요
 * - 현재는 INVENTORY_RESERVATION을 고려하지 않고 stock_quantity만 반환
 *   (INVENTORY_RESERVATION 구현 시 계산 로직 추가 필요)
 */
@Repository
class InventoryRepositoryImpl : InventoryRepository {

    // Thread-safe한 In-memory 저장소 (product_option_id -> InventoryEntity)
    private val inventories = ConcurrentHashMap<Long, InventoryEntity>()

    override fun calculateAvailableStock(productOptionId: Long): Int {
        // TODO: INVENTORY_RESERVATION 구현 시 다음 계산식 적용 필요
        // 계산식: INVENTORY.stock_quantity - SUM(INVENTORY_RESERVATION.quantity WHERE status = 'RESERVED')

        // 현재는 단순히 stock_quantity만 반환
        return inventories[productOptionId]?.stockQuantity ?: 0
    }

    override fun calculateAvailableStockBatch(productOptionIds: List<Long>): Map<Long, Int> {
        // TODO: INVENTORY_RESERVATION 구현 시 배치 조회 및 계산 로직 적용 필요
        // 계산식: INVENTORY.stock_quantity - SUM(INVENTORY_RESERVATION.quantity WHERE status = 'RESERVED')

        // 현재는 각 optionId에 대해 stock_quantity만 매핑하여 반환
        return productOptionIds.associateWith { optionId ->
            inventories[optionId]?.stockQuantity ?: 0
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
