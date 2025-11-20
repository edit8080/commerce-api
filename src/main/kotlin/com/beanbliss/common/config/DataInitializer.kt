package com.beanbliss.common.config

import com.beanbliss.domain.cart.entity.CartItemEntity
import com.beanbliss.domain.coupon.domain.DiscountType
import com.beanbliss.domain.coupon.entity.CouponEntity
import com.beanbliss.domain.coupon.entity.CouponTicketEntity
import com.beanbliss.domain.coupon.entity.CouponTicketStatus
import com.beanbliss.domain.inventory.entity.InventoryEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import com.beanbliss.domain.order.entity.OrderEntity
import com.beanbliss.domain.order.entity.OrderItemEntity
import com.beanbliss.domain.order.entity.OrderStatus
import com.beanbliss.domain.product.entity.ProductEntity
import com.beanbliss.domain.product.entity.ProductOptionEntity
import com.beanbliss.domain.user.entity.BalanceEntity
import com.beanbliss.domain.user.entity.UserEntity
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * [책임]: 애플리케이션 시작 시 더미 데이터를 DB에 추가
 * dev 프로파일에서만 실행되며, 각 도메인별로 100건의 더미 데이터를 생성합니다.
 *
 * [실행 시점]:
 * - ApplicationReadyEvent: DB 설정 완료 후 실행
 * - @Profile("dev"): dev 프로파일에서만 동작
 *
 * [생성 데이터]:
 * - User (100명) + Balance
 * - Product (100개) + ProductOption (3개씩) + Inventory
 * - Coupon (100개) + CouponTicket (100개씩)
 * - Cart (100개)
 * - Order (100개) + OrderItem + InventoryReservation
 */
