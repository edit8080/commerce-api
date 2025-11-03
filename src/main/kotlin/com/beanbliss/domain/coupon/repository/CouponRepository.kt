package com.beanbliss.domain.coupon.repository

import java.time.LocalDateTime

/**
 * [책임]: 쿠폰 영속성 계층의 계약 정의
 * Service는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface CouponRepository {
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
}

/**
 * 남은 수량 정보를 포함한 쿠폰 데이터
 */
data class CouponWithQuantity(
    val id: Long,
    val name: String,
    val discountType: String,
    val discountValue: Int,
    val minOrderAmount: Int,
    val maxDiscountAmount: Int,
    val totalQuantity: Int,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val remainingQuantity: Int // COUPON_TICKET에서 계산된 값
)
