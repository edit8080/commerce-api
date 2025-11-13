package com.beanbliss.domain.order.usecase

import com.beanbliss.domain.user.service.BalanceService
import com.beanbliss.domain.cart.domain.CartItemDetail
import com.beanbliss.domain.cart.service.CartService
import com.beanbliss.domain.product.service.ProductOptionService
import com.beanbliss.domain.coupon.dto.CouponValidationResult
import com.beanbliss.domain.coupon.service.CouponService
import com.beanbliss.domain.inventory.service.InventoryReservationService
import com.beanbliss.domain.inventory.service.InventoryService
import com.beanbliss.domain.order.dto.OrderCreationData
import com.beanbliss.domain.order.entity.OrderEntity
import com.beanbliss.domain.order.entity.OrderItemEntity
import com.beanbliss.domain.order.exception.CartEmptyException
import com.beanbliss.domain.order.service.OrderService
import com.beanbliss.domain.user.service.UserService
import org.springframework.stereotype.Component

/**
 * [책임]: 주문 생성 UseCase 구현
 * - 여러 도메인 Service 조율
 * - 복잡한 비즈니스 트랜잭션 오케스트레이션 (12단계)
 * - 도메인 데이터 반환 (Controller에서 Presentation DTO로 변환)
 *
 * [DIP 준수]:
 * - UserService, CartService, ProductOptionService, CouponService, InventoryReservationService, InventoryService, OrderService, BalanceService에만 의존
 *
 * [트랜잭션]:
 * - 각 Service의 메서드에 @Transactional 적용
 * - UseCase는 트랜잭션 조율만 담당
 */
