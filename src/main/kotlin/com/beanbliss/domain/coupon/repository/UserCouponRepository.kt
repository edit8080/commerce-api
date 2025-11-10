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

    /**
     * 사용자 쿠폰 목록 조회 (Coupon 정보 포함, 페이징, 정렬)
     * - USER_COUPON과 COUPON을 JOIN
     * - isAvailable 계산: (status == 'ISSUED') AND (validFrom <= now <= validUntil)
     * - 정렬: isAvailable DESC, issuedAt DESC (쿼리 레벨에서 수행)
     * - 페이징: LIMIT, OFFSET
     *
     * @param userId 사용자 ID
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @param now 현재 시간 (isAvailable 계산용)
     * @return 사용자 쿠폰 목록 (Coupon 정보 + isAvailable 포함, 정렬 및 페이징 적용)
     */
    fun findByUserIdWithPaging(userId: Long, page: Int, size: Int, now: java.time.LocalDateTime): List<UserCouponWithCoupon>

    /**
     * 특정 사용자의 전체 쿠폰 개수 조회
     *
     * @param userId 사용자 ID
     * @return 전체 쿠폰 개수
     */
    fun countByUserId(userId: Long): Long
}
