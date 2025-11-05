package com.beanbliss.domain.product.service

import com.beanbliss.domain.product.repository.ProductRepository
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
 * [리팩토링 후 책임]:
 * - ProductService는 Product 도메인만 담당 (재고 제외)
 * - getActiveProducts(): 상품 목록 조회 (availableStock은 0으로 초기화)
 * - countActiveProducts(): 활성 상품 총 개수 조회
 *
 * [검증 목표]:
 * 1. ProductRepository에 올바른 파라미터로 요청하는가?
 * 2. Repository 결과를 그대로 반환하는가?
 * 3. SRP 준수: Inventory 도메인에 접근하지 않는가?
 *
 * [관련 API]:
 * - GET /api/products (UseCase 레벨에서 사용)
 */
@DisplayName("상품 목록 조회 Service 테스트")
class ProductListServiceTest {

    // Mock 객체 (Repository Interface에 의존)
    private val productRepository: ProductRepository = mockk()

    // 테스트 대상 (Service 인터페이스로 선언)
    private lateinit var productService: ProductService

    @BeforeEach
    fun setUp() {
        // ProductService는 Product 도메인만 담당
        productService = ProductServiceImpl(productRepository)
    }

    @Test
    @DisplayName("getActiveProducts()는 ProductRepository에 올바른 파라미터로 요청해야 한다")
    fun `getActiveProducts는 ProductRepository에 올바른 파라미터로 요청해야 한다`() {
        // Given
        val page = 1
        val size = 10
        val sortBy = "created_at"
        val sortDirection = "DESC"
        val mockProducts = listOf(
            createMockProduct(1L, "에티오피아 예가체프", listOf(1L, 2L))
        )

        every {
            productRepository.findActiveProducts(page, size, sortBy, sortDirection)
        } returns mockProducts

        // When
        val result = productService.getActiveProducts(page, size, sortBy, sortDirection)

        // Then
        // [SRP 검증]: ProductRepository만 호출해야 함
        verify(exactly = 1) {
            productRepository.findActiveProducts(page, size, sortBy, sortDirection)
        }

        // [비즈니스 로직 검증]: Repository 결과를 그대로 반환해야 함
        assertEquals(1, result.size)
        assertEquals(1L, result[0].productId)
        assertEquals("에티오피아 예가체프", result[0].name)
        assertEquals(2, result[0].options.size)
    }

    @Test
    @DisplayName("getActiveProducts()는 빈 목록을 그대로 반환해야 한다")
    fun `getActiveProducts는 빈 목록을 그대로 반환해야 한다`() {
        // Given
        val page = 1
        val size = 10

        every {
            productRepository.findActiveProducts(page, size, "created_at", "DESC")
        } returns emptyList()

        // When
        val result = productService.getActiveProducts(page, size, "created_at", "DESC")

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    @DisplayName("countActiveProducts()는 ProductRepository의 개수를 그대로 반환해야 한다")
    fun `countActiveProducts는 ProductRepository의 개수를 그대로 반환해야 한다`() {
        // Given
        val totalElements = 42L

        every { productRepository.countActiveProducts() } returns totalElements

        // When
        val result = productService.countActiveProducts()

        // Then
        assertEquals(totalElements, result)
        verify(exactly = 1) { productRepository.countActiveProducts() }
    }

    @Test
    @DisplayName("getActiveProducts()로 조회된 옵션의 availableStock은 0이어야 한다 (UseCase에서 채움)")
    fun `getActiveProducts로 조회된 옵션의 availableStock은 0이어야 한다`() {
        // Given
        val mockProducts = listOf(
            createMockProduct(1L, "상품", listOf(1L, 2L))
        )

        every {
            productRepository.findActiveProducts(any(), any(), any(), any())
        } returns mockProducts

        // When
        val result = productService.getActiveProducts(1, 10, "created_at", "DESC")

        // Then
        // [비즈니스 로직 검증]: ProductService는 재고를 계산하지 않음
        // UseCase에서 InventoryService를 통해 재고를 채울 예정
        result.forEach { product ->
            product.options.forEach { option ->
                assertEquals(0, option.availableStock, "ProductService는 재고를 계산하지 않으므로 0이어야 함")
            }
        }
    }

    @Test
    @DisplayName("ProductService는 다양한 정렬 조건으로 요청할 수 있어야 한다")
    fun `ProductService는 다양한 정렬 조건으로 요청할 수 있어야 한다`() {
        // Given
        val page = 1
        val size = 10
        val sortBy = "price"
        val sortDirection = "ASC"

        every {
            productRepository.findActiveProducts(page, size, sortBy, sortDirection)
        } returns emptyList()

        // When
        productService.getActiveProducts(page, size, sortBy, sortDirection)

        // Then
        verify(exactly = 1) {
            productRepository.findActiveProducts(page, size, sortBy, sortDirection)
        }
    }

    // === Helper Methods ===

    /**
     * 테스트용 Mock 상품 생성
     */
    private fun createMockProduct(
        productId: Long,
        name: String,
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
                availableStock = 0 // ProductService는 재고를 계산하지 않음 (UseCase에서 채움)
            )
        }

        return ProductResponse(
            productId = productId,
            name = name,
            description = "Test Description",
            brand = "Test Brand",
            createdAt = LocalDateTime.now(),
            options = options
        )
    }
}
