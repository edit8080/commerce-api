package com.beanbliss.domain.cart.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * 장바구니 추가 요청 DTO
 *
 * [제약사항]:
 * - userId: 1 이상
 * - productOptionId: 1 이상
 * - quantity: 1 ~ 999
 */
data class AddToCartRequest(
    @field:Min(value = 1, message = "사용자 ID는 1 이상이어야 합니다.")
    val userId: Long,

    @field:Min(value = 1, message = "상품 옵션 ID는 1 이상이어야 합니다.")
    val productOptionId: Long,

    @field:Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    @field:Max(value = 999, message = "수량은 999개 이하여야 합니다.")
    val quantity: Int
)
