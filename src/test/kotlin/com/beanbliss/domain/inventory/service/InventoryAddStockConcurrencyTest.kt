package com.beanbliss.domain.inventory.service

import com.beanbliss.common.test.ConcurrencyTestBase
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
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

/**
 * 재고 추가 동시성 테스트
 *
 * [테스트 목표]:
 * 1. Lost Update 문제 검증 - 비관적 락 없이 동시에 재고를 추가할 때 발생
 *
 * [테스트 시나리오]:
 * - 시나리오 1: Lost Update - 두 관리자가 동시에 재고 추가
 * - 시나리오 2: 대량 동시 추가 - 여러 관리자가 동시에 재고 추가
 *
 * [TransactionTemplate 사용]:
 * - @Transactional 대신 TransactionTemplate 사용
 * - commit 시점을 명시적으로 제어
 */
@DisplayName("재고 추가 동시성 테스트")
class InventoryAddStockConcurrencyTest : ConcurrencyTestBase() {
    @Autowired
    private lateinit var inventoryService: InventoryService

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager
    private lateinit var transactionTemplate: TransactionTemplate

    private lateinit var testProduct: ProductEntity
    private lateinit var testOption: ProductOptionEntity

    @BeforeEach
    fun setUp() {
        transactionTemplate = TransactionTemplate(transactionManager)
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
        transactionTemplate.execute {
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

            // 초기 재고 10개
            val inventory = InventoryEntity(
                productOptionId = testOption.id,
                stockQuantity = 10,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            entityManager.persist(inventory)
            entityManager.flush()
            entityManager.clear()
        }
    }

    @Test
    @DisplayName("시나리오 1: Lost Update - 두 관리자가 동시에 100개, 50개 추가")
    fun `Lost Update 시나리오_두 관리자 동시 추가`() {
        // Given
        val executor = Executors.newFixedThreadPool(2)
        val latch = CountDownLatch(2)
        val results = mutableListOf<Int>()

        // When - 관리자 A: +100
        executor.submit {
            try {
                Thread.sleep(10)
                transactionTemplate.execute {
                    val result = inventoryService.addStock(testOption.id, 100)
                    synchronized(results) {
                        results.add(result)
                    }
                }
            } finally {
                latch.countDown()
            }
        }

        // 관리자 B: +50
        executor.submit {
            try {
                Thread.sleep(10)
                transactionTemplate.execute {
                    val result = inventoryService.addStock(testOption.id, 50)
                    synchronized(results) {
                        results.add(result)
                    }
                }
            } finally {
                latch.countDown()
            }
        }

        latch.await()
        executor.shutdown()

        // Then
        val finalStock = jdbcTemplate.queryForObject(
            "SELECT stock_quantity FROM inventory WHERE product_option_id = ?",
            Int::class.java,
            testOption.id
        )

        assertEquals(160, finalStock, "재고 추가가 정확히 반영되어야 합니다")
    }

    @Test
    @DisplayName("시나리오 2: 대량 동시 추가 - 10명이 동시에 10개씩 추가")
    fun `대량 동시 추가_Lost Update 검증`() {
        // Given
        val concurrentManagers = 10
        val addQuantity = 10
        val executor = Executors.newFixedThreadPool(concurrentManagers)
        val latch = CountDownLatch(concurrentManagers)
        val successCount = AtomicInteger(0)

        // When - 10명이 동시에 10개씩 추가
        repeat(concurrentManagers) {
            executor.submit {
                try {
                    transactionTemplate.execute {
                        inventoryService.addStock(testOption.id, addQuantity)
                        successCount.incrementAndGet()
                    }
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
            testOption.id
        )

        val expectedStock = 10 + (successCount.get() * addQuantity)
        assertEquals(expectedStock, finalStock, "재고 추가가 정확히 반영되어야 합니다")
    }
}
