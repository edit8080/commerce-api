package com.beanbliss.domain.order.repository

import com.beanbliss.common.test.RepositoryTestBase
import com.beanbliss.domain.order.entity.OrderEntity
import com.beanbliss.domain.order.entity.OrderItemEntity
import com.beanbliss.domain.order.entity.OrderStatus
import com.beanbliss.domain.product.entity.ProductEntity
import com.beanbliss.domain.product.entity.ProductOptionEntity
import com.beanbliss.domain.user.entity.UserEntity
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("OrderItem Repository 통합 테스트")
@Transactional
class OrderItemRepositoryImplTest : RepositoryTestBase() {

    @Autowired
    private lateinit var orderItemRepository: OrderItemRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testUser: UserEntity
    private lateinit var testProduct1: ProductEntity
    private lateinit var testProduct2: ProductEntity
    private lateinit var testProduct3: ProductEntity
    private lateinit var testOption1: ProductOptionEntity
    private lateinit var testOption2: ProductOptionEntity
    private lateinit var testOption3: ProductOptionEntity
    private lateinit var testOrder: OrderEntity

    @BeforeEach
    fun setUpTestData() {
        // 테스트 사용자 생성
        testUser = UserEntity(
            email = "test@example.com",
            password = "password123",
            name = "테스트 사용자"
        )
        entityManager.persist(testUser)

        // 테스트 상품 1 생성 (가장 많이 팔린 상품)
        testProduct1 = ProductEntity(
            name = "에티오피아 예가체프",
            description = "고급 원두",
            brand = "Bean Bliss"
        )
        entityManager.persist(testProduct1)

        testOption1 = ProductOptionEntity(
            optionCode = "ETH-001",
            productId = testProduct1.id,
            origin = "Ethiopia",
            grindType = "Whole Bean",
            weightGrams = 200,
            price = BigDecimal("15000"),
            isActive = true
        )
        entityManager.persist(testOption1)

        // 테스트 상품 2 생성 (중간 판매량)
        testProduct2 = ProductEntity(
            name = "콜롬비아 수프리모",
            description = "중간 로스트",
            brand = "Bean Bliss"
        )
        entityManager.persist(testProduct2)

        testOption2 = ProductOptionEntity(
            optionCode = "COL-001",
            productId = testProduct2.id,
            origin = "Colombia",
            grindType = "Whole Bean",
            weightGrams = 250,
            price = BigDecimal("18000"),
            isActive = true
        )
        entityManager.persist(testOption2)

        // 테스트 상품 3 생성 (적은 판매량, 비활성)
        testProduct3 = ProductEntity(
            name = "브라질 산토스",
            description = "라이트 로스트",
            brand = "Bean Bliss"
        )
        entityManager.persist(testProduct3)

        testOption3 = ProductOptionEntity(
            optionCode = "BRA-001",
            productId = testProduct3.id,
            origin = "Brazil",
            grindType = "Ground",
            weightGrams = 300,
            price = BigDecimal("12000"),
            isActive = false  // 비활성
        )
        entityManager.persist(testOption3)

        // 테스트 주문 생성
        testOrder = OrderEntity(
            userId = testUser.id,
            status = OrderStatus.PAYMENT_COMPLETED,
            originalAmount = BigDecimal("50000"),
            discountAmount = BigDecimal("0"),
            finalAmount = BigDecimal("50000"),
            shippingAddress = "서울시 강남구"
        )
        entityManager.persist(testOrder)

        entityManager.flush()
        entityManager.clear()
    }

    @Test
    @DisplayName("주문 아이템 일괄 저장")
    fun `saveAll should save multiple order items`() {
        // Given
        val orderItems = listOf(
            OrderItemEntity(
                orderId = testOrder.id,
                productOptionId = testOption1.id,
                quantity = 2,
                unitPrice = BigDecimal("15000"),
                totalPrice = BigDecimal("30000")
            ),
            OrderItemEntity(
                orderId = testOrder.id,
                productOptionId = testOption2.id,
                quantity = 1,
                unitPrice = BigDecimal("18000"),
                totalPrice = BigDecimal("18000")
            )
        )

        // When
        val savedOrderItems = orderItemRepository.saveAll(orderItems)

        // Then
        assertEquals(2, savedOrderItems.size)
        assertTrue(savedOrderItems.all { it.id > 0 })
        assertEquals(2, savedOrderItems[0].quantity)
        assertEquals(1, savedOrderItems[1].quantity)
    }

