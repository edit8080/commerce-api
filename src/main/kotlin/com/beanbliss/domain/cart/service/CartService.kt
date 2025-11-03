package com.beanbliss.domain.cart.service

import com.beanbliss.domain.cart.dto.AddToCartRequest
import com.beanbliss.domain.cart.dto.CartItemResponse

/**
 * [책임]: 장바구니 관리 기능의 '계약' 정의.
 * Controller는 이 인터페이스에만 의존합니다. (DIP 준수)
 *
 * [주요 기능]:
 * - 장바구니에 상품 추가
 * - 기존 장바구니 아이템 수량 증가
 */
interface CartService {
    /**
     * 장바구니에 상품 추가
     *
     * [비즈니스 규칙]:
     * - 같은 사용자가 같은 상품 옵션을 추가하면 기존 수량 증가
     * - 활성 옵션만 추가 가능
     * - 최대 수량: 999개
     *
     * @param request 장바구니 추가 요청
     * @return 추가/수정된 장바구니 아이템 정보 및 신규 여부
     * @throws ResourceNotFoundException 사용자 또는 상품 옵션이 없는 경우
     * @throws InvalidParameterException 최대 수량 초과 시
     */
    fun addToCart(request: AddToCartRequest): AddToCartResult
}

/**
 * 장바구니 추가 결과
 *
 * @property cartItem 추가/수정된 장바구니 아이템 정보
 * @property isNewItem 신규 아이템 여부 (true: 신규 추가, false: 기존 수량 증가)
 */
data class AddToCartResult(
    val cartItem: CartItemResponse,
    val isNewItem: Boolean
)
