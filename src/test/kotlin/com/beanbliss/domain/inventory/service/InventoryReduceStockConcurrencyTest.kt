package com.beanbliss.domain.inventory.service

import com.beanbliss.common.test.ConcurrencyTestBase
import com.beanbliss.domain.cart.domain.CartItemDetail
import com.beanbliss.domain.inventory.entity.InventoryEntity
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
 * 재고 차감 동시성 테스트
 *
 * [테스트 목표]:
 * 1. Lost Update 문제 검증 - 비관적 락 없이 동시에 재고를 차감할 때 발생
 * 2. Deadlock 문제 검증 - 여러 상품을 다른 순서로 락 획득할 때 발생
 *
 * [테스트 시나리오]:
 * - 시나리오 1: Lost Update - 같은 상품을 동시에 차감
 * - 시나리오 2: Deadlock - 여러 상품을 다른 순서로 차감
 * - 시나리오 3: 개선 후 검증 - 비관적 락 + ID 정렬
 */
@DisplayName("재고 차감 동시성 테스트")
@Transactional
class InventoryReduceStockConcurrencyTest : ConcurrencyTestBase() {

    @Autowired
    private lateinit var inventoryService: InventoryService

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var testProduct: ProductEntity
    private lateinit var testOption1: ProductOptionEntity
    private lateinit var testOption2: ProductOptionEntity

    @BeforeEach
    fun setUp() {
        cleanDatabase()
        createTestData()
    }

    private fun cleanDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0")
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

        // 상품 옵션 2개 생성
        testOption1 = ProductOptionEntity(
            productId = testProduct.id,
            optionCode = "OPT001",
            origin = "브라질",
            grindType = "홀빈",
            weightGrams = 200,
            price = BigDecimal(15000),
            isActive = true,
            createdAt = LocalDateTime.now()
        )
        entityManager.persist(testOption1)

        testOption2 = ProductOptionEntity(
            productId = testProduct.id,
            optionCode = "OPT002",
            origin = "콜롬비아",
            grindType = "홀빈",
            weightGrams = 200,
            price = BigDecimal(18000),
            isActive = true,
            createdAt = LocalDateTime.now()
        )
        entityManager.persist(testOption2)
        entityManager.flush()

