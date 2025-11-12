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
 *
 * [설계 변경]:
 * - PRODUCT_OPTION과의 JOIN 제거
 * - ORDER_ITEM 테이블만 조회
 */
interface OrderItemJpaRepository : JpaRepository<OrderItemEntity, Long> {
    /**
     * 지정된 기간 동안 상품 옵션별 주문 수량 집계 (ORDER_ITEM만)
     *
     * [도메인 분리 원칙]:
     * - PRODUCT_OPTION과의 JOIN 제거
     * - ORDER 도메인만 조회
     * - 활성/비활성 필터링은 UseCase에서 처리
     *
     * @return List<Array<Any>> = [[productOptionId: Long, totalCount: Long], ...]
     */
    @Query("""
        SELECT oi.productOptionId, SUM(oi.quantity)
        FROM OrderItemEntity oi
        WHERE oi.createdAt >= :startDate
        GROUP BY oi.productOptionId
        ORDER BY SUM(oi.quantity) DESC
    """)
    fun findTopOrderedProductOptions(
        @Param("startDate") startDate: LocalDateTime
    ): List<Array<Any>>
}

/**
 * [책임]: OrderItemRepository 인터페이스 구현체
 * - OrderItemJpaRepository를 활용하여 실제 DB 접근
 * - ORDER_ITEM 테이블만 조회 (도메인 간 JOIN 제거)
 */
@Repository
class OrderItemRepositoryImpl(
    private val orderItemJpaRepository: OrderItemJpaRepository
) : OrderItemRepository {

    override fun saveAll(orderItems: List<OrderItemEntity>): List<OrderItemEntity> {
        return orderItemJpaRepository.saveAll(orderItems).toList()
    }

    override fun findTopOrderedProductOptions(startDate: LocalDateTime, limit: Int): List<ProductOptionOrderCount> {
        val results = orderItemJpaRepository.findTopOrderedProductOptions(startDate)

        // limit 적용하여 상위 N개만 반환
        return results.take(limit).map { row ->
            ProductOptionOrderCount(
                productOptionId = row[0] as Long,
                totalOrderCount = (row[1] as Number).toInt()
            )
        }
    }
}
