package com.beanbliss.domain.inventory.service

import com.beanbliss.domain.cart.dto.CartItemResponse
import com.beanbliss.domain.inventory.entity.InventoryReservationEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import com.beanbliss.domain.inventory.repository.InventoryRepository
import com.beanbliss.domain.inventory.repository.InventoryReservationRepository
import com.beanbliss.domain.order.exception.DuplicateReservationException
import com.beanbliss.domain.order.exception.InsufficientAvailableStockException
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

/**
 * InventoryService의 재고 예약 비즈니스 로직을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. 중복 예약 방지: 활성 예약이 있을 경우 DuplicateReservationException 발생
 * 2. 가용 재고 검증: 재고 부족 시 InsufficientAvailableStockException 발생
 * 3. 예약 생성: Repository.save가 올바르게 호출되는가?
 * 4. 만료 시각: 예약 후 30분 만료가 올바르게 계산되는가?
 * 5. 응답 DTO 변환: Entity가 Response DTO로 올바르게 변환되는가?
 *
 * [관련 UseCase]:
 * - ReserveOrderUseCase
 */
@DisplayName("재고 예약 Service 테스트")
class InventoryReserveServiceTest {

    // Mock 객체 (Repository Interface에 의존)
    private val inventoryRepository: InventoryRepository = mockk()
    private val inventoryReservationRepository: InventoryReservationRepository = mockk()

    // 테스트 대상 (Service 인터페이스로 선언)
    private lateinit var inventoryService: InventoryService

    @BeforeEach
    fun setUp() {
        inventoryService = InventoryServiceImpl(inventoryRepository, inventoryReservationRepository)
    }

    private val now = LocalDateTime.now()

