package com.beanbliss.domain.product.usecase

import com.beanbliss.domain.inventory.service.InventoryService
import com.beanbliss.domain.product.dto.ProductOptionResponse
import com.beanbliss.domain.product.dto.ProductResponse
import com.beanbliss.domain.product.service.ProductService
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * GetProductsUseCase의 멀티 도메인 오케스트레이션 로직을 검증하는 테스트
 *
 * [테스트 목표]:
 * 1. ProductService와 InventoryService를 올바르게 조율하는가?
 * 2. 상품 데이터와 재고 데이터를 정확히 병합하는가?
 * 3. 페이징 정보를 올바르게 조립하는가?
 * 4. 빈 목록 처리를 올바르게 하는가?
 *
 * [UseCase의 책임]:
 * - ProductService: 상품 도메인 데이터 조회
 * - InventoryService: 재고 도메인 데이터 조회
 * - UseCase: 두 Service의 결과를 병합하여 응답 조립
 *
 * [관련 API]:
 * - GET /api/products
 */
@DisplayName("상품 목록 조회 UseCase 테스트")
class GetProductsUseCaseTest {

    // Mock 객체 (Service Interface에 의존)
    private val productService: ProductService = mockk()
    private val inventoryService: InventoryService = mockk()

    // 테스트 대상
    private lateinit var getProductsUseCase: GetProductsUseCase

    @BeforeEach
    fun setUp() {
        getProductsUseCase = GetProductsUseCase(productService, inventoryService)
    }

    @Test
    @DisplayName("상품 목록 조회 성공 시 ProductService와 InventoryService를 올바르게 조율해야 한다")
    fun `상품 목록 조회 성공 시 ProductService와 InventoryService를 올바르게 조율해야 한다`() {
        // Given
        val page = 1
        val size = 10
        val mockProducts = listOf(
            createMockProduct(1L, "에티오피아 예가체프", listOf(1L, 2L)),
            createMockProduct(2L, "콜롬비아 수프리모", listOf(3L, 4L))
        )
        val totalElements = 42L

        // ProductService Mock 설정
        every {
            productService.getActiveProducts(page, size, "created_at", "DESC")
        } returns mockProducts

        every {
            productService.countActiveProducts()
        } returns totalElements

        // InventoryService Mock 설정
        val stockMap = mapOf(
            1L to 50,
            2L to 30,
            3L to 20,
            4L to 10
        )
        every {
            inventoryService.calculateAvailableStockBatch(listOf(1L, 2L, 3L, 4L))
        } returns stockMap

        // When
        val result = getProductsUseCase.getProducts(page, size)

        // Then
        // [검증 1]: ProductService가 올바르게 호출되었는가?
        verify(exactly = 1) {
            productService.getActiveProducts(page, size, "created_at", "DESC")
        }
        verify(exactly = 1) {
            productService.countActiveProducts()
        }

        // [검증 2]: InventoryService가 올바른 optionId 목록으로 호출되었는가?
        verify(exactly = 1) {
            inventoryService.calculateAvailableStockBatch(listOf(1L, 2L, 3L, 4L))
        }

        // [검증 3]: 상품 데이터와 재고 데이터가 올바르게 병합되었는가?
        assertEquals(2, result.products.size)

        // 첫 번째 상품 검증
        val product1 = result.products[0]
        assertEquals(1L, product1.productId)
        assertEquals("에티오피아 예가체프", product1.name)
        assertEquals(50, product1.options[0].availableStock)
        assertEquals(30, product1.options[1].availableStock)

        // 두 번째 상품 검증
        val product2 = result.products[1]
        assertEquals(2L, product2.productId)
        assertEquals("콜롬비아 수프리모", product2.name)
        assertEquals(20, product2.options[0].availableStock)
        assertEquals(10, product2.options[1].availableStock)

        // [검증 4]: totalElements가 올바르게 반환되었는가?
        assertEquals(totalElements, result.totalElements)
    }

    @Test
    @DisplayName("빈 목록 조회 시 빈 응답과 올바른 페이징 정보를 반환해야 한다")
    fun `빈 목록 조회 시 빈 응답과 올바른 페이징 정보를 반환해야 한다`() {
        // Given
        val page = 1
        val size = 10
        val totalElements = 0L

        every {
            productService.getActiveProducts(page, size, "created_at", "DESC")
        } returns emptyList()

        every {
            productService.countActiveProducts()
        } returns totalElements

        // When
        val result = getProductsUseCase.getProducts(page, size)

        // Then
        // [검증 1]: ProductService가 호출되었는가?
        verify(exactly = 1) {
            productService.getActiveProducts(page, size, "created_at", "DESC")
        }
        verify(exactly = 1) {
            productService.countActiveProducts()
        }

        // [검증 2]: InventoryService는 호출되지 않아야 한다 (조기 반환)
        verify(exactly = 0) {
            inventoryService.calculateAvailableStockBatch(any())
        }

        // [검증 3]: 빈 목록과 올바른 totalElements를 반환해야 한다
        assertTrue(result.products.isEmpty())
        assertEquals(totalElements, result.totalElements)
    }

