package com.beanbliss.domain.cart.service

import com.beanbliss.domain.cart.dto.CartItemResponse
import com.beanbliss.domain.product.repository.ProductOptionDetail

/**
 * [책임]: 장바구니 관리 기능의 '계약' 정의.
 * UseCase는 이 인터페이스에만 의존합니다. (DIP 준수)
 *
 * [주요 기능]:
 * - 장바구니 아이템 추가/수정 (Upsert)
 * - 장바구니 아이템 조회 (상품 정보 포함)
 */
interface CartService {
    /**
     * 장바구니 아이템 추가 또는 수정 (Upsert)
     *
     * [비즈니스 규칙]:
     * - 같은 사용자가 같은 상품 옵션을 추가하면 기존 수량 증가
     * - 최대 수량: 999개
     *
     * @param userId 사용자 ID
     * @param productOption 상품 옵션 정보
     * @param quantity 추가할 수량
     * @return 추가/수정된 장바구니 아이템 정보 및 신규 여부
     * @throws InvalidParameterException 최대 수량 초과 시
     */
    fun upsertCartItem(
        userId: Long,
        productOption: ProductOptionDetail,
        quantity: Int
    ): UpsertCartItemResult

    /**
     * 사용자의 장바구니 아이템 목록 조회 (상품 정보 포함)
     *
     * [비즈니스 규칙]:
     * - 장바구니가 비어 있으면 예외 발생
     *
     * @param userId 사용자 ID
     * @return 장바구니 아이템 목록 (상품명, 옵션 코드, 가격 등 포함)
     * @throws CartEmptyException 장바구니가 비어 있는 경우
     */
    fun getCartItemsWithProducts(userId: Long): List<CartItemResponse>
}

/**
 * 장바구니 Upsert 결과
 *
 * @property cartItem 추가/수정된 장바구니 아이템 정보
 * @property isNewItem 신규 아이템 여부 (true: 신규 추가, false: 기존 수량 증가)
 */
data class UpsertCartItemResult(
    val cartItem: CartItemResponse,
    val isNewItem: Boolean
)
