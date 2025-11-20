package com.beanbliss.domain.order.usecase

import com.beanbliss.common.test.ConcurrencyTestBase
import com.beanbliss.domain.cart.entity.CartItemEntity
import com.beanbliss.domain.cart.repository.CartItemJpaRepository
import com.beanbliss.domain.inventory.entity.InventoryEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import com.beanbliss.domain.inventory.repository.InventoryJpaRepository
import com.beanbliss.domain.inventory.repository.InventoryReservationJpaRepository
import com.beanbliss.domain.order.repository.OrderJpaRepository
import com.beanbliss.domain.product.entity.ProductEntity
import com.beanbliss.domain.product.entity.ProductOptionEntity
import com.beanbliss.domain.product.repository.ProductJpaRepository
import com.beanbliss.domain.product.repository.ProductOptionJpaRepository
import com.beanbliss.domain.user.entity.BalanceEntity
import com.beanbliss.domain.user.entity.UserEntity
import com.beanbliss.domain.user.repository.BalanceJpaRepository
import com.beanbliss.domain.user.repository.UserJpaRepository
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
 * 주문 생성 동시성 테스트
 *
 * [테스트 목표]:
 * 1. 동일 사용자의 동시 주문 시도 - 트랜잭션과 비관적 락으로 데이터 일관성 보장
 * 2. 재고 부족 시나리오 - 동시 주문 시 재고 정확성 보장
 * 3. 잔액 부족 시나리오 - 동시 주문 시 잔액 정확성 보장
 *
 * [테스트 시나리오]:
 * - 시나리오 1: 동일 사용자가 동시에 2개 주문 시도 (잔액 부족)
 * - 시나리오 2: 2명의 사용자가 동시에 같은 상품 주문 (재고 부족)
 * - 시나리오 3: 전체 트랜잭션 롤백 검증 (중간 단계 실패 시)
 *
 * [동시성 제어]:
 * - UseCase 레벨의 @Transactional로 전체 주문 프로세스를 하나의 트랜잭션으로 관리
 * - 재고 차감: 비관적 락 (FOR UPDATE)
 * - 잔액 차감: 비관적 락 (FOR UPDATE)
 */
@DisplayName("주문 생성 동시성 테스트")
class CreateOrderConcurrencyTest : ConcurrencyTestBase() {

    @Autowired
    private lateinit var createOrderUseCase: CreateOrderUseCase

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var balanceJpaRepository: BalanceJpaRepository

    @Autowired
    private lateinit var productJpaRepository: ProductJpaRepository

    @Autowired
    private lateinit var productOptionJpaRepository: ProductOptionJpaRepository

    @Autowired
    private lateinit var inventoryJpaRepository: InventoryJpaRepository

    @Autowired
    private lateinit var cartItemJpaRepository: CartItemJpaRepository

    @Autowired
    private lateinit var inventoryReservationJpaRepository: InventoryReservationJpaRepository

    @Autowired
    private lateinit var orderJpaRepository: OrderJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var testProduct: ProductEntity
    private lateinit var testOption: ProductOptionEntity

    @BeforeEach
    @Transactional
    fun setUp() {
        cleanDatabase()
        createTestData()
    }

