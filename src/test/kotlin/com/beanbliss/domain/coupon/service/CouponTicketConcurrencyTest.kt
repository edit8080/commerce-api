package com.beanbliss.domain.coupon.service

import com.beanbliss.common.test.ConcurrencyTestBase
import com.beanbliss.domain.coupon.domain.DiscountType
import com.beanbliss.domain.coupon.entity.CouponEntity
import com.beanbliss.domain.coupon.entity.CouponTicketEntity
import com.beanbliss.domain.coupon.entity.CouponTicketStatus
import com.beanbliss.domain.user.entity.UserEntity
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap

/**
 * 쿠폰 발급 동시성 테스트 (UseCase 기반)
 *
 * [테스트 목표]:
 * 1. CouponIssueUseCase를 통한 전체 발급 플로우의 동시성 제어 검증
 * 2. FOR UPDATE SKIP LOCKED가 동시 요청에서 정확하게 작동하는지 확인
 * 3. Over-Allocation 방지 - 티켓 수량을 초과한 발급 방지
 * 4. 트랜잭션 범위 내에서 원자성 보장
 *
 * [테스트 시나리오]:
 * - 시나리오 1: 기본 발급 플로우 - 20명이 10장 티켓을 두고 발급 경쟁
 *   (FOR UPDATE SKIP LOCKED로 Race Condition 방지, 조회 + 상태 변경이 원자적으로 처리됨)
 * - 시나리오 2: 대량 동시 발급 - 200명이 100장 티켓을 두고 발급 경쟁
 *   (정확히 100명만 성공, 나머지는 Out of Stock 예외)
 * - 시나리오 3: 중복 발급 방지 - FOR UPDATE SKIP LOCKED로 각 티켓이 한 번만 발급됨
 *   (30명이 30장 티켓 경쟁, 모든 요청 성공, 중복 발급 없음)
 *
 * [아키텍처 레이어]:
 * - UseCase (CouponIssueUseCase): 전체 발급 플로우 조율
 *   → Service 호출 순서 제어
 *   → 트랜잭션 경계 설정
 * - Service (CouponTicketService): 비즈니스 로직 처리
 *   → findAvailableTicketWithLock(): FOR UPDATE SKIP LOCKED로 조회
 *   → issueTicketToUser(): 상태 변경 + 사용자 정보 기록
 * - Repository: 데이터 접근 계층
 */
@DisplayName("쿠폰 발급 동시성 테스트 (UseCase 기반)")
class CouponTicketConcurrencyTest : ConcurrencyTestBase() {

    @Autowired
    private lateinit var couponIssueUseCase: CouponIssueUseCase

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager
    private lateinit var transactionTemplate: TransactionTemplate

    private lateinit var testCoupon: CouponEntity
    private lateinit var testUserIds: List<Long>

    @BeforeEach
    fun setUp() {
        transactionTemplate = TransactionTemplate(transactionManager)
        cleanDatabase()
        testUserIds = createTestUsers()
        createTestCoupon()
    }

