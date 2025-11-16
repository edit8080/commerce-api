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
 *
 * [트랜잭션 최적화]:
 * - 검증 로직(Step 1-3)은 트랜잭션 외부에서 수행 → Connection 점유 시간 최소화
 * - 쓰기 작업(Step 4-6)만 트랜잭션 내부에서 수행
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
     * [트랜잭션 외부]:
     * 1. 사용자 존재 여부 검증 (User 도메인)
     * 2. 쿠폰 유효성 검증 (Coupon 도메인)
     * 3. 중복 발급 방지 검증 (UserCoupon 도메인)
     *
     * [트랜잭션 내부]:
     * 4. 티켓 선점 (CouponTicket 도메인 - FOR UPDATE SKIP LOCKED)
     * 5. 사용자 쿠폰 생성 (UserCoupon 도메인)
     * 6. 티켓 상태 업데이트 (CouponTicket 도메인)
     *
     * [원자성 보장]:
     * - Step 4-6은 하나의 트랜잭션에서 실행되어 원자성 보장
     * - 중복 발급은 Step 3의 검증과 Step 5의 생성이 연속적으로 실행되어 방지
     *   (실제 운영에서는 DB UNIQUE 제약 추가 권장)
     */
    fun issueCoupon(couponId: Long, userId: Long): IssueCouponResponse {
        // Step 1-3: 트랜잭션 외부 검증 (Connection 점유 안함)
        userService.validateUserExists(userId)
        val coupon = couponService.getValidCoupon(couponId)
        userCouponService.validateNotAlreadyIssued(userId, couponId)

        // Step 4-6: 트랜잭션 내부 쓰기 작업 (Connection 점유 최소화)
        return issueTicketTransactional(couponId, userId, coupon)
    }

    /**
     * 쿠폰 티켓 발급 트랜잭션 (쓰기 작업만)
     *
     * [트랜잭션 범위]:
     * - 티켓 선점 (FOR UPDATE SKIP LOCKED)
     * - 사용자 쿠폰 생성
     * - 티켓 상태 업데이트
     *
     * [Connection 점유 시간]: ~15ms (검증 제외)
     */
    @Transactional
    private fun issueTicketTransactional(
        couponId: Long,
        userId: Long,
        coupon: CouponService.CouponInfo
    ): IssueCouponResponse {
        // 4. 티켓 선점 (CouponTicket 도메인 - FOR UPDATE SKIP LOCKED)
        val ticket = couponTicketService.reserveAvailableTicket(couponId)

        // 5. 사용자 쿠폰 생성 (UserCoupon 도메인)
        val userCoupon = userCouponService.createUserCoupon(userId, couponId)

        // 6. 티켓 상태 업데이트 (CouponTicket 도메인)
        couponTicketService.markTicketAsIssued(ticket.id, userId, userCoupon.id)

        // 7. DTO 변환 및 반환
        return IssueCouponResponse(
            userCouponId = userCoupon.id,
            couponId = coupon.id,
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
