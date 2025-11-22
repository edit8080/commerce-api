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
     * 사용자 ID와 상품 옵션 ID로 장바구니 아이템 조회 (비관적 락)
     *
     * [동시성 제어]:
     * - PESSIMISTIC_WRITE 락 사용
     * - SELECT ... FOR UPDATE
     * - 동시에 같은 사용자가 같은 상품 옵션을 추가할 때 Lost Update 방지
     *
     * @param userId 사용자 ID
     * @param productOptionId 상품 옵션 ID
     * @return 장바구니 아이템 (없으면 null)
     */
    fun findByUserIdAndProductOptionIdWithLock(userId: Long, productOptionId: Long): CartItem?

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
     * 장바구니 아이템 수량 업데이트 (Managed Entity 직접 수정)
     *
     * [동시성 제어]:
     * - 비관적 락으로 조회한 entity를 직접 수정하여 락 유지
     * - Detached Entity 문제 방지
     *
     * @param cartItemId 장바구니 아이템 ID
     * @param newQuantity 새로운 수량
     * @return 업데이트된 장바구니 아이템
     */
    fun updateQuantity(cartItemId: Long, newQuantity: Int): CartItem

    /**
     * 사용자의 모든 장바구니 아이템 삭제
     *
     * @param userId 사용자 ID
     */
    fun deleteByUserId(userId: Long)

    /**
     * 장바구니 아이템 수량 원자적 증가 (최대 수량 제한 포함)
     *
     * [동시성 제어]:
     * - 비관적 락과 함께 사용하여 Lost Update 방지
     * - UPDATE cart_item SET quantity = quantity + ? WHERE id = ? AND quantity + ? <= 999
     * - 최대 수량 검증을 UPDATE 쿼리에 포함하여 원자성 보장
     *
     * @param cartItemId 장바구니 아이템 ID
     * @param incrementBy 증가할 수량
     * @param maxQuantity 최대 허용 수량 (기본값: 999)
     * @return 업데이트된 행 수 (1: 성공, 0: 최대 수량 초과로 실패)
     */
    fun incrementQuantityWithLimit(cartItemId: Long, incrementBy: Int, maxQuantity: Int = 999): Int

    /**
     * 장바구니 아이템 UPSERT (INSERT OR UPDATE)
     *
     * [동시성 제어]:
     * - INSERT ... ON DUPLICATE KEY UPDATE 사용
     * - 원자적으로 INSERT 또는 UPDATE 수행
     * - 비관적 락 없이도 동시성 보장
     *
     * @param userId 사용자 ID
     * @param productOptionId 상품 옵션 ID
     * @param quantity 추가할 수량
     * @return 영향받은 행 수 (1: INSERT, 2: UPDATE 성공)
     */
    fun upsertCartItem(userId: Long, productOptionId: Long, quantity: Int): Int
}
