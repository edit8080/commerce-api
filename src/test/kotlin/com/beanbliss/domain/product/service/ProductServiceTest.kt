package com.beanbliss.domain.product.service

import com.beanbliss.domain.product.repository.ProductRepository
import com.beanbliss.domain.inventory.repository.InventoryRepository
import com.beanbliss.domain.product.dto.ProductResponse
import com.beanbliss.domain.product.dto.ProductOptionResponse
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDateTime

/**
 * ProductService의 비즈니스 로직을 검증하는 테스트
 *
 * 검증 목표:
 * 1. 가용 재고(availableStock)가 올바르게 계산되는가?
 * 2. 활성 옵션이 없는 상품은 목록에서 제외되는가?
 * 3. 페이지 정보를 올바르게 결합하는가?
 */
@DisplayName("상품 목록 조회 서비스 테스트")
class ProductServiceTest {

    // Mock 객체 (Repository Interface에 의존)
    private val productRepository: ProductRepository = mockk()
    private val inventoryRepository: InventoryRepository = mockk()

    // 테스트 대상 (Service 인터페이스로 선언)
    private lateinit var productService: ProductService

    @BeforeEach
    fun setUp() {
        productService = ProductServiceImpl(productRepository, inventoryRepository)
    }

    @Test
    @DisplayName("각 옵션의 가용 재고가 InventoryRepository로부터 올바르게 계산되어야 한다")
    fun `각 옵션의 가용 재고가 InventoryRepository로부터 올바르게 계산되어야 한다`() {
        // Given
        val page = 1
        val size = 10
        val mockProducts = listOf(
            createMockProductWithOptions(1L, "에티오피아 예가체프", listOf(
                Triple(1L, "ETH-WB-200", 0),  // availableStock = 0 (초기값)
                Triple(2L, "ETH-HD-200", 0)
            ))
        )

        every { productRepository.findActiveProducts(page, size, "created_at", "DESC") } returns mockProducts
        every { productRepository.countActiveProducts() } returns 1L

        // Batch 조회 Mocking: 한 번의 호출로 모든 재고 반환
        every { inventoryRepository.calculateAvailableStockBatch(listOf(1L, 2L)) } returns mapOf(
            1L to 50,
            2L to 8
        )

        // When
        val result = productService.getProducts(page, size)

        // Then
        // [비즈니스 로직 검증]: 가용 재고가 Repository에서 계산된 값으로 올바르게 설정되었는가?
        assertEquals(2, result.content[0].options.size)
        assertEquals(50, result.content[0].options[0].availableStock, "옵션 1의 가용 재고가 50이어야 함")
        assertEquals(8, result.content[0].options[1].availableStock, "옵션 2의 가용 재고가 8이어야 함")

        // [성능 최적화 검증]: Batch 조회가 정확히 한 번만 호출되어야 함 (N+1 문제 해결)
        verify(exactly = 1) { inventoryRepository.calculateAvailableStockBatch(listOf(1L, 2L)) }
        verify(exactly = 0) { inventoryRepository.calculateAvailableStock(any()) }
    }


    @Test
    @DisplayName("페이지 정보가 Repository 결과로 올바르게 조립되어야 한다")
    fun `페이지 정보가 Repository 결과로 올바르게 조립되어야 한다`() {
        // Given
        val page = 2
        val size = 5
        val totalElements = 12L
        val mockProducts = listOf(
            createMockProduct(1L, "상품", listOf(1L))
        )

        every { productRepository.findActiveProducts(page, size, "created_at", "DESC") } returns mockProducts
        every { productRepository.countActiveProducts() } returns totalElements
        every { inventoryRepository.calculateAvailableStockBatch(listOf(1L)) } returns mapOf(1L to 10)

        // When
        val result = productService.getProducts(page, size)

        // Then
        // [비즈니스 로직 검증]: 페이지 정보가 올바르게 조립되는가?
        // (페이징 계산은 PageCalculator의 책임이므로 여기서는 조립만 검증)
        assertEquals(page, result.pageable.pageNumber, "현재 페이지 번호가 일치해야 함")
        assertEquals(size, result.pageable.pageSize, "페이지 크기가 일치해야 함")
        assertEquals(totalElements, result.pageable.totalElements, "전체 상품 수가 일치해야 함")
        assertTrue(result.pageable.totalPages > 0, "전체 페이지 수가 계산되어야 함")
    }

