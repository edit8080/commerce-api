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
 * - 티켓 조회 및 선점 (FOR UPDATE SKIP LOCKED)
 * - 티켓을 사용자에게 발급 (상태 변경 + 사용자 정보 기록)
 *
 * [설계]:
 * - findAvailableTicketWithLock(): FOR UPDATE SKIP LOCKED로 티켓 조회
 *   (동시 요청은 자동으로 다음 티켓 조회)
 * - issueTicketToUser(): 티켓을 사용자에게 발급
 *   (상태 변경 AVAILABLE → ISSUED, 사용자 정보 기록)
 */
@Service
@Transactional(readOnly = true)
class CouponTicketService(
    private val couponTicketRepository: CouponTicketRepository
) {

    @Transactional
    fun createTickets(couponId: Long, totalQuantity: Int): List<CouponTicketEntity> {
        // 1. totalQuantity만큼 CouponTicketEntity 생성 (AVAILABLE 상태)
        val tickets = (1..totalQuantity).map {
            CouponTicketEntity(
                couponId = couponId,
                status = CouponTicketStatus.AVAILABLE
            )
        }

        // 2. 배치 삽입으로 저장
        return couponTicketRepository.saveAll(tickets)
    }

    @Transactional(readOnly = false)
    fun findAvailableTicketWithLock(couponId: Long): CouponTicketEntity {
        // FOR UPDATE SKIP LOCKED로 티켓 조회
        // - 이미 락이 걸린 행은 자동으로 건너뜀
        // - 조회된 티켓은 즉시 락 상태로 다른 요청 차단
        return couponTicketRepository.findAvailableTicketWithLock(couponId)
            ?: throw CouponOutOfStockException("쿠폰 재고가 부족합니다.")
    }

    @Transactional(readOnly = false)
    fun issueTicketToUser(ticketId: Long, userId: Long, userCouponId: Long) {
        // 티켓을 사용자에게 발급
        // - status: AVAILABLE → ISSUED로 변경
        // - userId, userCouponId, issuedAt 설정
        couponTicketRepository.issueTicketToUser(ticketId, userId, userCouponId)
    }
}
