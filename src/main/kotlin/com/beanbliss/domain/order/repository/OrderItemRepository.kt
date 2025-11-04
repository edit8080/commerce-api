package com.beanbliss.domain.order.repository

import com.beanbliss.domain.order.entity.OrderItemEntity

/**
 * [책임]: 주문 항목 영속성 계층의 계약 정의
 * UseCase는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface OrderItemRepository {
    /**
     * 주문 항목 목록 일괄 저장
     *
     * @param orderItems 저장할 주문 항목 목록
     * @return 저장된 OrderItemEntity 목록 (ID 포함)
     */
    fun saveAll(orderItems: List<OrderItemEntity>): List<OrderItemEntity>
}