    @Test
    @DisplayName("인기 상품 옵션 조회 - 주문 수량 기준 정렬")
    fun `findTopOrderedProductOptions should return product options sorted by order count`() {
        // Given: 여러 주문 아이템 생성
        // 옵션 1: 총 50개 주문 (30 + 20)
        val orderItem1_1 = OrderItemEntity(
            orderId = testOrder.id,
            productOptionId = testOption1.id,
            quantity = 30,
            unitPrice = BigDecimal("15000"),
            totalPrice = BigDecimal("450000"),
            createdAt = LocalDateTime.now().minusDays(5)
        )
        val orderItem1_2 = OrderItemEntity(
            orderId = testOrder.id,
            productOptionId = testOption1.id,
            quantity = 20,
            unitPrice = BigDecimal("15000"),
            totalPrice = BigDecimal("300000"),
            createdAt = LocalDateTime.now().minusDays(3)
        )

        // 옵션 2: 총 25개 주문
        val orderItem2 = OrderItemEntity(
            orderId = testOrder.id,
            productOptionId = testOption2.id,
            quantity = 25,
            unitPrice = BigDecimal("18000"),
            totalPrice = BigDecimal("450000"),
            createdAt = LocalDateTime.now().minusDays(4)
        )

        // 옵션 3: 총 10개 주문 (비활성 옵션 - 조회되지 않아야 함)
        val orderItem3 = OrderItemEntity(
            orderId = testOrder.id,
            productOptionId = testOption3.id,
            quantity = 10,
            unitPrice = BigDecimal("12000"),
            totalPrice = BigDecimal("120000"),
            createdAt = LocalDateTime.now().minusDays(2)
        )

        entityManager.persist(orderItem1_1)
        entityManager.persist(orderItem1_2)
        entityManager.persist(orderItem2)
        entityManager.persist(orderItem3)
        entityManager.flush()
        entityManager.clear()

        // When: 최근 7일 동안 상위 10개 조회 (ProductOption 단위)
        val startDate = LocalDateTime.now().minusDays(7)
        val topProductOptions = orderItemRepository.findTopOrderedProductOptions(startDate, 10)

        // Then: ORDER 도메인만 조회하므로 활성/비활성 관계없이 모든 옵션 반환
        // (활성 필터링은 UseCase에서 처리)
        assertEquals(3, topProductOptions.size)

        // 첫 번째 옵션 (50개)
        assertEquals(testOption1.id, topProductOptions[0].productOptionId)
        assertEquals(50, topProductOptions[0].totalOrderCount)

        // 두 번째 옵션 (25개)
        assertEquals(testOption2.id, topProductOptions[1].productOptionId)
        assertEquals(25, topProductOptions[1].totalOrderCount)

        // 세 번째 옵션 (10개 - 비활성이지만 Repository는 반환)
        assertEquals(testOption3.id, topProductOptions[2].productOptionId)
        assertEquals(10, topProductOptions[2].totalOrderCount)
    }

    @Test
    @DisplayName("인기 상품 옵션 조회 - 기간 필터링")
    fun `findTopOrderedProductOptions should filter by date range`() {
        // Given: 오래된 주문과 최근 주문 생성
        val oldOrderItem = OrderItemEntity(
            orderId = testOrder.id,
            productOptionId = testOption1.id,
            quantity = 100,
            unitPrice = BigDecimal("15000"),
            totalPrice = BigDecimal("1500000"),
            createdAt = LocalDateTime.now().minusDays(20)  // 20일 전
        )

        val recentOrderItem = OrderItemEntity(
            orderId = testOrder.id,
            productOptionId = testOption2.id,
            quantity = 10,
            unitPrice = BigDecimal("18000"),
            totalPrice = BigDecimal("180000"),
            createdAt = LocalDateTime.now().minusDays(2)  // 2일 전
        )

        entityManager.persist(oldOrderItem)
        entityManager.persist(recentOrderItem)
        entityManager.flush()
        entityManager.clear()

        // When: 최근 7일 동안만 조회 (ProductOption 단위)
        val startDate = LocalDateTime.now().minusDays(7)
        val topProductOptions = orderItemRepository.findTopOrderedProductOptions(startDate, 10)

        // Then: 최근 7일 이내 주문만 조회되어야 함 (옵션 2만)
        assertEquals(1, topProductOptions.size)
        assertEquals(testOption2.id, topProductOptions[0].productOptionId)
        assertEquals(10, topProductOptions[0].totalOrderCount)
    }

    @Test
    @DisplayName("인기 상품 옵션 조회 - LIMIT 파라미터 적용")
    fun `findTopOrderedProductOptions should respect limit parameter`() {
        // Given: 2개 옵션에 대한 주문 생성
        val orderItem1 = OrderItemEntity(
            orderId = testOrder.id,
            productOptionId = testOption1.id,
            quantity = 30,
            unitPrice = BigDecimal("15000"),
            totalPrice = BigDecimal("450000"),
            createdAt = LocalDateTime.now().minusDays(1)
        )

        val orderItem2 = OrderItemEntity(
            orderId = testOrder.id,
            productOptionId = testOption2.id,
            quantity = 20,
            unitPrice = BigDecimal("18000"),
            totalPrice = BigDecimal("360000"),
            createdAt = LocalDateTime.now().minusDays(1)
        )

        entityManager.persist(orderItem1)
        entityManager.persist(orderItem2)
        entityManager.flush()
        entityManager.clear()

        // When: LIMIT 1로 조회 (ProductOption 단위)
        val startDate = LocalDateTime.now().minusDays(7)
        val topProductOptions = orderItemRepository.findTopOrderedProductOptions(startDate, 1)

        // Then: 1개만 조회되어야 함 (가장 많이 팔린 옵션 1)
        assertEquals(1, topProductOptions.size)
        assertEquals(testOption1.id, topProductOptions[0].productOptionId)
    }

    @Test
    @DisplayName("인기 상품 옵션 조회 - 주문 아이템이 없는 경우")
    fun `findTopOrderedProductOptions should return empty list when no order items`() {
        // When: 주문 아이템이 없는 상태에서 조회 (ProductOption 단위)
        val startDate = LocalDateTime.now().minusDays(7)
        val topProductOptions = orderItemRepository.findTopOrderedProductOptions(startDate, 10)

        // Then
        assertTrue(topProductOptions.isEmpty())
    }
}
