package com.beanbliss.domain.coupon.service

import com.beanbliss.domain.coupon.entity.UserCouponEntity
import com.beanbliss.domain.coupon.exception.CouponAlreadyIssuedException
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
class UserCouponService(
    private val userCouponRepository: UserCouponRepository
) {

    /**
     * 사용자 쿠폰 목록 조회 결과 (도메인 데이터)
     */
    data class UserCouponsResult(
        val userCoupons: List<UserCouponWithCoupon>,
        val totalCount: Long
    )

    /**
     * 사용자 발급 쿠폰 목록 조회
     *
     * 1. Repository에서 사용자 쿠폰 목록 조회 (isAvailable 계산, 정렬, 페이징 완료된 상태)
     * 2. 전체 쿠폰 개수 조회
     * 3. 도메인 데이터 반환
     */
    fun getUserCoupons(userId: Long, page: Int, size: Int): UserCouponsResult {
        // 1. Repository에서 사용자 쿠폰 목록 조회 (isAvailable 계산, 정렬, 페이징 완료)
        val now = LocalDateTime.now()
        val userCouponsWithCoupon = userCouponRepository.findByUserIdWithPaging(userId, page, size, now)

        // 2. 전체 쿠폰 개수 조회
        val totalCount = userCouponRepository.countByUserId(userId)

        // 3. 도메인 데이터 반환
        return UserCouponsResult(
            userCoupons = userCouponsWithCoupon,
            totalCount = totalCount
        )
    }

    fun validateNotAlreadyIssued(userId: Long, couponId: Long) {
        if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw CouponAlreadyIssuedException("이미 발급받은 쿠폰입니다.")
        }
    }

    @Transactional
    fun createUserCoupon(userId: Long, couponId: Long): UserCouponEntity {
        return userCouponRepository.save(userId, couponId)
    }
}