@Component
class CreateOrderUseCase(
    private val userService: UserService,
    private val cartService: CartService,
    private val productOptionService: ProductOptionService,
    private val couponService: CouponService,
    private val inventoryReservationService: InventoryReservationService,
    private val inventoryService: InventoryService,
    private val orderService: OrderService,
    private val balanceService: BalanceService
) {

    /**
     * 주문 생성 결과 (도메인 데이터)
     */
    data class OrderCreationResult(
        val orderEntity: OrderEntity,
        val orderItemEntities: List<OrderItemEntity>,
        val cartItems: List<CartItemDetail>,
        val couponInfo: CouponService.CouponInfo?,
        val userCouponId: Long?,
        val originalAmount: Int,
        val discountAmount: Int,
        val finalAmount: Int,
        val shippingAddress: String
    )

    /**
     * 주문 생성 및 결제 처리 실행
     *
     * [주요 단계]:
     * 1. 사용자 존재 여부 검증
     * 2. 장바구니 조회 및 검증
     * 3. 쿠폰 검증 (유효성, 만료, 사용 여부, 최소 주문 금액)
     * 4. 금액 계산 (원금, 할인, 최종)
     * 5. 재고 예약 확인 (하이브리드 재고 관리)
     * 6. 재고 확인 및 차감 (비관적 락)
     * 7. 주문 생성 및 주문 아이템 생성
     * 8. 잔액 차감 (비관적 락)
     * 9. 재고 예약 상태 변경 (RESERVED → CONFIRMED)
     * 10. 쿠폰 사용 처리 (쿠폰이 제공된 경우만)
     * 11. 장바구니 비우기
     * 12. 도메인 데이터 반환
     *
     * @param userId 사용자 ID
     * @param userCouponId 사용자 쿠폰 ID (nullable - 쿠폰 미사용 가능)
     * @param shippingAddress 배송지 주소
     * @return 생성된 주문 정보
     * @throws UserCouponNotFoundException 쿠폰을 찾을 수 없는 경우
     * @throws UserCouponExpiredException 쿠폰이 만료된 경우
     * @throws UserCouponAlreadyUsedException 쿠폰이 이미 사용된 경우
     * @throws InvalidCouponOrderAmountException 최소 주문 금액 미달인 경우
     * @throws InventoryReservationNotFoundException 재고 예약을 찾을 수 없는 경우
     * @throws InventoryReservationExpiredException 재고 예약이 만료된 경우
     * @throws ProductOptionInactiveException 비활성화된 상품 옵션이 포함된 경우
     * @throws InsufficientBalanceException 사용자 잔액이 부족한 경우
     */
    fun createOrder(
        userId: Long,
        userCouponId: Long?,
        shippingAddress: String
    ): OrderCreationResult {
        // === Step 1-4: 사전 검증 및 금액 계산 (트랜잭션 외부) ===

        // Step 1: 사용자 존재 여부 검증
        userService.validateUserExists(userId)

        // Step 2: 장바구니 조회 (CART 도메인)
        val cartItems = cartService.getCartItems(userId)
        if (cartItems.isEmpty()) {
            throw CartEmptyException("장바구니가 비어 있습니다.")
        }

        // Step 2-1: 상품 옵션 정보 Batch 조회 (PRODUCT 도메인)
        val optionIds = cartItems.map { it.productOptionId }
        val productOptions = productOptionService.getOptionsBatch(optionIds)

        // Step 2-2: CART + PRODUCT 데이터 조합
        val cartItemDetails = cartItems.map { cartItem ->
            val productOption = productOptions[cartItem.productOptionId]
                ?: throw IllegalStateException("상품 옵션을 찾을 수 없습니다: ${cartItem.productOptionId}")

            CartItemDetail(
                cartItemId = cartItem.id,
                productOptionId = cartItem.productOptionId,
                productName = productOption.productName,
                optionCode = productOption.optionCode,
                origin = productOption.origin,
                grindType = productOption.grindType,
                weightGrams = productOption.weightGrams,
                price = productOption.price,
                quantity = cartItem.quantity,
                totalPrice = productOption.price * cartItem.quantity,
                createdAt = cartItem.createdAt,
                updatedAt = cartItem.updatedAt
            )
        }

        // Step 3: 쿠폰 검증 (CouponService에 위임)
        val couponValidation: CouponValidationResult? = if (userCouponId != null) {
            couponService.validateAndGetCoupon(userId, userCouponId)
        } else {
            null
        }

        // Step 4: 금액 계산
        val originalAmount = cartItemDetails.sumOf { it.totalPrice }
        val discountAmount = if (couponValidation != null) {
            couponService.calculateDiscount(couponValidation.coupon, originalAmount)
        } else {
            0
        }

        val finalAmount = originalAmount - discountAmount

        // === Step 5-11: 각 Service의 독립적인 트랜잭션 처리 ===

        // Step 5: 재고 예약 확인 (하이브리드 재고 관리)
        inventoryReservationService.validateReservations(userId, cartItemDetails)

        // Step 6: 재고 확인 및 차감 (비관적 락)
        inventoryService.reduceStockForOrder(cartItemDetails)

        // Step 7: 주문 생성 및 주문 아이템 생성
        val orderCreationData = OrderCreationData(
            userId = userId,
            cartItems = cartItemDetails,
            originalAmount = originalAmount,
            discountAmount = discountAmount,
            finalAmount = finalAmount,
            shippingAddress = shippingAddress,
            userCouponId = userCouponId
        )
        val orderResult = orderService.createOrderWithItems(orderCreationData)

        // Step 8: 잔액 차감 (비관적 락)
        balanceService.deductBalance(userId, finalAmount)

        // Step 9: 재고 예약 상태 변경 (RESERVED → CONFIRMED)
        inventoryReservationService.confirmReservations(userId, cartItemDetails)

        // Step 10: 쿠폰 사용 처리 (쿠폰이 제공된 경우만)
        if (userCouponId != null) {
            couponService.markCouponAsUsed(userCouponId, orderResult.orderEntity.id)
        }

        // Step 11: 장바구니 비우기
        cartService.clearCart(userId)

        // === Step 12: 도메인 데이터 반환 ===
        return OrderCreationResult(
            orderEntity = orderResult.orderEntity,
            orderItemEntities = orderResult.orderItemEntities,
            cartItems = cartItemDetails,
            couponInfo = couponValidation?.coupon,
            userCouponId = userCouponId,
            originalAmount = originalAmount,
            discountAmount = discountAmount,
            finalAmount = finalAmount,
            shippingAddress = shippingAddress
        )
    }
}
