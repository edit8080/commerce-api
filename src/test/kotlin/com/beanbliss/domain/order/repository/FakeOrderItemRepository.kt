package com.beanbliss.domain.order.repository

import com.beanbliss.domain.order.entity.OrderItemEntity

/**
 * 테스트용 In-Memory OrderItemRepository 구현체
 *
 * [특징]:
 * - 실제 DB 없이 메모리 기반으로 동작
 * - Repository 인터페이스의 계약을 준수
 * - 테스트 격리를 위해 각 테스트마다 새 인스턴스 생성
 */
class FakeOrderItemRepository : OrderItemRepository {

    private val orderItems = mutableMapOf<Long, OrderItemEntity>()
    private var nextId = 1L

    override fun saveAll(orderItems: List<OrderItemEntity>): List<OrderItemEntity> {
        return orderItems.map { orderItem ->
            val id = if (orderItem.id == 0L) nextId++ else orderItem.id
            val savedOrderItem = orderItem.copy(id = id)
            this.orderItems[id] = savedOrderItem
            savedOrderItem
        }
    }

    // === Test Helper Methods ===

    /**
     * 테스트용: 모든 데이터 삭제
     */
    fun clear() {
        orderItems.clear()
        nextId = 1L
    }

    /**
     * 테스트용: ID로 주문 항목 조회
     */
    fun findById(id: Long): OrderItemEntity? {
        return orderItems[id]
    }

    /**
     * 테스트용: 모든 주문 항목 조회
     */
    fun findAll(): List<OrderItemEntity> {
        return orderItems.values.toList()
    }

    /**
     * 테스트용: 주문 ID로 주문 항목 목록 조회
     */
    fun findByOrderId(orderId: Long): List<OrderItemEntity> {
        return orderItems.values.filter { it.orderId == orderId }
    }
}
