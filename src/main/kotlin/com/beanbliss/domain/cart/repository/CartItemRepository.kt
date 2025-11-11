package com.beanbliss.domain.cart.repository

import com.beanbliss.domain.cart.domain.CartItem

/**
 * [책임]: 장바구니 아이템 영속성 계층의 '계약' 정의.
 * Service는 이 인터페이스에만 의존합니다. (DIP 준수)
 *
 * [설계 변경]:
 * - 도메인 간 JOIN 제거: CART_ITEM 테이블만 조회
 * - CartItem 도메인 모델 반환 (CART 도메인만 포함)
 * - PRODUCT 정보는 UseCase 계층에서 조합
 *
 * [주요 기능]:
 * - 사용자별 장바구니 아이템 조회 (CART 도메인만)
 * - 신규 장바구니 아이템 저장
 * - 기존 장바구니 아이템 수량 업데이트
 */
interface CartItemRepository {
    /**
     * 사용자 ID로 장바구니 아이템 목록 조회 (CART 도메인만)
     *
     * [변경사항]:
     * - PRODUCT_OPTION, PRODUCT와의 JOIN 제거
     * - CART_ITEM 테이블만 조회
     *
     * @param userId 사용자 ID
     * @return 장바구니 아이템 목록 (비어있을 수 있음)
     */
    fun findByUserId(userId: Long): List<CartItem>

    /**
     * 사용자 ID와 상품 옵션 ID로 장바구니 아이템 조회 (CART 도메인만)
     *
     * [변경사항]:
     * - PRODUCT 정보 제거
     * - CART_ITEM 테이블만 조회
     *
     * @param userId 사용자 ID
     * @param productOptionId 상품 옵션 ID
     * @return 장바구니 아이템 (없으면 null)
     */
    fun findByUserIdAndProductOptionId(userId: Long, productOptionId: Long): CartItem?

    /**
     * 장바구니 아이템 ID로 조회 (CART 도메인만)
     *
     * @param cartItemId 장바구니 아이템 ID
     * @return 장바구니 아이템 (없으면 null)
     */
    fun findById(cartItemId: Long): CartItem?

    /**
     * 신규 장바구니 아이템 저장
     *
     * [변경사항]:
     * - CartItem 도메인 모델 사용
     * - PRODUCT 정보 제거
     *
     * @param cartItem 저장할 장바구니 아이템
     * @return 저장된 장바구니 아이템 (ID 포함)
     */
    fun save(cartItem: CartItem): CartItem

    /**
     * 사용자의 모든 장바구니 아이템 삭제
     *
     * @param userId 사용자 ID
     */
    fun deleteByUserId(userId: Long)
}
