package com.beanbliss.domain.coupon.service

import com.beanbliss.common.dto.PageableResponse
import com.beanbliss.common.pagination.PageCalculator
import com.beanbliss.domain.coupon.dto.UserCouponListData
import com.beanbliss.domain.coupon.dto.UserCouponListResponse
import com.beanbliss.domain.coupon.dto.UserCouponResponse
import com.beanbliss.domain.coupon.repository.UserCouponRepository
import com.beanbliss.domain.coupon.repository.UserCouponWithCoupon
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * [책임]: 사용자 쿠폰 비즈니스 로직 처리
 * - 사용자 발급 쿠폰 목록 조회
 * - isAvailable 계산 로직
 */
@Service
@Transactional(readOnly = true)
class UserCouponServiceImpl(
    private val userCouponRepository: UserCouponRepository
) : UserCouponService {

    /**
     * 사용자 발급 쿠폰 목록 조회
     *
     * 1. Repository에서 사용자 쿠폰 목록 조회 (isAvailable 계산, 정렬, 페이징 완료된 상태)
     * 2. 전체 쿠폰 개수 조회
     * 3. DTO 변환
     * 4. 응답 생성
     */
    override fun getUserCoupons(userId: Long, page: Int, size: Int): UserCouponListResponse {
        // 1. Repository에서 사용자 쿠폰 목록 조회 (isAvailable 계산, 정렬, 페이징 완료)
        val now = LocalDateTime.now()
        val userCouponsWithCoupon = userCouponRepository.findByUserIdWithPaging(userId, page, size, now)

        // 2. 전체 쿠폰 개수 조회
        val totalCount = userCouponRepository.countByUserId(userId)

        // 3. DTO 변환 (Repository에서 이미 isAvailable 계산 완료)
        val userCouponResponses = userCouponsWithCoupon.map { userCouponWithCoupon ->
            toUserCouponResponse(userCouponWithCoupon)
        }

        // 4. 페이징 정보 계산
        val totalPages = PageCalculator.calculateTotalPages(totalCount, size)

        // 5. 응답 생성
        return UserCouponListResponse(
            data = UserCouponListData(
                content = userCouponResponses,
                pageable = PageableResponse(
                    pageNumber = page,
                    pageSize = size,
                    totalElements = totalCount,
                    totalPages = totalPages
                )
            )
        )
    }

    /**
     * UserCouponWithCoupon -> UserCouponResponse 변환
     * (isAvailable은 Repository에서 이미 계산됨)
     */
    private fun toUserCouponResponse(
        userCouponWithCoupon: UserCouponWithCoupon
    ): UserCouponResponse {
        return UserCouponResponse(
            userCouponId = userCouponWithCoupon.userCouponId,
            couponId = userCouponWithCoupon.couponId,
            couponName = userCouponWithCoupon.couponName,
            discountType = userCouponWithCoupon.discountType,
            discountValue = userCouponWithCoupon.discountValue,
            minOrderAmount = userCouponWithCoupon.minOrderAmount,
            maxDiscountAmount = userCouponWithCoupon.maxDiscountAmount,
            status = userCouponWithCoupon.status,
            validFrom = userCouponWithCoupon.validFrom,
            validUntil = userCouponWithCoupon.validUntil,
            issuedAt = userCouponWithCoupon.issuedAt,
            usedAt = userCouponWithCoupon.usedAt,
            usedOrderId = userCouponWithCoupon.usedOrderId,
            isAvailable = userCouponWithCoupon.isAvailable  // Repository에서 계산된 값 사용
        )
    }
}
