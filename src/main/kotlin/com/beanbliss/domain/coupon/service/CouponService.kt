package com.beanbliss.domain.coupon.service

import com.beanbliss.domain.coupon.dto.CouponListResponse

/**
 * [책임]: 쿠폰 비즈니스 로직의 계약 정의
 * Controller는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface CouponService {
    /**
     * 쿠폰 목록 조회
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 쿠폰 목록 응답 (페이징 정보 포함)
     */
    fun getCoupons(page: Int, size: Int): CouponListResponse
}