    private fun cleanDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0")
        jdbcTemplate.execute("TRUNCATE TABLE user_coupon")
        jdbcTemplate.execute("TRUNCATE TABLE coupon_ticket")
        jdbcTemplate.execute("TRUNCATE TABLE coupon")
        jdbcTemplate.execute("TRUNCATE TABLE user")
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1")
    }

    /**
     * 티켓 생성 공통 헬퍼 메서드
     */
    private fun createTickets(totalTickets: Int) {
        transactionTemplate.execute {
            val tickets = (1..totalTickets).map {
                CouponTicketEntity(
                    couponId = testCoupon.id,
                    status = CouponTicketStatus.AVAILABLE,
                    createdAt = LocalDateTime.now()
                )
            }
            tickets.forEach { entityManager.persist(it) }
            entityManager.flush()
        }
    }

    /**
     * 발급된 티켓 수 조회
     */
    private fun getIssuedTicketCount(): Int {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM coupon_ticket WHERE coupon_id = ? AND status = 'ISSUED'",
            Int::class.java,
            testCoupon.id
        ) ?: 0
    }

    /**
     * 남은 AVAILABLE 티켓 수 조회
     */
    private fun getRemainingAvailableTickets(): Int {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM coupon_ticket WHERE coupon_id = ? AND status = 'AVAILABLE'",
            Int::class.java,
            testCoupon.id
        ) ?: 0
    }

    private fun createTestUsers(): List<Long> {
        return transactionTemplate.execute {
        // 테스트용 사용자 미리 생성 (동시성 테스트에서 사용)
        // 최대 동시 사용자 수(200명)보다 많이 생성
        val testUsers = (0..250).map { index ->
            UserEntity(
                email = "testuser${1000 + index}@example.com",
                password = "password",
                name = "TestUser${1000 + index}",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            }
            testUsers.forEach { entityManager.persist(it) }
            entityManager.flush()

            // 실제 생성된 사용자 ID를 DB에서 조회하여 반환
            jdbcTemplate.queryForList("SELECT id FROM user ORDER BY id", Long::class.java)
        } ?: emptyList()
    }

    private fun createTestCoupon() {
        transactionTemplate.execute {
            // 테스트 쿠폰 생성 (기준: CouponEntity)
            testCoupon = CouponEntity(
                name = "선착순 쿠폰",
                discountType = DiscountType.FIXED_AMOUNT,
                discountValue = BigDecimal(5000),
                minOrderAmount = BigDecimal(10000),
                maxDiscountAmount = BigDecimal(5000),
                totalQuantity = 100,  // 각 테스트에서 필요한 만큼 티켓 생성
                validFrom = LocalDateTime.now().minusDays(1),
                validUntil = LocalDateTime.now().plusDays(30),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            entityManager.persist(testCoupon)
            entityManager.flush()
        }
    }

    @Test
    @DisplayName("시나리오 1: 기본 발급 플로우 - 10개 티켓을 25명이 경쟁")
    fun `티켓 발급_조회와 상태변경이 원자적으로 처리`() {
        // Given - 10개 티켓 생성
        val totalTickets = 10
        createTickets(totalTickets)

        val concurrentUsers = 25
        val executor = Executors.newFixedThreadPool(concurrentUsers)
        val latch = CountDownLatch(concurrentUsers)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // When - 25명이 동시에 CouponIssueUseCase.issueCoupon() 호출
        // 10개 티켓만 있으므로 10명은 성공, 15명은 예외 발생
        repeat(concurrentUsers) { userIndex ->
            executor.submit {
                try {
                    transactionTemplate.execute {
                        val userId = testUserIds[userIndex]
                        couponIssueUseCase.issueCoupon(testCoupon.id, userId)
                        successCount.incrementAndGet()
                    }
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // Then - 발급 결과 검증
        val issuedTicketsFromDb = jdbcTemplate.queryForList(
            "SELECT id FROM coupon_ticket WHERE coupon_id = ? AND status = 'ISSUED'",
            Long::class.java,
            testCoupon.id
        )
        val issuedTicketCount = issuedTicketsFromDb.size
        val uniqueIssuedTicketCount = issuedTicketsFromDb.toSet().size
        val remainingAvailableTickets = getRemainingAvailableTickets()

        // 검증
        assertEquals(totalTickets, successCount.get(), "정확히 $totalTickets 명만 성공해야 합니다")
        assertEquals(concurrentUsers - totalTickets, failCount.get(), "정확히 ${concurrentUsers - totalTickets}명은 실패해야 합니다")
        assertEquals(totalTickets, issuedTicketCount, "정확히 $totalTickets 개의 티켓이 ISSUED 상태여야 합니다")
        assertEquals(totalTickets, uniqueIssuedTicketCount, "발급된 티켓은 모두 유니크해야 합니다 (중복 발급 방지)")
        assertEquals(0, remainingAvailableTickets, "모든 AVAILABLE 티켓이 발급되어야 합니다")
    }

    @Test
    @DisplayName("시나리오 2: 대량 동시 발급 (UseCase) - 200명이 100장 티켓 경쟁")
    fun `대량 동시 발급_100명만 성공`() {
        // Given - 100개 티켓 생성
        val totalTickets = 100
        createTickets(totalTickets)

        val concurrentUsers = 200
        val executor = Executors.newFixedThreadPool(concurrentUsers)
        val latch = CountDownLatch(concurrentUsers)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // When - 200명이 동시에 CouponIssueUseCase.issueCoupon() 호출
        // 100개 티켓만 있으므로 100명은 성공, 100명은 예외 발생
        repeat(concurrentUsers) { userIndex ->
            executor.submit {
                try {
                    transactionTemplate.execute {
                        val userId = testUserIds[userIndex]
                        couponIssueUseCase.issueCoupon(testCoupon.id, userId)
                        successCount.incrementAndGet()
                    }
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executor.shutdown()

        // Then - 발급 결과 검증
        val issuedTickets = getIssuedTicketCount()
        val remainingAvailableTickets = getRemainingAvailableTickets()

        // 검증
        assertEquals(totalTickets, successCount.get(), "정확히 ${totalTickets}명만 성공해야 합니다")
        assertEquals(concurrentUsers - totalTickets, failCount.get(), "정확히 ${concurrentUsers - totalTickets}명은 실패해야 합니다")
        assertEquals(totalTickets, issuedTickets, "정확히 $totalTickets 개의 티켓이 ISSUED 상태여야 합니다")
        assertEquals(0, remainingAvailableTickets, "모든 AVAILABLE 티켓이 발급되어야 합니다")
    }

    @Test
    @DisplayName("시나리오 3: 중복 발급 방지 (UseCase) - FOR UPDATE SKIP LOCKED로 각 티켓이 한 번만 발급")
    fun `중복 발급 방지_각 티켓은 유니크한 사용자에게만 발급`() {
        // Given - 30개 티켓 생성
        val totalTickets = 30
        createTickets(totalTickets)

        val concurrentUsers = totalTickets
        val executor = Executors.newFixedThreadPool(concurrentUsers)
        val latch = CountDownLatch(concurrentUsers)
        val issuedUserIds = ConcurrentHashMap.newKeySet<Long>()
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // When - 30명이 동시에 CouponIssueUseCase.issueCoupon() 호출
        // FOR UPDATE SKIP LOCKED로 인해 각 티켓은 정확히 한 번만 조회됨
        // 각 요청은 SKIP LOCKED로 다른 요청이 락을 건 행을 건너뜀
        repeat(concurrentUsers) { userIndex ->
            executor.submit {
                try {
                    transactionTemplate.execute {
                        val userId = testUserIds[userIndex]
                        couponIssueUseCase.issueCoupon(testCoupon.id, userId)
                        issuedUserIds.add(userId)
                        successCount.incrementAndGet()
                    }
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executor.shutdown()

        // Then - 발급 결과 검증
        val issuedTickets = getIssuedTicketCount()
        val remainingAvailableTickets = getRemainingAvailableTickets()

        // 검증
        // FOR UPDATE SKIP LOCKED: 각 티켓이 정확히 한 번만 발급됨
        assertEquals(totalTickets, successCount.get(), "정확히 $totalTickets 명이 성공해야 합니다")
        assertEquals(0, failCount.get(), "실패한 요청이 없어야 합니다")
        assertEquals(totalTickets, issuedTickets, "정확히 $totalTickets 개의 티켓이 ISSUED 상태여야 합니다")
        assertEquals(0, remainingAvailableTickets, "모든 AVAILABLE 티켓이 발급되어야 합니다")
        assertEquals(totalTickets, issuedUserIds.size, "모든 티켓이 유니크한 사용자에게 발급되어야 합니다")
    }
}
