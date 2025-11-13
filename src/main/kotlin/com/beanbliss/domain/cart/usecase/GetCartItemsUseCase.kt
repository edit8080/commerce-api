package com.beanbliss.domain.cart.usecase

import com.beanbliss.domain.cart.domain.CartItemDetail

import com.beanbliss.domain.cart.service.CartService
import com.beanbliss.domain.order.exception.CartEmptyException
import com.beanbliss.domain.product.service.ProductOptionService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * [책임]: 장바구니 아이템 조회 UseCase (도메인 간 조율)
 *
 * [설계 원칙]:
 * - 여러 도메인 Service 조율
 * - 데이터 조합 및 변환
 * - 복합 도메인 비즈니스 로직
 *
 * [조율하는 Service]:
 * - CartService: CART 도메인
 * - ProductOptionService: PRODUCT 도메인
 *
 * [트랜잭션]:
 * - 읽기 전용 트랜잭션
 */
@Component
class GetCartItemsUseCase(
    private val cartService: CartService,
    private val productOptionService: ProductOptionService
) {
    /**
     * 사용자의 장바구니 아이템 목록 조회 (상품 정보 포함)
     *
     * [비즈니스 로직]:
     * 1. CART 도메인에서 장바구니 아이템 조회
     * 2. 장바구니가 비어있으면 예외 발생
     * 3. PRODUCT 도메인에서 상품 옵션 정보 Batch 조회
     * 4. 데이터 조합하여 CartItemDetail 생성
     *
     * [성능 최적화]:
     * - N+1 문제 방지: 상품 옵션 Batch 조회
     *
     * @param userId 사용자 ID
     * @return 장바구니 아이템 목록 (상품명, 옵션 코드, 가격 등 포함)
     * @throws CartEmptyException 장바구니가 비어 있는 경우
     */
    @Transactional(readOnly = true)
    fun execute(userId: Long): List<CartItemDetail> {
        // 1. CART 도메인: 장바구니 아이템 조회
        val cartItems = cartService.getCartItems(userId)

        // 2. 빈 장바구니 검증
        if (cartItems.isEmpty()) {
            throw CartEmptyException("장바구니가 비어 있습니다.")
        }

        // 3. PRODUCT 도메인: 상품 옵션 정보 Batch 조회
        val optionIds = cartItems.map { it.productOptionId }
        val productOptions = productOptionService.getOptionsBatch(optionIds)

        // 4. 데이터 조합
        return cartItems.map { cartItem ->
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
    }
}
