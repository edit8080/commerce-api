package com.beanbliss.domain.product.controller

import com.beanbliss.domain.product.dto.ProductOptionResponse
import com.beanbliss.domain.product.dto.ProductResponse
import com.beanbliss.domain.product.service.ProductService
import com.beanbliss.domain.product.usecase.GetPopularProductsUseCase
import com.beanbliss.domain.product.usecase.GetProductDetailUseCase
import com.beanbliss.domain.product.usecase.GetProductsUseCase
import com.beanbliss.common.exception.ResourceNotFoundException
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

/**
 * 상품 상세 조회 Controller의 HTTP 요청/응답을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. API 엔드포인트가 올바른 경로와 메서드로 매핑되는가?
 * 2. Path Variable이 올바르게 바인딩되는가?
 * 3. UseCase 결과가 올바른 JSON 형식으로 반환되는가?
 * 4. 존재하지 않는 상품 조회 시 적절한 에러 응답이 반환되는가?
 * 5. 적절한 HTTP 상태 코드가 반환되는가?
 *
 * [관련 API]:
 * - GET /api/products/{productId}
 */
@WebMvcTest(ProductController::class)
@DisplayName("상품 상세 조회 Controller 테스트")
class ProductDetailControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var productService: ProductService

    @MockkBean
    private lateinit var getProductsUseCase: GetProductsUseCase

    @MockkBean
    private lateinit var getProductDetailUseCase: GetProductDetailUseCase

    @MockkBean
    private lateinit var getPopularProductsUseCase: GetPopularProductsUseCase

    @Test
    @DisplayName("GET /api/products/{productId} - 정상 조회 시 200 OK와 상품 상세 정보를 반환해야 한다")
    fun `정상 조회 시 200 OK와 상품 상세 정보를 반환해야 한다`() {
        // Given
        val productId = 1L
        val mockResponse = ProductResponse(
            productId = productId,
            name = "에티오피아 예가체프 G1",
            description = "플로럴한 향과 밝은 산미가 특징인 에티오피아 대표 원두",
            brand = "Bean Bliss",
            createdAt = LocalDateTime.of(2025, 1, 15, 10, 30),
            options = listOf(
                ProductOptionResponse(
                    optionId = 1L,
                    optionCode = "ETH-WB-200",
                    origin = "Ethiopia",
                    grindType = "WHOLE_BEANS",
                    weightGrams = 200,
                    price = 18000,
                    availableStock = 50
                ),
                ProductOptionResponse(
                    optionId = 2L,
                    optionCode = "ETH-HD-200",
                    origin = "Ethiopia",
                    grindType = "HAND_DRIP",
                    weightGrams = 200,
                    price = 21000,
                    availableStock = 8
                )
            )
        )

        every { getProductDetailUseCase.getProductDetail(productId) } returns mockResponse

        // When & Then
        mockMvc.perform(
            get("/api/products/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.productId").value(productId))
            .andExpect(jsonPath("$.data.name").value("에티오피아 예가체프 G1"))
            .andExpect(jsonPath("$.data.description").value("플로럴한 향과 밝은 산미가 특징인 에티오피아 대표 원두"))
            .andExpect(jsonPath("$.data.brand").value("Bean Bliss"))
            .andExpect(jsonPath("$.data.createdAt").value("2025-01-15T10:30:00"))
            .andExpect(jsonPath("$.data.options").isArray)
            .andExpect(jsonPath("$.data.options.length()").value(2))
            .andExpect(jsonPath("$.data.options[0].optionId").value(1))
            .andExpect(jsonPath("$.data.options[0].optionCode").value("ETH-WB-200"))
            .andExpect(jsonPath("$.data.options[0].origin").value("Ethiopia"))
            .andExpect(jsonPath("$.data.options[0].grindType").value("WHOLE_BEANS"))
            .andExpect(jsonPath("$.data.options[0].weightGrams").value(200))
            .andExpect(jsonPath("$.data.options[0].price").value(18000))
            .andExpect(jsonPath("$.data.options[0].availableStock").value(50))
            .andExpect(jsonPath("$.data.options[1].optionId").value(2))
            .andExpect(jsonPath("$.data.options[1].availableStock").value(8))
    }

    @Test
    @DisplayName("GET /api/products/{productId} - 존재하지 않는 상품 ID 조회 시 404 Not Found를 반환해야 한다")
    fun `존재하지 않는 상품 ID 조회 시 404 Not Found를 반환해야 한다`() {
        // Given
        val nonExistentProductId = 999L

        every { getProductDetailUseCase.getProductDetail(nonExistentProductId) } throws
                ResourceNotFoundException("상품 ID: $nonExistentProductId 의 상품을 찾을 수 없습니다.")

        // When & Then
        mockMvc.perform(
            get("/api/products/{productId}", nonExistentProductId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("상품 ID: $nonExistentProductId 의 상품을 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("GET /api/products/{productId} - 활성 옵션이 없는 상품 조회 시 404 Not Found를 반환해야 한다")
    fun `활성 옵션이 없는 상품 조회 시 404 Not Found를 반환해야 한다`() {
        // Given
        val productId = 1L

        every { getProductDetailUseCase.getProductDetail(productId) } throws
                ResourceNotFoundException("상품 ID: $productId 의 활성 옵션이 없습니다.")

        // When & Then
        mockMvc.perform(
            get("/api/products/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    @DisplayName("GET /api/products/{productId} - productId가 숫자가 아닐 경우 400 Bad Request를 반환해야 한다")
    fun `productId가 숫자가 아닐 경우 400 Bad Request를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            get("/api/products/{productId}", "invalid")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("GET /api/products/{productId} - 옵션이 정렬되어 반환되어야 한다")
    fun `옵션이 정렬되어 반환되어야 한다`() {
        // Given
        val productId = 1L
        val mockResponse = ProductResponse(
            productId = productId,
            name = "에티오피아 예가체프 G1",
            description = "Test Description",
            brand = "Bean Bliss",
            createdAt = LocalDateTime.of(2025, 1, 15, 10, 30),
            options = listOf(
                // 정렬된 순서: 200g 핸드드립, 200g 홀빈, 500g 핸드드립, 500g 홀빈
                ProductOptionResponse(
                    optionId = 3L,
                    optionCode = "ETH-HD-200",
                    origin = "Ethiopia",
                    grindType = "HAND_DRIP",
                    weightGrams = 200,
                    price = 21000,
                    availableStock = 8
                ),
                ProductOptionResponse(
                    optionId = 2L,
                    optionCode = "ETH-WB-200",
                    origin = "Ethiopia",
                    grindType = "WHOLE_BEANS",
                    weightGrams = 200,
                    price = 18000,
                    availableStock = 50
                ),
                ProductOptionResponse(
                    optionId = 1L,
                    optionCode = "ETH-HD-500",
                    origin = "Ethiopia",
                    grindType = "HAND_DRIP",
                    weightGrams = 500,
                    price = 48000,
                    availableStock = 15
                ),
                ProductOptionResponse(
                    optionId = 4L,
                    optionCode = "ETH-WB-500",
                    origin = "Ethiopia",
                    grindType = "WHOLE_BEANS",
                    weightGrams = 500,
                    price = 42000,
                    availableStock = 0
                )
            )
        )

        every { getProductDetailUseCase.getProductDetail(productId) } returns mockResponse

        // When & Then
        mockMvc.perform(
            get("/api/products/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.options.length()").value(4))
            // 1번째: 200g 핸드드립
            .andExpect(jsonPath("$.data.options[0].weightGrams").value(200))
            .andExpect(jsonPath("$.data.options[0].grindType").value("HAND_DRIP"))
            // 2번째: 200g 홀빈
            .andExpect(jsonPath("$.data.options[1].weightGrams").value(200))
            .andExpect(jsonPath("$.data.options[1].grindType").value("WHOLE_BEANS"))
            // 3번째: 500g 핸드드립
            .andExpect(jsonPath("$.data.options[2].weightGrams").value(500))
            .andExpect(jsonPath("$.data.options[2].grindType").value("HAND_DRIP"))
            // 4번째: 500g 홀빈
            .andExpect(jsonPath("$.data.options[3].weightGrams").value(500))
            .andExpect(jsonPath("$.data.options[3].grindType").value("WHOLE_BEANS"))
    }

    @Test
    @DisplayName("GET /api/products/{productId} - 품절 옵션(availableStock=0)도 응답에 포함되어야 한다")
    fun `품절 옵션도 응답에 포함되어야 한다`() {
        // Given
        val productId = 1L
        val mockResponse = ProductResponse(
            productId = productId,
            name = "에티오피아 예가체프 G1",
            description = "Test Description",
            brand = "Bean Bliss",
            createdAt = LocalDateTime.of(2025, 1, 15, 10, 30),
            options = listOf(
                ProductOptionResponse(
                    optionId = 1L,
                    optionCode = "ETH-WB-200",
                    origin = "Ethiopia",
                    grindType = "WHOLE_BEANS",
                    weightGrams = 200,
                    price = 18000,
                    availableStock = 0  // 품절
                ),
                ProductOptionResponse(
                    optionId = 2L,
                    optionCode = "ETH-HD-200",
                    origin = "Ethiopia",
                    grindType = "HAND_DRIP",
                    weightGrams = 200,
                    price = 21000,
                    availableStock = 10  // 재고 있음
                )
            )
        )

        every { getProductDetailUseCase.getProductDetail(productId) } returns mockResponse

        // When & Then
        mockMvc.perform(
            get("/api/products/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.options.length()").value(2))
            .andExpect(jsonPath("$.data.options[0].availableStock").value(0))
            .andExpect(jsonPath("$.data.options[1].availableStock").value(10))
    }

    @Test
    @DisplayName("GET /api/products/{productId} - Service를 정확한 파라미터로 호출해야 한다")
    fun `Service를 정확한 파라미터로 호출해야 한다`() {
        // Given
        val productId = 123L
        val mockResponse = ProductResponse(
            productId = productId,
            name = "Test Product",
            description = "Test Description",
            brand = "Test Brand",
            createdAt = LocalDateTime.now(),
            options = listOf(
                ProductOptionResponse(
                    optionId = 1L,
                    optionCode = "TEST-CODE",
                    origin = "Test",
                    grindType = "WHOLE_BEANS",
                    weightGrams = 200,
                    price = 20000,
                    availableStock = 10
                )
            )
        )

        every { getProductDetailUseCase.getProductDetail(productId) } returns mockResponse

        // When
        mockMvc.perform(
            get("/api/products/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)

        // Then: MockK의 every에서 정확한 파라미터로 호출되었는지 이미 검증됨
        // 만약 다른 파라미터로 호출되었다면 every 매칭이 실패하고 에러 발생
    }
}
