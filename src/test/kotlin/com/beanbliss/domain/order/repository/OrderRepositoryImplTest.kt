package com.beanbliss.domain.order.repository

import com.beanbliss.common.test.RepositoryTestBase
import com.beanbliss.domain.order.entity.OrderEntity
import com.beanbliss.domain.order.entity.OrderStatus
import com.beanbliss.domain.user.entity.UserEntity
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@DisplayName("Order Repository 통합 테스트")
@Transactional
class OrderRepositoryImplTest : RepositoryTestBase() {

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testUser: UserEntity

    @BeforeEach
    fun setUpTestData() {
        // 테스트 사용자 생성
        testUser = UserEntity(
            email = "test@example.com",
            password = "password123",
            name = "테스트 사용자"
        )
        entityManager.persist(testUser)
        entityManager.flush()
        entityManager.clear()
    }

    @Test
    @DisplayName("주문 저장 - 신규 주문 생성")
    fun `save should create new order`() {
        // Given
        val newOrder = OrderEntity(
            userId = testUser.id,
            status = OrderStatus.PAYMENT_COMPLETED,
            originalAmount = BigDecimal("30000"),
            discountAmount = BigDecimal("3000"),
            finalAmount = BigDecimal("27000"),
            shippingAddress = "서울시 강남구 테헤란로 123"
        )

        // When
        val savedOrder = orderRepository.save(newOrder)

        // Then
        assertTrue(savedOrder.id > 0)
        assertEquals(testUser.id, savedOrder.userId)
        assertEquals(OrderStatus.PAYMENT_COMPLETED, savedOrder.status)
        assertEquals(BigDecimal("30000"), savedOrder.originalAmount)
        assertEquals(BigDecimal("3000"), savedOrder.discountAmount)
        assertEquals(BigDecimal("27000"), savedOrder.finalAmount)
        assertEquals("서울시 강남구 테헤란로 123", savedOrder.shippingAddress)
        assertNull(savedOrder.trackingNumber)
    }

    @Test
    @DisplayName("주문 저장 - 쿠폰 적용 주문 생성")
    fun `save should create order with coupon`() {
        // Given
        val newOrder = OrderEntity(
            userId = testUser.id,
            userCouponId = 1L,
            status = OrderStatus.PAYMENT_COMPLETED,
            originalAmount = BigDecimal("50000"),
            discountAmount = BigDecimal("10000"),
            finalAmount = BigDecimal("40000"),
            shippingAddress = "부산시 해운대구 센텀시티로 456"
        )

        // When
        val savedOrder = orderRepository.save(newOrder)

        // Then
        assertTrue(savedOrder.id > 0)
        assertEquals(1L, savedOrder.userCouponId)
        assertEquals(BigDecimal("10000"), savedOrder.discountAmount)
    }

    @Test
    @DisplayName("주문 저장 - 주문 상태 업데이트")
    fun `save should update order status`() {
        // Given: 기존 주문 생성
        val existingOrder = OrderEntity(
            userId = testUser.id,
            status = OrderStatus.PAYMENT_COMPLETED,
            originalAmount = BigDecimal("30000"),
            discountAmount = BigDecimal("0"),
            finalAmount = BigDecimal("30000"),
            shippingAddress = "서울시 송파구 올림픽로 789"
        )
        val savedOrder = orderRepository.save(existingOrder)
        entityManager.flush()
        entityManager.clear()

        // When: 주문 상태 업데이트
        savedOrder.status = OrderStatus.SHIPPING
        savedOrder.trackingNumber = "123456789"
        val updatedOrder = orderRepository.save(savedOrder)

        // Then
        assertEquals(OrderStatus.SHIPPING, updatedOrder.status)
        assertEquals("123456789", updatedOrder.trackingNumber)
    }
}
