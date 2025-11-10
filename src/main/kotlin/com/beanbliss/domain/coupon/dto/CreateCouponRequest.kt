package com.beanbliss.domain.coupon.dto

import com.beanbliss.domain.coupon.domain.DiscountType
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime
import kotlin.reflect.KClass

/**
 * 할인값 범위 검증 어노테이션
 *
 * [검증 규칙]:
 * - 정률 할인(PERCENTAGE): 1 이상 100 이하
 * - 정액 할인(FIXED_AMOUNT): 1 이상
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [DiscountValueValidator::class])
annotation class ValidDiscountValue(
    val message: String = "할인값이 유효하지 않습니다.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

/**
 * 할인값 검증 Validator
 */
class DiscountValueValidator : ConstraintValidator<ValidDiscountValue, CreateCouponRequest> {
    override fun isValid(request: CreateCouponRequest?, context: ConstraintValidatorContext?): Boolean {
        if (request == null) {
            return true // null 체크는 @NotNull이 담당
        }

        return when (request.discountType) {
            DiscountType.PERCENTAGE -> {
                if (request.discountValue !in 1..100) {
                    context?.disableDefaultConstraintViolation()
                    context?.buildConstraintViolationWithTemplate(
                        "정률 할인의 할인값은 1 이상 100 이하여야 합니다."
                    )?.addPropertyNode("discountValue")?.addConstraintViolation()
                    return false
                }
                true
            }
            DiscountType.FIXED_AMOUNT -> {
                if (request.discountValue < 1) {
                    context?.disableDefaultConstraintViolation()
                    context?.buildConstraintViolationWithTemplate(
                        "정액 할인의 할인값은 1 이상이어야 합니다."
                    )?.addPropertyNode("discountValue")?.addConstraintViolation()
                    return false
                }
                true
            }
        }
    }
}

/**
 * 유효 기간 순서 검증 어노테이션
 *
 * [검증 규칙]:
 * - validFrom은 validUntil보다 이전이어야 함
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [DateRangeValidator::class])
annotation class ValidDateRange(
    val message: String = "유효 시작 일시는 종료 일시보다 이전이어야 합니다.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

/**
 * 유효 기간 순서 검증 Validator
 */
class DateRangeValidator : ConstraintValidator<ValidDateRange, CreateCouponRequest> {
    override fun isValid(request: CreateCouponRequest?, context: ConstraintValidatorContext?): Boolean {
        if (request == null) {
            return true // null 체크는 @NotNull이 담당
        }

        return if (request.validFrom.isBefore(request.validUntil)) {
            true
        } else {
            context?.disableDefaultConstraintViolation()
            context?.buildConstraintViolationWithTemplate(
                "유효 시작 일시는 종료 일시보다 이전이어야 합니다."
            )?.addPropertyNode("validFrom")?.addConstraintViolation()
            false
        }
    }
}

/**
 * 쿠폰 생성 요청 DTO
 *
 * [검증 규칙]:
 * - 필수 필드: name, discountType, discountValue, totalQuantity, validFrom, validUntil
 * - 할인 타입: PERCENTAGE 또는 FIXED_AMOUNT
 * - 할인값 범위:
 *   - 정률 할인(PERCENTAGE): 1~100
 *   - 정액 할인(FIXED_AMOUNT): 1 이상
 * - 유효 기간: validFrom < validUntil
 * - 발급 수량: 1~10,000
 */
@ValidDiscountValue
@ValidDateRange
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
