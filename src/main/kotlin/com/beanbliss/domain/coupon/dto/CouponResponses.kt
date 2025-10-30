package com.beanbliss.domain.coupon.dto

import java.math.BigDecimal
import java.time.LocalDateTime

// 1. 발급 가능한 쿠폰 목록 조회 응답
data class AvailableCouponsResponse(
    val coupons: List<AvailableCouponDto>,
    val pagination: PaginationDto
)

data class AvailableCouponDto(
    val id: Long,
    val name: String,
    val discountType: String,
    val discountValue: BigDecimal,
    val minOrderAmount: BigDecimal,
    val maxDiscountAmount: BigDecimal,
    val totalQuantity: Int,
    val remainingQuantity: Int,
    val issueRate: Double,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val isIssuable: Boolean,
    val createdAt: LocalDateTime
)

data class PaginationDto(
    val currentPage: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int
)

// 2. 쿠폰 상세 정보 조회 응답
data class CouponDetailResponse(
    val id: Long,
    val name: String,
    val description: String,
    val discountType: String,
    val discountValue: BigDecimal,
    val minOrderAmount: BigDecimal,
    val maxDiscountAmount: BigDecimal,
    val totalQuantity: Int,
    val issuedQuantity: Int,
    val remainingQuantity: Int,
    val issueRate: Double,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val isIssuable: Boolean,
    val conditions: CouponConditionsDto,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class CouponConditionsDto(
    val minOrderAmount: BigDecimal,
    val maxDiscountAmount: BigDecimal,
    val description: String
)

// 3. 사용자별 쿠폰 목록 조회 응답
data class UserCouponsResponse(
    val coupons: List<UserCouponDto>,
    val summary: CouponSummaryDto,
    val pagination: PaginationDto
)

data class UserCouponDto(
    val userCouponId: Long,
    val couponId: Long,
    val name: String,
    val discountType: String,
    val discountValue: BigDecimal,
    val minOrderAmount: BigDecimal,
    val maxDiscountAmount: BigDecimal,
    val status: String,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val isUsable: Boolean,
    val usedOrderId: Long?,
    val usedAt: LocalDateTime?,
    val issuedAt: LocalDateTime
)

data class CouponSummaryDto(
    val totalCount: Int,
    val issuedCount: Int,
    val usedCount: Int,
    val expiredCount: Int
)

// 4. 쿠폰 발급 응답
data class IssueCouponResponse(
    val userCouponId: Long,
    val couponId: Long,
    val name: String,
    val discountType: String,
    val discountValue: BigDecimal,
    val minOrderAmount: BigDecimal,
    val maxDiscountAmount: BigDecimal,
    val status: String,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val isUsable: Boolean,
    val issuedAt: LocalDateTime
)
