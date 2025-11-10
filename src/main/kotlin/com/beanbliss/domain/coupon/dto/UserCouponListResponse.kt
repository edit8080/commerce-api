package com.beanbliss.domain.coupon.dto

import com.beanbliss.common.dto.PageableResponse

/**
 * 사용자 쿠폰 목록 응답 DTO
 *
 * [책임]:
 * - 사용자 쿠폰 목록과 페이징 정보를 함께 반환
 */
data class UserCouponListResponse(
    val data: UserCouponListData
)

data class UserCouponListData(
    val content: List<UserCouponResponse>,
    val pageable: PageableResponse
)
