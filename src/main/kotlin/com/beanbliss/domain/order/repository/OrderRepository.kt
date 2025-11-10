package com.beanbliss.domain.order.repository

import com.beanbliss.domain.order.entity.OrderEntity

/**
 * [책임]: 주문 영속성 계층의 계약 정의
 * UseCase는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface OrderRepository {
    /**
     * 주문 정보 저장
     *
     * @param order 저장할 주문 정보
     * @return 저장된 OrderEntity (ID 포함)
     */
    fun save(order: OrderEntity): OrderEntity
}
