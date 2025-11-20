package com.beanbliss.domain.inventory.controller

import com.beanbliss.common.exception.ResourceNotFoundException
import com.beanbliss.domain.inventory.exception.MaxStockExceededException
import com.beanbliss.domain.inventory.service.InventoryService
import com.beanbliss.domain.inventory.usecase.AddStockResult
import com.beanbliss.domain.inventory.usecase.InventoryAddStockUseCase
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * 재고 추가 Controller의 HTTP 요청/응답을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. API 엔드포인트가 올바른 경로와 메서드로 매핑되는가?
 * 2. 요청 본문(JSON)이 올바르게 바인딩되는가?
 * 3. Path Variable이 올바르게 바인딩되는가?
 * 4. UseCase 결과가 올바른 JSON 형식으로 반환되는가?
 * 5. 유효성 검증이 올바르게 수행되는가?
 * 6. 예외 상황에서 적절한 HTTP 상태 코드가 반환되는가?
 *
 * [관련 API]:
 * - POST /api/inventories/{productOptionId}/add
 */
@WebMvcTest(InventoryController::class)
@DisplayName("재고 추가 Controller 테스트")
class InventoryAddStockControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var inventoryService: InventoryService

    @MockkBean
    private lateinit var inventoryAddStockUseCase: InventoryAddStockUseCase

    @MockkBean
    private lateinit var getInventoriesUseCase: com.beanbliss.domain.inventory.usecase.GetInventoriesUseCase

    @Test
    @DisplayName("POST /api/inventories/{productOptionId}/add - 재고 추가 성공 시 200 OK와 현재 재고를 반환해야 한다")
    fun `재고 추가 성공 시 200 OK와 현재 재고를 반환해야 한다`() {
        // Given
        val productOptionId = 1L
        val quantity = 50
        val currentStock = 58
        val requestBody = mapOf("quantity" to quantity)

        every { inventoryAddStockUseCase.addStock(productOptionId, quantity) } returns
                AddStockResult(productOptionId, currentStock)

        // When & Then
        mockMvc.perform(
            post("/api/inventories/$productOptionId/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.productOptionId").value(productOptionId))
            .andExpect(jsonPath("$.data.currentStock").value(currentStock))
    }

    @Test
    @DisplayName("POST /api/inventories/{productOptionId}/add - 재고 추가 후 현재 재고 수량만 응답에 포함되어야 한다")
    fun `재고 추가 후 현재 재고 수량만 응답에 포함되어야 한다`() {
        // Given
        val productOptionId = 2L
        val quantity = 100
        val currentStock = 150
        val requestBody = mapOf("quantity" to quantity)

        every { inventoryAddStockUseCase.addStock(productOptionId, quantity) } returns
                AddStockResult(productOptionId, currentStock)

        // When & Then
        mockMvc.perform(
            post("/api/inventories/$productOptionId/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.productOptionId").value(productOptionId))
            .andExpect(jsonPath("$.data.currentStock").value(currentStock))
            // previousStock과 addedQuantity는 응답에 포함되지 않아야 함
            .andExpect(jsonPath("$.data.previousStock").doesNotExist())
            .andExpect(jsonPath("$.data.addedQuantity").doesNotExist())
    }

    @Test
    @DisplayName("POST /api/inventories/{productOptionId}/add - 수량이 1 미만일 경우 400 Bad Request를 반환해야 한다")
    fun `수량이 1 미만일 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val productOptionId = 1L
        val invalidQuantity = 0
        val requestBody = mapOf("quantity" to invalidQuantity)

        // When & Then
        mockMvc.perform(
            post("/api/inventories/$productOptionId/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.message").value("수량은 1개 이상이어야 합니다."))
    }

    @Test
    @DisplayName("POST /api/inventories/{productOptionId}/add - 수량이 음수일 경우 400 Bad Request를 반환해야 한다")
    fun `수량이 음수일 경우 400 Bad Request를 반환해야 한다`() {
        // Given
        val productOptionId = 1L
        val invalidQuantity = -10
        val requestBody = mapOf("quantity" to invalidQuantity)

        // When & Then
        mockMvc.perform(
            post("/api/inventories/$productOptionId/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
    }

    @Test
    @DisplayName("POST /api/inventories/{productOptionId}/add - 상품 옵션이 존재하지 않으면 404 Not Found를 반환해야 한다")
    fun `상품 옵션이 존재하지 않으면 404 Not Found를 반환해야 한다`() {
        // Given
        val productOptionId = 999L
        val quantity = 50
        val requestBody = mapOf("quantity" to quantity)

        every { inventoryAddStockUseCase.addStock(productOptionId, quantity) } throws
                ResourceNotFoundException("상품 옵션 ID: $productOptionId 을(를) 찾을 수 없습니다.")

        // When & Then
        mockMvc.perform(
            post("/api/inventories/$productOptionId/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("상품 옵션 ID: $productOptionId 을(를) 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("POST /api/inventories/{productOptionId}/add - 재고 정보가 존재하지 않으면 404 Not Found를 반환해야 한다")
    fun `재고 정보가 존재하지 않으면 404 Not Found를 반환해야 한다`() {
        // Given
        val productOptionId = 1L
        val quantity = 50
        val requestBody = mapOf("quantity" to quantity)

        every { inventoryAddStockUseCase.addStock(productOptionId, quantity) } throws
                ResourceNotFoundException("상품 옵션 ID: $productOptionId 의 재고 정보를 찾을 수 없습니다.")

        // When & Then
        mockMvc.perform(
            post("/api/inventories/$productOptionId/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
    }

    @Test
    @DisplayName("POST /api/inventories/{productOptionId}/add - 최대 재고 수량을 초과하면 400 Bad Request를 반환해야 한다")
    fun `최대 재고 수량을 초과하면 400 Bad Request를 반환해야 한다`() {
        // Given
        val productOptionId = 1L
        val quantity = 999_999
        val currentStock = 10
        val requestBody = mapOf("quantity" to quantity)

        every { inventoryAddStockUseCase.addStock(productOptionId, quantity) } throws
                MaxStockExceededException(
                    "재고 추가 후 총 수량이 최대 허용량(1,000,000개)을 초과합니다. " +
                            "현재: $currentStock, 추가 요청: $quantity"
                )

        // When & Then
        mockMvc.perform(
            post("/api/inventories/$productOptionId/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("MAX_STOCK_EXCEEDED"))
            .andExpect(jsonPath("$.message").value(
                "재고 추가 후 총 수량이 최대 허용량(1,000,000개)을 초과합니다. " +
                        "현재: $currentStock, 추가 요청: $quantity"
            ))
    }

    @Test
    @DisplayName("POST /api/inventories/{productOptionId}/add - Path Variable이 올바르게 바인딩되어야 한다")
    fun `Path Variable이 올바르게 바인딩되어야 한다`() {
        // Given
        val productOptionId = 12345L
        val quantity = 50
        val requestBody = mapOf("quantity" to quantity)

        every { inventoryAddStockUseCase.addStock(productOptionId, quantity) } returns
                AddStockResult(productOptionId, 100)

        // When & Then
        mockMvc.perform(
            post("/api/inventories/$productOptionId/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.productOptionId").value(productOptionId))

        // [Path Variable 바인딩 검증]: UseCase가 올바른 productOptionId로 호출되었는지 확인
        // MockK의 every에서 정확한 파라미터로 호출되었는지 이미 검증됨
    }

    @Test
    @DisplayName("POST /api/inventories/{productOptionId}/add - Request Body가 올바르게 바인딩되어야 한다")
    fun `Request Body가 올바르게 바인딩되어야 한다`() {
        // Given
        val productOptionId = 1L
        val quantity = 200
        val requestBody = mapOf("quantity" to quantity)

        every { inventoryAddStockUseCase.addStock(productOptionId, quantity) } returns
                AddStockResult(productOptionId, 250)

        // When & Then
        mockMvc.perform(
            post("/api/inventories/$productOptionId/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
        )
            .andExpect(status().isOk)

        // [Request Body 바인딩 검증]: UseCase가 올바른 quantity로 호출되었는지 확인
        // MockK의 every에서 정확한 파라미터로 호출되었는지 이미 검증됨
    }


    @Test
    @DisplayName("POST /api/inventories/{productOptionId}/add - quantity 필드가 누락되면 400 Bad Request를 반환해야 한다")
    fun `quantity 필드가 누락되면 400 Bad Request를 반환해야 한다`() {
        // Given
        val productOptionId = 1L
        val emptyBody = mapOf<String, Any>()

        // When & Then
        mockMvc.perform(
            post("/api/inventories/$productOptionId/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyBody))
        )
            .andExpect(status().isBadRequest)
    }
}
