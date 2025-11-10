package com.beanbliss.domain.order.repository

import com.beanbliss.domain.order.entity.OrderItemEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * [책임]: Spring Data JPA를 활용한 OrderItem 영속성 처리
 * Infrastructure Layer에 속하며, JPA 기술에 종속적
 */
interface OrderItemJpaRepository : JpaRepository<OrderItemEntity, Long> {
    /**
     * 지정된 기간 동안 가장 많이 주문된 상품 조회
     *
     * [쿼리 로직]:
     * 1. ORDER_ITEM.created_at >= startDate 필터링
     * 2. PRODUCT_OPTION과 JOIN (product_option_id)
     * 3. PRODUCT_OPTION.is_active = true 필터링 (활성 상품만)
     * 4. product_id별로 quantity 합계 집계
     * 5. SUM(quantity) DESC 정렬
     * 6. limit 개수만큼 반환
     */
    @Query("""
        SELECT po.productId as productId, SUM(oi.quantity) as totalOrderCount
        FROM OrderItemEntity oi
        INNER JOIN ProductOptionEntity po ON oi.productOptionId = po.id
        WHERE oi.createdAt >= :startDate
        AND po.isActive = true
        GROUP BY po.productId
        ORDER BY SUM(oi.quantity) DESC
        LIMIT :limit
    """)
    fun findTopOrderedProducts(
        @Param("startDate") startDate: LocalDateTime,
        @Param("limit") limit: Int
    ): List<Array<Any>>
}

/**
 * [책임]: OrderItemRepository 인터페이스 구현체
 * - OrderItemJpaRepository를 활용하여 실제 DB 접근
 * - PRODUCT_OPTION, PRODUCT와 JOIN하여 인기 상품 조회
 */
@Repository
class OrderItemRepositoryImpl(
    private val orderItemJpaRepository: OrderItemJpaRepository
) : OrderItemRepository {

    override fun saveAll(orderItems: List<OrderItemEntity>): List<OrderItemEntity> {
        return orderItemJpaRepository.saveAll(orderItems).toList()
    }

    override fun findTopOrderedProducts(startDate: LocalDateTime, limit: Int): List<ProductOrderCount> {
        val results = orderItemJpaRepository.findTopOrderedProducts(startDate, limit)

        return results.map { row ->
            ProductOrderCount(
                productId = row[0] as Long,
                totalOrderCount = (row[1] as Number).toInt()
            )
        }
    }
}
