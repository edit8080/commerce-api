package com.beanbliss.domain.product.controller

import com.beanbliss.common.dto.PageableResponse
import com.beanbliss.domain.product.dto.ProductListResponse
import com.beanbliss.domain.product.dto.ProductOptionResponse
import com.beanbliss.domain.product.dto.ProductResponse
import com.beanbliss.domain.product.service.ProductService
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
 * 상품 목록 조회 Controller의 HTTP 요청/응답을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. API 엔드포인트가 올바른 경로와 메서드로 매핑되는가?
 * 2. 요청 파라미터가 올바르게 바인딩되는가?
 * 3. Service 결과가 올바른 JSON 형식으로 반환되는가?
 * 4. 파라미터 검증이 올바르게 수행되는가?
 * 5. 적절한 HTTP 상태 코드가 반환되는가?
 *
 * [관련 API]:
 * - GET /api/products
 */
@WebMvcTest(ProductController::class)
@DisplayName("상품 목록 조회 Controller 테스트")
class ProductListControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var productService: ProductService

    @Test
    @DisplayName("GET /api/products - 정상 조회 시 200 OK와 상품 목록을 반환해야 한다")
    fun `정상 조회 시 200 OK와 상품 목록을 반환해야 한다`() {
        // Given
        val page = 1
        val size = 10
        val mockResponse = ProductListResponse(
            content = listOf(
                ProductResponse(
                    productId = 1L,
                    name = "에티오피아 예가체프",
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
                            availableStock = 50
                        )
                    )
                )
            ),
            pageable = PageableResponse(
                pageNumber = page,
                pageSize = size,
                totalElements = 1L,
                totalPages = 1
            )
        )

        every { productService.getProducts(page, size) } returns mockResponse

        // When & Then
        mockMvc.perform(
            get("/api/products")
                .param("page", page.toString())
                .param("size", size.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content[0].productId").value(1))
            .andExpect(jsonPath("$.data.content[0].name").value("에티오피아 예가체프"))
            .andExpect(jsonPath("$.data.content[0].options[0].optionId").value(1))
            .andExpect(jsonPath("$.data.content[0].options[0].availableStock").value(50))
            .andExpect(jsonPath("$.data.pageable.pageNumber").value(page))
            .andExpect(jsonPath("$.data.pageable.pageSize").value(size))
            .andExpect(jsonPath("$.data.pageable.totalElements").value(1))
            .andExpect(jsonPath("$.data.pageable.totalPages").value(1))
    }

    @Test
    @DisplayName("GET /api/products - 파라미터 없이 조회 시 기본값(page=1, size=20)이 적용되어야 한다")
    fun `파라미터 없이 조회 시 기본값이 적용되어야 한다`() {
        // Given
        val defaultPage = 1
        val defaultSize = 20
        val mockResponse = ProductListResponse(
            content = emptyList(),
            pageable = PageableResponse(
                pageNumber = defaultPage,
                pageSize = defaultSize,
                totalElements = 0L,
                totalPages = 0
            )
        )

        every { productService.getProducts(defaultPage, defaultSize) } returns mockResponse

        // When & Then
        mockMvc.perform(
            get("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.pageable.pageNumber").value(defaultPage))
            .andExpect(jsonPath("$.data.pageable.pageSize").value(defaultSize))
    }

    @Test
    @DisplayName("GET /api/products - 상품이 없을 경우 빈 배열을 반환해야 한다")
    fun `상품이 없을 경우 빈 배열을 반환해야 한다`() {
        // Given
        val page = 1
        val size = 10
        val mockResponse = ProductListResponse(
            content = emptyList(),
            pageable = PageableResponse(
                pageNumber = page,
                pageSize = size,
                totalElements = 0L,
                totalPages = 0
            )
        )

        every { productService.getProducts(page, size) } returns mockResponse

        // When & Then
        mockMvc.perform(
            get("/api/products")
                .param("page", page.toString())
                .param("size", size.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content").isEmpty)
            .andExpect(jsonPath("$.data.pageable.totalElements").value(0))
    }

    @Test
    @DisplayName("GET /api/products - page가 1 미만일 경우 400 Bad Request를 반환해야 한다")
    fun `page가 1 미만일 경우 400 Bad Request를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            get("/api/products")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
    }

    @Test
    @DisplayName("GET /api/products - page가 음수일 경우 400 Bad Request를 반환해야 한다")
    fun `page가 음수일 경우 400 Bad Request를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            get("/api/products")
                .param("page", "-1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
    }

    @Test
    @DisplayName("GET /api/products - size가 1 미만일 경우 400 Bad Request를 반환해야 한다")
    fun `size가 1 미만일 경우 400 Bad Request를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            get("/api/products")
                .param("page", "1")
                .param("size", "0")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
    }

    @Test
    @DisplayName("GET /api/products - size가 100 초과일 경우 400 Bad Request를 반환해야 한다")
    fun `size가 100 초과일 경우 400 Bad Request를 반환해야 한다`() {
        // When & Then
        mockMvc.perform(
            get("/api/products")
                .param("page", "1")
                .param("size", "101")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
    }

    @Test
    @DisplayName("GET /api/products - Service를 정확한 파라미터로 호출해야 한다")
    fun `Service를 정확한 파라미터로 호출해야 한다`() {
        // Given
        val page = 2
        val size = 15
        val mockResponse = ProductListResponse(
            content = emptyList(),
            pageable = PageableResponse(
                pageNumber = page,
                pageSize = size,
                totalElements = 0L,
                totalPages = 0
            )
        )

        every { productService.getProducts(page, size) } returns mockResponse

        // When
        mockMvc.perform(
            get("/api/products")
                .param("page", page.toString())
                .param("size", size.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)

        // Then: MockK의 every에서 정확한 파라미터로 호출되었는지 이미 검증됨
        // 만약 다른 파라미터로 호출되었다면 every 매칭이 실패하고 에러 발생
    }
}
