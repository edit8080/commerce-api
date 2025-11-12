package com.beanbliss.domain.inventory.repository

import com.beanbliss.common.test.RepositoryTestBase
import com.beanbliss.domain.inventory.domain.Inventory
import com.beanbliss.domain.inventory.entity.InventoryEntity
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

@DisplayName("Inventory Repository 통합 테스트")
class InventoryRepositoryImplTest : RepositoryTestBase() {

    @Autowired
    private lateinit var inventoryRepository: InventoryRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testProduct: ProductEntity
    private lateinit var testOption1: ProductOptionEntity
    private lateinit var testOption2: ProductOptionEntity
    private lateinit var testOption3: ProductOptionEntity
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

        // 테스트 상품 생성
        testProduct = ProductEntity(
            name = "에티오피아 예가체프",
            description = "고급 원두",
            brand = "Bean Bliss"
        )
        entityManager.persist(testProduct)
        entityManager.flush() // Product ID 생성

        // 테스트 상품 옵션 생성
        testOption1 = ProductOptionEntity(
            optionCode = "ETH-001",
            productId = testProduct.id,
            origin = "Ethiopia",
            grindType = "Whole Bean",
            weightGrams = 200,
            price = BigDecimal("15000"),
            isActive = true
        )
        testOption2 = ProductOptionEntity(
            optionCode = "ETH-002",
            productId = testProduct.id,
            origin = "Ethiopia",
            grindType = "Ground",
            weightGrams = 200,
            price = BigDecimal("15500"),
            isActive = true
        )
        testOption3 = ProductOptionEntity(
            optionCode = "ETH-003",
            productId = testProduct.id,
            origin = "Ethiopia",
            grindType = "Whole Bean",
            weightGrams = 300,
            price = BigDecimal("20000"),
            isActive = true
        )
        entityManager.persist(testOption1)
        entityManager.persist(testOption2)
        entityManager.persist(testOption3)
        entityManager.flush() // ProductOption ID 생성

        // 재고 생성
        val inventory1 = InventoryEntity(
            productOptionId = testOption1.id,
            stockQuantity = 100
        )
        val inventory2 = InventoryEntity(
            productOptionId = testOption2.id,
            stockQuantity = 50
        )
        val inventory3 = InventoryEntity(
            productOptionId = testOption3.id,
            stockQuantity = 200
        )
        entityManager.persist(inventory1)
        entityManager.persist(inventory2)
        entityManager.persist(inventory3)

