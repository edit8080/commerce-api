package com.beanbliss.domain.product.usecase

import com.beanbliss.common.exception.ResourceNotFoundException
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
 * GetProductDetailUseCase의 멀티 도메인 오케스트레이션 로직을 검증하는 테스트
 *
 * [테스트 목표]:
 * 1. ProductService와 InventoryService를 올바르게 조율하는가?
 * 2. 상품 데이터와 재고 데이터를 정확히 병합하는가?
 * 3. 예외 상황에서 적절히 전파하는가?
 * 4. Batch 재고 조회를 올바르게 수행하는가?
 *
 * [UseCase의 책임]:
 * - ProductService: 상품 도메인 데이터 조회 (재고 제외)
 * - InventoryService: 재고 도메인 데이터 조회
 * - UseCase: 두 Service의 결과를 병합하여 응답 조립
 *
 * [관련 API]:
 * - GET /api/products/{productId}
 */
@DisplayName("상품 상세 조회 UseCase 테스트")
class GetProductDetailUseCaseTest {

    // Mock 객체 (Service Interface에 의존)
    private val productService: ProductService = mockk()
    private val inventoryService: InventoryService = mockk()

    // 테스트 대상
    private lateinit var getProductDetailUseCase: GetProductDetailUseCase

    @BeforeEach
    fun setUp() {
        getProductDetailUseCase = GetProductDetailUseCase(productService, inventoryService)
    }

