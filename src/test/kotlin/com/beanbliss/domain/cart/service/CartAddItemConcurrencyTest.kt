package com.beanbliss.domain.cart.service

import com.beanbliss.common.test.ConcurrencyTestBase
import com.beanbliss.domain.cart.entity.CartItemEntity
import com.beanbliss.domain.cart.repository.CartItemJpaRepository
import com.beanbliss.domain.product.entity.ProductEntity
import com.beanbliss.domain.product.entity.ProductOptionEntity
import com.beanbliss.domain.product.repository.ProductJpaRepository
import com.beanbliss.domain.product.repository.ProductOptionJpaRepository
import com.beanbliss.domain.user.entity.UserEntity
import com.beanbliss.domain.user.repository.UserJpaRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * 장바구니 추가 동시성 테스트
 *
 * [테스트 목표]:
 * 1. Lost Update 문제 검증 - 비관적 락으로 동시 수량 증가 시 정확성 보장
 * 2. 최대 수량 제한 검증 - 동시 추가 시에도 999개 제한 유지
 *
 * [테스트 시나리오]:
 * - 시나리오 1: Lost Update - 동일 사용자가 동일 상품을 동시에 10번 추가
 * - 시나리오 2: 대량 동시 추가 - 여러 사용자가 동시에 장바구니에 추가
 * - 시나리오 3: 최대 수량 제한 - 동시 추가 시 999개 초과 방지
 *
 * [동시성 제어]:
 * - 각 Service 호출이 독립적인 트랜잭션에서 실행됨
 * - Service 레이어의 @Transactional 애노테이션으로 트랜잭션 관리
 */
@DisplayName("장바구니 추가 동시성 테스트")
class CartAddItemConcurrencyTest : ConcurrencyTestBase() {

    @Autowired
    private lateinit var cartService: CartService

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var productJpaRepository: ProductJpaRepository

    @Autowired
    private lateinit var productOptionJpaRepository: ProductOptionJpaRepository

    @Autowired
    private lateinit var cartItemJpaRepository: CartItemJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    private lateinit var testProduct: ProductEntity
    private lateinit var testOption: ProductOptionEntity

    @BeforeEach
    fun setUp() {
        cleanDatabase()
        createTestData()
    }

