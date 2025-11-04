package com.beanbliss.domain.order.usecase

import com.beanbliss.domain.cart.repository.CartItemRepository
import com.beanbliss.domain.coupon.entity.CouponEntity
import com.beanbliss.domain.coupon.entity.UserCouponEntity
import com.beanbliss.domain.coupon.enums.UserCouponStatus
import com.beanbliss.domain.coupon.repository.CouponRepository
import com.beanbliss.domain.coupon.repository.UserCouponRepository
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import com.beanbliss.domain.inventory.repository.InventoryRepository
import com.beanbliss.domain.inventory.repository.InventoryReservationRepository
import com.beanbliss.domain.order.dto.AppliedCouponInfo
import com.beanbliss.domain.order.dto.CreateOrderResponse
import com.beanbliss.domain.order.dto.OrderItemResponse
import com.beanbliss.domain.order.dto.PriceInfo
import com.beanbliss.domain.order.entity.OrderEntity
import com.beanbliss.domain.order.entity.OrderItemEntity
import com.beanbliss.domain.order.entity.OrderStatus
import com.beanbliss.domain.order.exception.*
import com.beanbliss.domain.order.repository.OrderItemRepository
import com.beanbliss.domain.order.repository.OrderRepository
import com.beanbliss.domain.product.repository.ProductOptionRepository
import com.beanbliss.domain.user.repository.BalanceRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.min

/**
 * [책임]: 주문 생성 UseCase 구현
 * - 8개 도메인 Repository 조율
 * - 복잡한 비즈니스 트랜잭션 처리
 */