    @Test
    @DisplayName("재고 정보가 없는 옵션은 availableStock이 0으로 설정되어야 한다")
    fun `재고 정보가 없는 옵션은 availableStock이 0으로 설정되어야 한다`() {
        // Given
        val page = 1
        val size = 10
        val mockProducts = listOf(
            createMockProduct(1L, "상품", listOf(1L, 2L, 3L))
        )

        every {
            productService.getActiveProducts(page, size, "created_at", "DESC")
        } returns mockProducts

        every {
            productService.countActiveProducts()
        } returns 1L

        // InventoryService에서 일부 옵션의 재고만 반환
        val stockMap = mapOf(
            1L to 50
            // 2L, 3L은 재고 정보 없음
        )
        every {
            inventoryService.calculateAvailableStockBatch(listOf(1L, 2L, 3L))
        } returns stockMap

        // When
        val result = getProductsUseCase.getProducts(page, size)

        // Then
        // [검증]: 재고 정보가 없는 옵션은 0으로 설정되어야 한다
        val product = result.products[0]
        assertEquals(50, product.options[0].availableStock) // 재고 있음
        assertEquals(0, product.options[1].availableStock)  // 재고 없음 -> 0
        assertEquals(0, product.options[2].availableStock)  // 재고 없음 -> 0
    }

    @Test
    @DisplayName("Batch 재고 조회는 모든 옵션 ID를 한 번에 전달해야 한다")
    fun `Batch 재고 조회는 모든 옵션 ID를 한 번에 전달해야 한다`() {
        // Given
        val page = 1
        val size = 10
        val mockProducts = listOf(
            createMockProduct(1L, "상품1", listOf(1L, 2L)),
            createMockProduct(2L, "상품2", listOf(3L, 4L, 5L)),
            createMockProduct(3L, "상품3", listOf(6L))
        )

        every {
            productService.getActiveProducts(page, size, "created_at", "DESC")
        } returns mockProducts

        every {
            productService.countActiveProducts()
        } returns 3L

        every {
            inventoryService.calculateAvailableStockBatch(any())
        } returns emptyMap()

        // When
        getProductsUseCase.getProducts(page, size)

        // Then
        // [검증]: 모든 옵션 ID가 한 번의 호출로 전달되어야 한다 (N+1 문제 방지)
        verify(exactly = 1) {
            inventoryService.calculateAvailableStockBatch(listOf(1L, 2L, 3L, 4L, 5L, 6L))
        }
    }

    @Test
    @DisplayName("페이지 크기가 1일 때 올바른 총 페이지 수를 계산해야 한다")
    fun `페이지 크기가 1일 때 올바른 총 페이지 수를 계산해야 한다`() {
        // Given
        val page = 1
        val size = 1
        val mockProducts = listOf(
            createMockProduct(1L, "상품", listOf(1L))
        )
        val totalElements = 3L

        every {
            productService.getActiveProducts(page, size, "created_at", "DESC")
        } returns mockProducts

        every {
            productService.countActiveProducts()
        } returns totalElements

        every {
            inventoryService.calculateAvailableStockBatch(any())
        } returns emptyMap()

        // When
        val result = getProductsUseCase.getProducts(page, size)

        // Then
        // [검증]: totalElements가 올바르게 반환되어야 한다
        assertEquals(totalElements, result.totalElements)
    }

    @Test
    @DisplayName("마지막 페이지 조회 시 올바른 페이징 정보를 반환해야 한다")
    fun `마지막 페이지 조회 시 올바른 페이징 정보를 반환해야 한다`() {
        // Given
        val page = 5
        val size = 10
        val mockProducts = listOf(
            createMockProduct(1L, "상품", listOf(1L))
        )
        val totalElements = 42L // 5 pages (42 / 10 = 4.2 -> 5 pages)

        every {
            productService.getActiveProducts(page, size, "created_at", "DESC")
        } returns mockProducts

        every {
            productService.countActiveProducts()
        } returns totalElements

        every {
            inventoryService.calculateAvailableStockBatch(any())
        } returns emptyMap()

        // When
        val result = getProductsUseCase.getProducts(page, size)

        // Then
        // [검증]: totalElements가 올바르게 반환되어야 한다
        assertEquals(totalElements, result.totalElements)
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
                availableStock = 0 // ProductService는 재고를 계산하지 않음
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
