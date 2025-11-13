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
     * 지정된 기간 동안 가장 많이 주문된 상품 옵션 조회 (ORDER 도메인만)
     *
     * [설계 변경]:
     * - PRODUCT_OPTION과의 JOIN 제거
     * - ORDER_ITEM 테이블만 조회
     * - 상품 옵션별 주문 수량 반환
     * - UseCase에서 PRODUCT 정보와 조합
     *
     * [쿼리 로직]:
     * 1. ORDER_ITEM.created_at >= startDate 필터링
     * 2. product_option_id별로 quantity 합계 집계
     * 3. SUM(quantity) DESC 정렬
     * 4. limit 개수만큼 반환
     *
     * @param startDate 조회 시작 날짜
     * @param limit 조회할 상품 옵션 개수
     * @return 상품 옵션별 주문 수량 목록 (ORDER 도메인만, 정렬됨)
     */
    fun findTopOrderedProductOptions(startDate: LocalDateTime, limit: Int): List<ProductOptionOrderCount>
}

/**
 * 상품 옵션별 주문 수량 (ORDER 도메인만)
 */
data class ProductOptionOrderCount(
    val productOptionId: Long,
    val totalOrderCount: Int
)
