package com.beanbliss.domain.inventory.service

import com.beanbliss.common.test.ConcurrencyTestBase
import com.beanbliss.domain.cart.domain.CartItemDetail
import com.beanbliss.domain.inventory.entity.InventoryEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import com.beanbliss.domain.inventory.repository.InventoryReservationRepository
import com.beanbliss.domain.order.exception.InsufficientAvailableStockException
import com.beanbliss.domain.product.entity.ProductEntity
import com.beanbliss.domain.product.entity.ProductOptionEntity
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
 * 재고 예약 동시성 테스트
 *
 * [테스트 목표]:
 * 1. Race Condition 검증 - 가용 재고 조회와 예약 생성 사이의 경쟁 조건
 * 2. Over-Reservation 방지 - 실제 재고를 초과한 예약 방지
 *
 * [테스트 시나리오]:
 * - 시나리오 1: Race Condition - 10명의 사용자가 동시에 각 10개씩 예약 시도 (총 재고: 50개)
 * - 시나리오 2: 가용 재고 부족 예외 - 재고 부족 시 일부만 성공
 * - 시나리오 3: 중복 예약 방지 - 동일 사용자의 중복 예약 시도
 */
@DisplayName("재고 예약 동시성 테스트")
@Transactional
class InventoryReservationConcurrencyTest : ConcurrencyTestBase() {

    @Autowired
    private lateinit var inventoryService: InventoryService

    @Autowired
    private lateinit var inventoryReservationRepository: InventoryReservationRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var testProduct: ProductEntity
    private lateinit var testOption: ProductOptionEntity

    @BeforeEach
    fun setUp() {
        cleanDatabase()
        createTestData()
    }

