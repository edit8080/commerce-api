package com.beanbliss.domain.coupon.service

import com.beanbliss.domain.coupon.entity.CouponTicketEntity
import com.beanbliss.domain.coupon.entity.CouponTicketStatus
import com.beanbliss.domain.coupon.exception.CouponOutOfStockException
import com.beanbliss.domain.coupon.repository.CouponTicketRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [책임]: 쿠폰 티켓 비즈니스 로직 처리
 * - 티켓 일괄 생성
 * - 티켓 선점 (FOR UPDATE SKIP LOCKED)
 * - 티켓 상태 업데이트
 */
@Service
@Transactional(readOnly = true)
class CouponTicketService(
    private val couponTicketRepository: CouponTicketRepository
) {

    @Transactional
    fun createTickets(couponId: Long, totalQuantity: Int): List<CouponTicketEntity> {
        // 1. totalQuantity만큼 CouponTicketEntity 생성
        val tickets = (1..totalQuantity).map {
            CouponTicketEntity(
                couponId = couponId,
                status = CouponTicketStatus.AVAILABLE
            )
        }

        // 2. 배치 삽입으로 저장
        return couponTicketRepository.saveAll(tickets)
    }

    @Transactional
    fun reserveAvailableTicket(couponId: Long): CouponTicketEntity {
        return couponTicketRepository.findAvailableTicketWithLock(couponId)
            ?: throw CouponOutOfStockException("쿠폰 재고가 부족합니다.")
    }

    @Transactional
    fun markTicketAsIssued(ticketId: Long, userId: Long, userCouponId: Long) {
        couponTicketRepository.updateTicketAsIssued(ticketId, userId, userCouponId)
    }
}
