package com.beanbliss.domain.inventory.service

import com.beanbliss.common.exception.InvalidPageNumberException
import com.beanbliss.common.exception.InvalidPageSizeException
import com.beanbliss.domain.inventory.dto.InventoryResponse
import com.beanbliss.domain.inventory.repository.InventoryRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

/**
 * 재고 목록 조회 Service의 비즈니스 로직을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. 파라미터 유효성 검증이 올바르게 수행되는가?
 * 2. Repository 호출이 올바른 파라미터로 이루어지는가?
 * 3. 페이지 정보가 올바르게 조립되는가?
 * 4. created_at DESC 정렬이 적용되는가?
 *
 * [관련 API]:
 * - GET /api/inventories
 */
@DisplayName("재고 목록 조회 Service 테스트")
class InventoryListServiceTest {

    // Mock 객체 (Repository Interface에 의존)
    private val inventoryRepository: InventoryRepository = mockk()

    // 테스트 대상 (Service 인터페이스로 선언)
    private lateinit var inventoryService: InventoryService

    @BeforeEach
    fun setUp() {
        inventoryService = InventoryServiceImpl(inventoryRepository)
    }

    @Test
    @DisplayName("정상 조회 시 Repository의 findAllWithProductInfo와 count가 호출되어야 한다")
    fun `정상 조회 시 Repository의 findAllWithProductInfo와 count가 호출되어야 한다`() {
        // Given
        val page = 1
        val size = 20
        val mockInventories = listOf(
            createMockInventory(1L, 1L, "에티오피아 예가체프 G1", 1L, "ETH-HD-200", "핸드드립용 200g", 15000, 50),
            createMockInventory(2L, 1L, "에티오피아 예가체프 G1", 2L, "ETH-WB-500", "원두 500g", 28000, 30)
        )

        every {
            inventoryRepository.findAllWithProductInfo(page, size, "created_at", "DESC")
        } returns mockInventories
        every { inventoryRepository.count() } returns 45L

        // When
        val result = inventoryService.getInventories(page, size)

        // Then
        // [책임 검증]: Service는 Repository의 계약(Interface)을 올바르게 사용했는가?
        verify(exactly = 1) { inventoryRepository.findAllWithProductInfo(page, size, "created_at", "DESC") }
        verify(exactly = 1) { inventoryRepository.count() }

        // [비즈니스 로직 검증]: 조회된 데이터가 올바르게 반환되는가?
        assertEquals(2, result.content.size)
        assertEquals(1L, result.content[0].inventoryId)
        assertEquals("에티오피아 예가체프 G1", result.content[0].productName)
        assertEquals(50, result.content[0].stockQuantity)
        assertEquals(2L, result.content[1].inventoryId)
        assertEquals(30, result.content[1].stockQuantity)
    }

    @Test
    @DisplayName("페이지 정보가 Repository 결과로 올바르게 조립되어야 한다")
    fun `페이지 정보가 Repository 결과로 올바르게 조립되어야 한다`() {
        // Given
        val page = 2
        val size = 20
        val totalElements = 45L
        val mockInventories = listOf(
            createMockInventory(21L, 2L, "콜롬비아 수프리모", 3L, "COL-HD-200", "핸드드립용 200g", 17000, 40)
        )

        every {
            inventoryRepository.findAllWithProductInfo(page, size, "created_at", "DESC")
        } returns mockInventories
        every { inventoryRepository.count() } returns totalElements

        // When
        val result = inventoryService.getInventories(page, size)

        // Then
        // [비즈니스 로직 검증]: 페이지 정보가 올바르게 조립되는가?
        assertEquals(page, result.pageable.pageNumber, "현재 페이지 번호가 일치해야 함")
        assertEquals(size, result.pageable.pageSize, "페이지 크기가 일치해야 함")
        assertEquals(totalElements, result.pageable.totalElements, "전체 재고 개수가 일치해야 함")
        assertEquals(3, result.pageable.totalPages, "전체 페이지 수가 올바르게 계산되어야 함 (ceil(45/20) = 3)")
    }

    @Test
    @DisplayName("Service는 created_at DESC 정렬 기준으로 Repository를 호출해야 한다")
    fun `Service는 created_at DESC 정렬 기준으로 Repository를 호출해야 한다`() {
        // Given
        val page = 1
        val size = 20

        every {
            inventoryRepository.findAllWithProductInfo(page, size, "created_at", "DESC")
        } returns emptyList()
        every { inventoryRepository.count() } returns 0L

        // When
        inventoryService.getInventories(page, size)

        // Then
        // [Repository 호출 검증]: created_at DESC 정렬로 호출되었는가?
        verify(exactly = 1) { inventoryRepository.findAllWithProductInfo(page, size, "created_at", "DESC") }
        verify(exactly = 1) { inventoryRepository.count() }
    }

