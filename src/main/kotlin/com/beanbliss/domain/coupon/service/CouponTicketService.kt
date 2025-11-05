package com.beanbliss.domain.coupon.service

import com.beanbliss.domain.coupon.entity.CouponTicketEntity

/**
 * [책임]: 쿠폰 티켓 관리 기능의 '계약' 정의
 * UseCase는 이 인터페이스에만 의존합니다. (DIP 준수)
 */
interface CouponTicketService {
    /**
     * 발급 가능한 티켓 선점 (FOR UPDATE SKIP LOCKED)
     *
     * @param couponId 쿠폰 ID
     * @return 선점된 쿠폰 티켓 Entity
     * @throws CouponOutOfStockException 티켓이 없는 경우 (재고 소진)
     */
    fun reserveAvailableTicket(couponId: Long): CouponTicketEntity

    /**
     * 티켓 상태를 'ISSUED'로 업데이트
     *
     * @param ticketId 티켓 ID
     * @param userId 사용자 ID
     * @param userCouponId 사용자 쿠폰 ID
     */
    fun markTicketAsIssued(ticketId: Long, userId: Long, userCouponId: Long)
}
