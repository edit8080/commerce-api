package com.beanbliss.domain.order.usecase

import com.beanbliss.domain.cart.dto.CartItemResponse
import com.beanbliss.domain.coupon.service.CouponService
import com.beanbliss.domain.order.entity.OrderEntity
import com.beanbliss.domain.order.entity.OrderItemEntity

/**
 * [책임]: 주문 생성 UseCase의 계약 정의
 * Controller는 이 인터페이스에만 의존합니다 (DIP 준수)
 *
 * [UseCase 패턴]:
 * - 8개 도메인 Repository 조율 (CartItem, ProductOption, InventoryReservation, Inventory, UserCoupon, Coupon, Balance, Order, OrderItem)
 * - 복잡한 비즈니스 트랜잭션 처리 (12단계)
 * - 하이브리드 재고 관리 (예약 확정 + 실재고 차감)
 */
interface CreateOrderUseCase {
    /**
     * 주문 생성 결과 (도메인 데이터)
     */
    data class OrderCreationResult(
        val orderEntity: OrderEntity,
        val orderItemEntities: List<OrderItemEntity>,
        val cartItems: List<CartItemResponse>,
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
     * 1. 사용자 쿠폰 검증 (유효성, 만료, 사용 여부, 최소 주문 금액)
     * 2. 재고 예약 조회 및 검증 (존재 여부, 만료)
     * 3. 상품 옵션 활성 여부 검증
     * 4. 총 주문 금액 계산
     * 5. 쿠폰 할인 적용
     * 6. 사용자 잔액 검증
     * 7. [트랜잭션 시작]
     * 8. 재고 예약 확정 (RESERVED -> CONFIRMED)
     * 9. 실재고 차감 (INVENTORY 테이블 업데이트 with 비관적 락)
     * 10. 쿠폰 사용 처리 (ISSUED -> USED)
     * 11. 사용자 잔액 차감 (BALANCE 테이블 업데이트 with 비관적 락)
     * 12. 주문 및 주문 항목 저장
     * 13. 장바구니 비우기
     * 14. [트랜잭션 커밋]
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
    ): OrderCreationResult
}