@Component
class CreateOrderUseCaseImpl(
    private val cartItemRepository: CartItemRepository,
    private val productOptionRepository: ProductOptionRepository,
    private val inventoryReservationRepository: InventoryReservationRepository,
    private val inventoryRepository: InventoryRepository,
    private val userCouponRepository: UserCouponRepository,
    private val couponRepository: CouponRepository,
    private val balanceRepository: BalanceRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository
) : CreateOrderUseCase {

    override fun createOrder(
        userId: Long,
        userCouponId: Long?,
        shippingAddress: String
    ): CreateOrderResponse {
        // === Step 1-4: 트랜잭션 외부 검증 ===

        // Step 1: 장바구니 조회 및 검증
        val cartItems = cartItemRepository.findByUserId(userId)
        if (cartItems.isEmpty()) {
            throw CartEmptyException("장바구니가 비어 있습니다.")
        }

        // 상품 옵션 활성 여부 검증
        cartItems.forEach { cartItem ->
            val productOption = productOptionRepository.findActiveOptionWithProduct(cartItem.productOptionId)
            if (productOption == null || !productOption.isActive) {
                throw ProductOptionInactiveException("비활성화된 상품 옵션이 포함되어 있습니다.")
            }
        }

        // Step 2: 쿠폰 검증 및 조회 (userCouponId가 있는 경우만)
        var userCoupon: UserCouponEntity? = null
        var coupon: CouponEntity? = null

        if (userCouponId != null) {
            userCoupon = userCouponRepository.findById(userCouponId)
                ?: throw UserCouponNotFoundException("사용자 쿠폰을 찾을 수 없습니다.")

            // 소유권 확인
            if (userCoupon.userId != userId) {
                throw UserCouponNotFoundException("해당 쿠폰에 대한 권한이 없습니다.")
            }

            // 상태 확인
            if (userCoupon.status != UserCouponStatus.ISSUED) {
                throw UserCouponAlreadyUsedException("이미 사용된 쿠폰입니다.")
            }

            // 쿠폰 정보 조회
            coupon = couponRepository.findById(userCoupon.couponId)
                ?: throw UserCouponNotFoundException("쿠폰 정보를 찾을 수 없습니다.")

            // 유효 기간 확인
            val now = LocalDateTime.now()
            if (now < coupon.validFrom || now > coupon.validUntil) {
                throw UserCouponExpiredException("쿠폰이 만료되었습니다.")
            }
        }

        // Step 3: 금액 계산
        val totalAmount = cartItems.sumOf { it.totalPrice }

        var discountAmount = 0
        if (coupon != null) {
            // 최소 주문 금액 검증
            if (totalAmount < coupon.minOrderAmount) {
                throw InvalidCouponOrderAmountException(
                    "최소 주문 금액 ${coupon.minOrderAmount}원을 충족하지 못했습니다."
                )
            }

            // 할인 금액 계산
            discountAmount = when (coupon.discountType) {
                "PERCENTAGE" -> {
                    val calculated = (totalAmount * coupon.discountValue) / 100
                    min(calculated, coupon.maxDiscountAmount ?: Int.MAX_VALUE)
                }
                "FIXED_AMOUNT" -> coupon.discountValue
                else -> 0
            }
        }

        val finalAmount = totalAmount - discountAmount

        // Step 4: 잔액 사전 검증 (트랜잭션 밖에서 미리 확인)
        val balance = balanceRepository.findByUserId(userId)
            ?: throw InsufficientBalanceException("잔액 정보를 찾을 수 없습니다.")

        if (balance.amount < finalAmount) {
            throw InsufficientBalanceException(
                "사용자 잔액이 부족합니다. (필요: $finalAmount, 보유: ${balance.amount})"
            )
        }

        // === Step 5-12: 트랜잭션 내부 처리 ===
        return executeTransaction(
            userId = userId,
            cartItems = cartItems,
            userCoupon = userCoupon,
            coupon = coupon,
            totalAmount = totalAmount,
            discountAmount = discountAmount,
            finalAmount = finalAmount,
            shippingAddress = shippingAddress
        )
    }

    @Transactional
    private fun executeTransaction(
        userId: Long,
        cartItems: List<com.beanbliss.domain.cart.dto.CartItemResponse>,
        userCoupon: UserCouponEntity?,
        coupon: CouponEntity?,
        totalAmount: Int,
        discountAmount: Int,
        finalAmount: Int,
        shippingAddress: String
    ): CreateOrderResponse {
        val now = LocalDateTime.now()

        // Step 5: 재고 예약 확인 및 검증
        val reservations = inventoryReservationRepository.findActiveReservationsByUserId(userId)

        if (reservations.isEmpty()) {
            throw InventoryReservationNotFoundException("재고 예약을 찾을 수 없습니다.")
        }

        // 재고 예약 만료 확인
        reservations.forEach { reservation ->
            if (reservation.expiresAt <= now) {
                throw InventoryReservationExpiredException("재고 예약이 만료되었습니다.")
            }
        }

        // Step 6: 재고 차감 (간단한 버전 - 비관적 락 생략)
        // TODO: 실제로는 FOR UPDATE를 사용한 비관적 락 필요
        cartItems.forEach { cartItem ->
            val inventory = inventoryRepository.calculateAvailableStock(cartItem.productOptionId)
            if (inventory < cartItem.quantity) {
                throw com.beanbliss.domain.inventory.exception.InsufficientStockException(
                    "재고가 부족합니다. 상품 옵션 ID: ${cartItem.productOptionId}"
                )
            }
            // TODO: 실제 재고 차감 로직 (InventoryRepository.decreaseStock() 메서드 필요)
        }

        // Step 7: 주문 생성
        val order = OrderEntity(
            id = 0L,
            userId = userId,
            totalAmount = totalAmount,
            discountAmount = discountAmount,
            finalAmount = finalAmount,
            shippingAddress = shippingAddress,
            orderStatus = OrderStatus.PAYMENT_COMPLETED,
            orderedAt = now,
            updatedAt = now
        )
        val savedOrder = orderRepository.save(order)

        // Step 8: 주문 항목 생성
        val orderItemEntities = cartItems.map { cartItem ->
            OrderItemEntity(
                id = 0L,
                orderId = savedOrder.id,
                productOptionId = cartItem.productOptionId,
                quantity = cartItem.quantity,
                unitPrice = cartItem.price,
                totalPrice = cartItem.totalPrice
            )
        }
        val savedOrderItems = orderItemRepository.saveAll(orderItemEntities)

        // Step 9: 잔액 차감 (간단한 버전 - 비관적 락 생략)
        // TODO: 실제로는 FOR UPDATE를 사용한 비관적 락 필요
        val balance = balanceRepository.findByUserId(userId)
            ?: throw InsufficientBalanceException("잔액 정보를 찾을 수 없습니다.")

        if (balance.amount < finalAmount) {
            throw InsufficientBalanceException("사용자 잔액이 부족합니다.")
        }

        // TODO: BalanceRepository.decreaseBalance() 메서드 필요

        // Step 10: 재고 예약 확정 (RESERVED -> CONFIRMED)
        // TODO: InventoryReservationRepository.bulkUpdateStatus() 메서드 필요
        // 현재는 간단하게 처리
        reservations.forEach { reservation ->
            val updated = reservation.copy(
                status = InventoryReservationStatus.CONFIRMED,
                updatedAt = now
            )
            inventoryReservationRepository.save(updated)
        }

        // Step 11: 쿠폰 사용 처리 (쿠폰이 있는 경우만)
        if (userCoupon != null) {
            // TODO: UserCouponRepository.updateStatus() 메서드 필요
            // 현재는 간단하게 처리
            val updatedCoupon = userCoupon.copy(
                status = UserCouponStatus.USED,
                usedOrderId = savedOrder.id,
                usedAt = now,
                updatedAt = now
            )
            userCouponRepository.save(updatedCoupon.userId, updatedCoupon.couponId)
        }

        // Step 12: 장바구니 비우기
        cartItemRepository.deleteByUserId(userId)

        // Response 생성
        return CreateOrderResponse(
            orderId = savedOrder.id,
            orderStatus = savedOrder.orderStatus,
            orderItems = savedOrderItems.map { orderItem ->
                val cartItem = cartItems.find { it.productOptionId == orderItem.productOptionId }!!
                OrderItemResponse(
                    productOptionId = orderItem.productOptionId,
                    productName = cartItem.productName,
                    optionCode = cartItem.optionCode,
                    quantity = orderItem.quantity,
                    unitPrice = orderItem.unitPrice,
                    totalPrice = orderItem.totalPrice
                )
            },
            appliedCoupon = if (coupon != null && userCoupon != null) {
                AppliedCouponInfo(
                    couponId = coupon.id,
                    couponCode = coupon.name, // TODO: CouponEntity에 code 필드 필요
                    couponName = coupon.name,
                    discountAmount = discountAmount,
                    expiresAt = coupon.validUntil
                )
            } else null,
            priceInfo = PriceInfo(
                totalProductAmount = totalAmount,
                discountAmount = discountAmount,
                finalAmount = finalAmount
            ),
            shippingAddress = shippingAddress,
            orderedAt = savedOrder.orderedAt
        )
    }
}
