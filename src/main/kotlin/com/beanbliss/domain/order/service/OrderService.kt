package com.beanbliss.domain.order.service

import com.beanbliss.domain.order.dto.ProductOrderCount

/**
 * [책임]: 주문 비즈니스 로직의 계약 정의
 * UseCase는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface OrderService {
    /**
     * 지정된 기간 동안 가장 많이 주문된 상품 조회
     *
     * [비즈니스 로직]:
     * 1. ORDER_ITEM.created_at >= (현재 - period일) 필터링 (성능 최적화: JOIN 전 필터링)
     * 2. PRODUCT_OPTION과 JOIN하여 is_active = true인 옵션만 포함
     * 3. product_id별로 quantity 합계 집계
     * 4. 주문 수(totalOrderCount) 내림차순, productId 오름차순 정렬
     * 5. limit 개수만큼 반환
     *
     * @param period 조회 기간 (일 단위, 1~90일)
     * @param limit 조회할 상품 개수 (1~50개)
     * @return 상품별 주문 수량 목록 (활성 상품만, 정렬됨)
     */
    fun getTopOrderedProducts(period: Int, limit: Int): List<ProductOrderCount>
}
