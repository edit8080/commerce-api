package com.beanbliss.domain.coupon.repository

import com.beanbliss.domain.coupon.entity.UserCouponEntity

/**
 * [책임]: 사용자 쿠폰 영속성 계층의 계약 정의
 * UseCase는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface UserCouponRepository {
    /**
     * 사용자가 특정 쿠폰을 이미 발급받았는지 확인
     * - 중복 발급 방지를 위한 검증
     *
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 발급 여부 (true: 이미 발급됨, false: 발급 안 됨)
     */
    fun existsByUserIdAndCouponId(userId: Long, couponId: Long): Boolean

    /**
     * 사용자 쿠폰 생성
     * - userId, couponId, status='ISSUED' 설정
     * - createdAt, updatedAt 자동 설정
     *
     * @param userId 사용자 ID
     * @param couponId 쿠폰 ID
     * @return 생성된 UserCouponEntity
     */
    fun save(userId: Long, couponId: Long): UserCouponEntity

    /**
     * 사용자 쿠폰 ID로 조회
     *
     * @param id 사용자 쿠폰 ID
     * @return UserCouponEntity (없으면 null)
     */
    fun findById(id: Long): UserCouponEntity?

    /**
     * 특정 사용자의 모든 쿠폰 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 쿠폰 목록
     */
    fun findAllByUserId(userId: Long): List<UserCouponEntity>
}
