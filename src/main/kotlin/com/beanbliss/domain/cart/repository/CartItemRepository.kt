package com.beanbliss.domain.cart.repository

/**
 * [책임]: 장바구니 아이템 영속성 계층의 '계약' 정의.
 * Service는 이 인터페이스에만 의존합니다. (DIP 준수)
 *
 * [주요 기능]:
 * - 사용자별 장바구니 아이템 조회
 * - 신규 장바구니 아이템 저장
 * - 기존 장바구니 아이템 수량 업데이트
 */
interface CartItemRepository {
    /**
     * 사용자 ID로 장바구니 아이템 목록 조회
     *
     * @param userId 사용자 ID
     * @return 장바구니 아이템 목록 (비어있을 수 있음)
     */
    fun findByUserId(userId: Long): List<CartItemDetail>

    /**
     * 사용자 ID와 상품 옵션 ID로 장바구니 아이템 조회
     *
     * @param userId 사용자 ID
     * @param productOptionId 상품 옵션 ID
     * @return 장바구니 아이템 (없으면 null)
     */
    fun findByUserIdAndProductOptionId(userId: Long, productOptionId: Long): CartItemDetail?

    /**
     * 신규 장바구니 아이템 저장
     *
     * @param cartItem 저장할 장바구니 아이템 정보
     * @param userId 사용자 ID (신규 저장 시 필요)
     * @return 저장된 장바구니 아이템 (ID 포함)
     */
    fun save(cartItem: CartItemDetail, userId: Long): CartItemDetail

    /**
     * 기존 장바구니 아이템 수량 업데이트
     *
     * @param cartItemId 장바구니 아이템 ID
     * @param newQuantity 새로운 수량
     * @return 업데이트된 장바구니 아이템
     */
    fun updateQuantity(cartItemId: Long, newQuantity: Int): CartItemDetail

    /**
     * 사용자의 모든 장바구니 아이템 삭제
     *
     * @param userId 사용자 ID
     */
    fun deleteByUserId(userId: Long)
}