@Component
@org.springframework.context.annotation.Profile("dev")
class DataInitializer(
    private val entityManager: EntityManager
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BATCH_SIZE = 20
        private const val TOTAL_SIZE = 100

        private val COFFEE_BRANDS = listOf(
            "라보나", "스타벅스", "롯데칸타타", "이디야", "카페베네",
            "탐앤탐스", "투썸플레이스", "할리스", "커피빈", "엔제리너스",
            "공차", "설빙", "빽다방", "카페베이", "서울브로스"
        )

        private val GRIND_TYPES = listOf("분쇄", "홀빈", "에스프레소")
        private val ORIGINS = listOf("에티오피아", "브라질", "콜롬비아", "인도네시아", "과테말라", "케냐", "파나마")
    }

    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun initializeData() {
        log.info("🚀 Starting dummy data initialization for dev profile...")

        try {
            if (isDataAlreadyInitialized()) {
                log.info("✓ Dummy data already exists, skipping initialization")
                return
            }

            val startTime = System.currentTimeMillis()

            // 1. Create Users (100) + Balance
            val users = createUsers()
            log.info("✓ Created ${users.size} users")

            // 2. Create Products (100) + ProductOptions (3 each) + Inventory
            val productOptions = createProductsAndOptions()
            log.info("✓ Created ${productOptions.size} product options")

            // 3. Create Coupons (100) + CouponTickets (100 each)
            val coupons = createCouponsAndTickets()
            log.info("✓ Created ${coupons.size} coupons with tickets")

            // 4. Create CartItems (100)
            val cartItems = createCartItems(users, productOptions)
            log.info("✓ Created ${cartItems.size} cart items")

            // 5. Create Orders (100) + OrderItems + InventoryReservations
            val orders = createOrdersWithItems(users, productOptions)
            log.info("✓ Created ${orders.size} orders with items")

            val elapsedTime = System.currentTimeMillis() - startTime
            log.info("✅ Dummy data initialization completed in ${elapsedTime}ms")

        } catch (e: Exception) {
            log.error("❌ Failed to initialize dummy data", e)
            throw e
        }
    }

    private fun isDataAlreadyInitialized(): Boolean {
        val userCount = entityManager.createQuery("SELECT COUNT(u) FROM UserEntity u", Long::class.java).singleResult
        return userCount > 0
    }

    // ============================================================================
    // 1. User 생성 (100명) + Balance
    // ============================================================================
    private fun createUsers(): List<UserEntity> {
        val users = mutableListOf<UserEntity>()

        for (i in 1..TOTAL_SIZE) {
            val user = UserEntity(
                email = "user$i@beanbliss.com",
                password = "password$i",
                name = "사용자$i"
            )
            entityManager.persist(user)
            users.add(user)

            // Balance 생성 (초기 잔액 100,000원)
            val balance = BalanceEntity(
                userId = user.id,
                amount = BigDecimal("100000.00")
            )
            entityManager.persist(balance)

            if (i % BATCH_SIZE == 0) {
                entityManager.flush()
                entityManager.clear()
            }
        }

        entityManager.flush()
        return users
    }

    // ============================================================================
    // 2. Product 생성 (100개) + ProductOption (3개씩) + Inventory
    // ============================================================================
    private fun createProductsAndOptions(): List<ProductOptionEntity> {
        val productOptions = mutableListOf<ProductOptionEntity>()

        for (i in 1..TOTAL_SIZE) {
            val product = ProductEntity(
                name = "${COFFEE_BRANDS[i % COFFEE_BRANDS.size]} 프리미엄 원두 $i",
                description = "고급 커피 원두로, 깊고 풍부한 맛을 자랑합니다. 신선한 로스팅과 정교한 분쇄 기술이 적용되었습니다.",
                brand = COFFEE_BRANDS[i % COFFEE_BRANDS.size]
            )
            entityManager.persist(product)

            // ProductOption 3개 생성 (250g, 500g, 1kg)
            val weights = listOf(250, 500, 1000)
            val basePrices = listOf(15000, 25000, 40000)

            for (j in 0 until 3) {
                val option = ProductOptionEntity(
                    optionCode = "OPTION-${i}-${j + 1}",
                    productId = product.id,
                    origin = ORIGINS[(i + j) % ORIGINS.size],
                    grindType = GRIND_TYPES[j % GRIND_TYPES.size],
                    weightGrams = weights[j],
                    price = BigDecimal(basePrices[j]),
                    isActive = true
                )
                entityManager.persist(option)
                productOptions.add(option)

                // Inventory 생성 (초기 재고 1000개)
                val inventory = InventoryEntity(
                    productOptionId = option.id,
                    stockQuantity = 1000
                )
                entityManager.persist(inventory)
            }

            if (i % BATCH_SIZE == 0) {
                entityManager.flush()
                entityManager.clear()
            }
        }

        entityManager.flush()
        return productOptions
    }

    // ============================================================================
    // 3. Coupon 생성 (100개) + CouponTicket (100개씩)
    // ============================================================================
    private fun createCouponsAndTickets(): List<CouponEntity> {
        val coupons = mutableListOf<CouponEntity>()

        for (i in 1..TOTAL_SIZE) {
            val discountType = if (i % 2 == 0) DiscountType.PERCENTAGE else DiscountType.FIXED_AMOUNT
            val discountValue = if (i % 2 == 0) BigDecimal("10") else BigDecimal("5000")

            val coupon = CouponEntity(
                name = "${if (i % 2 == 0) "정률" else "정액"} 할인 쿠폰 $i",
                discountType = discountType,
                discountValue = discountValue,
                minOrderAmount = BigDecimal("10000"),
                maxDiscountAmount = BigDecimal("10000"),
                totalQuantity = 100,
                validFrom = LocalDateTime.now().minusDays(1),
                validUntil = LocalDateTime.now().plusDays(30)
            )
            entityManager.persist(coupon)
            coupons.add(coupon)

            // CouponTicket 100개 생성
            for (j in 1..100) {
                val ticket = CouponTicketEntity(
                    couponId = coupon.id,
                    status = CouponTicketStatus.AVAILABLE
                )
                entityManager.persist(ticket)
            }

            if (i % BATCH_SIZE == 0) {
                entityManager.flush()
                entityManager.clear()
            }
        }

        entityManager.flush()
        return coupons
    }

    // ============================================================================
    // 4. CartItem 생성 (100개)
    // ============================================================================
    private fun createCartItems(
        users: List<UserEntity>,
        productOptions: List<ProductOptionEntity>
    ): List<CartItemEntity> {
        val cartItems = mutableListOf<CartItemEntity>()

        for (i in 1..TOTAL_SIZE) {
            val user = users[i % users.size]
            val option = productOptions[i % productOptions.size]

            val cartItem = CartItemEntity(
                userId = user.id,
                productOptionId = option.id,
                quantity = (i % 5) + 1  // 1~5개
            )
            entityManager.persist(cartItem)
            cartItems.add(cartItem)

            if (i % BATCH_SIZE == 0) {
                entityManager.flush()
                entityManager.clear()
            }
        }

        entityManager.flush()
        return cartItems
    }

    // ============================================================================
    // 5. Order 생성 (100개) + OrderItem + InventoryReservation
    // ============================================================================
    private fun createOrdersWithItems(
        users: List<UserEntity>,
        productOptions: List<ProductOptionEntity>
    ): List<OrderEntity> {
        val orders = mutableListOf<OrderEntity>()

        for (i in 1..TOTAL_SIZE) {
            val user = users[i % users.size]
            val option = productOptions[i % productOptions.size]

            val unitPrice = option.price
            val quantity = (i % 3) + 1
            val totalPrice = unitPrice.multiply(BigDecimal(quantity))

            val order = OrderEntity(
                userId = user.id,
                status = OrderStatus.values()[(i / 25) % OrderStatus.values().size],  // 균등 분배
                originalAmount = totalPrice,
                discountAmount = if (i % 10 == 0) BigDecimal("5000") else BigDecimal("0"),
                finalAmount = totalPrice.subtract(if (i % 10 == 0) BigDecimal("5000") else BigDecimal("0")),
                shippingAddress = "서울시 강남구 테헤란로 ${100 + i}번길 $i-$i",
                trackingNumber = if (i % 5 == 0) "TRACK${System.currentTimeMillis()}" else null
            )
            entityManager.persist(order)
            orders.add(order)

            // OrderItem 생성 (1~3개)
            for (j in 1..((i % 3) + 1)) {
                val itemOption = productOptions[(i + j) % productOptions.size]
                val itemQuantity = (j % 2) + 1

                val orderItem = OrderItemEntity(
                    orderId = order.id,
                    productOptionId = itemOption.id,
                    quantity = itemQuantity,
                    unitPrice = itemOption.price,
                    totalPrice = itemOption.price.multiply(BigDecimal(itemQuantity))
                )
                entityManager.persist(orderItem)
            }

            // InventoryReservation 생성 (일부만)
            if (i % 2 == 0) {
                val reservation = InventoryReservationEntity(
                    productOptionId = option.id,
                    userId = user.id,
                    quantity = quantity,
                    status = InventoryReservationStatus.values()[(i % 3)],
                    reservedAt = LocalDateTime.now().minusHours((i % 24).toLong()),
                    expiresAt = LocalDateTime.now().plusMinutes(10)
                )
                entityManager.persist(reservation)
            }

            if (i % BATCH_SIZE == 0) {
                entityManager.flush()
                entityManager.clear()
            }
        }

        entityManager.flush()
        return orders
    }
}
