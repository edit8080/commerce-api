package com.beanbliss.domain.coupon.service

import com.beanbliss.common.test.ConcurrencyTestBase
import com.beanbliss.domain.coupon.domain.DiscountType
import com.beanbliss.domain.coupon.entity.CouponEntity
import com.beanbliss.domain.coupon.entity.CouponTicketEntity
import com.beanbliss.domain.coupon.entity.CouponTicketStatus
import com.beanbliss.domain.coupon.exception.CouponOutOfStockException
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * 쿠폰 티켓 동시성 테스트
 *
 * [테스트 목표]:
 * 1. Race Condition 검증 - FOR UPDATE SKIP LOCKED 동작 확인
 * 2. Over-Allocation 방지 - 티켓 수량을 초과한 발급 방지
 * 3. 중복 티켓 발급 방지 - 동일 티켓이 여러 사용자에게 발급되지 않도록
 *
 * [테스트 시나리오]:
 * - 시나리오 1: Race Condition - 100명이 50장 티켓 선착순 발급
 * - 시나리오 2: 티켓 고갈 - 모든 티켓 소진 후 예외 처리
 * - 시나리오 3: 대량 동시 발급 - 200명이 100장 티켓 경쟁
 */
@DisplayName("쿠폰 티켓 동시성 테스트")
@Transactional
class CouponTicketConcurrencyTest : ConcurrencyTestBase() {

    @Autowired
    private lateinit var couponTicketService: CouponTicketService

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var testCoupon: CouponEntity

    @BeforeEach
    fun setUp() {
        cleanDatabase()
        createTestData()
    }

