package com.beanbliss.domain.inventory.repository

import com.beanbliss.common.test.RepositoryTestBase
import com.beanbliss.domain.inventory.entity.InventoryReservationEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import com.beanbliss.domain.product.entity.ProductEntity
import com.beanbliss.domain.product.entity.ProductOptionEntity
import com.beanbliss.domain.user.entity.UserEntity
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("InventoryReservation Repository 통합 테스트")
class InventoryReservationRepositoryImplTest : RepositoryTestBase() {

    @Autowired
    private lateinit var inventoryReservationRepository: InventoryReservationRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testUser1: UserEntity
    private lateinit var testUser2: UserEntity
    private lateinit var testProduct: ProductEntity
    private lateinit var testOption: ProductOptionEntity

    @BeforeEach
    fun setUpTestData() {
        // 테스트 사용자 생성
        testUser1 = UserEntity(
            email = "user1@example.com",
            password = "password123",
            name = "사용자1"
        )
        testUser2 = UserEntity(
            email = "user2@example.com",
            password = "password123",
            name = "사용자2"
        )
        entityManager.persist(testUser1)
        entityManager.persist(testUser2)

        // 테스트 상품 생성
        testProduct = ProductEntity(
            name = "에티오피아 예가체프",
            description = "고급 원두",
            brand = "Bean Bliss"
        )
        entityManager.persist(testProduct)

        // 테스트 상품 옵션 생성
        testOption = ProductOptionEntity(
            optionCode = "ETH-001",
            productId = testProduct.id,
            origin = "Ethiopia",
            grindType = "Whole Bean",
            weightGrams = 200,
            price = BigDecimal("15000"),
            isActive = true
        )
        entityManager.persist(testOption)

        entityManager.flush()
        entityManager.clear()
    }

