package com.beanbliss.domain.cart.usecase

import com.beanbliss.domain.cart.domain.CartItemDetail
import com.beanbliss.domain.cart.dto.AddToCartRequest
import com.beanbliss.domain.cart.service.CartService
import com.beanbliss.domain.product.service.ProductOptionService
import com.beanbliss.domain.user.service.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * [책임]: 장바구니 추가 기능의 오케스트레이션
 *
 * [설계 변경]:
 * - 도메인 간 데이터 조합을 UseCase에서 처리
 * - CartService는 CART 도메인만, ProductOptionService는 PRODUCT 도메인만
 *
 * [오케스트레이션 흐름]:
 * 1. UserService: 사용자 존재 여부 검증
 * 2. ProductOptionService: 활성 상품 옵션 조회 (PRODUCT 도메인)
 * 3. CartService: 장바구니 아이템 추가/수정 (CART 도메인)
 * 4. 데이터 조합: CartItemDetail 생성
 *
 * [DIP 준수]:
 * - Service에만 의존
 */
@Component
class AddToCartUseCase(
    private val userService: UserService,
    private val productOptionService: ProductOptionService,
    private val cartService: CartService
) {

    /**
     * 장바구니에 상품 추가
     *
     * [비즈니스 로직]:
     * 1. 사용자 검증
     * 2. PRODUCT 도메인: 상품 옵션 검증
     * 3. CART 도메인: 장바구니 추가
     * 4. 데이터 조합
     *
     * @param request 장바구니 추가 요청
     * @return 추가/수정된 장바구니 아이템 정보 및 신규 여부
     */
    @Transactional
    fun addToCart(request: AddToCartRequest): AddToCartUseCaseResult {
        // 1. 사용자 존재 여부 검증
        userService.validateUserExists(request.userId)

        // 2. PRODUCT 도메인: 활성 상품 옵션 조회
        val productOption = productOptionService.getActiveOptionWithProduct(request.productOptionId)

        // 3. CART 도메인: 장바구니 아이템 추가/수정
        val result = cartService.addCartItem(
            userId = request.userId,
            productOptionId = request.productOptionId,
            quantity = request.quantity
        )

        // 4. 데이터 조합
        val cartItemDetail = CartItemDetail(
            cartItemId = result.cartItem.id,
            productOptionId = result.cartItem.productOptionId,
            productName = productOption.productName,
            optionCode = productOption.optionCode,
            origin = productOption.origin,
            grindType = productOption.grindType,
            weightGrams = productOption.weightGrams,
            price = productOption.price,
            quantity = result.cartItem.quantity,
            totalPrice = productOption.price * result.cartItem.quantity,
            createdAt = result.cartItem.createdAt,
            updatedAt = result.cartItem.updatedAt
        )

        return AddToCartUseCaseResult(
            cartItem = cartItemDetail,
            isNewItem = result.isNewItem
        )
    }
}

/**
 * 장바구니 추가 UseCase 결과
 *
 * @property cartItem 추가/수정된 장바구니 아이템 정보 (CART + PRODUCT 도메인 조합)
 * @property isNewItem 신규 아이템 여부 (true: 신규 추가, false: 기존 수량 증가)
 */
data class AddToCartUseCaseResult(
    val cartItem: CartItemDetail,
    val isNewItem: Boolean
)
