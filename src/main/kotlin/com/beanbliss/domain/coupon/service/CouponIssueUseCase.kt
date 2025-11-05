package com.beanbliss.domain.coupon.service

import com.beanbliss.domain.coupon.dto.IssueCouponResponse
import com.beanbliss.domain.user.service.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * [책임]: 쿠폰 발급 비즈니스 플로우 조율
 * - 여러 Service를 조율하여 복잡한 비즈니스 트랜잭션 수행
 * - UseCase 패턴으로 다중 도메인 의존성 추상화
 *
 * [UseCase 패턴 적용]:
 * - `UserService`: 사용자 존재 여부 검증
 * - `CouponService`: 쿠폰 정보 조회 및 유효성 검증
 * - `UserCouponService`: 사용자 쿠폰 생성 및 중복 발급 확인
 * - `CouponTicketService`: 티켓 선점 및 상태 업데이트 (FOR UPDATE SKIP LOCKED)
 */
@Component
class CouponIssueUseCase(
    private val userService: UserService,
    private val couponService: CouponService,
    private val userCouponService: UserCouponService,
    private val couponTicketService: CouponTicketService
) {
    /**
     * 쿠폰 발급
     *
     * 1. 사용자 존재 여부 검증 (User 도메인)
     * 2. 쿠폰 유효성 검증 (Coupon 도메인)
     * 3. 중복 발급 방지 검증 (UserCoupon 도메인)
     * 4. 티켓 선점 (CouponTicket 도메인 - FOR UPDATE SKIP LOCKED)
     * 5. 사용자 쿠폰 생성 (UserCoupon 도메인)
     * 6. 티켓 상태 업데이트 (CouponTicket 도메인)
     * 7. DTO 변환 및 반환
     */
    @Transactional
    fun issueCoupon(couponId: Long, userId: Long): IssueCouponResponse {
        // 1. 사용자 존재 여부 검증 (User 도메인)
        userService.validateUserExists(userId)

        // 2. 쿠폰 유효성 검증 (Coupon 도메인)
        val coupon = couponService.getValidCoupon(couponId)

        // 3. 중복 발급 방지 검증 (UserCoupon 도메인)
        userCouponService.validateNotAlreadyIssued(userId, couponId)

        // 4. 티켓 선점 (CouponTicket 도메인 - FOR UPDATE SKIP LOCKED)
        val ticket = couponTicketService.reserveAvailableTicket(couponId)

        // 5. 사용자 쿠폰 생성 (UserCoupon 도메인)
        val userCoupon = userCouponService.createUserCoupon(userId, couponId)

        // 6. 티켓 상태 업데이트 (CouponTicket 도메인)
        couponTicketService.markTicketAsIssued(ticket.id!!, userId, userCoupon.id)

        // 7. DTO 변환 및 반환
        return IssueCouponResponse(
            userCouponId = userCoupon.id,
            couponId = coupon.id!!,
            userId = userId,
            couponName = coupon.name,
            discountType = coupon.discountType,
            discountValue = coupon.discountValue,
            minOrderAmount = coupon.minOrderAmount,
            maxDiscountAmount = coupon.maxDiscountAmount,
            status = userCoupon.status,
            validFrom = coupon.validFrom,
            validUntil = coupon.validUntil,
            issuedAt = userCoupon.createdAt
        )
    }
}
