package com.beanbliss.domain.coupon.controller

import com.beanbliss.common.dto.ApiResponse
import com.beanbliss.domain.coupon.dto.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime

@RestController
@RequestMapping("/coupons")
class CouponController {

    // Mock 쿠폰 데이터
    private val mockCoupons = listOf(
        MockCoupon(
            id = 1L,
            name = "오픈 기념 선착순 100명 10% 할인",
            description = "Bean Bliss 오픈을 기념하여 선착순 100명에게 드리는 특별 할인 쿠폰입니다.",
            discountType = "PERCENTAGE",
            discountValue = BigDecimal("10"),
            minOrderAmount = BigDecimal("20000"),
            maxDiscountAmount = BigDecimal("5000"),
            totalQuantity = 100,
            issuedQuantity = 58,
            validFrom = LocalDateTime.of(2025, 10, 20, 0, 0),
            validUntil = LocalDateTime.of(2025, 11, 20, 23, 59),
            createdAt = LocalDateTime.of(2025, 10, 20, 9, 0)
        ),
        MockCoupon(
            id = 2L,
            name = "신규 회원 환영 5,000원 할인",
            description = "신규 가입 회원을 위한 특별 할인 쿠폰입니다.",
            discountType = "FIXED_AMOUNT",
            discountValue = BigDecimal("5000"),
            minOrderAmount = BigDecimal("30000"),
            maxDiscountAmount = BigDecimal("5000"),
            totalQuantity = 500,
            issuedQuantity = 500,
            validFrom = LocalDateTime.of(2025, 10, 15, 0, 0),
            validUntil = LocalDateTime.of(2025, 12, 31, 23, 59),
            createdAt = LocalDateTime.of(2025, 10, 15, 9, 0)
        ),
        MockCoupon(
            id = 3L,
            name = "가을맞이 특별 할인 15%",
            description = "가을 시즌을 맞이하여 제공하는 특별 할인입니다.",
            discountType = "PERCENTAGE",
            discountValue = BigDecimal("15"),
            minOrderAmount = BigDecimal("50000"),
            maxDiscountAmount = BigDecimal("10000"),
            totalQuantity = 200,
            issuedQuantity = 145,
            validFrom = LocalDateTime.of(2025, 9, 1, 0, 0),
            validUntil = LocalDateTime.of(2025, 11, 30, 23, 59),
            createdAt = LocalDateTime.of(2025, 9, 1, 9, 0)
        ),
        MockCoupon(
            id = 4L,
            name = "첫 구매 감사 쿠폰 3,000원",
            description = "첫 구매 고객을 위한 감사 쿠폰입니다.",
            discountType = "FIXED_AMOUNT",
            discountValue = BigDecimal("3000"),
            minOrderAmount = BigDecimal("15000"),
            maxDiscountAmount = BigDecimal("3000"),
            totalQuantity = 1000,
            issuedQuantity = 678,
            validFrom = LocalDateTime.of(2025, 8, 1, 0, 0),
            validUntil = LocalDateTime.of(2025, 10, 15, 23, 59),
            createdAt = LocalDateTime.of(2025, 8, 1, 9, 0)
        )
    )

    // Mock 사용자 쿠폰 데이터 (userId = 1 가정)
    private val mockUserCoupons = mutableListOf(
        MockUserCoupon(
            userCouponId = 15L,
            couponId = 1L,
            userId = 1L,
            status = "ISSUED",
            usedOrderId = null,
            usedAt = null,
            issuedAt = LocalDateTime.of(2025, 10, 25, 14, 30)
        ),
        MockUserCoupon(
            userCouponId = 8L,
            couponId = 3L,
            userId = 1L,
            status = "USED",
            usedOrderId = 1024L,
            usedAt = LocalDateTime.of(2025, 10, 22, 11, 45),
            issuedAt = LocalDateTime.of(2025, 10, 15, 9, 20)
        ),
        MockUserCoupon(
            userCouponId = 3L,
            couponId = 4L,
            userId = 1L,
            status = "EXPIRED",
            usedOrderId = null,
            usedAt = null,
            issuedAt = LocalDateTime.of(2025, 8, 5, 16, 0)
        )
    )

    private val now = LocalDateTime.now()

    // 1. 발급 가능한 쿠폰 목록 조회
    @GetMapping("/available")
    fun getAvailableCoupons(
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?
    ): ResponseEntity<ApiResponse<AvailableCouponsResponse>> {
        val coupons = mockCoupons.map { coupon ->
            val remainingQuantity = coupon.totalQuantity - coupon.issuedQuantity
            val issueRate = (coupon.issuedQuantity.toDouble() / coupon.totalQuantity) * 100

            AvailableCouponDto(
                id = coupon.id,
                name = coupon.name,
                discountType = coupon.discountType,
                discountValue = coupon.discountValue,
                minOrderAmount = coupon.minOrderAmount,
                maxDiscountAmount = coupon.maxDiscountAmount,
                totalQuantity = coupon.totalQuantity,
                remainingQuantity = remainingQuantity,
                issueRate = issueRate,
                validFrom = coupon.validFrom,
                validUntil = coupon.validUntil,
                isIssuable = remainingQuantity > 0 && now.isBefore(coupon.validUntil),
                createdAt = coupon.createdAt
            )
        }

        return ResponseEntity.ok(
            ApiResponse(
                data = AvailableCouponsResponse(
                    coupons = coupons,
                    pagination = PaginationDto(
                        currentPage = page ?: 1,
                        pageSize = size ?: 20,
                        totalElements = mockCoupons.size.toLong(),
                        totalPages = 1
                    )
                )
            )
        )
    }

