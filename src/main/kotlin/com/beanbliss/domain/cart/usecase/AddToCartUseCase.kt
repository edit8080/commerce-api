package com.beanbliss.domain.cart.usecase

import com.beanbliss.domain.cart.dto.AddToCartRequest
import com.beanbliss.domain.cart.repository.CartItemDetail
import com.beanbliss.domain.cart.service.CartService
import com.beanbliss.domain.product.service.ProductOptionService
import com.beanbliss.domain.user.service.UserService
import org.springframework.stereotype.Component

/**
 * [책임]: 장바구니 추가 기능의 오케스트레이션
 * - 여러 Service를 조율하여 장바구니 추가 흐름 구성
 * - Repository JOIN DTO 반환 (Controller에서 Presentation DTO로 변환)
 * - 트랜잭션은 각 Service가 독립적으로 관리
 *
 * [오케스트레이션 흐름]:
 * 1. UserService: 사용자 존재 여부 검증
 * 2. ProductOptionService: 활성 상품 옵션 조회
 * 3. CartService: 장바구니 아이템 추가/수정
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
     * [오케스트레이션]:
     * 1. 사용자 검증 (UserService)
     * 2. 상품 옵션 검증 (ProductOptionService)
     * 3. 장바구니 추가/수정 (CartService)
     *
     * @param request 장바구니 추가 요청
     * @return 추가/수정된 장바구니 아이템 정보 및 신규 여부
     * @throws ResourceNotFoundException 사용자 또는 상품 옵션이 없는 경우
     * @throws InvalidParameterException 최대 수량 초과 시
     */
    fun addToCart(request: AddToCartRequest): AddToCartUseCaseResult {
        // 1. 사용자 존재 여부 검증 (User 도메인)
        userService.validateUserExists(request.userId)

        // 2. 활성 상품 옵션 조회 (Product 도메인)
        val productOption = productOptionService.getActiveOptionWithProduct(request.productOptionId)

        // 3. 장바구니 아이템 추가/수정 (Cart 도메인)
        val result = cartService.upsertCartItem(
            userId = request.userId,
            productOption = productOption,
            quantity = request.quantity
        )

        return AddToCartUseCaseResult(
            cartItem = result.cartItem,
            isNewItem = result.isNewItem
        )
    }
}

/**
 * 장바구니 추가 UseCase 결과 (Repository JOIN DTO)
 *
 * @property cartItem 추가/수정된 장바구니 아이템 정보
 * @property isNewItem 신규 아이템 여부 (true: 신규 추가, false: 기존 수량 증가)
 */
data class AddToCartUseCaseResult(
    val cartItem: CartItemDetail,
    val isNewItem: Boolean
)
