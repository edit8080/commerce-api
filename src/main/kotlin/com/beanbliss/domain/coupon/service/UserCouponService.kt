package com.beanbliss.domain.coupon.service

import com.beanbliss.domain.coupon.dto.UserCouponListResponse

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
}