    @Test
    @DisplayName("상품 상세 조회 성공 시 ProductService와 InventoryService를 올바르게 조율해야 한다")
    fun `상품 상세 조회 성공 시 ProductService와 InventoryService를 올바르게 조율해야 한다`() {
        // Given
        val productId = 1L
        val mockProduct = createMockProduct(
            productId = productId,
            name = "에티오피아 예가체프 G1",
            optionIds = listOf(1L, 2L)
        )

        // ProductService Mock 설정
        every {
            productService.getProductWithOptions(productId)
        } returns mockProduct

        // InventoryService Mock 설정
        val stockMap = mapOf(
            1L to 50,
            2L to 30
        )
        every {
            inventoryService.calculateAvailableStockBatch(listOf(1L, 2L))
        } returns stockMap

        // When
        val result = getProductDetailUseCase.getProductDetail(productId)

        // Then
        // [검증 1]: ProductService가 올바르게 호출되었는가?
        verify(exactly = 1) {
            productService.getProductWithOptions(productId)
        }

        // [검증 2]: InventoryService가 올바른 optionId 목록으로 호출되었는가?
        verify(exactly = 1) {
            inventoryService.calculateAvailableStockBatch(listOf(1L, 2L))
        }

        // [검증 3]: 상품 데이터와 재고 데이터가 올바르게 병합되었는가?
        assertEquals(productId, result.productId)
        assertEquals("에티오피아 예가체프 G1", result.name)
        assertEquals(2, result.options.size)
        assertEquals(50, result.options[0].availableStock)
        assertEquals(30, result.options[1].availableStock)
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID 조회 시 ResourceNotFoundException이 전파되어야 한다")
    fun `존재하지 않는 상품 ID 조회 시 ResourceNotFoundException이 전파되어야 한다`() {
        // Given
        val productId = 999L

        every {
            productService.getProductWithOptions(productId)
        } throws ResourceNotFoundException("상품 ID: $productId 의 상품을 찾을 수 없습니다.")

        // When & Then
        // [검증]: ProductService에서 발생한 예외가 그대로 전파되어야 함
        val exception = assertThrows(ResourceNotFoundException::class.java) {
            getProductDetailUseCase.getProductDetail(productId)
        }

        assertTrue(exception.message?.contains("$productId") ?: false)

        // [검증]: ProductService 호출 후 예외 발생으로 InventoryService는 호출되지 않아야 함
        verify(exactly = 1) {
            productService.getProductWithOptions(productId)
        }
        verify(exactly = 0) {
            inventoryService.calculateAvailableStockBatch(any())
        }
    }

    @Test
    @DisplayName("활성 옵션이 없는 상품 조회 시 ResourceNotFoundException이 전파되어야 한다")
    fun `활성 옵션이 없는 상품 조회 시 ResourceNotFoundException이 전파되어야 한다`() {
        // Given
        val productId = 1L

        every {
            productService.getProductWithOptions(productId)
        } throws ResourceNotFoundException("상품 ID: $productId 의 활성 옵션이 없습니다.")

        // When & Then
        // [검증]: ProductService에서 발생한 예외가 그대로 전파되어야 함
        val exception = assertThrows(ResourceNotFoundException::class.java) {
            getProductDetailUseCase.getProductDetail(productId)
        }

        assertTrue(exception.message?.contains("옵션") ?: false)

        // [검증]: InventoryService는 호출되지 않아야 함
        verify(exactly = 0) {
            inventoryService.calculateAvailableStockBatch(any())
        }
    }

    @Test
    @DisplayName("재고 정보가 없는 옵션은 availableStock이 0으로 설정되어야 한다")
    fun `재고 정보가 없는 옵션은 availableStock이 0으로 설정되어야 한다`() {
        // Given
        val productId = 1L
        val mockProduct = createMockProduct(
            productId = productId,
            name = "상품",
            optionIds = listOf(1L, 2L, 3L)
        )

        every {
            productService.getProductWithOptions(productId)
        } returns mockProduct

        // InventoryService에서 일부 옵션의 재고만 반환
        val stockMap = mapOf(
            1L to 50
            // 2L, 3L은 재고 정보 없음
        )
        every {
            inventoryService.calculateAvailableStockBatch(listOf(1L, 2L, 3L))
        } returns stockMap

        // When
        val result = getProductDetailUseCase.getProductDetail(productId)

        // Then
        // [검증]: 재고 정보가 없는 옵션은 0으로 설정되어야 한다
        assertEquals(50, result.options[0].availableStock) // 재고 있음
        assertEquals(0, result.options[1].availableStock)  // 재고 없음 -> 0
        assertEquals(0, result.options[2].availableStock)  // 재고 없음 -> 0
    }

    @Test
    @DisplayName("Batch 재고 조회는 모든 옵션 ID를 한 번에 전달해야 한다")
    fun `Batch 재고 조회는 모든 옵션 ID를 한 번에 전달해야 한다`() {
        // Given
        val productId = 1L
        val mockProduct = createMockProduct(
            productId = productId,
            name = "상품",
            optionIds = listOf(1L, 2L, 3L, 4L, 5L)
        )

        every {
            productService.getProductWithOptions(productId)
        } returns mockProduct

        every {
            inventoryService.calculateAvailableStockBatch(any())
        } returns emptyMap()

        // When
        getProductDetailUseCase.getProductDetail(productId)

        // Then
        // [검증]: 모든 옵션 ID가 한 번의 호출로 전달되어야 한다 (N+1 문제 방지)
        verify(exactly = 1) {
            inventoryService.calculateAvailableStockBatch(listOf(1L, 2L, 3L, 4L, 5L))
        }
    }

    @Test
    @DisplayName("옵션이 정렬되어 반환되어야 한다")
    fun `옵션이 정렬되어 반환되어야 한다`() {
        // Given
        val productId = 1L
        // Repository는 이미 정렬된 옵션을 반환함
        val sortedOptions = listOf(
            createMockOption(3L, "ETH-HD-200", 200, "HAND_DRIP"),      // 200g, 핸드드립
            createMockOption(2L, "ETH-WB-200", 200, "WHOLE_BEANS"),    // 200g, 홀빈
            createMockOption(1L, "ETH-HD-500", 500, "HAND_DRIP"),      // 500g, 핸드드립
            createMockOption(4L, "ETH-WB-500", 500, "WHOLE_BEANS")     // 500g, 홀빈
        )

        val mockProduct = ProductResponse(
            productId = productId,
            name = "에티오피아 예가체프 G1",
            description = "Test Description",
            brand = "Bean Bliss",
            createdAt = LocalDateTime.now(),
            options = sortedOptions
        )

        every {
            productService.getProductWithOptions(productId)
        } returns mockProduct

        val stockMap = mapOf(
            1L to 15,
            2L to 50,
            3L to 8,
            4L to 0
        )
        every {
            inventoryService.calculateAvailableStockBatch(listOf(3L, 2L, 1L, 4L))
        } returns stockMap

        // When
        val result = getProductDetailUseCase.getProductDetail(productId)

        // Then
        // [검증]: UseCase가 Service의 정렬 순서를 유지하는가?
        val resultOptions = result.options
        assertEquals(4, resultOptions.size)

        // 정렬 순서: 200g-핸드드립, 200g-홀빈, 500g-핸드드립, 500g-홀빈
        assertEquals(3L, resultOptions[0].optionId, "1번째는 200g 핸드드립이어야 함")
        assertEquals(200, resultOptions[0].weightGrams)
        assertEquals("HAND_DRIP", resultOptions[0].grindType)
        assertEquals(8, resultOptions[0].availableStock)

        assertEquals(2L, resultOptions[1].optionId, "2번째는 200g 홀빈이어야 함")
        assertEquals(200, resultOptions[1].weightGrams)
        assertEquals("WHOLE_BEANS", resultOptions[1].grindType)
        assertEquals(50, resultOptions[1].availableStock)

        assertEquals(1L, resultOptions[2].optionId, "3번째는 500g 핸드드립이어야 함")
        assertEquals(500, resultOptions[2].weightGrams)
        assertEquals("HAND_DRIP", resultOptions[2].grindType)
        assertEquals(15, resultOptions[2].availableStock)

        assertEquals(4L, resultOptions[3].optionId, "4번째는 500g 홀빈이어야 함")
        assertEquals(500, resultOptions[3].weightGrams)
        assertEquals("WHOLE_BEANS", resultOptions[3].grindType)
        assertEquals(0, resultOptions[3].availableStock)
    }

    @Test
    @DisplayName("품절 옵션(availableStock=0)도 응답에 포함되어야 한다")
    fun `품절 옵션도 응답에 포함되어야 한다`() {
        // Given
        val productId = 1L
        val mockProduct = createMockProduct(
            productId = productId,
            name = "품절 옵션 포함 상품",
            optionIds = listOf(1L, 2L)
        )

        every {
            productService.getProductWithOptions(productId)
        } returns mockProduct

        // 한 옵션은 재고 있음, 한 옵션은 품절
        val stockMap = mapOf(
            1L to 0,  // 품절
            2L to 10  // 재고 있음
        )
        every {
            inventoryService.calculateAvailableStockBatch(listOf(1L, 2L))
        } returns stockMap

        // When
        val result = getProductDetailUseCase.getProductDetail(productId)

        // Then
        // [검증]: 품절 옵션도 응답에 포함되어야 함
        assertEquals(2, result.options.size)
        assertEquals(0, result.options[0].availableStock)
        assertEquals(10, result.options[1].availableStock)
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
                origin = "Ethiopia",
                grindType = "WHOLE_BEANS",
                weightGrams = 200,
                price = 20000,
                availableStock = 0 // ProductService는 재고를 계산하지 않음
            )
        }

        return ProductResponse(
            productId = productId,
            name = name,
            description = "플로럴한 향과 밝은 산미가 특징인 에티오피아 대표 원두",
            brand = "Bean Bliss",
            createdAt = LocalDateTime.now(),
            options = options
        )
    }

    /**
     * 테스트용 Mock 옵션 생성 (정렬 테스트용)
     */
    private fun createMockOption(
        optionId: Long,
        optionCode: String,
        weightGrams: Int,
        grindType: String
    ): ProductOptionResponse {
        return ProductOptionResponse(
            optionId = optionId,
            optionCode = optionCode,
            origin = "Ethiopia",
            grindType = grindType,
            weightGrams = weightGrams,
            price = 20000,
            availableStock = 0
        )
    }
}
