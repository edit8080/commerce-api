package com.beanbliss.domain.order.usecase

import com.beanbliss.domain.cart.dto.CartItemResponse
import com.beanbliss.domain.cart.repository.CartItemRepository
import com.beanbliss.domain.inventory.entity.InventoryReservationEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import com.beanbliss.domain.inventory.repository.InventoryRepository
import com.beanbliss.domain.inventory.repository.InventoryReservationRepository
import com.beanbliss.domain.order.exception.CartEmptyException
import com.beanbliss.domain.order.exception.DuplicateReservationException
import com.beanbliss.domain.order.exception.InsufficientAvailableStockException
import com.beanbliss.domain.order.exception.ProductOptionInactiveException
import com.beanbliss.domain.product.repository.ProductOptionDetail
import com.beanbliss.domain.product.repository.ProductOptionRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

/**
 * [책임]: ReserveOrderUseCase의 비즈니스 로직 검증
 * - 여러 Repository 조율 검증
 * - 예약 생성 로직 검증
 * - 예외 처리 및 트랜잭션 롤백 검증
 */
@DisplayName("주문 예약 UseCase 테스트")
class ReserveOrderUseCaseTest {

    private lateinit var reserveOrderUseCase: ReserveOrderUseCase
    private lateinit var cartItemRepository: CartItemRepository
    private lateinit var productOptionRepository: ProductOptionRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var inventoryReservationRepository: InventoryReservationRepository

    @BeforeEach
    fun setUp() {
        cartItemRepository = mockk()
        productOptionRepository = mockk()
        inventoryRepository = mockk()
        inventoryReservationRepository = mockk()

        reserveOrderUseCase = ReserveOrderUseCaseImpl(
            cartItemRepository,
            productOptionRepository,
            inventoryRepository,
            inventoryReservationRepository
        )
    }

