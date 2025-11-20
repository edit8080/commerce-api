package com.beanbliss.domain.order.repository

import com.beanbliss.domain.order.entity.OrderEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * [책임]: Spring Data JPA를 활용한 Order 영속성 처리
 * Infrastructure Layer에 속하며, JPA 기술에 종속적
 */
interface OrderJpaRepository : JpaRepository<OrderEntity, Long>

/**
 * [책임]: OrderRepository 인터페이스 구현체
 * - OrderJpaRepository를 활용하여 실제 DB 접근
 */
@Repository
class OrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository
) : OrderRepository {

    override fun save(order: OrderEntity): OrderEntity {
        return orderJpaRepository.save(order)
    }
}