    // 2. 쿠폰 상세 정보 조회
    @GetMapping("/{couponId}")
    fun getCouponDetail(@PathVariable couponId: Long): ResponseEntity<ApiResponse<CouponDetailResponse>> {
        val coupon = mockCoupons.first { it.id == couponId }
        val remainingQuantity = coupon.totalQuantity - coupon.issuedQuantity
        val issueRate = (coupon.issuedQuantity.toDouble() / coupon.totalQuantity) * 100

        val response = CouponDetailResponse(
            id = coupon.id,
            name = coupon.name,
            description = coupon.description,
            discountType = coupon.discountType,
            discountValue = coupon.discountValue,
            minOrderAmount = coupon.minOrderAmount,
            maxDiscountAmount = coupon.maxDiscountAmount,
            totalQuantity = coupon.totalQuantity,
            issuedQuantity = coupon.issuedQuantity,
            remainingQuantity = remainingQuantity,
            issueRate = issueRate,
            validFrom = coupon.validFrom,
            validUntil = coupon.validUntil,
            isIssuable = remainingQuantity > 0 && now.isBefore(coupon.validUntil),
            conditions = CouponConditionsDto(
                minOrderAmount = coupon.minOrderAmount,
                maxDiscountAmount = coupon.maxDiscountAmount,
                description = "${coupon.minOrderAmount}원 이상 구매 시 사용 가능하며, 최대 할인 금액은 ${coupon.maxDiscountAmount}원입니다."
            ),
            createdAt = coupon.createdAt,
            updatedAt = coupon.createdAt
        )

        return ResponseEntity.ok(ApiResponse(data = response))
    }

    // 3. 사용자별 쿠폰 목록 조회
    @GetMapping("/user/{userId}")
    fun getUserCoupons(
        @PathVariable userId: Long,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?
    ): ResponseEntity<ApiResponse<UserCouponsResponse>> {
        val userCoupons = mockUserCoupons.filter { it.userId == userId }

        val coupons = userCoupons.map { userCoupon ->
            val coupon = mockCoupons.first { it.id == userCoupon.couponId }

            UserCouponDto(
                userCouponId = userCoupon.userCouponId,
                couponId = coupon.id,
                name = coupon.name,
                discountType = coupon.discountType,
                discountValue = coupon.discountValue,
                minOrderAmount = coupon.minOrderAmount,
                maxDiscountAmount = coupon.maxDiscountAmount,
                status = userCoupon.status,
                validFrom = coupon.validFrom,
                validUntil = coupon.validUntil,
                isUsable = userCoupon.status == "ISSUED" && now.isBefore(coupon.validUntil),
                usedOrderId = userCoupon.usedOrderId,
                usedAt = userCoupon.usedAt,
                issuedAt = userCoupon.issuedAt
            )
        }

        val summary = CouponSummaryDto(
            totalCount = coupons.size,
            issuedCount = coupons.count { it.status == "ISSUED" },
            usedCount = coupons.count { it.status == "USED" },
            expiredCount = coupons.count { it.status == "EXPIRED" }
        )

        return ResponseEntity.ok(
            ApiResponse(
                data = UserCouponsResponse(
                    coupons = coupons,
                    summary = summary,
                    pagination = PaginationDto(
                        currentPage = page ?: 1,
                        pageSize = size ?: 20,
                        totalElements = coupons.size.toLong(),
                        totalPages = 1
                    )
                )
            )
        )
    }

    // 4. 쿠폰 발급
    @PostMapping("/{couponId}/issue")
    fun issueCoupon(@PathVariable couponId: Long): ResponseEntity<ApiResponse<IssueCouponResponse>> {
        val coupon = mockCoupons.first { it.id == couponId }
        val issuedAt = LocalDateTime.now()

        val response = IssueCouponResponse(
            userCouponId = 101L,
            couponId = coupon.id,
            name = coupon.name,
            discountType = coupon.discountType,
            discountValue = coupon.discountValue,
            minOrderAmount = coupon.minOrderAmount,
            maxDiscountAmount = coupon.maxDiscountAmount,
            status = "ISSUED",
            validFrom = coupon.validFrom,
            validUntil = coupon.validUntil,
            isUsable = true,
            issuedAt = issuedAt
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse(data = response))
    }

    // Mock 데이터 클래스
    private data class MockCoupon(
        val id: Long,
        val name: String,
        val description: String,
        val discountType: String,
        val discountValue: BigDecimal,
        val minOrderAmount: BigDecimal,
        val maxDiscountAmount: BigDecimal,
        val totalQuantity: Int,
        var issuedQuantity: Int,
        val validFrom: LocalDateTime,
        val validUntil: LocalDateTime,
        val createdAt: LocalDateTime
    )

    private data class MockUserCoupon(
        val userCouponId: Long,
        val couponId: Long,
        val userId: Long,
        val status: String,
        val usedOrderId: Long?,
        val usedAt: LocalDateTime?,
        val issuedAt: LocalDateTime
    )
}
