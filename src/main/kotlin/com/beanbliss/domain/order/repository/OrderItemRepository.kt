package com.beanbliss.domain.order.repository

import com.beanbliss.domain.order.entity.OrderItemEntity
import java.time.LocalDateTime

/**
 * [책임]: 주문 항목 영속성 계층의 계약 정의
 * Service는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface OrderItemRepository {
    /**
     * 주문 항목 목록 일괄 저장
     *
     * @param orderItems 저장할 주문 항목 목록
     * @return 저장된 OrderItemEntity 목록 (ID 포함)
     */
    fun saveAll(orderItems: List<OrderItemEntity>): List<OrderItemEntity>

    /**
     * 지정된 기간 동안 가장 많이 주문된 상품 조회
     *
     * [쿼리 로직]:
     * 1. ORDER_ITEM.created_at >= startDate 필터링 (성능 최적화: JOIN 전 필터링)
     * 2. PRODUCT_OPTION과 JOIN (product_option_id)
     * 3. PRODUCT_OPTION.is_active = true 필터링 (활성 상품만)
     * 4. product_id별로 quantity 합계 집계
     * 5. SUM(quantity) DESC, product_id ASC 정렬
     * 6. limit 개수만큼 반환
     *
     * @param startDate 조회 시작 날짜
     * @param limit 조회할 상품 개수
     * @return 상품별 주문 수량 목록 (활성 상품만, 정렬됨)
     */
    fun findTopOrderedProducts(startDate: LocalDateTime, limit: Int): List<ProductOrderCount>
}
