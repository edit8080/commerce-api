package com.beanbliss.domain.order.repository

import com.beanbliss.domain.order.entity.OrderEntity
import com.beanbliss.domain.order.entity.OrderStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * [책임]: OrderRepository의 계약 검증
 * - 주문 저장
 * - ID 자동 할당
 */
@DisplayName("주문 Repository 테스트")
class OrderRepositoryTest {

    private lateinit var orderRepository: OrderRepository
    private lateinit var fakeOrderRepository: FakeOrderRepository

    @BeforeEach
    fun setUp() {
        fakeOrderRepository = FakeOrderRepository()
        orderRepository = fakeOrderRepository
    }

    @Test
    @DisplayName("주문 저장 시_ID가 자동 할당되어야 한다")
    fun `주문 저장 시_ID가 자동 할당되어야 한다`() {
        // Given
        val now = LocalDateTime.now()
        val order = OrderEntity(
            id = 0L,
            userId = 123L,
            totalAmount = 30000,
            discountAmount = 3000,
            finalAmount = 27000,
            shippingAddress = "서울시 강남구 테헤란로 123",
            orderStatus = OrderStatus.PAYMENT_COMPLETED,
            orderedAt = now,
            updatedAt = now
        )

        // When
        val saved = orderRepository.save(order)

        // Then
        assertTrue(saved.id > 0, "ID should be auto-generated")
        assertEquals(123L, saved.userId)
        assertEquals(30000, saved.totalAmount)
        assertEquals(3000, saved.discountAmount)
        assertEquals(27000, saved.finalAmount)
        assertEquals("서울시 강남구 테헤란로 123", saved.shippingAddress)
        assertEquals(OrderStatus.PAYMENT_COMPLETED, saved.orderStatus)
    }

    @Test
    @DisplayName("주문 저장 시_모든 필드가 정확히 저장되어야 한다")
    fun `주문 저장 시_모든 필드가 정확히 저장되어야 한다`() {
        // Given
        val now = LocalDateTime.now()
        val order = OrderEntity(
            id = 0L,
            userId = 456L,
            totalAmount = 50000,
            discountAmount = 5000,
            finalAmount = 45000,
            shippingAddress = "부산시 해운대구 센텀시티로 100",
            orderStatus = OrderStatus.PAYMENT_COMPLETED,
            orderedAt = now,
            updatedAt = now
        )

        // When
        val saved = orderRepository.save(order)

        // Then
        val found = fakeOrderRepository.findById(saved.id)
        assertNotNull(found)
        assertEquals(saved.id, found!!.id)
        assertEquals(456L, found.userId)
        assertEquals(50000, found.totalAmount)
        assertEquals(5000, found.discountAmount)
        assertEquals(45000, found.finalAmount)
        assertEquals("부산시 해운대구 센텀시티로 100", found.shippingAddress)
        assertEquals(OrderStatus.PAYMENT_COMPLETED, found.orderStatus)
    }

    @Test
    @DisplayName("여러 주문을 저장할 때_각각 고유한 ID가 할당되어야 한다")
    fun `여러 주문을 저장할 때_각각 고유한 ID가 할당되어야 한다`() {
        // Given
        val now = LocalDateTime.now()
        val order1 = OrderEntity(
            id = 0L,
            userId = 123L,
            totalAmount = 30000,
            discountAmount = 0,
            finalAmount = 30000,
            shippingAddress = "서울시 강남구",
            orderStatus = OrderStatus.PAYMENT_COMPLETED,
            orderedAt = now,
            updatedAt = now
        )
        val order2 = OrderEntity(
            id = 0L,
            userId = 456L,
            totalAmount = 50000,
            discountAmount = 0,
            finalAmount = 50000,
            shippingAddress = "부산시 해운대구",
            orderStatus = OrderStatus.PAYMENT_COMPLETED,
            orderedAt = now,
            updatedAt = now
        )

        // When
        val saved1 = orderRepository.save(order1)
        val saved2 = orderRepository.save(order2)

        // Then
        assertTrue(saved1.id > 0)
        assertTrue(saved2.id > 0)
        assertNotEquals(saved1.id, saved2.id, "Each order should have unique ID")
    }

    @Test
    @DisplayName("ID가 이미 있는 주문을 저장하면_기존 ID를 유지해야 한다")
    fun `ID가 이미 있는 주문을 저장하면_기존 ID를 유지해야 한다`() {
        // Given
        val now = LocalDateTime.now()
        val order = OrderEntity(
            id = 0L,
            userId = 123L,
            totalAmount = 30000,
            discountAmount = 0,
            finalAmount = 30000,
            shippingAddress = "서울시 강남구",
            orderStatus = OrderStatus.PAYMENT_COMPLETED,
            orderedAt = now,
            updatedAt = now
        )
        val savedOrder = orderRepository.save(order)

        // When - 상태 변경 후 다시 저장
        val updatedOrder = savedOrder.copy(orderStatus = OrderStatus.PREPARING)
        val resaved = orderRepository.save(updatedOrder)

        // Then
        assertEquals(savedOrder.id, resaved.id, "ID should remain the same")
        assertEquals(OrderStatus.PREPARING, resaved.orderStatus)
    }
}
