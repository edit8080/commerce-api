package com.beanbliss.domain.coupon.service

import com.beanbliss.common.dto.PageableResponse
import com.beanbliss.common.pagination.PageCalculator
import com.beanbliss.domain.coupon.dto.CouponListData
import com.beanbliss.domain.coupon.dto.CouponListResponse
import com.beanbliss.domain.coupon.dto.CouponResponse
import com.beanbliss.domain.coupon.repository.CouponRepository
import com.beanbliss.domain.coupon.repository.CouponWithQuantity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * [책임]: 쿠폰 목록 조회 비즈니스 로직 구현
 * - Repository 호출
 * - isIssuable 계산
 * - DTO 변환
 * - 페이징 정보 구성
 */
@Service
@Transactional(readOnly = true)
class CouponServiceImpl(
    private val couponRepository: CouponRepository
) : CouponService {

    override fun getCoupons(page: Int, size: Int): CouponListResponse {
        // 1. Repository에서 쿠폰 목록 조회 (정렬: created_at DESC)
        val coupons = couponRepository.findAllCoupons(
            page = page,
            size = size,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // 2. Repository에서 전체 쿠폰 개수 조회
        val totalCount = couponRepository.countAllCoupons()

        // 3. CouponWithQuantity -> CouponResponse 변환 및 isIssuable 계산
        val now = LocalDateTime.now()
        val couponResponses = coupons.map { coupon ->
            toCouponResponse(coupon, now)
        }

        // 4. 페이징 정보 구성 (PageCalculator 사용)
        val totalPages = PageCalculator.calculateTotalPages(totalCount, size)

        val pageable = PageableResponse(
            pageNumber = page,
            pageSize = size,
            totalElements = totalCount,
            totalPages = totalPages
        )

        // 5. 최종 응답 구성
        return CouponListResponse(
            data = CouponListData(
                content = couponResponses,
                pageable = pageable
            )
        )
    }

    /**
     * CouponWithQuantity를 CouponResponse로 변환하고 isIssuable 계산
     *
     * isIssuable 조건:
     * - 현재 시각이 유효 기간 내 (now BETWEEN validFrom AND validUntil)
     * - 남은 수량이 1개 이상 (remainingQuantity > 0)
     */
    private fun toCouponResponse(coupon: CouponWithQuantity, now: LocalDateTime): CouponResponse {
        val isInValidPeriod = now.isAfter(coupon.validFrom) && now.isBefore(coupon.validUntil)
        val hasRemainingQuantity = coupon.remainingQuantity > 0
        val isIssuable = isInValidPeriod && hasRemainingQuantity

        return CouponResponse(
            couponId = coupon.id,
            name = coupon.name,
            discountType = coupon.discountType,
            discountValue = coupon.discountValue,
            minOrderAmount = coupon.minOrderAmount,
            maxDiscountAmount = coupon.maxDiscountAmount,
            remainingQuantity = coupon.remainingQuantity,
            totalQuantity = coupon.totalQuantity,
            validFrom = coupon.validFrom,
            validUntil = coupon.validUntil,
            isIssuable = isIssuable
        )
    }
}
