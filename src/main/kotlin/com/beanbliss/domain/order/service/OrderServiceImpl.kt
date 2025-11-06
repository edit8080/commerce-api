package com.beanbliss.domain.order.service

import com.beanbliss.domain.order.dto.OrderCreationData
import com.beanbliss.domain.order.dto.OrderCreationResult
import com.beanbliss.domain.order.dto.OrderItemResponse
import com.beanbliss.domain.order.dto.ProductOrderCount
import com.beanbliss.domain.order.entity.OrderEntity
import com.beanbliss.domain.order.entity.OrderItemEntity
import com.beanbliss.domain.order.entity.OrderStatus
import com.beanbliss.domain.order.repository.OrderItemRepository
import com.beanbliss.domain.order.repository.OrderRepository
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
class OrderServiceImpl(
    private val orderRepository: OrderRepository,
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

    /**
     * 주문 및 주문 아이템 생성
     *
     * [구현 전략]:
     * 1. OrderEntity 생성 (결제 완료 상태)
     * 2. OrderRepository를 통해 주문 저장
     * 3. OrderItemEntity 목록 생성 (장바구니 아이템 기반)
     * 4. OrderItemRepository를 통해 주문 아이템 배치 저장
     * 5. OrderCreationResult로 변환 및 반환
     *
     * [트랜잭션]:
     * - @Transactional로 원자성 보장
     *
     * @param data 주문 생성 데이터
     * @return 생성된 주문 정보
     */
    @Transactional
    override fun createOrderWithItems(data: OrderCreationData): OrderCreationResult {
        val now = LocalDateTime.now()

        // 1. OrderEntity 생성
        val orderEntity = OrderEntity(
            id = 0L, // Auto-generated
            userId = data.userId,
            totalAmount = data.originalAmount,
            discountAmount = data.discountAmount,
            finalAmount = data.finalAmount,
            shippingAddress = data.shippingAddress,
            orderStatus = OrderStatus.PAYMENT_COMPLETED,
            orderedAt = now,
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
                unitPrice = cartItem.price,
                totalPrice = cartItem.totalPrice
            )
        }

        // 4. 주문 아이템 배치 저장
        orderItemRepository.saveAll(orderItems)

        // 5. OrderCreationResult로 변환
        val orderItemResponses = data.cartItems.map { cartItem ->
            OrderItemResponse(
                productOptionId = cartItem.productOptionId,
                productName = cartItem.productName,
                optionCode = cartItem.optionCode,
                quantity = cartItem.quantity,
                unitPrice = cartItem.price,
                totalPrice = cartItem.totalPrice
            )
        }

        return OrderCreationResult(
            orderId = savedOrder.id,
            orderStatus = savedOrder.orderStatus,
            orderItems = orderItemResponses,
            orderedAt = savedOrder.orderedAt
        )
    }
}
