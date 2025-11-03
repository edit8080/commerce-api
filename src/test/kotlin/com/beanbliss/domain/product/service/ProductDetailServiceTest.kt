package com.beanbliss.domain.product.service

import com.beanbliss.domain.product.repository.ProductRepository
import com.beanbliss.domain.inventory.repository.InventoryRepository
import com.beanbliss.domain.product.dto.ProductResponse
import com.beanbliss.domain.product.dto.ProductOptionResponse
import com.beanbliss.common.exception.ResourceNotFoundException
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDateTime

/**
 * 상품 상세 조회 Service의 비즈니스 로직을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. ProductRepository를 통해 상품 ID로 상품을 조회할 수 있는가?
 * 2. 각 옵션의 가용 재고가 InventoryRepository를 통해 올바르게 계산되는가?
 * 3. 옵션 목록이 용량(weightGrams) → 분쇄 타입(grindType) 순으로 정렬되는가?
 * 4. 존재하지 않는 상품 ID 조회 시 적절한 예외가 발생하는가?
 * 5. 활성 옵션이 없는 상품 조회 시 적절한 예외가 발생하는가?
 *
 * [관련 API]:
 * - GET /api/products/{productId}
 */
@DisplayName("상품 상세 조회 Service 테스트")
class ProductDetailServiceTest {

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
    @DisplayName("상품 ID로 상품 상세 정보를 조회할 수 있어야 한다")
    fun `상품 ID로 상품 상세 정보를 조회할 수 있어야 한다`() {
        // Given
        val productId = 1L
        val mockProduct = createMockProduct(
            productId = productId,
            name = "에티오피아 예가체프 G1",
            optionIds = listOf(1L, 2L)
        )

        every { productRepository.findByIdWithOptions(productId) } returns mockProduct
        every { inventoryRepository.calculateAvailableStockBatch(listOf(1L, 2L)) } returns mapOf(
            1L to 50,
            2L to 8
        )

        // When
        val result = productService.getProductDetail(productId)

        // Then
        // [비즈니스 로직 검증]: ProductRepository를 통해 상품을 조회했는가?
        assertEquals(productId, result.productId)
        assertEquals("에티오피아 예가체프 G1", result.name)
        assertEquals(2, result.options.size)

        // [Repository 호출 검증]: findByIdWithOptions가 정확히 한 번 호출되어야 함
        verify(exactly = 1) { productRepository.findByIdWithOptions(productId) }
    }

    @Test
    @DisplayName("각 옵션의 가용 재고가 InventoryRepository를 통해 올바르게 계산되어야 한다")
    fun `각 옵션의 가용 재고가 InventoryRepository를 통해 올바르게 계산되어야 한다`() {
        // Given
        val productId = 1L
        val mockProduct = createMockProduct(
            productId = productId,
            name = "에티오피아 예가체프 G1",
            optionIds = listOf(1L, 2L)
        )

        every { productRepository.findByIdWithOptions(productId) } returns mockProduct
        every { inventoryRepository.calculateAvailableStockBatch(listOf(1L, 2L)) } returns mapOf(
            1L to 50,
            2L to 8
        )

        // When
        val result = productService.getProductDetail(productId)

        // Then
        // [비즈니스 로직 검증]: 가용 재고가 Repository에서 계산된 값으로 올바르게 설정되었는가?
        assertEquals(50, result.options[0].availableStock, "옵션 1의 가용 재고가 50이어야 함")
        assertEquals(8, result.options[1].availableStock, "옵션 2의 가용 재고가 8이어야 함")

        // [성능 최적화 검증]: Batch 조회가 정확히 한 번만 호출되어야 함 (N+1 문제 해결)
        verify(exactly = 1) { inventoryRepository.calculateAvailableStockBatch(listOf(1L, 2L)) }
        verify(exactly = 0) { inventoryRepository.calculateAvailableStock(any()) }
    }