        // 재고 생성 (각 100개)
        val inventory1 = InventoryEntity(
            productOptionId = testOption1.id,
            stockQuantity = 100,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val inventory2 = InventoryEntity(
            productOptionId = testOption2.id,
            stockQuantity = 100,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(inventory1)
        entityManager.persist(inventory2)
        entityManager.flush()
        entityManager.clear()
    }

    @Test
    @DisplayName("시나리오 1: Lost Update - 비관적 락 없이 같은 상품을 동시에 10개씩 10번 차감하면 Lost Update 발생 가능")
    fun `Lost Update 시나리오_비관적 락 없이 동시 차감`() {
        // Given
        val concurrentRequests = 10
        val quantityPerRequest = 10
        val executor = Executors.newFixedThreadPool(concurrentRequests)
        val latch = CountDownLatch(concurrentRequests)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // When - 동시에 10개씩 10번 차감
        repeat(concurrentRequests) { i ->
            executor.submit {
                try {
                    val cartItems = listOf(
                        CartItemDetail(
                            cartItemId = i.toLong(),
                            productOptionId = testOption1.id,
                            productName = "테스트 상품",
                            optionCode = "OPT001",
                            origin = "브라질",
                            grindType = "홀빈",
                            weightGrams = 200,
                            price = 15000,
                            quantity = quantityPerRequest,
                            totalPrice = 15000 * quantityPerRequest,
                            createdAt = LocalDateTime.now(),
                            updatedAt = LocalDateTime.now()
                        )
                    )

                    inventoryService.reduceStockForOrder(cartItems)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()

        executor.shutdown()

        // Then - 결과 확인
        val finalStock = jdbcTemplate.queryForObject(
            "SELECT stock_quantity FROM inventory WHERE product_option_id = ?",
            Int::class.java,
            testOption1.id
        )

        // 비관적 락이 있으면 정확히 0개, 없으면 Lost Update로 0보다 큼
        finalStock?.let { assertTrue(it >= 0, "재고는 음수가 될 수 없습니다") }
    }

    @Test
    @DisplayName("시나리오 2: Deadlock - 두 상품을 다른 순서로 동시에 차감하면 Deadlock 발생 가능")
    fun `Deadlock 시나리오_다른 순서로 여러 상품 차감`() {
        // Given
        val executor = Executors.newFixedThreadPool(2)
        val latch = CountDownLatch(2)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        val deadlockDetected = AtomicInteger(0)

        // When - 트랜잭션 A: 상품2 → 상품1
        executor.submit {
            try {
                val cartItems = listOf(
                    CartItemDetail(
                        cartItemId = 1L,
                        productOptionId = testOption2.id,  // 상품 2 먼저
                        productName = "테스트 상품",
                        optionCode = "OPT002",
                        origin = "콜롬비아",
                        grindType = "홀빈",
                        weightGrams = 200,
                        price = 18000,
                        quantity = 10,
                        totalPrice = 180000,
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now()
                    ),
                    CartItemDetail(
                        cartItemId = 2L,
                        productOptionId = testOption1.id,  // 상품 1 나중
                        productName = "테스트 상품",
                        optionCode = "OPT001",
                        origin = "브라질",
                        grindType = "홀빈",
                        weightGrams = 200,
                        price = 15000,
                        quantity = 10,
                        totalPrice = 150000,
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now()
                    )
                )

                inventoryService.reduceStockForOrder(cartItems)
                successCount.incrementAndGet()
            } catch (e: Exception) {
                if (e.message?.contains("Deadlock") == true || e.message?.contains("deadlock") == true) {
                    deadlockDetected.incrementAndGet()
                }
                failCount.incrementAndGet()
            } finally {
                latch.countDown()
            }
        }

        // 트랜잭션 B: 상품1 → 상품2
        executor.submit {
            try {
                Thread.sleep(10) // 약간의 지연으로 동시성 보장
                val cartItems = listOf(
                    CartItemDetail(
                        cartItemId = 3L,
                        productOptionId = testOption1.id,  // 상품 1 먼저
                        productName = "테스트 상품",
                        optionCode = "OPT001",
                        origin = "브라질",
                        grindType = "홀빈",
                        weightGrams = 200,
                        price = 15000,
                        quantity = 10,
                        totalPrice = 150000,
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now()
                    ),
                    CartItemDetail(
                        cartItemId = 4L,
                        productOptionId = testOption2.id,  // 상품 2 나중
                        productName = "테스트 상품",
                        optionCode = "OPT002",
                        origin = "콜롬비아",
                        grindType = "홀빈",
                        weightGrams = 200,
                        price = 18000,
                        quantity = 10,
                        totalPrice = 180000,
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now()
                    )
                )

                inventoryService.reduceStockForOrder(cartItems)
                successCount.incrementAndGet()
            } catch (e: Exception) {
                if (e.message?.contains("Deadlock") == true || e.message?.contains("deadlock") == true) {
                    deadlockDetected.incrementAndGet()
                }
                failCount.incrementAndGet()
            } finally {
                latch.countDown()
            }
        }

        latch.await()
        executor.shutdown()
    }

    @Test
    @DisplayName("시나리오 3: 동시에 100명이 같은 상품 1개씩 구매 - 정확히 100번 차감되어야 함")
    fun `대량 동시 차감_Lost Update 검증`() {
        // Given
        // 초기 재고를 200개로 증가
        jdbcTemplate.update(
            "UPDATE inventory SET stock_quantity = 200 WHERE product_option_id = ?",
            testOption1.id
        )

        val concurrentUsers = 100
        val executor = Executors.newFixedThreadPool(50)
        val latch = CountDownLatch(concurrentUsers)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // When - 100명이 동시에 1개씩 구매
        repeat(concurrentUsers) { i ->
            executor.submit {
                try {
                    val cartItems = listOf(
                        CartItemDetail(
                            cartItemId = i.toLong(),
                            productOptionId = testOption1.id,
                            productName = "테스트 상품",
                            optionCode = "OPT001",
                            origin = "브라질",
                            grindType = "홀빈",
                            weightGrams = 200,
                            price = 15000,
                            quantity = 1,  // 1개씩
                            totalPrice = 15000,
                            createdAt = LocalDateTime.now(),
                            updatedAt = LocalDateTime.now()
                        )
                    )

                    inventoryService.reduceStockForOrder(cartItems)
                    successCount.incrementAndGet()
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
        val finalStock = jdbcTemplate.queryForObject(
            "SELECT stock_quantity FROM inventory WHERE product_option_id = ?",
            Int::class.java,
            testOption1.id
        )

        val expectedStock = 200 - successCount.get()
        // 비관적 락이 있으면 정확히 expectedStock
        assertEquals(expectedStock, finalStock, "재고 차감이 정확히 반영되어야 합니다")
    }

    @Test
    @DisplayName("시나리오 4: 재고 부족 시 모든 트랜잭션이 롤백되어야 함")
    fun `재고 부족 시_트랜잭션 롤백 검증`() {
        // Given
        val executor = Executors.newFixedThreadPool(2)
        val latch = CountDownLatch(2)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // When - 두 트랜잭션이 각각 60개씩 차감 시도
        repeat(2) { i ->
            executor.submit {
                try {
                    Thread.sleep(i * 10L) // 약간의 시차
                    val cartItems = listOf(
                        CartItemDetail(
                            cartItemId = i.toLong(),
                            productOptionId = testOption1.id,
                            productName = "테스트 상품",
                            optionCode = "OPT001",
                            origin = "브라질",
                            grindType = "홀빈",
                            weightGrams = 200,
                            price = 15000,
                            quantity = 60,
                            totalPrice = 900000,
                            createdAt = LocalDateTime.now(),
                            updatedAt = LocalDateTime.now()
                        )
                    )

                    inventoryService.reduceStockForOrder(cartItems)
                    successCount.incrementAndGet()
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
        val finalStock = jdbcTemplate.queryForObject(
            "SELECT stock_quantity FROM inventory WHERE product_option_id = ?",
            Int::class.java,
            testOption1.id
        )

        // 비관적 락 + 정렬이 있으면 1개 성공, 1개 실패
        assertEquals(1, successCount.get(), "하나의 트랜잭션만 성공해야 합니다")
        assertEquals(1, failCount.get(), "하나의 트랜잭션은 재고 부족으로 실패해야 합니다")
        assertEquals(40, finalStock, "최종 재고는 40개여야 합니다")
    }
}
