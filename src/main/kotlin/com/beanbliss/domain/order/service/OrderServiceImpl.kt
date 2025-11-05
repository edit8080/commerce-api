package com.beanbliss.domain.order.service

import com.beanbliss.domain.order.dto.ProductOrderCount
import com.beanbliss.domain.order.repository.OrderItemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * [책임]: 주문 비즈니스 로직 구현
 *
 * [DIP 준수]:
 * - OrderItemRepository 인터페이스에만 의존
 */
@Service
@Transactional(readOnly = true)
class OrderServiceImpl(
    private val orderItemRepository: OrderItemRepository
) : OrderService {

    /**
     * 지정된 기간 동안 가장 많이 주문된 상품 조회
     *
     * [구현 전략]:
     * 1. period를 기반으로 startDate 계산 (현재 - period일)
     * 2. OrderItemRepository에 쿼리 위임
     *    - Repository에서 활성 상품 필터링 및 집계 수행
     *    - Repository에서 정렬 및 limit 적용
     * 3. 결과 반환
     *
     * @param period 조회 기간 (일 단위)
     * @param limit 조회할 상품 개수
     * @return 상품별 주문 수량 목록
     */
    override fun getTopOrderedProducts(period: Int, limit: Int): List<ProductOrderCount> {
        // 1. 조회 시작 날짜 계산 (현재 시각 - period일)
        val startDate = LocalDateTime.now().minusDays(period.toLong())

        // 2. Repository를 통해 주문 수량 조회
        return orderItemRepository.findTopOrderedProducts(startDate, limit)
    }
}