    @Test
    @DisplayName("재고 예약 성공 시 모든 Repository 메서드가 올바른 순서로 호출되어야 한다")
    fun `재고 예약 성공 시_모든 Repository 메서드가 올바른 순서로 호출되어야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val cartItems = listOf(
            CartItemResponse(
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

        val productOption = ProductOptionDetail(
            optionId = 10L,
            productId = 1L,
            productName = "에티오피아 예가체프 G1",
            optionCode = "ETH-HD-200",
            origin = "에티오피아",
            grindType = "홀빈",
            weightGrams = 200,
            price = 15000,
            isActive = true
        )

        val savedReservation = InventoryReservationEntity(
            id = 1001L,
            productOptionId = 10L,
            userId = userId,
            quantity = 2,
            status = InventoryReservationStatus.RESERVED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )

        // Mocking
        every { cartItemRepository.findByUserId(userId) } returns cartItems
        every { productOptionRepository.findActiveOptionWithProduct(10L) } returns productOption
        every { inventoryReservationRepository.countActiveReservations(userId) } returns 0
        every { inventoryRepository.calculateAvailableStock(10L) } returns 10
        every { inventoryReservationRepository.save(any()) } returns savedReservation

        // When
        val result = reserveOrderUseCase.reserveOrder(userId)

        // Then
        // [TDD 검증 목표 1]: Repository 호출 순서 및 횟수 검증
        verifyOrder {
            cartItemRepository.findByUserId(userId)
            productOptionRepository.findActiveOptionWithProduct(10L)
            inventoryReservationRepository.countActiveReservations(userId)
            inventoryRepository.calculateAvailableStock(10L)
            inventoryReservationRepository.save(any())
        }

        verify(exactly = 1) { cartItemRepository.findByUserId(userId) }
        verify(exactly = 1) { productOptionRepository.findActiveOptionWithProduct(10L) }
        verify(exactly = 1) { inventoryReservationRepository.countActiveReservations(userId) }
        verify(exactly = 1) { inventoryRepository.calculateAvailableStock(10L) }
        verify(exactly = 1) { inventoryReservationRepository.save(any()) }

        // [TDD 검증 목표 2]: 응답 DTO 변환 검증
        assertEquals(1, result.reservations.size)
        assertEquals(1001L, result.reservations[0].reservationId)
        assertEquals(10L, result.reservations[0].productOptionId)
        assertEquals("에티오피아 예가체프 G1", result.reservations[0].productName)
        assertEquals("ETH-HD-200", result.reservations[0].optionCode)
        assertEquals(2, result.reservations[0].quantity)
        assertEquals(InventoryReservationStatus.RESERVED, result.reservations[0].status)
    }

    @Test
    @DisplayName("장바구니가 비어있을 경우 CartEmptyException이 발생하고 save가 호출되지 않아야 한다")
    fun `장바구니가 비어있을 경우_CartEmptyException이 발생하고_save가 호출되지 않아야 한다`() {
        // Given
        val userId = 123L
        every { cartItemRepository.findByUserId(userId) } returns emptyList()

        // When & Then
        assertThrows<CartEmptyException> {
            reserveOrderUseCase.reserveOrder(userId)
        }

        // [TDD 검증 목표]: 예외 발생 시 save가 호출되지 않았는지 검증
        verify(exactly = 1) { cartItemRepository.findByUserId(userId) }
        verify(exactly = 0) { inventoryReservationRepository.save(any()) }
    }

    @Test
    @DisplayName("비활성화된 상품 옵션이 포함된 경우 ProductOptionInactiveException이 발생해야 한다")
    fun `비활성화된 상품 옵션이 포함된 경우_ProductOptionInactiveException이 발생해야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val cartItems = listOf(
            CartItemResponse(
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

        // 비활성화된 상품 옵션
        val inactiveProductOption = ProductOptionDetail(
            optionId = 10L,
            productId = 1L,
            productName = "에티오피아 예가체프 G1",
            optionCode = "ETH-HD-200",
            origin = "에티오피아",
            grindType = "홀빈",
            weightGrams = 200,
            price = 15000,
            isActive = false  // 비활성화
        )

        every { cartItemRepository.findByUserId(userId) } returns cartItems
        every { productOptionRepository.findActiveOptionWithProduct(10L) } returns inactiveProductOption

        // When & Then
        assertThrows<ProductOptionInactiveException> {
            reserveOrderUseCase.reserveOrder(userId)
        }

        verify(exactly = 1) { cartItemRepository.findByUserId(userId) }
        verify(exactly = 1) { productOptionRepository.findActiveOptionWithProduct(10L) }
        verify(exactly = 0) { inventoryReservationRepository.save(any()) }
    }

    @Test
    @DisplayName("이미 진행 중인 예약이 있을 경우 DuplicateReservationException이 발생해야 한다")
    fun `이미 진행 중인 예약이 있을 경우_DuplicateReservationException이 발생해야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val cartItems = listOf(
            CartItemResponse(
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

        val productOption = ProductOptionDetail(
            optionId = 10L,
            productId = 1L,
            productName = "에티오피아 예가체프 G1",
            optionCode = "ETH-HD-200",
            origin = "에티오피아",
            grindType = "홀빈",
            weightGrams = 200,
            price = 15000,
            isActive = true
        )

        every { cartItemRepository.findByUserId(userId) } returns cartItems
        every { productOptionRepository.findActiveOptionWithProduct(10L) } returns productOption
        every { inventoryReservationRepository.countActiveReservations(userId) } returns 1  // 활성 예약 존재

        // When & Then
        assertThrows<DuplicateReservationException> {
            reserveOrderUseCase.reserveOrder(userId)
        }

        verify(exactly = 1) { cartItemRepository.findByUserId(userId) }
        verify(exactly = 1) { productOptionRepository.findActiveOptionWithProduct(10L) }
        verify(exactly = 1) { inventoryReservationRepository.countActiveReservations(userId) }
        verify(exactly = 0) { inventoryReservationRepository.save(any()) }
    }

    @Test
    @DisplayName("가용 재고가 부족할 경우 InsufficientAvailableStockException이 발생해야 한다")
    fun `가용 재고가 부족할 경우_InsufficientAvailableStockException이 발생해야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val cartItems = listOf(
            CartItemResponse(
                cartItemId = 1L,
                productOptionId = 10L,
                productName = "에티오피아 예가체프 G1",
                optionCode = "ETH-HD-200",
                origin = "에티오피아",
                grindType = "홀빈",
                weightGrams = 200,
                price = 15000,
                quantity = 5,  // 요청 수량 5개
                totalPrice = 75000,
                createdAt = now,
                updatedAt = now
            )
        )

        val productOption = ProductOptionDetail(
            optionId = 10L,
            productId = 1L,
            productName = "에티오피아 예가체프 G1",
            optionCode = "ETH-HD-200",
            origin = "에티오피아",
            grindType = "홀빈",
            weightGrams = 200,
            price = 15000,
            isActive = true
        )

        every { cartItemRepository.findByUserId(userId) } returns cartItems
        every { productOptionRepository.findActiveOptionWithProduct(10L) } returns productOption
        every { inventoryReservationRepository.countActiveReservations(userId) } returns 0
        every { inventoryRepository.calculateAvailableStock(10L) } returns 3  // 가용 재고 3개 (부족)

        // When & Then
        assertThrows<InsufficientAvailableStockException> {
            reserveOrderUseCase.reserveOrder(userId)
        }

        verify(exactly = 1) { cartItemRepository.findByUserId(userId) }
        verify(exactly = 1) { productOptionRepository.findActiveOptionWithProduct(10L) }
        verify(exactly = 1) { inventoryReservationRepository.countActiveReservations(userId) }
        verify(exactly = 1) { inventoryRepository.calculateAvailableStock(10L) }
        verify(exactly = 0) { inventoryReservationRepository.save(any()) }
    }

