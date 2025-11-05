package com.beanbliss.domain.inventory.repository

import com.beanbliss.domain.inventory.dto.InventoryResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * InventoryRepository의 재고 목록 조회 로직을 검증하는 테스트
 *
 * 검증 목표:
 * 1. INVENTORY + PRODUCT_OPTION + PRODUCT를 JOIN하여 조회하는가?
 * 2. 지정된 정렬 기준으로 정렬되는가? (created_at DESC)
 * 3. 페이징이 올바르게 적용되는가?
 * 4. count()가 정확한 전체 개수를 반환하는가?
 */
@DisplayName("재고 목록 조회 Repository 테스트")
class InventoryListRepositoryTest {

    private lateinit var inventoryRepository: FakeInventoryListRepository

    @BeforeEach
    fun setUp() {
        inventoryRepository = FakeInventoryListRepository()
    }

    @Test
    @DisplayName("created_at DESC 순으로 정렬되어야 한다")
    fun `created_at DESC 순으로 정렬되어야 한다`() {
        // Given: 3개의 재고 (다른 등록 시각)
        inventoryRepository.addInventory(
            createInventoryResponse(
                inventoryId = 1L,
                productName = "재고 A",
                createdAt = LocalDateTime.of(2025, 11, 1, 10, 0)
            )
        )
        inventoryRepository.addInventory(
            createInventoryResponse(
                inventoryId = 2L,
                productName = "재고 B",
                createdAt = LocalDateTime.of(2025, 11, 4, 10, 0) // 최신
            )
        )
        inventoryRepository.addInventory(
            createInventoryResponse(
                inventoryId = 3L,
                productName = "재고 C",
                createdAt = LocalDateTime.of(2025, 11, 3, 10, 0)
            )
        )

        // When
        val result = inventoryRepository.findAllWithProductInfo(
            page = 1,
            size = 10,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        assertEquals(3, result.size)
        assertEquals("재고 B", result[0].productName, "최신 재고가 첫 번째여야 함")
        assertEquals("재고 C", result[1].productName, "두 번째로 최신 재고")
        assertEquals("재고 A", result[2].productName, "가장 오래된 재고가 마지막")

        // 날짜 순서 확인
        assertTrue(
            result[0].createdAt.isAfter(result[1].createdAt),
            "첫 번째 재고가 두 번째보다 최신이어야 함"
        )
        assertTrue(
            result[1].createdAt.isAfter(result[2].createdAt),
            "두 번째 재고가 세 번째보다 최신이어야 함"
        )
    }

    @Test
    @DisplayName("created_at ASC 순으로 정렬되어야 한다")
    fun `created_at ASC 순으로 정렬되어야 한다`() {
        // Given: 3개의 재고 (다른 등록 시각)
        inventoryRepository.addInventory(
            createInventoryResponse(
                inventoryId = 1L,
                productName = "재고 A",
                createdAt = LocalDateTime.of(2025, 11, 1, 10, 0) // 가장 오래됨
            )
        )
        inventoryRepository.addInventory(
            createInventoryResponse(
                inventoryId = 2L,
                productName = "재고 B",
                createdAt = LocalDateTime.of(2025, 11, 4, 10, 0)
            )
        )
        inventoryRepository.addInventory(
            createInventoryResponse(
                inventoryId = 3L,
                productName = "재고 C",
                createdAt = LocalDateTime.of(2025, 11, 3, 10, 0)
            )
        )

        // When
        val result = inventoryRepository.findAllWithProductInfo(
            page = 1,
            size = 10,
            sortBy = "created_at",
            sortDirection = "ASC"
        )

        // Then
        assertEquals(3, result.size)
        assertEquals("재고 A", result[0].productName, "가장 오래된 재고가 첫 번째여야 함")
        assertEquals("재고 C", result[1].productName, "두 번째로 오래된 재고")
        assertEquals("재고 B", result[2].productName, "최신 재고가 마지막")

        // 날짜 순서 확인
        assertTrue(
            result[0].createdAt.isBefore(result[1].createdAt),
            "첫 번째 재고가 두 번째보다 오래되어야 함"
        )
        assertTrue(
            result[1].createdAt.isBefore(result[2].createdAt),
            "두 번째 재고가 세 번째보다 오래되어야 함"
        )
    }

    @Test
    @DisplayName("페이징이 올바르게 적용되어야 한다 - 첫 번째 페이지")
    fun `페이징이 올바르게 적용되어야 한다 - 첫 번째 페이지`() {
        // Given: 총 12개의 재고
        for (i in 1..12) {
            inventoryRepository.addInventory(
                createInventoryResponse(
                    inventoryId = i.toLong(),
                    productName = "재고 $i",
                    createdAt = LocalDateTime.now().plusDays(i.toLong())
                )
            )
        }

        // When: 페이지 크기 5로 첫 번째 페이지 조회
        val result = inventoryRepository.findAllWithProductInfo(
            page = 1,
            size = 5,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        assertEquals(5, result.size, "첫 번째 페이지는 5개의 재고를 포함해야 함")
    }

    @Test
    @DisplayName("페이징이 올바르게 적용되어야 한다 - 두 번째 페이지")
    fun `페이징이 올바르게 적용되어야 한다 - 두 번째 페이지`() {
        // Given: 총 12개의 재고
        for (i in 1..12) {
            inventoryRepository.addInventory(
                createInventoryResponse(
                    inventoryId = i.toLong(),
                    productName = "재고 $i",
                    createdAt = LocalDateTime.now().plusDays(i.toLong())
                )
            )
        }

        // When: 페이지 크기 5로 두 번째 페이지 조회
        val result = inventoryRepository.findAllWithProductInfo(
            page = 2,
            size = 5,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        assertEquals(5, result.size, "두 번째 페이지도 5개의 재고를 포함해야 함")
    }

    @Test
    @DisplayName("페이징이 올바르게 적용되어야 한다 - 마지막 페이지")
    fun `페이징이 올바르게 적용되어야 한다 - 마지막 페이지`() {
        // Given: 총 12개의 재고
        for (i in 1..12) {
            inventoryRepository.addInventory(
                createInventoryResponse(
                    inventoryId = i.toLong(),
                    productName = "재고 $i",
                    createdAt = LocalDateTime.now().plusDays(i.toLong())
                )
            )
        }

        // When: 페이지 크기 5로 세 번째 페이지 조회
        val result = inventoryRepository.findAllWithProductInfo(
            page = 3,
            size = 5,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        assertEquals(2, result.size, "마지막 페이지는 나머지 2개의 재고만 포함해야 함")
    }

    @Test
    @DisplayName("재고가 없을 경우 빈 리스트를 반환해야 한다")
    fun `재고가 없을 경우 빈 리스트를 반환해야 한다`() {
        // Given: 재고 없음

        // When
        val result = inventoryRepository.findAllWithProductInfo(
            page = 1,
            size = 20,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        assertTrue(result.isEmpty(), "재고가 없으면 빈 리스트를 반환해야 함")
    }

    @Test
    @DisplayName("count()는 전체 재고 개수를 정확하게 반환해야 한다")
    fun `count()는 전체 재고 개수를 정확하게 반환해야 한다`() {
        // Given: 45개의 재고
        for (i in 1..45) {
            inventoryRepository.addInventory(
                createInventoryResponse(
                    inventoryId = i.toLong(),
                    productName = "재고 $i",
                    createdAt = LocalDateTime.now()
                )
            )
        }

        // When
        val count = inventoryRepository.count()

        // Then
        assertEquals(45L, count, "전체 재고 개수가 정확하게 반환되어야 함")
    }

    @Test
    @DisplayName("count()는 재고가 없을 경우 0을 반환해야 한다")
    fun `count()는 재고가 없을 경우 0을 반환해야 한다`() {
        // Given: 재고 없음

        // When
        val count = inventoryRepository.count()

        // Then
        assertEquals(0L, count, "재고가 없으면 0을 반환해야 함")
    }

    @Test
    @DisplayName("상품 정보와 옵션 정보가 함께 조회되어야 한다")
    fun `상품 정보와 옵션 정보가 함께 조회되어야 한다`() {
        // Given: 재고 정보 (상품, 옵션 포함)
        inventoryRepository.addInventory(
            InventoryResponse(
                inventoryId = 1L,
                productId = 10L,
                productName = "에티오피아 예가체프 G1",
                productOptionId = 100L,
                optionCode = "ETH-HD-200",
                optionName = "핸드드립용 200g",
                price = 15000,
                stockQuantity = 50,
                createdAt = LocalDateTime.now()
            )
        )

        // When
        val result = inventoryRepository.findAllWithProductInfo(
            page = 1,
            size = 10,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        assertEquals(1, result.size)
        val inventory = result[0]
        assertEquals(1L, inventory.inventoryId)
        assertEquals(10L, inventory.productId)
        assertEquals("에티오피아 예가체프 G1", inventory.productName)
        assertEquals(100L, inventory.productOptionId)
        assertEquals("ETH-HD-200", inventory.optionCode)
        assertEquals("핸드드립용 200g", inventory.optionName)
        assertEquals(15000, inventory.price)
        assertEquals(50, inventory.stockQuantity)
    }

    @Test
    @DisplayName("동일 상품의 다른 옵션 재고가 각각 조회되어야 한다")
    fun `동일 상품의 다른 옵션 재고가 각각 조회되어야 한다`() {
        // Given: 동일 상품의 2개 옵션
        inventoryRepository.addInventory(
            InventoryResponse(
                inventoryId = 1L,
                productId = 10L,
                productName = "에티오피아 예가체프 G1",
                productOptionId = 100L,
                optionCode = "ETH-HD-200",
                optionName = "핸드드립용 200g",
                price = 15000,
                stockQuantity = 50,
                createdAt = LocalDateTime.of(2025, 11, 4, 10, 30)
            )
        )
        inventoryRepository.addInventory(
            InventoryResponse(
                inventoryId = 2L,
                productId = 10L,
                productName = "에티오피아 예가체프 G1",
                productOptionId = 101L,
                optionCode = "ETH-WB-500",
                optionName = "원두 500g",
                price = 28000,
                stockQuantity = 30,
                createdAt = LocalDateTime.of(2025, 11, 4, 9, 15)
            )
        )

        // When
        val result = inventoryRepository.findAllWithProductInfo(
            page = 1,
            size = 10,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        assertEquals(2, result.size, "동일 상품의 2개 옵션이 각각 조회되어야 함")
        assertEquals(1L, result[0].inventoryId)
        assertEquals(100L, result[0].productOptionId)
        assertEquals(2L, result[1].inventoryId)
        assertEquals(101L, result[1].productOptionId)
    }

    @Test
    @DisplayName("재고가 0인 항목도 조회되어야 한다")
    fun `재고가 0인 항목도 조회되어야 한다`() {
        // Given: 재고 0
        inventoryRepository.addInventory(
            createInventoryResponse(
                inventoryId = 1L,
                productName = "품절 상품",
                stockQuantity = 0
            )
        )

        // When
        val result = inventoryRepository.findAllWithProductInfo(
            page = 1,
            size = 10,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        assertEquals(1, result.size)
        assertEquals(0, result[0].stockQuantity, "재고가 0인 항목도 조회되어야 함")
    }

    // === Helper Methods ===

    /**
     * 테스트용 재고 응답 생성 (기본값 사용)
     */
    private fun createInventoryResponse(
        inventoryId: Long,
        productName: String,
        stockQuantity: Int = 100,
        createdAt: LocalDateTime = LocalDateTime.now()
    ): InventoryResponse {
        return InventoryResponse(
            inventoryId = inventoryId,
            productId = inventoryId, // 간단히 inventoryId를 productId로 사용
            productName = productName,
            productOptionId = inventoryId, // 간단히 inventoryId를 productOptionId로 사용
            optionCode = "OPT-$inventoryId",
            optionName = "옵션 $inventoryId",
            price = 10000,
            stockQuantity = stockQuantity,
            createdAt = createdAt
        )
    }
}
