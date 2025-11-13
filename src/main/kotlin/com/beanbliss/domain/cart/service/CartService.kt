package com.beanbliss.domain.cart.service

import com.beanbliss.domain.cart.domain.CartItem
import com.beanbliss.domain.cart.repository.CartItemRepository
import com.beanbliss.common.exception.InvalidParameterException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [책임]: 장바구니 비즈니스 로직 구현 (CART 도메인만)
 *
 * [설계 변경]:
 * - CART 도메인만 처리
 * - ProductOptionRepository 의존성 제거
 * - PRODUCT 정보는 UseCase에서 조합
 *
 * [비즈니스 규칙]:
 * - 중복 아이템 처리 (수량 증가)
 * - 최대 수량 제한 검증 (999개)
 * - Cart 도메인 내 데이터 관리
 *
 * [트랜잭션]: @Transactional을 통해 원자성 보장
 *
 * [DIP 준수]:
 * - CartItemRepository Interface에만 의존
 */
@Service
@Transactional
class CartService(
    private val cartItemRepository: CartItemRepository
) {

    companion object {
        private const val MAX_QUANTITY = 999
    }

    /**
     * 장바구니 아이템 조회 (CART 도메인만)
     *
     * [변경사항]:
     * - PRODUCT 정보 제거
     * - CartItem 도메인 모델 반환
     *
     * @param userId 사용자 ID
     * @return 장바구니 아이템 목록 (CART 도메인만, 비어있을 수 있음)
     */
    @Transactional(readOnly = true)
    fun getCartItems(userId: Long): List<CartItem> {
        return cartItemRepository.findByUserId(userId)
    }

    /**
     * 장바구니 아이템 추가 (신규 또는 수량 증가)
     *
     * [비즈니스 규칙]:
     * - 같은 사용자가 같은 상품 옵션을 추가하면 기존 수량 증가
     * - 최대 수량: 999개
     * - PRODUCT 정보는 UseCase에서 검증
     *
     * @param userId 사용자 ID
     * @param productOptionId 상품 옵션 ID
     * @param quantity 추가할 수량
     * @return 추가/수정된 장바구니 아이템 (isNewItem: 신규 여부)
     * @throws InvalidParameterException 최대 수량 초과 시
     */
    fun addCartItem(
        userId: Long,
        productOptionId: Long,
        quantity: Int
    ): CartItemUpsertResult {
        // 1. 기존 장바구니 아이템 조회
        val existingCartItem = cartItemRepository.findByUserIdAndProductOptionId(userId, productOptionId)

        return if (existingCartItem != null) {
            // 2-1. 기존 아이템 존재: 수량 증가
            val newQuantity = existingCartItem.quantity + quantity

            // 최대 수량 검증
            if (newQuantity > MAX_QUANTITY) {
                throw InvalidParameterException("장바구니 내 동일 옵션의 최대 수량은 ${MAX_QUANTITY}개입니다.")
            }

            // 수량 업데이트
            val updatedCartItem = existingCartItem.updateQuantity(newQuantity)
            val saved = cartItemRepository.save(updatedCartItem)

            CartItemUpsertResult(
                cartItem = saved,
                isNewItem = false
            )
        } else {
            // 2-2. 신규 아이템: 저장
            // 최대 수량 검증
            if (quantity > MAX_QUANTITY) {
                throw InvalidParameterException("장바구니 내 동일 옵션의 최대 수량은 ${MAX_QUANTITY}개입니다.")
            }

            // 신규 장바구니 아이템 생성
            val newCartItem = CartItem(
                id = 0L,
                userId = userId,
                productOptionId = productOptionId,
                quantity = quantity
            )

            val saved = cartItemRepository.save(newCartItem)

            CartItemUpsertResult(
                cartItem = saved,
                isNewItem = true
            )
        }
    }

    /**
     * 장바구니 아이템 수량 업데이트
     *
     * [비즈니스 규칙]:
     * - 최대 수량: 999개
     * - 수량은 1개 이상
     *
     * @param cartItemId 장바구니 아이템 ID
     * @param newQuantity 새로운 수량
     * @return 업데이트된 장바구니 아이템
     * @throws IllegalArgumentException 장바구니 아이템을 찾을 수 없는 경우
     * @throws InvalidParameterException 최대 수량 초과 시
     */
    fun updateCartItemQuantity(cartItemId: Long, newQuantity: Int): CartItem {
        // 최대 수량 검증
        if (newQuantity > MAX_QUANTITY) {
            throw InvalidParameterException("장바구니 내 동일 옵션의 최대 수량은 ${MAX_QUANTITY}개입니다.")
        }

        // 장바구니 아이템 조회
        val cartItem = cartItemRepository.findById(cartItemId)
            ?: throw IllegalArgumentException("장바구니 아이템을 찾을 수 없습니다. ID: $cartItemId")

        // 수량 업데이트
        val updatedCartItem = cartItem.updateQuantity(newQuantity)
        return cartItemRepository.save(updatedCartItem)
    }

    /**
     * 사용자의 장바구니 비우기
     *
     * [비즈니스 규칙]:
     * - 사용자의 모든 장바구니 아이템 삭제
     *
     * @param userId 사용자 ID
     */
    fun clearCart(userId: Long) {
        cartItemRepository.deleteByUserId(userId)
    }
}

/**
 * 장바구니 Upsert 결과
 *
 * @property cartItem 추가/수정된 장바구니 아이템 (CART 도메인만)
 * @property isNewItem 신규 아이템 여부 (true: 신규 추가, false: 기존 수량 증가)
 */
data class CartItemUpsertResult(
    val cartItem: CartItem,
    val isNewItem: Boolean
)