    @Test
    @DisplayName("재고가 없을 경우 빈 리스트를 반환해야 한다")
    fun `재고가 없을 경우 빈 리스트를 반환해야 한다`() {
        // Given
        val page = 1
        val size = 20

        every {
            inventoryRepository.findAllWithProductInfo(page, size, "created_at", "DESC")
        } returns emptyList()
        every { inventoryRepository.count() } returns 0L

        // When
        val result = inventoryService.getInventories(page, size)

        // Then
        assertTrue(result.content.isEmpty())
        assertEquals(0L, result.pageable.totalElements)
        assertEquals(0, result.pageable.totalPages)
    }

    @Test
    @DisplayName("page가 1 미만일 경우 InvalidPageNumberException이 발생해야 한다")
    fun `page가 1 미만일 경우 InvalidPageNumberException이 발생해야 한다`() {
        // Given
        val invalidPage = 0
        val size = 20

        // When & Then
        val exception = assertThrows<InvalidPageNumberException> {
            inventoryService.getInventories(invalidPage, size)
        }

        assertEquals("페이지 번호는 1 이상이어야 합니다.", exception.message)

        // [책임 검증]: 유효성 검증 실패 시 Repository는 호출되지 않아야 함
        verify(exactly = 0) { inventoryRepository.findAllWithProductInfo(any(), any(), any(), any()) }
        verify(exactly = 0) { inventoryRepository.count() }
    }

    @Test
    @DisplayName("page가 음수일 경우 InvalidPageNumberException이 발생해야 한다")
    fun `page가 음수일 경우 InvalidPageNumberException이 발생해야 한다`() {
        // Given
        val invalidPage = -1
        val size = 20

        // When & Then
        val exception = assertThrows<InvalidPageNumberException> {
            inventoryService.getInventories(invalidPage, size)
        }

        assertEquals("페이지 번호는 1 이상이어야 합니다.", exception.message)

        // [책임 검증]: 유효성 검증 실패 시 Repository는 호출되지 않아야 함
        verify(exactly = 0) { inventoryRepository.findAllWithProductInfo(any(), any(), any(), any()) }
        verify(exactly = 0) { inventoryRepository.count() }
    }

    @Test
    @DisplayName("size가 1 미만일 경우 InvalidPageSizeException이 발생해야 한다")
    fun `size가 1 미만일 경우 InvalidPageSizeException이 발생해야 한다`() {
        // Given
        val page = 1
        val invalidSize = 0

        // When & Then
        val exception = assertThrows<InvalidPageSizeException> {
            inventoryService.getInventories(page, invalidSize)
        }

        assertEquals("페이지 크기는 1 이상 100 이하여야 합니다.", exception.message)

        // [책임 검증]: 유효성 검증 실패 시 Repository는 호출되지 않아야 함
        verify(exactly = 0) { inventoryRepository.findAllWithProductInfo(any(), any(), any(), any()) }
        verify(exactly = 0) { inventoryRepository.count() }
    }

    @Test
    @DisplayName("size가 100 초과일 경우 InvalidPageSizeException이 발생해야 한다")
    fun `size가 100 초과일 경우 InvalidPageSizeException이 발생해야 한다`() {
        // Given
        val page = 1
        val invalidSize = 101

        // When & Then
        val exception = assertThrows<InvalidPageSizeException> {
            inventoryService.getInventories(page, invalidSize)
        }

        assertEquals("페이지 크기는 1 이상 100 이하여야 합니다.", exception.message)

        // [책임 검증]: 유효성 검증 실패 시 Repository는 호출되지 않아야 함
        verify(exactly = 0) { inventoryRepository.findAllWithProductInfo(any(), any(), any(), any()) }
        verify(exactly = 0) { inventoryRepository.count() }
    }

    @Test
    @DisplayName("전체 페이지 수가 올바르게 계산되어야 한다 (올림 처리)")
    fun `전체 페이지 수가 올바르게 계산되어야 한다`() {
        // Given
        val page = 1
        val size = 20
        val totalElements = 50L // 50개의 재고 (20 + 20 + 10 = 3페이지)

        every {
            inventoryRepository.findAllWithProductInfo(page, size, "created_at", "DESC")
        } returns listOf(createMockInventory(1L, 1L, "상품", 1L, "CODE-1", "옵션1", 10000, 10))
        every { inventoryRepository.count() } returns totalElements

        // When
        val result = inventoryService.getInventories(page, size)

        // Then
        // totalPages = ceil(50 / 20) = ceil(2.5) = 3
        assertEquals(3, result.pageable.totalPages, "50개 재고를 20개씩 페이지네이션하면 3페이지가 되어야 함")
    }

    // === Helper Methods ===

    /**
     * 테스트용 Mock 재고 정보 생성
     */
    private fun createMockInventory(
        inventoryId: Long,
        productId: Long,
        productName: String,
        productOptionId: Long,
        optionCode: String,
        optionName: String,
        price: Int,
        stockQuantity: Int
    ): InventoryResponse {
        return InventoryResponse(
            inventoryId = inventoryId,
            productId = productId,
            productName = productName,
            productOptionId = productOptionId,
            optionCode = optionCode,
            optionName = optionName,
            price = price,
            stockQuantity = stockQuantity,
            createdAt = LocalDateTime.now()
        )
    }
}
