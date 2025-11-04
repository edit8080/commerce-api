package com.beanbliss.domain.inventory.repository

import com.beanbliss.domain.inventory.entity.InventoryReservationEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * [책임]: InventoryReservationRepository의 계약 검증
 * - 활성 예약 개수 조회
 * - 예약 수량 합계 조회
 * - 예약 저장
 * - 활성 예약 목록 조회
 */
@DisplayName("재고 예약 Repository 테스트")
class InventoryReservationRepositoryTest {

    private lateinit var inventoryReservationRepository: InventoryReservationRepository

    @BeforeEach
    fun setUp() {
        inventoryReservationRepository = FakeInventoryReservationRepository()
    }

    @Test
    @DisplayName("활성 예약이 없을 경우 0을 반환해야 한다")
    fun `활성 예약이 없을 경우_0을 반환해야 한다`() {
        // Given
        val userId = 123L

        // When
        val count = inventoryReservationRepository.countActiveReservations(userId)

        // Then
        assertEquals(0, count)
    }

    @Test
    @DisplayName("활성 예약이 있을 경우 개수를 반환해야 한다")
    fun `활성 예약이 있을 경우_개수를 반환해야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val reservation1 = InventoryReservationEntity(
            id = 0L,
            productOptionId = 10L,
            userId = userId,
            quantity = 2,
            status = InventoryReservationStatus.RESERVED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )

        val reservation2 = InventoryReservationEntity(
            id = 0L,
            productOptionId = 20L,
            userId = userId,
            quantity = 1,
            status = InventoryReservationStatus.RESERVED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )

        inventoryReservationRepository.save(reservation1)
        inventoryReservationRepository.save(reservation2)

        // When
        val count = inventoryReservationRepository.countActiveReservations(userId)

        // Then
        assertEquals(2, count)
    }

    @Test
    @DisplayName("만료된 예약은 활성 예약 개수에 포함되지 않아야 한다")
    fun `만료된 예약은 활성 예약 개수에 포함되지 않아야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val expiredReservation = InventoryReservationEntity(
            id = 0L,
            productOptionId = 10L,
            userId = userId,
            quantity = 2,
            status = InventoryReservationStatus.RESERVED,
            reservedAt = now.minusHours(1),
            expiresAt = now.minusMinutes(10),  // 만료됨
            updatedAt = now.minusHours(1)
        )

        inventoryReservationRepository.save(expiredReservation)

        // When
        val count = inventoryReservationRepository.countActiveReservations(userId)

