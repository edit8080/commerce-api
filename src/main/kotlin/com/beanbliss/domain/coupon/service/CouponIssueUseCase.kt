package com.beanbliss.domain.coupon.service

import com.beanbliss.common.exception.ResourceNotFoundException
import com.beanbliss.domain.coupon.dto.IssueCouponResponse
import com.beanbliss.domain.coupon.exception.*
import com.beanbliss.domain.coupon.repository.CouponRepository
import com.beanbliss.domain.coupon.repository.CouponTicketRepository
import com.beanbliss.domain.coupon.repository.UserCouponRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * [책임]: 쿠폰 발급 비즈니스 플로우 조율
 * - 여러 Repository를 조율하여 복잡한 비즈니스 트랜잭션 수행
 * - Facade 패턴으로 다중 도메인 의존성 추상화
 */
@Component
class CouponIssueUseCase(
    private val couponRepository: CouponRepository,
    private val couponTicketRepository: CouponTicketRepository,
    private val userCouponRepository: UserCouponRepository
) {
    /**
     * 쿠폰 발급
     *
     * 1. 쿠폰 유효성 검증 (존재 여부, 유효기간)
     * 2. 중복 발급 방지 검증
     * 3. 티켓 선점 (FOR UPDATE SKIP LOCKED)
     * 4. 사용자 쿠폰 생성
     * 5. 티켓 상태 업데이트
     * 6. DTO 변환 및 반환
     */
    @Transactional
    fun issueCoupon(couponId: Long, userId: Long): IssueCouponResponse {
        // 1. 쿠폰 조회 및 존재 여부 검증
        val coupon = couponRepository.findById(couponId)
            ?: throw ResourceNotFoundException("쿠폰을 찾을 수 없습니다.")

        // 2. 유효기간 검증
        val now = LocalDateTime.now()
        if (now.isBefore(coupon.validFrom)) {
            throw CouponNotStartedException("아직 사용할 수 없는 쿠폰입니다.")
        }
        if (now.isAfter(coupon.validUntil)) {
            throw CouponExpiredException("유효기간이 만료된 쿠폰입니다.")
        }

        // 3. 중복 발급 방지 검증
        if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw CouponAlreadyIssuedException("이미 발급받은 쿠폰입니다.")
        }

        // 4. 티켓 선점 (FOR UPDATE SKIP LOCKED)
        val ticket = couponTicketRepository.findAvailableTicketWithLock(couponId)
            ?: throw CouponOutOfStockException("쿠폰 재고가 부족합니다.")

        // 5. 사용자 쿠폰 생성
        val userCoupon = userCouponRepository.save(userId, couponId)

        // 6. 티켓 상태 업데이트 (AVAILABLE -> ISSUED)
        couponTicketRepository.updateTicketAsIssued(ticket.id, userId, userCoupon.id)

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
