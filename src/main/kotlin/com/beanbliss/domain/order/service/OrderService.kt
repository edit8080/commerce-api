package com.beanbliss.domain.order.service

import com.beanbliss.domain.order.dto.OrderCreationData
import com.beanbliss.domain.order.entity.OrderEntity
import com.beanbliss.domain.order.entity.OrderItemEntity
import com.beanbliss.domain.order.entity.OrderStatus
import com.beanbliss.domain.order.repository.OrderItemRepository
import com.beanbliss.domain.order.repository.OrderRepository
import com.beanbliss.domain.order.repository.ProductOptionOrderCount
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * [책임]: 주문 비즈니스 로직 구현
 *
 * [DIP 준수]:
 * - OrderRepository, OrderItemRepository 인터페이스에만 의존
 */
@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository
) {

    /**
     * 주문 생성 결과 (도메인 데이터)
     */
    data class OrderCreationResult(
        val orderEntity: OrderEntity,
        val orderItemEntities: List<OrderItemEntity>
    )

    /**
     * 지정된 기간 동안 가장 많이 주문된 상품 옵션 조회 (ORDER 도메인만)
     *
     * [설계 변경]:
     * - PRODUCT_OPTION과의 JOIN 제거
     * - ORDER_ITEM 테이블만 조회
     * - 상품 옵션별 주문 수량 반환
     * - UseCase에서 PRODUCT 정보와 조합하여 집계
     *
     * [구현 전략]:
     * 1. period를 기반으로 startDate 계산 (현재 - period일)
     * 2. OrderItemRepository에 쿼리 위임
     *    - Repository에서 product_option_id별 집계 수행
     *    - Repository에서 정렬 및 limit 적용
     * 3. 결과 반환
     *
     * @param period 조회 기간 (일 단위)
     * @param limit 조회할 상품 옵션 개수
     * @return 상품 옵션별 주문 수량 목록 (ORDER 도메인만)
     */
    fun getTopOrderedProductOptions(period: Int, limit: Int): List<ProductOptionOrderCount> {
        // 1. 조회 시작 날짜 계산 (현재 시각 - period일)
        val startDate = LocalDateTime.now().minusDays(period.toLong())

        // 2. Repository를 통해 상품 옵션별 주문 수량 조회
        return orderItemRepository.findTopOrderedProductOptions(startDate, limit)
    }

    /**
     * 주문 및 주문 아이템 생성
     *
     * [구현 전략]:
     * 1. OrderEntity 생성 (결제 완료 상태)
     * 2. OrderRepository를 통해 주문 저장
     * 3. OrderItemEntity 목록 생성 (장바구니 아이템 기반)
     * 4. OrderItemRepository를 통해 주문 아이템 배치 저장
     * 5. 도메인 엔티티 반환
     *
     * [트랜잭션]:
     * - @Transactional로 원자성 보장
     *
     * @param data 주문 생성 데이터
     * @return 생성된 주문 엔티티 + 주문 아이템 엔티티 목록
     */
    @Transactional
    fun createOrderWithItems(data: OrderCreationData): OrderCreationResult {
        val now = LocalDateTime.now()

        // 1. OrderEntity 생성
        val orderEntity = OrderEntity(
            id = 0L, // Auto-generated
            userId = data.userId,
            userCouponId = data.userCouponId,
            status = OrderStatus.PAYMENT_COMPLETED,
            originalAmount = data.originalAmount.toBigDecimal(),
            discountAmount = data.discountAmount.toBigDecimal(),
            finalAmount = data.finalAmount.toBigDecimal(),
            shippingAddress = data.shippingAddress,
            trackingNumber = null,
            createdAt = now,
            updatedAt = now
        )

        // 2. 주문 저장
        val savedOrder = orderRepository.save(orderEntity)

        // 3. OrderItemEntity 목록 생성
        val orderItems = data.cartItems.map { cartItem ->
            OrderItemEntity(
                id = 0L, // Auto-generated
                orderId = savedOrder.id,
                productOptionId = cartItem.productOptionId,
                quantity = cartItem.quantity,
                unitPrice = cartItem.price.toBigDecimal(),
                totalPrice = cartItem.totalPrice.toBigDecimal(),
                createdAt = now
            )
        }

        // 4. 주문 아이템 배치 저장
        val savedOrderItems = orderItemRepository.saveAll(orderItems)

        // 5. 도메인 엔티티 반환
        return OrderCreationResult(
            orderEntity = savedOrder,
            orderItemEntities = savedOrderItems
        )
    }
}
