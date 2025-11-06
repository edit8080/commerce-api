package com.beanbliss.domain.order.dto

import com.beanbliss.domain.cart.dto.CartItemResponse

/**
 * 주문 생성 데이터
 *
 * @property userId 사용자 ID
 * @property cartItems 장바구니 아이템 목록
 * @property originalAmount 원가 (할인 전 금액)
 * @property discountAmount 할인 금액
 * @property finalAmount 최종 결제 금액
 * @property shippingAddress 배송지 주소
 * @property userCouponId 사용한 쿠폰 ID (nullable)
 */
data class OrderCreationData(
    val userId: Long,
    val cartItems: List<CartItemResponse>,
    val originalAmount: Int,
    val discountAmount: Int,
    val finalAmount: Int,
    val shippingAddress: String,
    val userCouponId: Long?
)