        // Then
        assertEquals(0, count)
    }

    @Test
    @DisplayName("CANCELLED 상태의 예약은 활성 예약 개수에 포함되지 않아야 한다")
    fun `CANCELLED 상태의 예약은 활성 예약 개수에 포함되지 않아야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val cancelledReservation = InventoryReservationEntity(
            id = 0L,
            productOptionId = 10L,
            userId = userId,
            quantity = 2,
            status = InventoryReservationStatus.CANCELLED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )

        inventoryReservationRepository.save(cancelledReservation)

        // When
        val count = inventoryReservationRepository.countActiveReservations(userId)

        // Then
        assertEquals(0, count)
    }

    @Test
    @DisplayName("상품 옵션별 예약 수량 합계를 정확히 계산해야 한다")
    fun `상품 옵션별 예약 수량 합계를 정확히 계산해야 한다`() {
        // Given
        val productOptionId = 10L
        val now = LocalDateTime.now()

        val reservation1 = InventoryReservationEntity(
            id = 0L,
            productOptionId = productOptionId,
            userId = 100L,
            quantity = 3,
            status = InventoryReservationStatus.RESERVED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )

        val reservation2 = InventoryReservationEntity(
            id = 0L,
            productOptionId = productOptionId,
            userId = 200L,
            quantity = 2,
            status = InventoryReservationStatus.CONFIRMED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )

        inventoryReservationRepository.save(reservation1)
        inventoryReservationRepository.save(reservation2)

        // When
        val sum = inventoryReservationRepository.sumQuantityByProductOptionIdAndStatus(
            productOptionId,
            InventoryReservationStatus.activeStatuses()
        )

        // Then
        assertEquals(5, sum)
    }

    @Test
    @DisplayName("특정 상태의 예약만 수량 합계에 포함되어야 한다")
    fun `특정 상태의 예약만 수량 합계에 포함되어야 한다`() {
        // Given
        val productOptionId = 10L
        val now = LocalDateTime.now()

        val reservedReservation = InventoryReservationEntity(
            id = 0L,
            productOptionId = productOptionId,
            userId = 100L,
            quantity = 3,
            status = InventoryReservationStatus.RESERVED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )

        val cancelledReservation = InventoryReservationEntity(
            id = 0L,
            productOptionId = productOptionId,
            userId = 200L,
            quantity = 5,
            status = InventoryReservationStatus.CANCELLED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )

        inventoryReservationRepository.save(reservedReservation)
        inventoryReservationRepository.save(cancelledReservation)

        // When
        val sum = inventoryReservationRepository.sumQuantityByProductOptionIdAndStatus(
            productOptionId,
            listOf(InventoryReservationStatus.RESERVED)
        )

        // Then
        assertEquals(3, sum)  // CANCELLED는 포함되지 않음
    }

    @Test
    @DisplayName("예약이 없을 경우 수량 합계는 0이어야 한다")
    fun `예약이 없을 경우_수량 합계는 0이어야 한다`() {
        // Given
        val productOptionId = 10L

        // When
        val sum = inventoryReservationRepository.sumQuantityByProductOptionIdAndStatus(
            productOptionId,
            InventoryReservationStatus.activeStatuses()
        )

        // Then
        assertEquals(0, sum)
    }

    @Test
    @DisplayName("예약 저장 시 ID가 할당되어야 한다")
    fun `예약 저장 시_ID가 할당되어야 한다`() {
        // Given
        val now = LocalDateTime.now()
        val reservation = InventoryReservationEntity(
            id = 0L,
            productOptionId = 10L,
            userId = 123L,
            quantity = 2,
            status = InventoryReservationStatus.RESERVED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )

        // When
        val saved = inventoryReservationRepository.save(reservation)

        // Then
        assertTrue(saved.id > 0)
        assertEquals(10L, saved.productOptionId)
        assertEquals(123L, saved.userId)
        assertEquals(2, saved.quantity)
        assertEquals(InventoryReservationStatus.RESERVED, saved.status)
    }

    @Test
    @DisplayName("사용자의 활성 예약 목록을 조회할 수 있어야 한다")
    fun `사용자의 활성 예약 목록을 조회할 수 있어야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val reservation1 = InventoryReservationEntity(
            id = 0L,
            productOptionId = 10L,
            userId = userId,
            quantity = 2,
            status = InventoryReservationStatus.RESERVED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )

        val reservation2 = InventoryReservationEntity(
            id = 0L,
            productOptionId = 20L,
            userId = userId,
            quantity = 1,
            status = InventoryReservationStatus.CONFIRMED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )

        // 다른 사용자의 예약
        val otherUserReservation = InventoryReservationEntity(
            id = 0L,
            productOptionId = 30L,
            userId = 999L,
            quantity = 3,
            status = InventoryReservationStatus.RESERVED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )

        inventoryReservationRepository.save(reservation1)
        inventoryReservationRepository.save(reservation2)
        inventoryReservationRepository.save(otherUserReservation)

        // When
        val reservations = inventoryReservationRepository.findActiveReservationsByUserId(userId)

        // Then
        assertEquals(2, reservations.size)
        assertTrue(reservations.all { it.userId == userId })
        assertTrue(reservations.any { it.productOptionId == 10L })
        assertTrue(reservations.any { it.productOptionId == 20L })
    }

    @Test
    @DisplayName("만료되거나 취소된 예약은 활성 예약 목록에 포함되지 않아야 한다")
    fun `만료되거나 취소된 예약은 활성 예약 목록에 포함되지 않아야 한다`() {
        // Given
        val userId = 123L
        val now = LocalDateTime.now()

        val activeReservation = InventoryReservationEntity(
            id = 0L,
            productOptionId = 10L,
            userId = userId,
            quantity = 2,
            status = InventoryReservationStatus.RESERVED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )

        val expiredReservation = InventoryReservationEntity(
            id = 0L,
            productOptionId = 20L,
            userId = userId,
            quantity = 1,
            status = InventoryReservationStatus.RESERVED,
            reservedAt = now.minusHours(1),
            expiresAt = now.minusMinutes(10),
            updatedAt = now.minusHours(1)
        )

        val cancelledReservation = InventoryReservationEntity(
            id = 0L,
            productOptionId = 30L,
            userId = userId,
            quantity = 3,
            status = InventoryReservationStatus.CANCELLED,
            reservedAt = now,
            expiresAt = now.plusMinutes(30),
            updatedAt = now
        )

        inventoryReservationRepository.save(activeReservation)
        inventoryReservationRepository.save(expiredReservation)
        inventoryReservationRepository.save(cancelledReservation)

        // When
        val reservations = inventoryReservationRepository.findActiveReservationsByUserId(userId)

        // Then
        assertEquals(1, reservations.size)
        assertEquals(10L, reservations[0].productOptionId)
    }
}
