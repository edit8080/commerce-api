package com.beanbliss.domain.cart.domain

import java.time.LocalDateTime

/**
 * [책임]: 장바구니 아이템 도메인 모델 (CART 도메인만)
 *
 * [설계 원칙]:
 * - CART 도메인에만 속하는 데이터만 포함
 * - PRODUCT 도메인 정보는 포함하지 않음 (productOptionId만 참조)
 * - 순수한 도메인 모델로 비즈니스 로직만 담당
 *
 * [사용처]:
 * - Repository: CART_ITEM 테이블과 매핑
 * - Service: CART 도메인 비즈니스 로직
 */
data class CartItem(
    val id: Long = 0L,
    val userId: Long,
    val productOptionId: Long,  // PRODUCT 도메인 참조 (ID만)
    val quantity: Int,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * 수량 업데이트
     *
     * @param newQuantity 새로운 수량
     * @return 수량이 업데이트된 새로운 CartItem
     */
    fun updateQuantity(newQuantity: Int): CartItem {
        require(newQuantity > 0) { "수량은 1개 이상이어야 합니다." }
        return this.copy(quantity = newQuantity, updatedAt = LocalDateTime.now())
    }

    /**
     * 수량 증가
     *
     * @param amount 증가할 수량
     * @return 수량이 증가된 새로운 CartItem
     */
    fun increaseQuantity(amount: Int): CartItem {
        require(amount > 0) { "증가 수량은 1개 이상이어야 합니다." }
        return this.copy(quantity = this.quantity + amount, updatedAt = LocalDateTime.now())
    }
}
