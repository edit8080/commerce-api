package com.beanbliss.domain.coupon.repository

import com.beanbliss.domain.coupon.entity.CouponEntity

/**
 * [책임]: 쿠폰 영속성 계층의 계약 정의
 * Service는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface CouponRepository {
    /**
     * 쿠폰 ID로 조회
     *
     * @param couponId 쿠폰 ID
     * @return CouponEntity (없으면 null)
     */
    fun findById(couponId: Long): CouponEntity?

    /**
     * 모든 쿠폰 조회 (페이징 및 정렬 적용)
     * - remainingQuantity를 포함한 CouponWithQuantity 반환
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @param sortBy 정렬 필드 (created_at, name)
     * @param sortDirection 정렬 순서 (ASC, DESC)
     */
    fun findAllCoupons(page: Int, size: Int, sortBy: String, sortDirection: String): List<CouponWithQuantity>

    /**
     * 전체 쿠폰 개수 조회
     */
    fun countAllCoupons(): Long

    /**
     * 쿠폰 저장
     *
     * @param coupon 저장할 쿠폰 Entity
     * @return 저장된 쿠폰 Entity (ID 포함)
     */
    fun save(coupon: CouponEntity): CouponEntity

    /**
     * 여러 쿠폰 ID로 일괄 조회
     *
     * [성능 최적화]:
     * - N+1 문제 방지: WHERE coupon.id IN (...) 사용
     * - 단일 쿼리로 모든 쿠폰 조회
     *
     * @param couponIds 쿠폰 ID 리스트
     * @return 쿠폰 Entity 리스트 (존재하는 것만 반환)
     */
    fun findByIdsBatch(couponIds: List<Long>): List<CouponEntity>
}
