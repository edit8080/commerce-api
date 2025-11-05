package com.beanbliss.domain.coupon.dto

import com.beanbliss.domain.coupon.domain.DiscountType
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

/**
 * 쿠폰 생성 요청 DTO
 */
data class CreateCouponRequest(
    @field:NotBlank(message = "쿠폰명은 필수입니다.")
    val name: String,

    @field:NotNull(message = "할인 타입은 필수입니다.")
    val discountType: DiscountType,

    @field:NotNull(message = "할인값은 필수입니다.")
    val discountValue: Int,

    val minOrderAmount: Int = 0,

    val maxDiscountAmount: Int? = null,

    @field:NotNull(message = "총 발급 수량은 필수입니다.")
    @field:Min(value = 1, message = "총 발급 수량은 1개 이상이어야 합니다.")
    @field:Max(value = 10000, message = "총 발급 수량은 10,000개 이하여야 합니다.")
    val totalQuantity: Int,

    @field:NotNull(message = "유효 시작 일시는 필수입니다.")
    val validFrom: LocalDateTime,

    @field:NotNull(message = "유효 종료 일시는 필수입니다.")
    val validUntil: LocalDateTime
)
