package com.beanbliss.domain.coupon.controller

import com.beanbliss.common.dto.PageableResponse
import com.beanbliss.common.pagination.PageCalculator
import com.beanbliss.domain.coupon.dto.UserCouponListData
import com.beanbliss.domain.coupon.dto.UserCouponListResponse
import com.beanbliss.domain.coupon.dto.UserCouponResponse
import com.beanbliss.domain.coupon.service.UserCouponService
import org.springframework.web.bind.annotation.*

/**
 * 사용자 쿠폰 관련 API 엔드포인트를 제공하는 Controller
 *
 * [책임]:
 * 1. HTTP 요청을 받아 파라미터를 추출
 * 2. Service 계층에 비즈니스 로직 위임
 * 3. Service 결과를 HTTP 응답으로 변환
 *
 * [참고]:
 * - 페이징 파라미터 검증은 PageCalculator에서 수행
 */
@RestController
@RequestMapping("/api/users")
class UserCouponController(
    private val userCouponService: UserCouponService
) {

    /**
     * 사용자 발급 쿠폰 목록 조회 API
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호 (1부터 시작, 기본값: 1)
     * @param size 페이지 크기 (기본값: 10, 최대: 100)
     * @return 사용자 쿠폰 목록과 페이징 정보
     * @throws InvalidParameterException 페이징 파라미터가 유효하지 않을 경우
     */
    @GetMapping("/{userId}/coupons")
    fun getUserCoupons(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): UserCouponListResponse {
        // 1. 파라미터 검증 (PageCalculator에 위임)
        PageCalculator.validatePageParameters(page, size)

        // 2. Service 호출
        val result = userCouponService.getUserCoupons(userId, page, size)

        // 3. 도메인 데이터 → DTO 변환 (Controller 책임)
        val totalPages = PageCalculator.calculateTotalPages(result.totalCount, size)
        val userCouponResponses = result.userCoupons.map { userCoupon ->
            UserCouponResponse(
                userCouponId = userCoupon.userCouponId,
                couponId = userCoupon.couponId,
                couponName = userCoupon.couponName,
                discountType = userCoupon.discountType,
                discountValue = userCoupon.discountValue,
                minOrderAmount = userCoupon.minOrderAmount,
                maxDiscountAmount = userCoupon.maxDiscountAmount,
                status = userCoupon.status,
                validFrom = userCoupon.validFrom,
                validUntil = userCoupon.validUntil,
                issuedAt = userCoupon.issuedAt,
                usedAt = userCoupon.usedAt,
                usedOrderId = userCoupon.usedOrderId,
                isAvailable = userCoupon.isAvailable
            )
        }

        val pageable = PageableResponse(
            pageNumber = page,
            pageSize = size,
            totalElements = result.totalCount,
            totalPages = totalPages
        )

        // 4. 응답 반환
        return UserCouponListResponse(
            data = UserCouponListData(
                content = userCouponResponses,
                pageable = pageable
            )
        )
    }
}