    private fun cleanDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0")
        jdbcTemplate.execute("TRUNCATE TABLE coupon_ticket")
        jdbcTemplate.execute("TRUNCATE TABLE coupon")
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1")
    }

    private fun createTestData() {
        // 테스트 쿠폰 생성
        testCoupon = CouponEntity(
            name = "선착순 쿠폰",
            discountType = DiscountType.FIXED_AMOUNT,
            discountValue = BigDecimal(5000),
            minOrderAmount = BigDecimal(10000),
            maxDiscountAmount = BigDecimal(5000),
            totalQuantity = 50,
            validFrom = LocalDateTime.now().minusDays(1),
            validUntil = LocalDateTime.now().plusDays(30),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(testCoupon)
        entityManager.flush()
    }

    @Test
    @DisplayName("시나리오 1: Race Condition - 100명이 50장 티켓 선착순 발급")
    fun `Race Condition_선착순 티켓 발급`() {
        // Given - 50개 티켓 생성
        val totalTickets = 50
        val tickets = (1..totalTickets).map {
            CouponTicketEntity(
                couponId = testCoupon.id,
                status = CouponTicketStatus.AVAILABLE,
                createdAt = LocalDateTime.now()
            )
        }
        tickets.forEach { entityManager.persist(it) }
        entityManager.flush()
        entityManager.clear()

        val concurrentUsers = 100
        val executor = Executors.newFixedThreadPool(concurrentUsers)
        val latch = CountDownLatch(concurrentUsers)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // When - 100명이 동시에 티켓 예약 시도
        repeat(concurrentUsers) { userId ->
            executor.submit {
                try {
                    couponTicketService.reserveAvailableTicket(testCoupon.id)
                    successCount.incrementAndGet()
                } catch (e: CouponOutOfStockException) {
                    failCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executor.shutdown()

        // Then
        val issuedTickets = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM coupon_ticket WHERE coupon_id = ? AND status = 'AVAILABLE' AND user_id IS NULL",
            Int::class.java,
            testCoupon.id
        ) ?: 0

        // 검증
        assertEquals(totalTickets, successCount.get(), "정확히 50명만 성공해야 합니다")
        assertEquals(50, failCount.get(), "정확히 50명은 실패해야 합니다")
        assertEquals(0, issuedTickets, "모든 티켓이 예약되어야 합니다")
    }

    @Test
    @DisplayName("시나리오 2: 티켓 고갈 - 모든 티켓 소진 후 예외 처리")
    fun `티켓 고갈_예외 처리`() {
        // Given - 10개 티켓 생성
        val totalTickets = 10
        val tickets = (1..totalTickets).map {
            CouponTicketEntity(
                couponId = testCoupon.id,
                status = CouponTicketStatus.AVAILABLE,
                createdAt = LocalDateTime.now()
            )
        }
        tickets.forEach { entityManager.persist(it) }
        entityManager.flush()
        entityManager.clear()

        val concurrentUsers = 20
        val executor = Executors.newFixedThreadPool(concurrentUsers)
        val latch = CountDownLatch(concurrentUsers)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // When - 20명이 동시에 티켓 예약 시도
        repeat(concurrentUsers) { userId ->
            executor.submit {
                try {
                    couponTicketService.reserveAvailableTicket(testCoupon.id)
                    successCount.incrementAndGet()
                } catch (e: CouponOutOfStockException) {
                    failCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executor.shutdown()

        // Then
        val remainingTickets = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM coupon_ticket WHERE coupon_id = ? AND status = 'AVAILABLE' AND user_id IS NULL",
            Int::class.java,
            testCoupon.id
        ) ?: 0

        // 검증
        assertEquals(totalTickets, successCount.get(), "정확히 10명만 성공해야 합니다")
        assertEquals(10, failCount.get(), "정확히 10명은 실패해야 합니다")
        assertEquals(0, remainingTickets, "모든 티켓이 소진되어야 합니다")
    }

    @Test
    @DisplayName("시나리오 3: 대량 동시 발급 - 200명이 100장 티켓 경쟁")
    fun `대량 동시 발급_티켓 경쟁`() {
        // Given - 100개 티켓 생성
        val totalTickets = 100
        val tickets = (1..totalTickets).map {
            CouponTicketEntity(
                couponId = testCoupon.id,
                status = CouponTicketStatus.AVAILABLE,
                createdAt = LocalDateTime.now()
            )
        }
        tickets.forEach { entityManager.persist(it) }
        entityManager.flush()
        entityManager.clear()

        val concurrentUsers = 200
        val executor = Executors.newFixedThreadPool(concurrentUsers)
        val latch = CountDownLatch(concurrentUsers)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // When - 200명이 동시에 티켓 예약 시도
        repeat(concurrentUsers) { userId ->
            executor.submit {
                try {
                    couponTicketService.reserveAvailableTicket(testCoupon.id)
                    successCount.incrementAndGet()
                } catch (e: CouponOutOfStockException) {
                    failCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executor.shutdown()

        // Then
        val remainingTickets = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM coupon_ticket WHERE coupon_id = ? AND status = 'AVAILABLE' AND user_id IS NULL",
            Int::class.java,
            testCoupon.id
        ) ?: 0

        // 검증
        assertEquals(totalTickets, successCount.get(), "정확히 100명만 성공해야 합니다")
        assertEquals(100, failCount.get(), "정확히 100명은 실패해야 합니다")
        assertEquals(0, remainingTickets, "모든 티켓이 소진되어야 합니다")
    }

    @Test
    @DisplayName("시나리오 4: 중복 티켓 발급 방지 - 각 티켓은 한 명에게만 발급")
    fun `중복 티켓 발급 방지_유니크 티켓 보장`() {
        // Given - 30개 티켓 생성
        val totalTickets = 30
        val tickets = (1..totalTickets).map {
            CouponTicketEntity(
                couponId = testCoupon.id,
                status = CouponTicketStatus.AVAILABLE,
                createdAt = LocalDateTime.now()
            )
        }
        tickets.forEach { entityManager.persist(it) }
        entityManager.flush()
        entityManager.clear()

        val concurrentUsers = 30
        val executor = Executors.newFixedThreadPool(concurrentUsers)
        val latch = CountDownLatch(concurrentUsers)
        val reservedTicketIds = mutableSetOf<Long>()

        // When - 30명이 동시에 티켓 예약 시도
        repeat(concurrentUsers) { userId ->
            executor.submit {
                try {
                    val ticket = couponTicketService.reserveAvailableTicket(testCoupon.id)
                    synchronized(reservedTicketIds) {
                        reservedTicketIds.add(ticket.id)
                    }
                } catch (e: Exception) {
                    // Ignore exceptions
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executor.shutdown()

        // Then
        // 검증: 예약된 티켓 ID가 모두 유니크해야 함
        assertEquals(totalTickets, reservedTicketIds.size, "모든 티켓이 유니크하게 발급되어야 합니다")
    }
}
