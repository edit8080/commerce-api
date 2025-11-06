package com.beanbliss.domain.order.controller

import com.beanbliss.domain.order.dto.*
import com.beanbliss.domain.order.usecase.CreateOrderUseCase
import com.beanbliss.domain.order.usecase.ReserveOrderUseCase
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * [책임]: 주문 관련 API 엔드포인트
 * - 주문 예약 (재고 예약)
 * - 주문 생성 (결제 처리)
 */
@RestController
@RequestMapping("/api/order")
class OrderController(
    private val reserveOrderUseCase: ReserveOrderUseCase,
    private val createOrderUseCase: CreateOrderUseCase
) {

    /**
     * 주문 예약 API (재고 예약)
     *
     * @param request 예약 요청 (사용자 ID)
     * @return 예약 결과 (data envelope 형태)
     */
    @PostMapping("/reserve")
    fun reserveOrder(
        @Valid @RequestBody request: ReserveOrderRequest
    ): ResponseEntity<Map<String, ReserveOrderResponse>> {
        // UseCase에 위임 (도메인 데이터 반환)
        val reservationItems = reserveOrderUseCase.reserveOrder(request.userId)

        // Entity → Response DTO 변환
        val reservationResponses = reservationItems.map { item ->
            InventoryReservationItemResponse(
                reservationId = item.reservationEntity.id,
                productOptionId = item.reservationEntity.productOptionId,
                productName = item.productName,
                optionCode = item.optionCode,
                quantity = item.reservationEntity.quantity,
                status = item.reservationEntity.status,
                availableStock = item.availableStockAfterReservation,
                reservedAt = item.reservationEntity.reservedAt,
                expiresAt = item.reservationEntity.expiresAt
            )
        }

        val response = ReserveOrderResponse(reservations = reservationResponses)

        // data envelope 형태로 응답 반환
        return ResponseEntity.ok(mapOf("data" to response))
    }

    /**
     * 주문 생성 및 결제 API
     *
     * @param request 주문 생성 요청 (사용자 ID, 쿠폰 ID, 배송지 주소)
     * @return 주문 생성 결과 (data envelope 형태)
     */
    @PostMapping("/create")
    fun createOrder(
        @Valid @RequestBody request: CreateOrderRequest
    ): ResponseEntity<Map<String, CreateOrderResponse>> {
        // UseCase에 위임 (도메인 데이터 반환)
        val result = createOrderUseCase.createOrder(
            userId = request.userId,
            userCouponId = request.userCouponId,
            shippingAddress = request.shippingAddress
        )

        // Entity → Response DTO 변환

        // 1. OrderItemResponse 목록 생성 (OrderItemEntity + CartItemResponse 조합)
        val orderItemResponses = result.orderItemEntities.map { orderItem ->
            val cartItem = result.cartItems.find { it.productOptionId == orderItem.productOptionId }
                ?: throw IllegalStateException("CartItem을 찾을 수 없습니다: ${orderItem.productOptionId}")

            OrderItemResponse(
                productOptionId = orderItem.productOptionId,
                productName = cartItem.productName,
                optionCode = cartItem.optionCode,
                quantity = orderItem.quantity,
                unitPrice = orderItem.unitPrice,
                totalPrice = orderItem.totalPrice
            )
        }

        // 2. AppliedCouponInfo 생성 (nullable) - Service DTO 사용
        val appliedCouponInfo = if (result.userCouponId != null && result.couponInfo != null) {
            AppliedCouponInfo(
                userCouponId = result.userCouponId,
                couponName = result.couponInfo.name,
                discountType = result.couponInfo.discountType,
                discountValue = result.couponInfo.discountValue
            )
        } else {
            null
        }

        // 3. PriceInfo 생성
        val priceInfo = PriceInfo(
            totalProductAmount = result.originalAmount,
            discountAmount = result.discountAmount,
            finalAmount = result.finalAmount
        )

        // 4. CreateOrderResponse 조립
        val response = CreateOrderResponse(
            orderId = result.orderEntity.id,
            orderStatus = result.orderEntity.orderStatus,
            orderItems = orderItemResponses,
            appliedCoupon = appliedCouponInfo,
            priceInfo = priceInfo,
            shippingAddress = result.shippingAddress,
            orderedAt = result.orderEntity.orderedAt
        )

        // data envelope 형태로 응답 반환
        return ResponseEntity.ok(mapOf("data" to response))
    }
}