    private fun cleanDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0")
        jdbcTemplate.execute("DELETE FROM order_item")
        jdbcTemplate.execute("DELETE FROM `order`")
        jdbcTemplate.execute("DELETE FROM inventory_reservation")
        jdbcTemplate.execute("DELETE FROM cart_item")
        jdbcTemplate.execute("DELETE FROM balance")
        jdbcTemplate.execute("DELETE FROM user_coupon")
        jdbcTemplate.execute("DELETE FROM coupon")
        jdbcTemplate.execute("DELETE FROM inventory")
        jdbcTemplate.execute("DELETE FROM product_option")
        jdbcTemplate.execute("DELETE FROM product")
        jdbcTemplate.execute("DELETE FROM `user`")
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1")
    }

    private fun createTestData() {
        // 테스트 사용자 생성 (user_id 1-3)
        val users = (1..3).map { userId ->
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
            price = BigDecimal(10000),
            isActive = true,
            createdAt = LocalDateTime.now()
        )
        productOptionJpaRepository.save(testOption)

        // 재고 생성 (재고: 20개)
        val inventory = InventoryEntity(
            productOptionId = testOption.id,
            stockQuantity = 20,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        inventoryJpaRepository.save(inventory)
    }

    @Test
    @DisplayName("시나리오 1: 동일 사용자가 동시에 2개 주문 시도 - 잔액 부족으로 하나는 실패")
    fun `동일 사용자_동시 주문_잔액 부족`() {
        // Given
        val userId = 1L
        val initialBalance = 15000 // 15,000원 잔액
        val orderAmount = 10000 // 주문당 10,000원 (1개 × 10,000원)
        val quantity = 1

        // 사용자 잔액 설정
        val balance = BalanceEntity(
            userId = userId,
            amount = BigDecimal(initialBalance),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        balanceJpaRepository.save(balance)

        // 장바구니와 재고 예약을 미리 생성
        val now = LocalDateTime.now()
        val cartItem = CartItemEntity(
            userId = userId,
            productOptionId = testOption.id,
            quantity = quantity,
            createdAt = now
        )
        cartItemJpaRepository.save(cartItem)

        val reservation = InventoryReservationEntity(
            productOptionId = testOption.id,
            userId = userId,
            quantity = quantity,
            status = InventoryReservationStatus.RESERVED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )
        inventoryReservationJpaRepository.save(reservation)

        val concurrentRequests = 2
        val executor = Executors.newFixedThreadPool(concurrentRequests)
        val latch = CountDownLatch(concurrentRequests)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // When - 2번 동시에 주문 시도
        repeat(concurrentRequests) { index ->
            executor.submit {
                try {
                    // 주문 생성
                    createOrderUseCase.createOrder(
                        userId = userId,
                        userCouponId = null,
                        shippingAddress = "테스트 주소 $index"
                    )
                    successCount.incrementAndGet()
                    println("Order succeeded for request $index")
                } catch (e: Exception) {
                    println("Order failed for request $index: ${e.javaClass.simpleName} - ${e.message}")
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // Then
        val finalBalance = balanceJpaRepository.findByUserId(userId)
        val orders = orderJpaRepository.findAll().filter { it.userId == userId }

        // 검증: 하나는 성공, 하나는 실패
        assertEquals(1, successCount.get(), "1개 주문만 성공해야 합니다")
        assertEquals(1, failCount.get(), "1개 주문은 잔액 부족으로 실패해야 합니다")
        assertEquals(1, orders.size, "1개 주문만 생성되어야 합니다")
        assertEquals(0, BigDecimal(initialBalance - orderAmount).compareTo(finalBalance?.amount), "잔액은 정확히 1번 주문 금액만 차감되어야 합니다")
    }

    @Test
    @DisplayName("시나리오 2: 2명의 사용자가 동시에 같은 상품 주문 - 재고 부족으로 하나는 실패")
    fun `다수 사용자_동시 주문_재고 부족`() {
        // Given
        val user1Id = 1L
        val user2Id = 2L
        val quantity = 15 // 각각 15개씩 주문 시도 (재고 20개)

        // 사용자 잔액 설정
        val users = listOf(user1Id, user2Id)
        users.forEach { userId ->
            val balance = BalanceEntity(
                userId = userId,
                amount = BigDecimal(200000),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            balanceJpaRepository.save(balance)

            // 각 사용자에 대한 장바구니 및 재고 예약 생성
            createCartAndReservation(userId, testOption.id, quantity)
        }

        val concurrentRequests = 2
        val executor = Executors.newFixedThreadPool(concurrentRequests)
        val latch = CountDownLatch(concurrentRequests)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // When - 2명이 동시에 주문 시도
        users.forEachIndexed { index, userId ->
            executor.submit {
                try {
                    // 주문 생성
                    createOrderUseCase.createOrder(
                        userId = userId,
                        userCouponId = null,
                        shippingAddress = "테스트 주소 $index"
                    )
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    println("Order failed for user $userId: ${e.javaClass.simpleName} - ${e.message}")
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // Then
        val finalInventory = inventoryJpaRepository.findByProductOptionId(testOption.id)
        val orders = orderJpaRepository.findAll()

        // 검증: 하나는 성공, 하나는 실패
        assertEquals(1, successCount.get(), "1개 주문만 성공해야 합니다")
        assertEquals(1, failCount.get(), "1개 주문은 재고 부족으로 실패해야 합니다")
        assertEquals(1, orders.size, "1개 주문만 생성되어야 합니다")
        assertEquals(5, finalInventory?.stockQuantity, "재고는 정확히 15개만 차감되어야 합니다 (20 - 15 = 5)")
    }

    @Test
    @DisplayName("시나리오 3: 동시 주문 성공 케이스 - 충분한 재고와 잔액")
    fun `동시 주문_모두 성공`() {
        // Given
        val user1Id = 1L
        val user2Id = 2L
        val quantity = 5 // 각각 5개씩 주문 (재고 20개면 충분)

        // 사용자 잔액 설정
        val users = listOf(user1Id, user2Id)
        users.forEach { userId ->
            val balance = BalanceEntity(
                userId = userId,
                amount = BigDecimal(100000),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            balanceJpaRepository.save(balance)

            // 각 사용자에 대한 장바구니 및 재고 예약 생성
            createCartAndReservation(userId, testOption.id, quantity)
        }

        val concurrentRequests = 2
        val executor = Executors.newFixedThreadPool(concurrentRequests)
        val latch = CountDownLatch(concurrentRequests)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // When - 2명이 동시에 주문 시도
        users.forEachIndexed { index, userId ->
            executor.submit {
                try {
                    // 주문 생성
                    createOrderUseCase.createOrder(
                        userId = userId,
                        userCouponId = null,
                        shippingAddress = "테스트 주소 $index"
                    )
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    println("Order failed for user $userId: ${e.javaClass.simpleName} - ${e.message}")
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        // Then
        val finalInventory = inventoryJpaRepository.findByProductOptionId(testOption.id)
        val orders = orderJpaRepository.findAll()

        // 검증: 모두 성공
        assertEquals(2, successCount.get(), "2개 주문 모두 성공해야 합니다")
        assertEquals(0, failCount.get(), "실패한 주문이 없어야 합니다")
        assertEquals(2, orders.size, "2개 주문이 생성되어야 합니다")
        assertEquals(10, finalInventory?.stockQuantity, "재고는 정확히 10개 차감되어야 합니다 (20 - 10 = 10)")
    }

    /**
     * 장바구니와 재고 예약 생성 헬퍼 메서드
     * - 각 스레드에서 독립적으로 호출
     */
    @Transactional
    private fun createCartAndReservation(userId: Long, productOptionId: Long, quantity: Int) {
        val now = LocalDateTime.now()

        // 장바구니 아이템 생성
        val cartItem = CartItemEntity(
            userId = userId,
            productOptionId = productOptionId,
            quantity = quantity,
            createdAt = now
        )
        cartItemJpaRepository.save(cartItem)

        // 재고 예약 생성
        val reservation = InventoryReservationEntity(
            productOptionId = productOptionId,
            userId = userId,
            quantity = quantity,
            status = InventoryReservationStatus.RESERVED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )
        inventoryReservationJpaRepository.save(reservation)
    }
}