package com.beanbliss.domain.order.usecase

import com.beanbliss.domain.cart.service.CartService
import com.beanbliss.domain.coupon.dto.CouponValidationResult
import com.beanbliss.domain.coupon.service.CouponService
import com.beanbliss.domain.inventory.service.InventoryReservationService
import com.beanbliss.domain.inventory.service.InventoryService
import com.beanbliss.domain.order.dto.AppliedCouponInfo
import com.beanbliss.domain.order.dto.CreateOrderResponse
import com.beanbliss.domain.order.dto.OrderCreationData
import com.beanbliss.domain.order.dto.PriceInfo
import com.beanbliss.domain.order.service.OrderService
import com.beanbliss.domain.user.service.BalanceService
import com.beanbliss.domain.user.service.UserService
import org.springframework.stereotype.Component

/**
 * [책임]: 주문 생성 UseCase 구현
 * - 7개 도메인 Service 조율 (UserService, CartService, CouponService, InventoryReservationService, InventoryService, OrderService, BalanceService)
 * - 복잡한 비즈니스 트랜잭션 오케스트레이션
 * - 각 Service의 독립적인 트랜잭션 관리
 */
@Component
class CreateOrderUseCaseImpl(
    private val userService: UserService,
    private val cartService: CartService,
    private val couponService: CouponService,
    private val inventoryReservationService: InventoryReservationService,
    private val inventoryService: InventoryService,
    private val orderService: OrderService,
    private val balanceService: BalanceService
) : CreateOrderUseCase {

    override fun createOrder(
        userId: Long,
        userCouponId: Long?,
        shippingAddress: String
    ): CreateOrderResponse {
        // === Step 1-4: 트랜잭션 외부 검증 (각 Service 호출) ===

        // Step 1: 사용자 존재 확인
        userService.validateUserExists(userId)

        // Step 2: 장바구니 조회 및 검증
        val cartItems = cartService.getCartItemsWithProducts(userId)
        cartService.validateCartItems(cartItems)

        // Step 3: 쿠폰 유효성 검증 (쿠폰이 제공된 경우만)
        val couponValidation: CouponValidationResult? = if (userCouponId != null) {
            couponService.validateAndGetCoupon(userId, userCouponId)
        } else {
            null
        }

        // Step 4: 상품 금액 계산
        val originalAmount = cartItems.sumOf { it.totalPrice }

        // 쿠폰 할인 금액 계산
        val discountAmount = if (couponValidation != null) {
            couponService.calculateDiscount(couponValidation.coupon, originalAmount)
        } else {
            0
        }

        val finalAmount = originalAmount - discountAmount

        // === Step 5-11: 각 Service의 독립적인 트랜잭션 처리 ===

        // Step 5: 재고 예약 확인 (하이브리드 재고 관리)
        inventoryReservationService.validateReservations(userId, cartItems)

        // Step 6: 재고 확인 및 차감 (비관적 락)
        inventoryService.reduceStockForOrder(cartItems)

        // Step 7: 주문 생성 및 주문 아이템 생성
        val orderCreationData = OrderCreationData(
            userId = userId,
            cartItems = cartItems,
            originalAmount = originalAmount,
            discountAmount = discountAmount,
            finalAmount = finalAmount,
            shippingAddress = shippingAddress,
            userCouponId = userCouponId
        )
        val order = orderService.createOrderWithItems(orderCreationData)

        // Step 8: 잔액 차감 (비관적 락)
        balanceService.deductBalance(userId, finalAmount)

        // Step 9: 재고 예약 상태 변경 (RESERVED → CONFIRMED)
        inventoryReservationService.confirmReservations(userId, cartItems)

        // Step 10: 쿠폰 사용 처리 (쿠폰이 제공된 경우만)
        if (userCouponId != null) {
            couponService.markCouponAsUsed(userCouponId, order.orderId)
        }

        // Step 11: 장바구니 비우기
        cartService.clearCart(userId)

        // === Step 12: DTO 변환 및 응답 ===
        return CreateOrderResponse(
            orderId = order.orderId,
            orderStatus = order.orderStatus,
            orderItems = order.orderItems,
            appliedCoupon = if (couponValidation != null) {
                AppliedCouponInfo(
                    couponId = couponValidation.coupon.id!!,
                    couponCode = couponValidation.coupon.name,
                    couponName = couponValidation.coupon.name,
                    discountAmount = discountAmount,
                    expiresAt = couponValidation.coupon.validUntil
                )
            } else null,
            priceInfo = PriceInfo(
                totalProductAmount = originalAmount,
                discountAmount = discountAmount,
                finalAmount = finalAmount
            ),
            shippingAddress = shippingAddress,
            orderedAt = order.orderedAt
        )
    }
}
