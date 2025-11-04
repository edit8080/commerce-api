package com.beanbliss.domain.order.repository

import com.beanbliss.domain.order.entity.OrderEntity

/**
 * 테스트용 In-Memory OrderRepository 구현체
 *
 * [특징]:
 * - 실제 DB 없이 메모리 기반으로 동작
 * - Repository 인터페이스의 계약을 준수
 * - 테스트 격리를 위해 각 테스트마다 새 인스턴스 생성
 */
class FakeOrderRepository : OrderRepository {

    private val orders = mutableMapOf<Long, OrderEntity>()
    private var nextId = 1L

    override fun save(order: OrderEntity): OrderEntity {
        val id = if (order.id == 0L) nextId++ else order.id
        val savedOrder = order.copy(id = id)
        orders[id] = savedOrder
        return savedOrder
    }

    // === Test Helper Methods ===

    /**
     * 테스트용: 모든 데이터 삭제
     */
    fun clear() {
        orders.clear()
        nextId = 1L
    }

    /**
     * 테스트용: ID로 주문 조회
     */
    fun findById(id: Long): OrderEntity? {
        return orders[id]
    }

    /**
     * 테스트용: 모든 주문 조회
     */
    fun findAll(): List<OrderEntity> {
        return orders.values.toList()
    }
}