    private fun cleanDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0")
        jdbcTemplate.execute("TRUNCATE TABLE inventory_reservation")
        jdbcTemplate.execute("TRUNCATE TABLE inventory")
        jdbcTemplate.execute("TRUNCATE TABLE product_option")
        jdbcTemplate.execute("TRUNCATE TABLE product")
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1")
    }

    private fun createTestData() {
        // 테스트 상품 생성
        testProduct = ProductEntity(
            name = "테스트 원두",
            description = "동시성 테스트용",
            brand = "테스트 브랜드",
            createdAt = LocalDateTime.now()
        )
        entityManager.persist(testProduct)
        entityManager.flush()

        // 상품 옵션 생성
        testOption = ProductOptionEntity(
            productId = testProduct.id,
            optionCode = "OPT001",
            origin = "브라질",
            grindType = "홀빈",
            weightGrams = 200,
            price = BigDecimal(15000),
            isActive = true,
            createdAt = LocalDateTime.now()
        )
        entityManager.persist(testOption)
        entityManager.flush()

        // 초기 재고 50개
        val inventory = InventoryEntity(
            productOptionId = testOption.id,
            stockQuantity = 50,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(inventory)
        entityManager.flush()
        entityManager.clear()
    }

    @Test
    @DisplayName("시나리오 1: Race Condition - 10명이 동시에 10개씩 예약 시도 (총 재고: 50개)")
    fun `Race Condition_가용 재고 초과 예약 방지`() {
        // Given
        val concurrentUsers = 10
        val reserveQuantity = 10
        val executor = Executors.newFixedThreadPool(concurrentUsers)
        val latch = CountDownLatch(concurrentUsers)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // When - 10명이 동시에 10개씩 예약 시도
        repeat(concurrentUsers) { userId ->
            executor.submit {
                try {
                    val price = testOption.price.toInt()
                    val cartItems = listOf(
                        CartItemDetail(
                            cartItemId = 1L,
                            productOptionId = testOption.id,
                            quantity = reserveQuantity,
                            productName = testProduct.name,
                            optionCode = testOption.optionCode,
                            price = price,
                            origin = testOption.origin,
                            grindType = testOption.grindType,
                            weightGrams = testOption.weightGrams,
                            totalPrice = price * reserveQuantity,
                            createdAt = LocalDateTime.now(),
                            updatedAt = LocalDateTime.now()
                        )
                    )

                    inventoryService.reserveInventory(userId.toLong() + 1, cartItems)
                    successCount.incrementAndGet()
                } catch (e: InsufficientAvailableStockException) {
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

        // Then - 최종 검증
        val totalReserved = jdbcTemplate.queryForObject(
            """
            SELECT COALESCE(SUM(quantity), 0)
            FROM inventory_reservation
            WHERE product_option_id = ? AND status IN ('RESERVED', 'CONFIRMED')
            """,
            Int::class.java,
            testOption.id
        ) ?: 0

        // 검증
        assertTrue(totalReserved <= 50, "예약 수량이 재고를 초과하면 안 됩니다")
        assertEquals(successCount.get() * reserveQuantity, totalReserved, "성공 건수와 예약 수량이 일치해야 합니다")
    }

    @Test
    @DisplayName("시나리오 2: 가용 재고 부족 예외 - 20명이 동시에 5개씩 예약 시도 (총 재고: 50개)")
    fun `가용 재고 부족_일부만 성공`() {
        // Given
        val concurrentUsers = 20
        val reserveQuantity = 5
        val executor = Executors.newFixedThreadPool(concurrentUsers)
        val latch = CountDownLatch(concurrentUsers)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // When - 20명이 동시에 5개씩 예약 시도
        repeat(concurrentUsers) { userId ->
            executor.submit {
                try {
                    val price = testOption.price.toInt()
                    val cartItems = listOf(
                        CartItemDetail(
                            cartItemId = 1L,
                            productOptionId = testOption.id,
                            quantity = reserveQuantity,
                            productName = testProduct.name,
                            optionCode = testOption.optionCode,
                            price = price,
                            origin = testOption.origin,
                            grindType = testOption.grindType,
                            weightGrams = testOption.weightGrams,
                            totalPrice = price * reserveQuantity,
                            createdAt = LocalDateTime.now(),
                            updatedAt = LocalDateTime.now()
                        )
                    )

                    inventoryService.reserveInventory(userId.toLong() + 1, cartItems)
                    successCount.incrementAndGet()
                } catch (e: InsufficientAvailableStockException) {
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
        val totalReserved = jdbcTemplate.queryForObject(
            """
            SELECT COALESCE(SUM(quantity), 0)
            FROM inventory_reservation
            WHERE product_option_id = ? AND status IN ('RESERVED', 'CONFIRMED')
            """,
            Int::class.java,
            testOption.id
        ) ?: 0

        // 검증
        assertTrue(totalReserved <= 50, "예약 수량이 재고를 초과하면 안 됩니다")
        assertEquals(10, successCount.get(), "정확히 10명만 성공해야 합니다")
        assertEquals(10, failCount.get(), "정확히 10명은 실패해야 합니다")
    }

    @Test
    @DisplayName("시나리오 3: 중복 예약 방지 - 동일 사용자가 동시에 2번 예약 시도")
    fun `중복 예약 방지_동일 사용자 동시 예약`() {
        // Given
        val userId = 1L
        val executor = Executors.newFixedThreadPool(2)
        val latch = CountDownLatch(2)
        val successCount = AtomicInteger(0)
        val duplicateCount = AtomicInteger(0)

        // When - 동일 사용자가 동시에 2번 예약 시도
        repeat(2) { attempt ->
            executor.submit {
                try {
                    Thread.sleep(10) // 동시성 보장
                    val price = testOption.price.toInt()
                    val quantity = 10
                    val cartItems = listOf(
                        CartItemDetail(
                            cartItemId = 1L,
                            productOptionId = testOption.id,
                            quantity = quantity,
                            productName = testProduct.name,
                            optionCode = testOption.optionCode,
                            price = price,
                            origin = testOption.origin,
                            grindType = testOption.grindType,
                            weightGrams = testOption.weightGrams,
                            totalPrice = price * quantity,
                            createdAt = LocalDateTime.now(),
                            updatedAt = LocalDateTime.now()
                        )
                    )

                    inventoryService.reserveInventory(userId, cartItems)
                    successCount.incrementAndGet()
                } catch (e: com.beanbliss.domain.order.exception.DuplicateReservationException) {
                    duplicateCount.incrementAndGet()
                } catch (e: Exception) {
                    // Ignore other exceptions
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // Then
        val reservationCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM inventory_reservation
            WHERE user_id = ? AND status IN ('RESERVED', 'CONFIRMED')
            """,
            Int::class.java,
            userId
        ) ?: 0

        // 검증
        assertEquals(1, successCount.get(), "정확히 1번만 성공해야 합니다")
        assertEquals(1, duplicateCount.get(), "정확히 1번은 중복 예약으로 실패해야 합니다")
        assertEquals(1, reservationCount, "최종 예약 건수는 1건이어야 합니다")
    }

    @Test
    @DisplayName("시나리오 4: 복합 Race Condition - 여러 상품 옵션에 대한 동시 예약")
    fun `복합 Race Condition_여러 상품 옵션 동시 예약`() {
        // Given - 추가 상품 옵션 생성
        val testOption2 = ProductOptionEntity(
            productId = testProduct.id,
            optionCode = "OPT002",
            origin = "콜롬비아",
            grindType = "분쇄",
            weightGrams = 100,
            price = BigDecimal(12000),
            isActive = true,
            createdAt = LocalDateTime.now()
        )
        entityManager.persist(testOption2)
        entityManager.flush()

        val inventory2 = InventoryEntity(
            productOptionId = testOption2.id,
            stockQuantity = 30,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(inventory2)
        entityManager.flush()
        entityManager.clear()

        val concurrentUsers = 10
        val executor = Executors.newFixedThreadPool(concurrentUsers)
        val latch = CountDownLatch(concurrentUsers)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // When - 10명이 동시에 2개 옵션 예약 시도
        repeat(concurrentUsers) { userId ->
            executor.submit {
                try {
                    val price1 = testOption.price.toInt()
                    val quantity1 = 10
                    val price2 = testOption2.price.toInt()
                    val quantity2 = 5
                    val cartItems = listOf(
                        CartItemDetail(
                            cartItemId = 1L,
                            productOptionId = testOption.id,
                            quantity = quantity1,
                            productName = testProduct.name,
                            optionCode = testOption.optionCode,
                            price = price1,
                            origin = testOption.origin,
                            grindType = testOption.grindType,
                            weightGrams = testOption.weightGrams,
                            totalPrice = price1 * quantity1,
                            createdAt = LocalDateTime.now(),
                            updatedAt = LocalDateTime.now()
                        ),
                        CartItemDetail(
                            cartItemId = 2L,
                            productOptionId = testOption2.id,
                            quantity = quantity2,
                            productName = testProduct.name,
                            optionCode = testOption2.optionCode,
                            price = price2,
                            origin = testOption2.origin,
                            grindType = testOption2.grindType,
                            weightGrams = testOption2.weightGrams,
                            totalPrice = price2 * quantity2,
                            createdAt = LocalDateTime.now(),
                            updatedAt = LocalDateTime.now()
                        )
                    )

                    inventoryService.reserveInventory(userId.toLong() + 1, cartItems)
                    successCount.incrementAndGet()
                } catch (e: InsufficientAvailableStockException) {
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
        val totalReserved1 = jdbcTemplate.queryForObject(
            """
            SELECT COALESCE(SUM(quantity), 0)
            FROM inventory_reservation
            WHERE product_option_id = ? AND status IN ('RESERVED', 'CONFIRMED')
            """,
            Int::class.java,
            testOption.id
        ) ?: 0

        val totalReserved2 = jdbcTemplate.queryForObject(
            """
            SELECT COALESCE(SUM(quantity), 0)
            FROM inventory_reservation
            WHERE product_option_id = ? AND status IN ('RESERVED', 'CONFIRMED')
            """,
            Int::class.java,
            testOption2.id
        ) ?: 0

        // 검증
        assertTrue(totalReserved1 <= 50, "옵션 1 예약 수량이 재고를 초과하면 안 됩니다")
        assertTrue(totalReserved2 <= 30, "옵션 2 예약 수량이 재고를 초과하면 안 됩니다")
        assertEquals(successCount.get() * 10, totalReserved1, "옵션 1 성공 건수와 예약 수량이 일치해야 합니다")
        assertEquals(successCount.get() * 5, totalReserved2, "옵션 2 성공 건수와 예약 수량이 일치해야 합니다")
    }
}
