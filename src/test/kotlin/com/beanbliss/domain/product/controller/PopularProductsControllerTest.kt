package com.beanbliss.domain.product.controller

import com.beanbliss.common.exception.InvalidParameterException
import com.beanbliss.domain.product.dto.PopularProductInfo
import com.beanbliss.domain.product.dto.PopularProductsResponse
import com.beanbliss.domain.product.service.ProductService
import com.beanbliss.domain.product.usecase.GetPopularProductsUseCase
import com.beanbliss.domain.product.usecase.GetProductDetailUseCase
import com.beanbliss.domain.product.usecase.GetProductsUseCase
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

/**
 * 인기 상품 조회 Controller의 HTTP 요청/응답을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. API 엔드포인트가 올바른 경로와 메서드로 매핑되는가?
 * 2. 요청 파라미터가 올바르게 바인딩되는가?
 * 3. UseCase 결과가 올바른 JSON 형식으로 반환되는가?
 * 4. 기본값이 올바르게 적용되는가?
 * 5. 유효성 검증이 올바르게 수행되는가?
 * 6. 예외 상황에서 적절한 HTTP 상태 코드가 반환되는가?
 *
 * [관련 API]:
 * - GET /api/products/popular
 */
@WebMvcTest(ProductController::class)
@DisplayName("인기 상품 조회 Controller 테스트")
class PopularProductsControllerTest {

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
    @DisplayName("GET /api/products/popular - 정상 조회 시 200 OK와 인기 상품 목록을 반환해야 한다")
    fun `정상 조회 시 200 OK와 인기 상품 목록을 반환해야 한다`() {
        // Given
        val period = 7
        val limit = 10
        val mockResponse = PopularProductsResponse(
            products = listOf(
                PopularProductInfo(
                    productId = 1L,
                    productName = "에티오피아 예가체프 G1",
                    brand = "Bean Bliss",
                    totalOrderCount = 150,
                    description = "플로럴하고 과일향이 풍부한 에티오피아 대표 원두"
                ),
                PopularProductInfo(
                    productId = 2L,
                    productName = "콜롬비아 수프리모",
                    brand = "Bean Bliss",
                    totalOrderCount = 120,
                    description = "부드러운 맛과 균형잡힌 바디감"
                )
            )
        )

        every { getPopularProductsUseCase.getPopularProducts(period, limit) } returns mockResponse

        // When & Then
        mockMvc.perform(
            get("/api/products/popular")
                .param("period", period.toString())
                .param("limit", limit.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.products").isArray)
            .andExpect(jsonPath("$.data.products[0].productId").value(1))
            .andExpect(jsonPath("$.data.products[0].productName").value("에티오피아 예가체프 G1"))
            .andExpect(jsonPath("$.data.products[0].brand").value("Bean Bliss"))
            .andExpect(jsonPath("$.data.products[0].totalOrderCount").value(150))
            .andExpect(jsonPath("$.data.products[0].description").value("플로럴하고 과일향이 풍부한 에티오피아 대표 원두"))
            .andExpect(jsonPath("$.data.products[1].productId").value(2))
            .andExpect(jsonPath("$.data.products[1].totalOrderCount").value(120))
    }

    @Test
    @DisplayName("GET /api/products/popular - 파라미터 없이 조회 시 기본값(period=7, limit=10)이 적용되어야 한다")
    fun `파라미터 없이 조회 시 기본값이 적용되어야 한다`() {
        // Given
        val defaultPeriod = 7
        val defaultLimit = 10
        val mockResponse = PopularProductsResponse(products = emptyList())

        every { getPopularProductsUseCase.getPopularProducts(defaultPeriod, defaultLimit) } returns mockResponse

        // When & Then
        mockMvc.perform(
            get("/api/products/popular")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.products").isArray)
    }

    @Test
    @DisplayName("GET /api/products/popular - 인기 상품이 없을 경우 빈 배열을 반환해야 한다")
    fun `인기 상품이 없을 경우 빈 배열을 반환해야 한다`() {
        // Given
        val period = 7
        val limit = 10
        val mockResponse = PopularProductsResponse(products = emptyList())

        every { getPopularProductsUseCase.getPopularProducts(period, limit) } returns mockResponse

        // When & Then
        mockMvc.perform(
            get("/api/products/popular")
                .param("period", period.toString())
                .param("limit", limit.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.products").isArray)
            .andExpect(jsonPath("$.data.products").isEmpty)
    }

    @Test
    @DisplayName("GET /api/products/popular - period가 1 미만일 경우 400 Bad Request를 반환해야 한다")
    fun `period가 1 미만일 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val invalidPeriod = 0
        val limit = 10

        // Controller에서 파라미터 검증 수행 (UseCase 호출 전)

        // When & Then
        mockMvc.perform(
            get("/api/products/popular")
                .param("period", invalidPeriod.toString())
                .param("limit", limit.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
            .andExpect(jsonPath("$.message").value("period는 1 이상 90 이하여야 합니다."))
    }

    @Test
    @DisplayName("GET /api/products/popular - period가 90 초과일 경우 400 Bad Request를 반환해야 한다")
    fun `period가 90 초과일 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val invalidPeriod = 91
        val limit = 10

        // Controller에서 파라미터 검증 수행 (UseCase 호출 전)

        // When & Then
        mockMvc.perform(
            get("/api/products/popular")
                .param("period", invalidPeriod.toString())
                .param("limit", limit.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
            .andExpect(jsonPath("$.message").value("period는 1 이상 90 이하여야 합니다."))
    }

    @Test
    @DisplayName("GET /api/products/popular - limit가 1 미만일 경우 400 Bad Request를 반환해야 한다")
    fun `limit가 1 미만일 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val period = 7
        val invalidLimit = 0

        // Controller에서 파라미터 검증 수행 (UseCase 호출 전)

        // When & Then
        mockMvc.perform(
            get("/api/products/popular")
                .param("period", period.toString())
                .param("limit", invalidLimit.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
            .andExpect(jsonPath("$.message").value("limit는 1 이상 50 이하여야 합니다."))
    }

    @Test
    @DisplayName("GET /api/products/popular - limit가 50 초과일 경우 400 Bad Request를 반환해야 한다")
    fun `limit가 50 초과일 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val period = 7
        val invalidLimit = 51

        // Controller에서 파라미터 검증 수행 (UseCase 호출 전)

        // When & Then
        mockMvc.perform(
            get("/api/products/popular")
                .param("period", period.toString())
                .param("limit", invalidLimit.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
            .andExpect(jsonPath("$.message").value("limit는 1 이상 50 이하여야 합니다."))
    }

    @Test
    @DisplayName("GET /api/products/popular - UseCase를 정확한 파라미터로 호출해야 한다")
    fun `UseCase를 정확한 파라미터로 호출해야 한다`() {
        // Given
        val period = 30
        val limit = 20
        val mockResponse = PopularProductsResponse(products = emptyList())

        every { getPopularProductsUseCase.getPopularProducts(period, limit) } returns mockResponse

        // When
        mockMvc.perform(
            get("/api/products/popular")
                .param("period", period.toString())
                .param("limit", limit.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)

        // Then: MockK의 every에서 정확한 파라미터로 호출되었는지 이미 검증됨
        // 만약 다른 파라미터로 호출되었다면 every 매칭이 실패하고 에러 발생
    }

    @Test
    @DisplayName("GET /api/products/popular - Response에 totalOrderCount가 내림차순으로 정렬되어 반환되어야 한다")
    fun `Response에 totalOrderCount가 내림차순으로 정렬되어 반환되어야 한다`() {
        // Given
        val period = 7
        val limit = 3
        val mockResponse = PopularProductsResponse(
            products = listOf(
                PopularProductInfo(1L, "상품1", "브랜드1", 150, "설명1"),
                PopularProductInfo(2L, "상품2", "브랜드2", 120, "설명2"),
                PopularProductInfo(3L, "상품3", "브랜드3", 100, "설명3")
            )
        )

        every { getPopularProductsUseCase.getPopularProducts(period, limit) } returns mockResponse

        // When & Then
        mockMvc.perform(
            get("/api/products/popular")
                .param("period", period.toString())
                .param("limit", limit.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.products[0].totalOrderCount").value(150))
            .andExpect(jsonPath("$.data.products[1].totalOrderCount").value(120))
            .andExpect(jsonPath("$.data.products[2].totalOrderCount").value(100))
    }
}
