package com.beanbliss.domain.coupon.service

import com.beanbliss.domain.coupon.dto.UserCouponListResponse
import com.beanbliss.domain.coupon.entity.UserCouponEntity

/**
 * [책임]: 사용자 쿠폰 관리 기능의 '계약' 정의
 * Controller는 이 인터페이스에만 의존합니다. (DIP 준수)
 */
interface UserCouponService {
    /**
     * 사용자 발급 쿠폰 목록 조회
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 사용자 쿠폰 목록 및 페이징 정보
     */
    fun getUserCoupons(userId: Long, page: Int, size: Int): UserCouponListResponse

    /**
     * 중복 발급 방지 검증
     *
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @throws CouponAlreadyIssuedException 이미 발급받은 쿠폰인 경우
     */
    fun validateNotAlreadyIssued(userId: Long, couponId: Long)

    /**
     * 사용자 쿠폰 생성
     *
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 생성된 사용자 쿠폰 Entity
     */
    fun createUserCoupon(userId: Long, couponId: Long): UserCouponEntity
}
