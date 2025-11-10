package com.beanbliss.domain.order.usecase

import com.beanbliss.domain.cart.repository.CartItemDetail
import com.beanbliss.domain.cart.service.CartService
import com.beanbliss.domain.inventory.entity.InventoryReservationEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import com.beanbliss.domain.inventory.service.InventoryService
import com.beanbliss.domain.product.service.ProductService
import com.beanbliss.domain.user.service.UserService
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * [책임]: ReserveOrderUseCase의 Service 조율 검증
 * - 여러 Service를 올바른 순서로 호출하는지 검증
 * - DTO 변환이 올바르게 수행되는지 검증
 * - 비즈니스 로직 검증은 각 Service 테스트에서 수행
 */
@DisplayName("주문 예약 UseCase 테스트")
class ReserveOrderUseCaseTest {

    private val userService: UserService = mockk()
    private val cartService: CartService = mockk()
    private val productService: ProductService = mockk()
    private val inventoryService: InventoryService = mockk()
    private val reserveOrderUseCase = ReserveOrderUseCase(
        userService, cartService, productService, inventoryService
    )

    private val now = LocalDateTime.now()

    @Test
    @DisplayName("주문 예약 시 UserService, CartService, ProductService, InventoryService를 올바른 순서로 호출하고 응답을 반환해야 한다")
    fun `주문 예약 시_모든 Service를 올바른 순서로 호출하고_응답을 반환해야 한다`() {
        // Given
        val userId = 123L

        val cartItems = listOf(
            CartItemDetail(
                cartItemId = 1L,
                productOptionId = 10L,
                productName = "에티오피아 예가체프 G1",
                optionCode = "ETH-HD-200",
                origin = "에티오피아",
                grindType = "홀빈",
                weightGrams = 200,
                price = 15000,
                quantity = 2,
                totalPrice = 30000,
                createdAt = now,
                updatedAt = now
            )
        )

        val reservationEntity = InventoryReservationEntity(
            id = 1001L,
            productOptionId = 10L,
            userId = userId,
            quantity = 2,
            status = InventoryReservationStatus.RESERVED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )

        val reservations = listOf(
            InventoryService.ReservationItem(
                reservationEntity = reservationEntity,
                productName = "에티오피아 예가체프 G1",
                optionCode = "ETH-HD-200",
                availableStockAfterReservation = 8
            )
        )

        // Mocking
        every { userService.validateUserExists(userId) } just Runs
        every { cartService.getCartItemsWithProducts(userId) } returns cartItems
        every { productService.validateProductOptionsActive(listOf(10L)) } just Runs
        every { inventoryService.reserveInventory(userId, cartItems) } returns reservations

        // When
        val result = reserveOrderUseCase.reserveOrder(userId)

        // Then
        // [검증 1]: Service 호출 순서 - UserService.validateUserExists() 먼저 호출
        // [검증 2]: Service 호출 순서 - CartService.getCartItemsWithProducts() 다음 호출
        // [검증 3]: Service 호출 순서 - ProductService.validateProductOptionsActive() 다음 호출
        // [검증 4]: Service 호출 순서 - InventoryService.reserveInventory() 마지막 호출
        verifyOrder {
            userService.validateUserExists(userId)
            cartService.getCartItemsWithProducts(userId)
            productService.validateProductOptionsActive(listOf(10L))
            inventoryService.reserveInventory(userId, cartItems)
        }

        verify(exactly = 1) { userService.validateUserExists(userId) }
        verify(exactly = 1) { cartService.getCartItemsWithProducts(userId) }
        verify(exactly = 1) { productService.validateProductOptionsActive(listOf(10L)) }
        verify(exactly = 1) { inventoryService.reserveInventory(userId, cartItems) }

        // [검증 5]: 응답 DTO 반환 검증
        assertEquals(1, result.size)
        assertEquals(1001L, result[0].reservationEntity.id)
        assertEquals(10L, result[0].reservationEntity.productOptionId)
        assertEquals("에티오피아 예가체프 G1", result[0].productName)
        assertEquals(2, result[0].reservationEntity.quantity)
    }
}
