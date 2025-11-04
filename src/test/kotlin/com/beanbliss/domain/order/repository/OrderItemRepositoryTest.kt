package com.beanbliss.domain.order.repository

import com.beanbliss.domain.order.entity.OrderItemEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * [책임]: OrderItemRepository의 계약 검증
 * - 주문 항목 일괄 저장
 * - ID 자동 할당
 */
@DisplayName("주문 항목 Repository 테스트")
class OrderItemRepositoryTest {

    private lateinit var orderItemRepository: OrderItemRepository
    private lateinit var fakeOrderItemRepository: FakeOrderItemRepository

    @BeforeEach
    fun setUp() {
        fakeOrderItemRepository = FakeOrderItemRepository()
        orderItemRepository = fakeOrderItemRepository
    }

    @Test
    @DisplayName("주문 항목 일괄 저장 시_각 항목에 ID가 자동 할당되어야 한다")
    fun `주문 항목 일괄 저장 시_각 항목에 ID가 자동 할당되어야 한다`() {
        // Given
        val orderItems = listOf(
            OrderItemEntity(
                id = 0L,
                orderId = 1L,
                productOptionId = 10L,
                quantity = 2,
                unitPrice = 15000,
                totalPrice = 30000
            ),
            OrderItemEntity(
                id = 0L,
                orderId = 1L,
                productOptionId = 20L,
                quantity = 1,
                unitPrice = 20000,
                totalPrice = 20000
            )
        )

        // When
        val savedItems = orderItemRepository.saveAll(orderItems)

        // Then
        assertEquals(2, savedItems.size)
        assertTrue(savedItems[0].id > 0, "First item should have auto-generated ID")
        assertTrue(savedItems[1].id > 0, "Second item should have auto-generated ID")
        assertNotEquals(savedItems[0].id, savedItems[1].id, "Each item should have unique ID")
    }

    @Test
    @DisplayName("주문 항목 일괄 저장 시_모든 필드가 정확히 저장되어야 한다")
    fun `주문 항목 일괄 저장 시_모든 필드가 정확히 저장되어야 한다`() {
        // Given
        val orderItems = listOf(
            OrderItemEntity(
                id = 0L,
                orderId = 1L,
                productOptionId = 10L,
                quantity = 2,
                unitPrice = 15000,
                totalPrice = 30000
            ),
            OrderItemEntity(
                id = 0L,
                orderId = 1L,
                productOptionId = 20L,
                quantity = 3,
                unitPrice = 12000,
                totalPrice = 36000
            )
        )

        // When
        val savedItems = orderItemRepository.saveAll(orderItems)

        // Then
        // First item
        assertEquals(1L, savedItems[0].orderId)
        assertEquals(10L, savedItems[0].productOptionId)
        assertEquals(2, savedItems[0].quantity)
        assertEquals(15000, savedItems[0].unitPrice)
        assertEquals(30000, savedItems[0].totalPrice)

        // Second item
        assertEquals(1L, savedItems[1].orderId)
        assertEquals(20L, savedItems[1].productOptionId)
        assertEquals(3, savedItems[1].quantity)
        assertEquals(12000, savedItems[1].unitPrice)
        assertEquals(36000, savedItems[1].totalPrice)
    }

    @Test
    @DisplayName("빈 리스트를 저장하면_빈 리스트가 반환되어야 한다")
    fun `빈 리스트를 저장하면_빈 리스트가 반환되어야 한다`() {
        // Given
        val emptyList = emptyList<OrderItemEntity>()

        // When
        val result = orderItemRepository.saveAll(emptyList)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    @DisplayName("저장된 주문 항목을 조회할 수 있어야 한다")
    fun `저장된 주문 항목을 조회할 수 있어야 한다`() {
        // Given
        val orderItems = listOf(
            OrderItemEntity(
                id = 0L,
                orderId = 1L,
                productOptionId = 10L,
                quantity = 2,
                unitPrice = 15000,
                totalPrice = 30000
            )
        )

        // When
        val savedItems = orderItemRepository.saveAll(orderItems)
        val found = fakeOrderItemRepository.findById(savedItems[0].id)

        // Then
        assertNotNull(found)
        assertEquals(savedItems[0].id, found!!.id)
        assertEquals(1L, found.orderId)
        assertEquals(10L, found.productOptionId)
        assertEquals(2, found.quantity)
    }

    @Test
    @DisplayName("같은 주문의 여러 항목을 저장하고 조회할 수 있어야 한다")
    fun `같은 주문의 여러 항목을 저장하고 조회할 수 있어야 한다`() {
        // Given
        val orderId = 100L
        val orderItems = listOf(
            OrderItemEntity(
                id = 0L,
                orderId = orderId,
                productOptionId = 10L,
                quantity = 2,
                unitPrice = 15000,
                totalPrice = 30000
            ),
            OrderItemEntity(
                id = 0L,
                orderId = orderId,
                productOptionId = 20L,
                quantity = 1,
                unitPrice = 20000,
                totalPrice = 20000
            ),
            OrderItemEntity(
                id = 0L,
                orderId = orderId,
                productOptionId = 30L,
                quantity = 3,
                unitPrice = 10000,
                totalPrice = 30000
            )
        )

        // When
        orderItemRepository.saveAll(orderItems)
        val foundItems = fakeOrderItemRepository.findByOrderId(orderId)

        // Then
        assertEquals(3, foundItems.size)
        assertTrue(foundItems.all { it.orderId == orderId })
    }
}