    @Test
    @DisplayName("옵션이 용량(weightGrams) 오름차순, 분쇄 타입(grindType) 오름차순으로 정렬되어야 한다")
    fun `옵션이 용량 오름차순, 분쇄 타입 오름차순으로 정렬되어야 한다`() {
        // Given
        val productId = 1L
        // Repository는 이미 정렬된 옵션을 반환함 (Repository의 책임)
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

        every { productRepository.findByIdWithOptions(productId) } returns mockProduct
        // Repository가 정렬된 순서로 반환하므로: 3L, 2L, 1L, 4L 순서로 optionId 수집됨
        every { inventoryRepository.calculateAvailableStockBatch(listOf(3L, 2L, 1L, 4L)) } returns mapOf(
            1L to 10, 2L to 10, 3L to 10, 4L to 10
        )

        // When
        val result = productService.getProductDetail(productId)

        // Then
        // [비즈니스 로직 검증]: Service가 Repository의 정렬 순서를 유지하는가?
        val resultOptions = result.options
        assertEquals(4, resultOptions.size)

        // [Repository 호출 검증]: 정렬된 순서대로 optionId를 수집하여 Batch 조회를 호출했는가?
        verify(exactly = 1) { inventoryRepository.calculateAvailableStockBatch(listOf(3L, 2L, 1L, 4L)) }

        // 정렬 순서: 200g-핸드드립, 200g-홀빈, 500g-핸드드립, 500g-홀빈
        assertEquals(3L, resultOptions[0].optionId, "1번째는 200g 핸드드립이어야 함")
        assertEquals(200, resultOptions[0].weightGrams)
        assertEquals("HAND_DRIP", resultOptions[0].grindType)

        assertEquals(2L, resultOptions[1].optionId, "2번째는 200g 홀빈이어야 함")
        assertEquals(200, resultOptions[1].weightGrams)
        assertEquals("WHOLE_BEANS", resultOptions[1].grindType)

        assertEquals(1L, resultOptions[2].optionId, "3번째는 500g 핸드드립이어야 함")
        assertEquals(500, resultOptions[2].weightGrams)
        assertEquals("HAND_DRIP", resultOptions[2].grindType)

        assertEquals(4L, resultOptions[3].optionId, "4번째는 500g 홀빈이어야 함")
        assertEquals(500, resultOptions[3].weightGrams)
        assertEquals("WHOLE_BEANS", resultOptions[3].grindType)
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID 조회 시 ResourceNotFoundException이 발생해야 한다")
    fun `존재하지 않는 상품 ID 조회 시 ResourceNotFoundException이 발생해야 한다`() {
        // Given
        val nonExistentProductId = 999L

        every { productRepository.findByIdWithOptions(nonExistentProductId) } returns null

        // When & Then
        // [예외 처리 검증]: 존재하지 않는 상품 ID 조회 시 적절한 예외가 발생하는가?
        val exception = assertThrows(ResourceNotFoundException::class.java) {
            productService.getProductDetail(nonExistentProductId)
        }

        assertTrue(exception.message?.contains("$nonExistentProductId") ?: false,
            "예외 메시지에 상품 ID가 포함되어야 함")

        // [Repository 호출 검증]: findByIdWithOptions가 호출되어야 함
        verify(exactly = 1) { productRepository.findByIdWithOptions(nonExistentProductId) }

        // [부작용 방지]: 재고 조회는 호출되지 않아야 함
        verify(exactly = 0) { inventoryRepository.calculateAvailableStockBatch(any()) }
    }

    @Test
    @DisplayName("활성 옵션이 없는 상품 조회 시 ResourceNotFoundException이 발생해야 한다")
    fun `활성 옵션이 없는 상품 조회 시 ResourceNotFoundException이 발생해야 한다`() {
        // Given
        val productId = 1L
        val productWithNoActiveOptions = ProductResponse(
            productId = productId,
            name = "비활성 옵션만 있는 상품",
            description = "Test Description",
            brand = "Bean Bliss",
            createdAt = LocalDateTime.now(),
            options = emptyList() // Repository에서 이미 활성 옵션만 필터링되어 빈 리스트 반환
        )

        every { productRepository.findByIdWithOptions(productId) } returns productWithNoActiveOptions

        // When & Then
        // [예외 처리 검증]: 활성 옵션이 없는 경우 적절한 예외가 발생하는가?
        val exception = assertThrows(ResourceNotFoundException::class.java) {
            productService.getProductDetail(productId)
        }

        assertTrue(exception.message?.contains("옵션") ?: false,
            "예외 메시지에 '옵션' 관련 내용이 포함되어야 함")

        // [Repository 호출 검증]: findByIdWithOptions가 호출되어야 함
        verify(exactly = 1) { productRepository.findByIdWithOptions(productId) }

        // [부작용 방지]: 재고 조회는 호출되지 않아야 함
        verify(exactly = 0) { inventoryRepository.calculateAvailableStockBatch(any()) }
    }

    @Test
    @DisplayName("가용 재고가 0인 옵션도 응답에 포함되어야 한다")
    fun `가용 재고가 0인 옵션도 응답에 포함되어야 한다`() {
        // Given
        val productId = 1L
        val mockProduct = createMockProduct(
            productId = productId,
            name = "품절 옵션 포함 상품",
            optionIds = listOf(1L, 2L)
        )

        every { productRepository.findByIdWithOptions(productId) } returns mockProduct
        every { inventoryRepository.calculateAvailableStockBatch(listOf(1L, 2L)) } returns mapOf(
            1L to 0,  // 품절
            2L to 10  // 재고 있음
        )

        // When
        val result = productService.getProductDetail(productId)

        // Then
        // [비즈니스 로직 검증]: 재고가 0인 옵션도 포함되어야 함
        assertEquals(2, result.options.size)
        assertEquals(0, result.options[0].availableStock, "품절 상태(재고 0)가 반영되어야 함")
        assertEquals(10, result.options[1].availableStock, "재고 있는 옵션의 재고가 반영되어야 함")
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
                availableStock = 0 // Service에서 계산하여 채워질 예정
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
