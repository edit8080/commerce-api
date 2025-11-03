package com.beanbliss.domain.coupon.repository

import com.beanbliss.domain.coupon.entity.CouponTicketEntity
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * [책임]: CouponTicketRepository의 In-memory Fake 구현
 * - 단위 테스트를 위한 빠른 실행 환경 제공
 * - 동시성 제어를 위해 ConcurrentHashMap 사용
 */
class FakeCouponTicketRepository : CouponTicketRepository {

    private val tickets = ConcurrentHashMap<Long, CouponTicketEntity>()
    private val idGenerator = AtomicLong(1)

    // 테스트 헬퍼: 티켓 추가
    fun addTicket(ticket: CouponTicketEntity) {
        tickets[ticket.id] = ticket
    }

    // 테스트 헬퍼: 모든 티켓 삭제
    fun clear() {
        tickets.clear()
        idGenerator.set(1)
    }

    // 테스트 헬퍼: 티켓 조회 (테스트 검증용)
    fun getTicketById(id: Long): CouponTicketEntity? = tickets[id]

    override fun findAvailableTicketWithLock(couponId: Long): CouponTicketEntity? {
        // AVAILABLE 상태이고 userId가 null인 티켓 중 첫 번째 반환
        return tickets.values
            .filter { it.couponId == couponId && it.status == "AVAILABLE" && it.userId == null }
            .minByOrNull { it.id } // ID 순서로 선점 (SKIP LOCKED 시뮬레이션)
    }

    override fun updateTicketAsIssued(ticketId: Long, userId: Long, userCouponId: Long) {
        val ticket = tickets[ticketId] ?: throw IllegalArgumentException("티켓을 찾을 수 없습니다: $ticketId")

        val updatedTicket = ticket.copy(
            status = "ISSUED",
            userId = userId,
            userCouponId = userCouponId,
            issuedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        tickets[ticketId] = updatedTicket
    }

    override fun countAvailableTickets(couponId: Long): Int {
        return tickets.values.count {
            it.couponId == couponId && it.status == "AVAILABLE" && it.userId == null
        }
    }
}
