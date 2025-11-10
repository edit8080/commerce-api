package com.beanbliss.domain.coupon.repository

import com.beanbliss.domain.coupon.entity.CouponTicketEntity
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * [책임]: CouponTicketRepository의 In-memory 구현
 * - 선착순 쿠폰 티켓 관리
 * - FOR UPDATE SKIP LOCKED 시뮬레이션
 */
@Repository
class CouponTicketRepositoryImpl : CouponTicketRepository {

    private val tickets = ConcurrentHashMap<Long, CouponTicketEntity>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    /**
     * 샘플 데이터 초기화
     * - 쿠폰 ID 1번에 대해 10개의 티켓 생성
     * - 쿠폰 ID 2번에 대해 5개의 티켓 생성
     */
    private fun initializeSampleData() {
        val now = LocalDateTime.now()

        // 쿠폰 1번: 10개 티켓 (모두 AVAILABLE)
        repeat(10) { index ->
            val ticketId = idGenerator.getAndIncrement()
            tickets[ticketId] = CouponTicketEntity(
                id = ticketId,
                couponId = 1L,
                status = "AVAILABLE",
                userId = null,
                userCouponId = null,
                issuedAt = null,
                createdAt = now.minusDays(1),
                updatedAt = now.minusDays(1)
            )
        }

        // 쿠폰 2번: 5개 티켓 (모두 AVAILABLE)
        repeat(5) { index ->
            val ticketId = idGenerator.getAndIncrement()
            tickets[ticketId] = CouponTicketEntity(
                id = ticketId,
                couponId = 2L,
                status = "AVAILABLE",
                userId = null,
                userCouponId = null,
                issuedAt = null,
                createdAt = now.minusDays(2),
                updatedAt = now.minusDays(2)
            )
        }

        // 쿠폰 3번: 3개 티켓 (모두 ISSUED - 품절)
        repeat(3) { index ->
            val ticketId = idGenerator.getAndIncrement()
            tickets[ticketId] = CouponTicketEntity(
                id = ticketId,
                couponId = 3L,
                status = "ISSUED",
                userId = 999L,
                userCouponId = 999L,
                issuedAt = now.minusHours(1),
                createdAt = now.minusDays(3),
                updatedAt = now.minusHours(1)
            )
        }
    }

    override fun findAvailableTicketWithLock(couponId: Long): CouponTicketEntity? {
        // FOR UPDATE SKIP LOCKED 시뮬레이션
        // AVAILABLE 상태이고 userId가 null인 티켓 중 첫 번째 반환
        return tickets.values
            .filter { it.couponId == couponId && it.status == "AVAILABLE" && it.userId == null }
            .minByOrNull { it.id!! } // ID 순서로 선점
    }

    override fun updateTicketAsIssued(ticketId: Long, userId: Long, userCouponId: Long) {
        val ticket = tickets[ticketId]
            ?: throw IllegalArgumentException("티켓을 찾을 수 없습니다: $ticketId")

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

    override fun saveAll(ticketList: List<CouponTicketEntity>): List<CouponTicketEntity> {
        val savedTickets = ticketList.map { ticket ->
            // ID가 null이면 새로운 ID 생성
            val savedTicket = if (ticket.id == null) {
                ticket.copy(id = idGenerator.getAndIncrement())
            } else {
                ticket
            }

            tickets[savedTicket.id!!] = savedTicket
            savedTicket
        }

        return savedTickets
    }
}