    private fun cleanDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0")
        jdbcTemplate.execute("TRUNCATE TABLE cart_item")
        jdbcTemplate.execute("TRUNCATE TABLE product_option")
        jdbcTemplate.execute("TRUNCATE TABLE product")
        jdbcTemplate.execute("TRUNCATE TABLE user")
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1")
    }

    private fun createTestData() {
        transactionTemplate.execute {
            // 테스트 사용자 생성 (user_id 1-30)
            val users = (1..30).map { userId ->
                UserEntity(
                    email = "testuser$userId@example.com",
                    password = "password",
                    name = "TestUser$userId",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            }
            userJpaRepository.saveAll(users)

            // 테스트 상품 생성
            testProduct = ProductEntity(
                name = "테스트 원두",
                description = "동시성 테스트용",
                brand = "테스트 브랜드",
                createdAt = LocalDateTime.now()
            )
            productJpaRepository.save(testProduct)

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
            productOptionJpaRepository.save(testOption)
        }
    }

    @Test
    @DisplayName("시나리오 1: Lost Update - 동일 사용자가 동일 상품을 동시에 10번 추가")
    fun `Lost Update 시나리오_동일 상품 동시 추가`() {
        // Given
        val userId = 1L
        val concurrentRequests = 10
        val quantityPerRequest = 5
        val executor = Executors.newFixedThreadPool(concurrentRequests)
        val latch = CountDownLatch(concurrentRequests)
        val successCount = AtomicInteger(0)

        // 초기 장바구니 아이템 생성 (수량: 10)
        transactionTemplate.execute {
            val initialCartItem = CartItemEntity(
                id = 0L,
                userId = userId,
                productOptionId = testOption.id,
                quantity = 10,
                createdAt = LocalDateTime.now()
            )
            cartItemJpaRepository.save(initialCartItem)
        }

        // When - 10번 동시에 5개씩 추가
        repeat(concurrentRequests) { _ ->
            executor.submit {
                try {
                    cartService.addCartItem(userId, testOption.id, quantityPerRequest)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    println("[시나리오 1 실패] ${e.javaClass.simpleName}: ${e.message}")
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()

        executor.shutdown()

        // Then
        val cartItem = cartItemJpaRepository.findAll()
            .find { it.userId == userId && it.productOptionId == testOption.id }

        assertNotNull(cartItem, "장바구니 아이템이 존재해야 합니다")
        assertEquals(60, cartItem!!.quantity, "장바구니 수량 증가가 정확히 반영되어야 합니다")
    }

    @Test
    @DisplayName("시나리오 2: 대량 동시 추가 - 20명이 동시에 신규 아이템 추가")
    fun `대량 동시 추가_신규 아이템`() {
        // Given
        val concurrentUsers = 20
        val quantityPerUser = 3
        val executor = Executors.newFixedThreadPool(concurrentUsers)
        val latch = CountDownLatch(concurrentUsers)
        val successCount = AtomicInteger(0)

        // When - 20명이 동시에 신규 아이템 추가
        repeat(concurrentUsers) { userId ->
            executor.submit {
                try {
                    cartService.addCartItem(userId.toLong() + 1, testOption.id, quantityPerUser)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    println("[시나리오 2 실패] ${e.javaClass.simpleName}: ${e.message}")
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()

        executor.shutdown()

        // Then
        val totalCartItems = cartItemJpaRepository.findAll()
            .count { it.productOptionId == testOption.id }

        // 검증
        assertEquals(concurrentUsers, successCount.get(), "모든 요청이 성공해야 합니다")
        assertEquals(concurrentUsers, totalCartItems, "각 사용자마다 1개의 아이템이 생성되어야 합니다")
    }

    @Test
    @DisplayName("시나리오 3: 최대 수량 제한 - 동시 추가 시 999개 초과 방지")
    fun `최대 수량 제한_동시 추가 제한`() {
        // Given
        val userId = 1L
        val concurrentRequests = 5
        val quantityPerRequest = 300
        val executor = Executors.newFixedThreadPool(concurrentRequests)
        val latch = CountDownLatch(concurrentRequests)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // 초기 장바구니 아이템 생성 (수량: 500)
        transactionTemplate.execute {
            val initialCartItem = CartItemEntity(
                id = 0L,
                userId = userId,
                productOptionId = testOption.id,
                quantity = 500,
                createdAt = LocalDateTime.now()
            )
            cartItemJpaRepository.save(initialCartItem)
        }

        // When - 5번 동시에 300개씩 추가 시도
        repeat(concurrentRequests) { _ ->
            executor.submit {
                try {
                    cartService.addCartItem(userId, testOption.id, quantityPerRequest)
                    successCount.incrementAndGet()
                } catch (e: com.beanbliss.common.exception.InvalidParameterException) {
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
        val cartItem = cartItemJpaRepository.findAll()
            .find { it.userId == userId && it.productOptionId == testOption.id }

        assertNotNull(cartItem, "장바구니 아이템이 존재해야 합니다")
        assertTrue(cartItem!!.quantity <= 999, "최대 수량 999개를 초과하면 안 됩니다")
        assertTrue(successCount.get() >= 1, "최소 1건은 성공해야 합니다")
        assertTrue(failCount.get() >= 1, "최소 1건은 최대 수량 초과로 실패해야 합니다")
    }

    @Test
    @DisplayName("시나리오 4: 동일 사용자 다른 상품 옵션 동시 추가")
    fun `동일 사용자_다른 상품 옵션_동시 추가`() {
        // Given - 추가 상품 옵션 생성
        val testOption2 = transactionTemplate.execute {
            val option2 = ProductOptionEntity(
                productId = testProduct.id,
                optionCode = "OPT002",
                origin = "콜롬비아",
                grindType = "분쇄",
                weightGrams = 100,
                price = BigDecimal(12000),
                isActive = true,
                createdAt = LocalDateTime.now()
            )
            productOptionJpaRepository.save(option2)
            option2
        }!!

        val userId = 1L
        val concurrentRequests = 10
        val executor = Executors.newFixedThreadPool(concurrentRequests)
        val latch = CountDownLatch(concurrentRequests)
        val successCount = AtomicInteger(0)

        // When - 5번씩 각각 다른 옵션 추가
        repeat(5) { _ ->
            executor.submit {
                try {
                    cartService.addCartItem(userId, testOption.id, 3)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    println("[시나리오 4-옵션1 실패] ${e.javaClass.simpleName}: ${e.message}")
                } finally {
                    latch.countDown()
                }
            }
        }

        repeat(5) { _ ->
            executor.submit {
                try {
                    cartService.addCartItem(userId, testOption2.id, 2)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    println("[시나리오 4-옵션2 실패] ${e.javaClass.simpleName}: ${e.message}")
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()

        executor.shutdown()

        // Then
        val allCartItems = cartItemJpaRepository.findAll()

        val cartItem1 = allCartItems.find { it.userId == userId && it.productOptionId == testOption.id }
        val cartItem2 = allCartItems.find { it.userId == userId && it.productOptionId == testOption2.id }

        assertNotNull(cartItem1, "옵션 1 장바구니 아이템이 존재해야 합니다")
        assertNotNull(cartItem2, "옵션 2 장바구니 아이템이 존재해야 합니다")

        // 검증
        assertEquals(15, cartItem1!!.quantity, "옵션 1의 수량이 정확히 반영되어야 합니다")
        assertEquals(10, cartItem2!!.quantity, "옵션 2의 수량이 정확히 반영되어야 합니다")
    }
}