        entityManager.flush()
        entityManager.clear()
    }

    @Test
    @DisplayName("가용 재고 계산 - 예약 없음")
    fun `calculateAvailableStock should return full stock when no reservations`() {
        // When
        val availableStock = inventoryRepository.calculateAvailableStock(testOption1.id)

        // Then: 예약이 없으므로 전체 재고 반환
        assertEquals(100, availableStock)
    }

    @Test
    @DisplayName("가용 재고 계산 - RESERVED 상태 예약 차감")
    fun `calculateAvailableStock should subtract reserved quantity`() {
        // Given: RESERVED 상태 예약 추가
        val reservation = InventoryReservationEntity(
            productOptionId = testOption1.id,
            userId = testUser.id,
            quantity = 10,
            status = InventoryReservationStatus.RESERVED,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        entityManager.persist(reservation)
        entityManager.flush()
        entityManager.clear()

        // When
        val availableStock = inventoryRepository.calculateAvailableStock(testOption1.id)

        // Then: 100 - 10 = 90
        assertEquals(90, availableStock)
    }

    @Test
    @DisplayName("가용 재고 계산 - CONFIRMED 상태 예약 차감")
    fun `calculateAvailableStock should subtract confirmed quantity`() {
        // Given: CONFIRMED 상태 예약 추가
        val reservation = InventoryReservationEntity(
            productOptionId = testOption1.id,
            userId = testUser.id,
            quantity = 20,
            status = InventoryReservationStatus.CONFIRMED,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        entityManager.persist(reservation)
        entityManager.flush()
        entityManager.clear()

        // When
        val availableStock = inventoryRepository.calculateAvailableStock(testOption1.id)

        // Then: 100 - 20 = 80
        assertEquals(80, availableStock)
    }

    @Test
    @DisplayName("가용 재고 계산 - EXPIRED 상태는 차감하지 않음")
    fun `calculateAvailableStock should not subtract expired reservations`() {
        // Given: EXPIRED 상태 예약 추가
        val expiredReservation = InventoryReservationEntity(
            productOptionId = testOption1.id,
            userId = testUser.id,
            quantity = 30,
            status = InventoryReservationStatus.EXPIRED,
            expiresAt = LocalDateTime.now().minusMinutes(10)
        )
        entityManager.persist(expiredReservation)
        entityManager.flush()
        entityManager.clear()

        // When
        val availableStock = inventoryRepository.calculateAvailableStock(testOption1.id)

        // Then: EXPIRED는 차감하지 않으므로 전체 재고 반환
        assertEquals(100, availableStock)
    }

    @Test
    @DisplayName("가용 재고 Batch 계산 - 여러 옵션 한 번에 조회")
    fun `calculateAvailableStockBatch should return available stock for multiple options`() {
        // Given: 각 옵션에 예약 추가
        val reservation1 = InventoryReservationEntity(
            productOptionId = testOption1.id,
            userId = testUser.id,
            quantity = 10,
            status = InventoryReservationStatus.RESERVED,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        val reservation2 = InventoryReservationEntity(
            productOptionId = testOption2.id,
            userId = testUser.id,
            quantity = 5,
            status = InventoryReservationStatus.CONFIRMED,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        entityManager.persist(reservation1)
        entityManager.persist(reservation2)
        entityManager.flush()
        entityManager.clear()

        // When
        val productOptionIds = listOf(testOption1.id, testOption2.id, testOption3.id)
        val availableStockMap = inventoryRepository.calculateAvailableStockBatch(productOptionIds)

        // Then
        assertEquals(3, availableStockMap.size)
        assertEquals(90, availableStockMap[testOption1.id])  // 100 - 10
        assertEquals(45, availableStockMap[testOption2.id])  // 50 - 5
        assertEquals(200, availableStockMap[testOption3.id]) // 200 - 0
    }

    @Test
    @DisplayName("가용 재고 Batch 계산 - 빈 리스트")
    fun `calculateAvailableStockBatch should return empty map for empty list`() {
        // When
        val availableStockMap = inventoryRepository.calculateAvailableStockBatch(emptyList())

        // Then
        assertTrue(availableStockMap.isEmpty())
    }

    @Test
    @DisplayName("재고 목록 조회 (INVENTORY 도메인만) - 페이징 및 정렬")
    fun `findAll should return inventory without product info`() {
        // When: INVENTORY 테이블만 조회 (PRODUCT JOIN 제거)
        val inventories = inventoryRepository.findAll(
            page = 1,
            size = 10,
            sortBy = "stock_quantity",
            sortDirection = "ASC"
        )

        // Then: INVENTORY 도메인 정보만 반환
        assertEquals(3, inventories.size)

        // 첫 번째 (stockQuantity ASC 정렬)
        val firstInventory = inventories[0]
        assertEquals(testOption2.id, firstInventory.productOptionId)
        assertEquals(50, firstInventory.stockQuantity)
        // PRODUCT 정보는 UseCase 계층에서 조합하므로 여기서는 검증하지 않음
    }

    @Test
    @DisplayName("전체 재고 개수 조회")
    fun `count should return total inventory count`() {
        // When
        val count = inventoryRepository.count()

        // Then
        assertEquals(3, count)
    }

    @Test
    @DisplayName("상품 옵션 ID로 재고 조회 - 성공")
    fun `findByProductOptionId should return inventory when exists`() {
        // When
        val inventory = inventoryRepository.findByProductOptionId(testOption1.id)

        // Then
        assertNotNull(inventory)
        assertEquals(testOption1.id, inventory!!.productOptionId)
        assertEquals(100, inventory.stockQuantity)
    }

    @Test
    @DisplayName("상품 옵션 ID로 재고 조회 - 존재하지 않는 경우")
    fun `findByProductOptionId should return null when not exists`() {
        // When
        val inventory = inventoryRepository.findByProductOptionId(999L)

        // Then
        assertNull(inventory)
    }

    @Test
    @DisplayName("여러 상품 옵션 ID로 재고 일괄 조회")
    fun `findAllByProductOptionIds should return inventories for multiple option ids`() {
        // When
        val productOptionIds = listOf(testOption1.id, testOption2.id)
        val inventories = inventoryRepository.findAllByProductOptionIds(productOptionIds)

        // Then
        assertEquals(2, inventories.size)

        val inventoryMap = inventories.associateBy { it.productOptionId }
        assertTrue(inventoryMap.containsKey(testOption1.id))
        assertTrue(inventoryMap.containsKey(testOption2.id))
        assertEquals(100, inventoryMap[testOption1.id]!!.stockQuantity)
        assertEquals(50, inventoryMap[testOption2.id]!!.stockQuantity)
    }

    @Test
    @DisplayName("여러 상품 옵션 ID로 재고 일괄 조회 - 빈 리스트")
    fun `findAllByProductOptionIds should return empty list for empty ids`() {
        // When
        val inventories = inventoryRepository.findAllByProductOptionIds(emptyList())

        // Then
        assertTrue(inventories.isEmpty())
    }

    @Test
    @DisplayName("재고 저장 - 신규 생성")
    fun `save should create new inventory`() {
        // Given: 새로운 상품 옵션 생성
        val newOption = ProductOptionEntity(
            optionCode = "NEW-001",
            productId = testProduct.id,
            origin = "Colombia",
            grindType = "Whole Bean",
            weightGrams = 250,
            price = BigDecimal("18000"),
            isActive = true
        )
        entityManager.persist(newOption)
        entityManager.flush()

        val newInventory = Inventory(
            productOptionId = newOption.id,
            stockQuantity = 150
        )

        // When
        val savedInventory = inventoryRepository.save(newInventory)

        // Then
        assertEquals(newOption.id, savedInventory.productOptionId)
        assertEquals(150, savedInventory.stockQuantity)
    }

    @Test
    @DisplayName("재고 저장 - 기존 재고 업데이트")
    fun `save should update existing inventory`() {
        // Given: 기존 재고 조회 및 수정
        val existingInventory = inventoryRepository.findByProductOptionId(testOption1.id)!!
        val updatedInventory = Inventory(
            productOptionId = existingInventory.productOptionId,
            stockQuantity = 150
        )

        // When
        val savedInventory = inventoryRepository.save(updatedInventory)

        // Then
        assertEquals(testOption1.id, savedInventory.productOptionId)
        assertEquals(150, savedInventory.stockQuantity)

        // 실제로 업데이트되었는지 재조회 검증
        val reloadedInventory = inventoryRepository.findByProductOptionId(testOption1.id)
        assertEquals(150, reloadedInventory!!.stockQuantity)
    }
}