    @Test
    @DisplayName("가용 재고가 0인 옵션도 응답에 포함되어야 한다")
    fun `가용 재고가 0인 옵션도 응답에 포함되어야 한다`() {
        // Given
        val page = 1
        val size = 10
        val mockProducts = listOf(
            createMockProduct(1L, "품절 상품", listOf(1L))
        )

        every { productRepository.findActiveProducts(page, size, "created_at", "DESC") } returns mockProducts
        every { productRepository.countActiveProducts() } returns 1L
        every { inventoryRepository.calculateAvailableStockBatch(listOf(1L)) } returns mapOf(1L to 0)  // 품절

        // When
        val result = productService.getProducts(page, size)

        // Then
        // [비즈니스 로직 검증]: 재고가 0인 옵션도 포함되어야 함
        assertEquals(1, result.content.size)
        assertEquals(1, result.content[0].options.size)
        assertEquals(0, result.content[0].options[0].availableStock, "품절 상태(재고 0)가 반영되어야 함")
    }

    @Test
    @DisplayName("상품이 없을 경우 빈 리스트를 반환해야 한다")
    fun `상품이 없을 경우 빈 리스트를 반환해야 한다`() {
        // Given
        val page = 1
        val size = 10

        every { productRepository.findActiveProducts(page, size, "created_at", "DESC") } returns emptyList()
        every { productRepository.countActiveProducts() } returns 0L
        every { inventoryRepository.calculateAvailableStockBatch(emptyList()) } returns emptyMap()

        // When
        val result = productService.getProducts(page, size)

        // Then
        assertTrue(result.content.isEmpty())
        assertEquals(0L, result.pageable.totalElements)
        assertEquals(0, result.pageable.totalPages)
    }

    @Test
    @DisplayName("Service는 created_at DESC 정렬 기준으로 ProductRepository를 호출해야 한다")
    fun `Service는 created_at DESC 정렬 기준으로 ProductRepository를 호출해야 한다`() {
        // Given
        val page = 1
        val size = 10

        every { productRepository.findActiveProducts(page, size, "created_at", "DESC") } returns emptyList()
        every { productRepository.countActiveProducts() } returns 0L
        every { inventoryRepository.calculateAvailableStockBatch(emptyList()) } returns emptyMap()

        // When
        productService.getProducts(page, size)

        // Then
        // [Repository 호출 검증]: Service는 created_at DESC 정렬로 Repository를 호출해야 함
        verify(exactly = 1) { productRepository.findActiveProducts(page, size, "created_at", "DESC") }
        verify(exactly = 1) { productRepository.countActiveProducts() }
        verify(exactly = 1) { inventoryRepository.calculateAvailableStockBatch(emptyList()) }
    }

    // === Helper Methods ===

    /**
     * 테스트용 Mock 상품 생성 (현재 시각 기준)
     */
    private fun createMockProduct(
        productId: Long,
        name: String,
        optionIds: List<Long>
    ): ProductResponse {
        return createMockProductWithDate(productId, name, LocalDateTime.now(), optionIds)
    }

    /**
     * 테스트용 Mock 상품 생성 (특정 날짜)
     */
    private fun createMockProductWithDate(
        productId: Long,
        name: String,
        createdAt: LocalDateTime,
        optionIds: List<Long>
    ): ProductResponse {
        val options = optionIds.map { optionId ->
            ProductOptionResponse(
                optionId = optionId,
                optionCode = "TEST-CODE-$optionId",
                origin = "Test Origin",
                grindType = "WHOLE_BEANS",
                weightGrams = 200,
                price = 20000,
                availableStock = 0 // Service에서 계산하여 채워질 예정
            )
        }

        return ProductResponse(
            productId = productId,
            name = name,
            description = "Test Description",
            brand = "Test Brand",
            createdAt = createdAt,
            options = options
        )
    }

    /**
     * 테스트용 Mock 상품 생성 (옵션 상세 정보 포함)
     */
    private fun createMockProductWithOptions(
        productId: Long,
        name: String,
        options: List<Triple<Long, String, Int>> // (optionId, optionCode, availableStock)
    ): ProductResponse {
        val optionResponses = options.map { (optionId, optionCode, availableStock) ->
            ProductOptionResponse(
                optionId = optionId,
                optionCode = optionCode,
                origin = "Test Origin",
                grindType = "WHOLE_BEANS",
                weightGrams = 200,
                price = 20000,
                availableStock = availableStock
            )
        }

        return ProductResponse(
            productId = productId,
            name = name,
            description = "Test Description",
            brand = "Test Brand",
            createdAt = LocalDateTime.now(),
            options = optionResponses
        )
    }

}