    @Test
    @DisplayName("여러 상품이 장바구니에 있을 경우 모두 예약되어야 한다")
    fun `여러 상품이 장바구니에 있을 경우_모두 예약되어야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val cartItems = listOf(
            CartItemResponse(
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
            ),
            CartItemResponse(
                cartItemId = 2L,
                productOptionId = 20L,
                productName = "콜롬비아 수프리모",
                optionCode = "COL-WB-500",
                origin = "콜롬비아",
                grindType = "원두",
                weightGrams = 500,
                price = 25000,
                quantity = 1,
                totalPrice = 25000,
                createdAt = now,
                updatedAt = now
            )
        )

        val productOption1 = ProductOptionDetail(
            optionId = 10L,
            productId = 1L,
            productName = "에티오피아 예가체프 G1",
            optionCode = "ETH-HD-200",
            origin = "에티오피아",
            grindType = "홀빈",
            weightGrams = 200,
            price = 15000,
            isActive = true
        )

        val productOption2 = ProductOptionDetail(
            optionId = 20L,
            productId = 2L,
            productName = "콜롬비아 수프리모",
            optionCode = "COL-WB-500",
            origin = "콜롬비아",
            grindType = "원두",
            weightGrams = 500,
            price = 25000,
            isActive = true
        )

        val savedReservation1 = InventoryReservationEntity(
            id = 1001L,
            productOptionId = 10L,
            userId = userId,
            quantity = 2,
            status = InventoryReservationStatus.RESERVED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )

        val savedReservation2 = InventoryReservationEntity(
            id = 1002L,
            productOptionId = 20L,
            userId = userId,
            quantity = 1,
            status = InventoryReservationStatus.RESERVED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )

        every { cartItemRepository.findByUserId(userId) } returns cartItems
        every { productOptionRepository.findActiveOptionWithProduct(10L) } returns productOption1
        every { productOptionRepository.findActiveOptionWithProduct(20L) } returns productOption2
        every { inventoryReservationRepository.countActiveReservations(userId) } returns 0
        every { inventoryRepository.calculateAvailableStock(10L) } returns 10
        every { inventoryRepository.calculateAvailableStock(20L) } returns 15
        every { inventoryReservationRepository.save(match { it.productOptionId == 10L }) } returns savedReservation1
        every { inventoryReservationRepository.save(match { it.productOptionId == 20L }) } returns savedReservation2

        // When
        val result = reserveOrderUseCase.reserveOrder(userId)

        // Then
        verify(exactly = 1) { cartItemRepository.findByUserId(userId) }
        verify(exactly = 1) { productOptionRepository.findActiveOptionWithProduct(10L) }
        verify(exactly = 1) { productOptionRepository.findActiveOptionWithProduct(20L) }
        verify(exactly = 1) { inventoryReservationRepository.countActiveReservations(userId) }
        verify(exactly = 1) { inventoryRepository.calculateAvailableStock(10L) }
        verify(exactly = 1) { inventoryRepository.calculateAvailableStock(20L) }
        verify(exactly = 2) { inventoryReservationRepository.save(any()) }

        assertEquals(2, result.reservations.size)
        assertEquals(1001L, result.reservations[0].reservationId)
        assertEquals(1002L, result.reservations[1].reservationId)
    }

    @Test
    @DisplayName("예약 시각과 만료 시각이 올바르게 계산되어야 한다")
    fun `예약 시각과 만료 시각이 올바르게 계산되어야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val cartItems = listOf(
            CartItemResponse(
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

        val productOption = ProductOptionDetail(
            optionId = 10L,
            productId = 1L,
            productName = "에티오피아 예가체프 G1",
            optionCode = "ETH-HD-200",
            origin = "에티오피아",
            grindType = "홀빈",
            weightGrams = 200,
            price = 15000,
            isActive = true
        )

        val reservedAt = LocalDateTime.of(2025, 11, 4, 15, 30, 0)
        val expiresAt = reservedAt.plusMinutes(30)

        val savedReservation = InventoryReservationEntity(
            id = 1001L,
            productOptionId = 10L,
            userId = userId,
            quantity = 2,
            status = InventoryReservationStatus.RESERVED,
            reservedAt = reservedAt,
            expiresAt = expiresAt,
            updatedAt = reservedAt
        )

        every { cartItemRepository.findByUserId(userId) } returns cartItems
        every { productOptionRepository.findActiveOptionWithProduct(10L) } returns productOption
        every { inventoryReservationRepository.countActiveReservations(userId) } returns 0
        every { inventoryRepository.calculateAvailableStock(10L) } returns 10
        every { inventoryReservationRepository.save(any()) } returns savedReservation

        // When
        val result = reserveOrderUseCase.reserveOrder(userId)

        // Then
        assertEquals(reservedAt, result.reservations[0].reservedAt)
        assertEquals(expiresAt, result.reservations[0].expiresAt)
    }
}