    @Test
    @DisplayName("사용자의 활성 예약 개수 조회 - RESERVED 상태")
    fun `countActiveReservations should count reserved reservations`() {
        // Given: RESERVED 상태 예약 2개 생성
        val reservation1 = InventoryReservationEntity(
            productOptionId = testOption.id,
            userId = testUser1.id,
            quantity = 5,
            status = InventoryReservationStatus.RESERVED,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        val reservation2 = InventoryReservationEntity(
            productOptionId = testOption.id,
            userId = testUser1.id,
            quantity = 3,
            status = InventoryReservationStatus.RESERVED,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        entityManager.persist(reservation1)
        entityManager.persist(reservation2)
        entityManager.flush()
        entityManager.clear()

        // When
        val count = inventoryReservationRepository.countActiveReservations(testUser1.id)

        // Then
        assertEquals(2, count)
    }

    @Test
    @DisplayName("사용자의 활성 예약 개수 조회 - CONFIRMED 상태 포함")
    fun `countActiveReservations should count confirmed reservations`() {
        // Given: CONFIRMED 상태 예약 생성
        val reservation = InventoryReservationEntity(
            productOptionId = testOption.id,
            userId = testUser1.id,
            quantity = 5,
            status = InventoryReservationStatus.CONFIRMED,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        entityManager.persist(reservation)
        entityManager.flush()
        entityManager.clear()

        // When
        val count = inventoryReservationRepository.countActiveReservations(testUser1.id)

        // Then
        assertEquals(1, count)
    }

    @Test
    @DisplayName("사용자의 활성 예약 개수 조회 - 만료된 예약 제외")
    fun `countActiveReservations should not count expired reservations`() {
        // Given: 만료된 예약 생성
        val expiredReservation = InventoryReservationEntity(
            productOptionId = testOption.id,
            userId = testUser1.id,
            quantity = 5,
            status = InventoryReservationStatus.RESERVED,
            expiresAt = LocalDateTime.now().minusMinutes(10)  // 만료됨
        )
        entityManager.persist(expiredReservation)
        entityManager.flush()
        entityManager.clear()

        // When
        val count = inventoryReservationRepository.countActiveReservations(testUser1.id)

        // Then
        assertEquals(0, count)
    }

    @Test
    @DisplayName("상품 옵션별 예약 수량 합계 조회 - RESERVED 상태")
    fun `sumQuantityByProductOptionIdAndStatus should sum reserved quantities`() {
        // Given: 여러 사용자의 예약 생성
        val reservation1 = InventoryReservationEntity(
            productOptionId = testOption.id,
            userId = testUser1.id,
            quantity = 5,
            status = InventoryReservationStatus.RESERVED,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        val reservation2 = InventoryReservationEntity(
            productOptionId = testOption.id,
            userId = testUser2.id,
            quantity = 10,
            status = InventoryReservationStatus.RESERVED,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        entityManager.persist(reservation1)
        entityManager.persist(reservation2)
        entityManager.flush()
        entityManager.clear()

        // When
        val totalQuantity = inventoryReservationRepository.sumQuantityByProductOptionIdAndStatus(
            testOption.id,
            listOf(InventoryReservationStatus.RESERVED)
        )

        // Then
        assertEquals(15, totalQuantity)
    }

    @Test
    @DisplayName("상품 옵션별 예약 수량 합계 조회 - CONFIRMED 포함")
    fun `sumQuantityByProductOptionIdAndStatus should sum multiple statuses`() {
        // Given: RESERVED와 CONFIRMED 상태 예약 생성
        val reservation1 = InventoryReservationEntity(
            productOptionId = testOption.id,
            userId = testUser1.id,
            quantity = 5,
            status = InventoryReservationStatus.RESERVED,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        val reservation2 = InventoryReservationEntity(
            productOptionId = testOption.id,
            userId = testUser2.id,
            quantity = 7,
            status = InventoryReservationStatus.CONFIRMED,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        entityManager.persist(reservation1)
        entityManager.persist(reservation2)
        entityManager.flush()
        entityManager.clear()

        // When
        val totalQuantity = inventoryReservationRepository.sumQuantityByProductOptionIdAndStatus(
            testOption.id,
            listOf(InventoryReservationStatus.RESERVED, InventoryReservationStatus.CONFIRMED)
        )

        // Then
        assertEquals(12, totalQuantity)
    }

    @Test
    @DisplayName("예약 저장 - 신규 생성")
    fun `save should create new reservation`() {
        // Given
        val newReservation = InventoryReservationEntity(
            productOptionId = testOption.id,
            userId = testUser1.id,
            quantity = 5,
            status = InventoryReservationStatus.RESERVED,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )

        // When
        val savedReservation = inventoryReservationRepository.save(newReservation)

        // Then
        assertTrue(savedReservation.id > 0)
        assertEquals(testOption.id, savedReservation.productOptionId)
        assertEquals(testUser1.id, savedReservation.userId)
        assertEquals(5, savedReservation.quantity)
        assertEquals(InventoryReservationStatus.RESERVED, savedReservation.status)
    }

    @Test
    @DisplayName("예약 일괄 저장")
    fun `saveAll should save multiple reservations`() {
        // Given
        val reservation1 = InventoryReservationEntity(
            productOptionId = testOption.id,
            userId = testUser1.id,
            quantity = 5,
            status = InventoryReservationStatus.RESERVED,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        val reservation2 = InventoryReservationEntity(
            productOptionId = testOption.id,
            userId = testUser2.id,
            quantity = 3,
            status = InventoryReservationStatus.RESERVED,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )

        // When
        val savedReservations = inventoryReservationRepository.saveAll(listOf(reservation1, reservation2))

        // Then
        assertEquals(2, savedReservations.size)
        assertTrue(savedReservations.all { it.id > 0 })
    }

    @Test
    @DisplayName("사용자 ID로 활성 예약 목록 조회 - RESERVED 상태만")
    fun `findActiveReservationsByUserId should return active reservations`() {
        // Given: 여러 상태의 예약 생성
        val activeReservation = InventoryReservationEntity(
            productOptionId = testOption.id,
            userId = testUser1.id,
            quantity = 5,
            status = InventoryReservationStatus.RESERVED,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        val expiredReservation = InventoryReservationEntity(
            productOptionId = testOption.id,
            userId = testUser1.id,
            quantity = 3,
            status = InventoryReservationStatus.RESERVED,
            expiresAt = LocalDateTime.now().minusMinutes(10)  // 만료됨
        )
        val cancelledReservation = InventoryReservationEntity(
            productOptionId = testOption.id,
            userId = testUser1.id,
            quantity = 2,
            status = InventoryReservationStatus.CANCELLED,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        entityManager.persist(activeReservation)
        entityManager.persist(expiredReservation)
        entityManager.persist(cancelledReservation)
        entityManager.flush()
        entityManager.clear()

        // When
        val reservations = inventoryReservationRepository.findActiveReservationsByUserId(testUser1.id)

        // Then: RESERVED/CONFIRMED 상태이면서 만료되지 않은 것만 조회
        assertEquals(1, reservations.size)
        assertEquals(5, reservations[0].quantity)
    }

    @Test
    @DisplayName("사용자 ID로 활성 예약 목록 조회 - CONFIRMED 포함")
    fun `findActiveReservationsByUserId should include confirmed reservations`() {
        // Given: RESERVED와 CONFIRMED 예약 생성
        val reservedReservation = InventoryReservationEntity(
            productOptionId = testOption.id,
            userId = testUser1.id,
            quantity = 5,
            status = InventoryReservationStatus.RESERVED,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        val confirmedReservation = InventoryReservationEntity(
            productOptionId = testOption.id,
            userId = testUser1.id,
            quantity = 7,
            status = InventoryReservationStatus.CONFIRMED,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        entityManager.persist(reservedReservation)
        entityManager.persist(confirmedReservation)
        entityManager.flush()
        entityManager.clear()

        // When
        val reservations = inventoryReservationRepository.findActiveReservationsByUserId(testUser1.id)

        // Then
        assertEquals(2, reservations.size)
    }
}
