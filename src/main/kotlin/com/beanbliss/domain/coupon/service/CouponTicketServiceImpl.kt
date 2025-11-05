package com.beanbliss.domain.coupon.service

import com.beanbliss.domain.coupon.entity.CouponTicketEntity
import com.beanbliss.domain.coupon.exception.CouponOutOfStockException
import com.beanbliss.domain.coupon.repository.CouponTicketRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [책임]: 쿠폰 티켓 비즈니스 로직 처리
 * - 티켓 선점 (FOR UPDATE SKIP LOCKED)
 * - 티켓 상태 업데이트
 */
@Service
@Transactional(readOnly = true)
class CouponTicketServiceImpl(
    private val couponTicketRepository: CouponTicketRepository
) : CouponTicketService {

    @Transactional
    override fun reserveAvailableTicket(couponId: Long): CouponTicketEntity {
        return couponTicketRepository.findAvailableTicketWithLock(couponId)
            ?: throw CouponOutOfStockException("쿠폰 재고가 부족합니다.")
    }

    @Transactional
    override fun markTicketAsIssued(ticketId: Long, userId: Long, userCouponId: Long) {
        couponTicketRepository.updateTicketAsIssued(ticketId, userId, userCouponId)
    }
}