    @Test
    @DisplayName("정상 예약 시 중복 예약 확인, 재고 검증, 예약 생성이 순서대로 호출되어야 한다")
    fun `정상 예약 시_중복 예약 확인_재고 검증_예약 생성이 순서대로 호출되어야 한다`() {
        // Given
        val userId = 123L
        val cartItems = listOf(
            createCartItem(1L, 10L, "에티오피아 예가체프 G1", "ETH-HD-200", 2)
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
        every { inventoryReservationRepository.countActiveReservations(userId) } returns 0
        every { inventoryRepository.calculateAvailableStockBatch(listOf(10L)) } returns mapOf(10L to 10)
        every { inventoryReservationRepository.saveAll(any()) } returns listOf(savedReservation)

        // When
        val result = inventoryService.reserveInventory(userId, cartItems)

        // Then
        // [검증 1]: Repository 호출 순서 검증
        verifyOrder {
            inventoryReservationRepository.countActiveReservations(userId)
            inventoryRepository.calculateAvailableStockBatch(listOf(10L))
            inventoryReservationRepository.saveAll(any())
        }

        verify(exactly = 1) { inventoryReservationRepository.countActiveReservations(userId) }
        verify(exactly = 1) { inventoryRepository.calculateAvailableStockBatch(listOf(10L)) }
        verify(exactly = 1) { inventoryReservationRepository.saveAll(any()) }

        // [검증 2]: 응답 DTO 변환 검증
        assertEquals(1, result.size)
        assertEquals(1001L, result[0].reservationId)
        assertEquals(10L, result[0].productOptionId)
        assertEquals("에티오피아 예가체프 G1", result[0].productName)
        assertEquals("ETH-HD-200", result[0].optionCode)
        assertEquals(2, result[0].quantity)
        assertEquals(InventoryReservationStatus.RESERVED, result[0].status)
        assertEquals(8, result[0].availableStock) // 10 - 2 = 8
    }

    @Test
    @DisplayName("이미 활성 예약이 있을 경우 DuplicateReservationException이 발생하고 save가 호출되지 않아야 한다")
    fun `이미 활성 예약이 있을 경우_DuplicateReservationException이 발생하고_save가 호출되지 않아야 한다`() {
        // Given
        val userId = 123L
        val cartItems = listOf(
            createCartItem(1L, 10L, "에티오피아 예가체프 G1", "ETH-HD-200", 2)
        )

        // 활성 예약이 이미 존재
        every { inventoryReservationRepository.countActiveReservations(userId) } returns 1

        // When & Then
        // [핵심 비즈니스 규칙 검증]: 중복 예약 방지
        val exception = assertThrows<DuplicateReservationException> {
            inventoryService.reserveInventory(userId, cartItems)
        }

        assertEquals("이미 진행 중인 주문 예약이 있습니다.", exception.message)

        // [검증]: 예외 발생 시 재고 조회 및 saveAll은 호출되지 않아야 함
        verify(exactly = 1) { inventoryReservationRepository.countActiveReservations(userId) }
        verify(exactly = 0) { inventoryRepository.calculateAvailableStockBatch(any()) }
        verify(exactly = 0) { inventoryReservationRepository.saveAll(any()) }
    }

    @Test
    @DisplayName("가용 재고가 부족할 경우 InsufficientAvailableStockException이 발생하고 save가 호출되지 않아야 한다")
    fun `가용 재고가 부족할 경우_InsufficientAvailableStockException이 발생하고_save가 호출되지 않아야 한다`() {
        // Given
        val userId = 123L
        val cartItems = listOf(
            createCartItem(1L, 10L, "에티오피아 예가체프 G1", "ETH-HD-200", 5) // 요청 5개
        )

        every { inventoryReservationRepository.countActiveReservations(userId) } returns 0
        every { inventoryRepository.calculateAvailableStockBatch(listOf(10L)) } returns mapOf(10L to 3) // 가용 재고 3개 (부족)

        // When & Then
        // [핵심 비즈니스 규칙 검증]: 재고 부족 시 예외 발생
        val exception = assertThrows<InsufficientAvailableStockException> {
            inventoryService.reserveInventory(userId, cartItems)
        }

        assertTrue(exception.message!!.contains("가용 재고가 부족합니다"))
        assertTrue(exception.message!!.contains("10"))

        // [검증]: 예외 발생 시 saveAll은 호출되지 않아야 함
        verify(exactly = 1) { inventoryReservationRepository.countActiveReservations(userId) }
        verify(exactly = 1) { inventoryRepository.calculateAvailableStockBatch(listOf(10L)) }
        verify(exactly = 0) { inventoryReservationRepository.saveAll(any()) }
    }

    @Test
    @DisplayName("여러 상품 예약 시 모든 상품에 대해 재고 검증 및 예약이 수행되어야 한다")
    fun `여러 상품 예약 시_모든 상품에 대해 재고 검증 및 예약이 수행되어야 한다`() {
        // Given
        val userId = 123L
        val cartItems = listOf(
            createCartItem(1L, 10L, "에티오피아 예가체프 G1", "ETH-HD-200", 2),
            createCartItem(2L, 20L, "콜롬비아 수프리모", "COL-WB-500", 1)
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

        every { inventoryReservationRepository.countActiveReservations(userId) } returns 0
        every { inventoryRepository.calculateAvailableStockBatch(listOf(10L, 20L)) } returns mapOf(10L to 10, 20L to 15)
        every { inventoryReservationRepository.saveAll(any()) } returns listOf(savedReservation1, savedReservation2)

        // When
        val result = inventoryService.reserveInventory(userId, cartItems)

        // Then
        // [검증]: 모든 상품에 대해 재고 조회(bulk) 및 예약 생성(bulk)이 호출되어야 함
        verify(exactly = 1) { inventoryReservationRepository.countActiveReservations(userId) }
        verify(exactly = 1) { inventoryRepository.calculateAvailableStockBatch(listOf(10L, 20L)) }
        verify(exactly = 1) { inventoryReservationRepository.saveAll(any()) }

        // [검증]: 2개의 예약 결과가 반환되어야 함
        assertEquals(2, result.size)
        assertEquals(1001L, result[0].reservationId)
        assertEquals(10L, result[0].productOptionId)
        assertEquals(1002L, result[1].reservationId)
        assertEquals(20L, result[1].productOptionId)
    }

    @Test
    @DisplayName("예약 생성 시 만료 시각이 현재 시각 + 30분으로 설정되어야 한다")
    fun `예약 생성 시_만료 시각이 현재 시각 + 30분으로 설정되어야 한다`() {
        // Given
        val userId = 123L
        val cartItems = listOf(
            createCartItem(1L, 10L, "에티오피아 예가체프 G1", "ETH-HD-200", 2)
        )

        val reservedAt = LocalDateTime.of(2025, 11, 4, 15, 30, 0)
        val expiresAt = reservedAt.plusMinutes(30) // 30분 후

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

        every { inventoryReservationRepository.countActiveReservations(userId) } returns 0
        every { inventoryRepository.calculateAvailableStockBatch(listOf(10L)) } returns mapOf(10L to 10)
        every { inventoryReservationRepository.saveAll(any()) } returns listOf(savedReservation)

        // When
        val result = inventoryService.reserveInventory(userId, cartItems)

        // Then
        // [비즈니스 규칙 검증]: 예약 만료 시각이 30분 후로 설정되어야 함
        assertEquals(reservedAt, result[0].reservedAt)
        assertEquals(expiresAt, result[0].expiresAt)
        assertEquals(reservedAt.plusMinutes(30), result[0].expiresAt)
    }

    @Test
    @DisplayName("예약 생성 시 상품 정보가 응답 DTO에 올바르게 매핑되어야 한다")
    fun `예약 생성 시_상품 정보가 응답 DTO에 올바르게 매핑되어야 한다`() {
        // Given
        val userId = 123L
        val cartItems = listOf(
            createCartItem(1L, 10L, "콜롬비아 수프리모", "COL-ESP-500", 3)
        )

        val savedReservation = InventoryReservationEntity(
            id = 1001L,
            productOptionId = 10L,
            userId = userId,
            quantity = 3,
            status = InventoryReservationStatus.RESERVED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )

        every { inventoryReservationRepository.countActiveReservations(userId) } returns 0
        every { inventoryRepository.calculateAvailableStockBatch(listOf(10L)) } returns mapOf(10L to 20)
        every { inventoryReservationRepository.saveAll(any()) } returns listOf(savedReservation)

        // When
        val result = inventoryService.reserveInventory(userId, cartItems)

        // Then
        // [데이터 매핑 검증]: CartItem 정보가 Response DTO에 올바르게 복사되었는가?
        assertEquals("콜롬비아 수프리모", result[0].productName)
        assertEquals("COL-ESP-500", result[0].optionCode)
        assertEquals(3, result[0].quantity)
        assertEquals(17, result[0].availableStock) // 20 - 3 = 17
    }

    // === Helper Method ===

    private fun createCartItem(
        cartItemId: Long,
        productOptionId: Long,
        productName: String,
        optionCode: String,
        quantity: Int
    ): CartItemResponse {
        return CartItemResponse(
            cartItemId = cartItemId,
            productOptionId = productOptionId,
            productName = productName,
            optionCode = optionCode,
            origin = "에티오피아",
            grindType = "홀빈",
            weightGrams = 200,
            price = 15000,
            quantity = quantity,
            totalPrice = 15000 * quantity,
            createdAt = now,
            updatedAt = now
        )
    }
}
