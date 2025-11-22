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
 * [트랜잭션]:
 * - Class 레벨: readOnly = true (조회용)
 * - Method 레벨: @Transactional (쓰기 작업)
 *
 * [DIP 준수]:
 * - CartItemRepository Interface에만 의존
 */
@Service
@Transactional(readOnly = true)
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
     * [동시성 제어]:
     * - UPSERT (INSERT ... ON DUPLICATE KEY UPDATE) 사용
     * - 원자적으로 INSERT 또는 UPDATE 수행
     * - Lost Update 방지, UNIQUE 제약 위반 처리 불필요
     *
     * @param userId 사용자 ID
     * @param productOptionId 상품 옵션 ID
     * @param quantity 추가할 수량
     * @return 추가/수정된 장바구니 아이템 (isNewItem: 신규 여부)
     * @throws InvalidParameterException 최대 수량 초과 시
     */
    @Transactional
    fun addCartItem(
        userId: Long,
        productOptionId: Long,
        quantity: Int
    ): CartItemUpsertResult {
        // 최대 수량 검증
        if (quantity > MAX_QUANTITY) {
            throw InvalidParameterException("장바구니 내 동일 옵션의 최대 수량은 ${MAX_QUANTITY}개입니다.")
        }

        // UPSERT 실행 (INSERT OR UPDATE)
        // 반환값: 1 = INSERT, 2 = UPDATE 성공
        val affectedRows = cartItemRepository.upsertCartItem(
            userId = userId,
            productOptionId = productOptionId,
            quantity = quantity
        )

        // 결과 조회
        val cartItem = cartItemRepository.findByUserIdAndProductOptionId(userId, productOptionId)
            ?: throw IllegalStateException("Cart item not found after upsert")

        // 최대 수량 검증 (UPSERT 후 실제 수량 확인)
        // 초과 시 예외를 던져 트랜잭션 롤백
        if (cartItem.quantity > MAX_QUANTITY) {
            throw InvalidParameterException("장바구니 내 동일 옵션의 최대 수량은 ${MAX_QUANTITY}개입니다.")
        }

        return CartItemUpsertResult(
            cartItem = cartItem,
            isNewItem = affectedRows == 1  // 1이면 INSERT (신규), 2이면 UPDATE (기존)
        )
    }

    /**
     * 사용자의 장바구니 비우기
     *
     * [비즈니스 규칙]:
     * - 사용자의 모든 장바구니 아이템 삭제
     *
     * @param userId 사용자 ID
     */
    @Transactional
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
