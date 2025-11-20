package com.beanbliss.domain.inventory.controller

import com.beanbliss.common.dto.PageableResponse
import com.beanbliss.domain.inventory.dto.InventoryListResponse
import com.beanbliss.domain.inventory.dto.InventoryResponse
import com.beanbliss.domain.inventory.usecase.GetInventoriesUseCase
import com.beanbliss.domain.inventory.usecase.InventoryDetail
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
 * 재고 목록 조회 Controller의 HTTP 요청/응답을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. API 엔드포인트가 올바른 경로와 메서드로 매핑되는가?
 * 2. 요청 파라미터가 올바르게 바인딩되는가?
 * 3. UseCase 결과가 올바른 JSON 형식으로 반환되는가?
 * 4. 적절한 HTTP 상태 코드가 반환되는가?
 *
 * [참고]:
 * - 파라미터 유효성 검증은 Jakarta Validator가 자동으로 수행하므로 테스트 제외
 *   (Spring Framework가 보장하는 표준 동작이므로 별도 테스트 불필요)
 *
 * [관련 API]:
 * - GET /api/inventories
 */
@WebMvcTest(InventoryController::class)
@DisplayName("재고 목록 조회 Controller 테스트")
class InventoryListControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var getInventoriesUseCase: GetInventoriesUseCase

    @MockkBean
    private lateinit var inventoryAddStockUseCase: com.beanbliss.domain.inventory.usecase.InventoryAddStockUseCase

    @Test
    @DisplayName("GET /api/inventories - 정상 조회 시 200 OK와 재고 목록을 반환해야 한다")
    fun `정상 조회 시 200 OK와 재고 목록을 반환해야 한다`() {
        // Given
        val page = 1
        val size = 10
        val now = LocalDateTime.now()
        val mockUseCaseResult = GetInventoriesUseCase.InventoriesUseCaseResult(
            inventories = listOf(
                InventoryDetail(
                    inventoryId = 1L,
                    productId = 100L,
                    productName = "에티오피아 예가체프 G1",
                    productOptionId = 1L,
                    optionCode = "ETH-HD-200",
                    optionName = "핸드드립 200g",
                    price = 21000,
                    stockQuantity = 50,
                    createdAt = now
                ),
                InventoryDetail(
                    inventoryId = 2L,
                    productId = 101L,
                    productName = "콜롬비아 수프리모",
                    productOptionId = 2L,
                    optionCode = "COL-ESP-500",
                    optionName = "에스프레소 500g",
                    price = 35000,
                    stockQuantity = 30,
                    createdAt = now
                )
            ),
            totalElements = 45L
        )

        every { getInventoriesUseCase.getInventories(page, size) } returns mockUseCaseResult

        // When & Then
        mockMvc.perform(
            get("/api/inventories")
                .param("page", page.toString())
                .param("size", size.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content[0].inventoryId").value(1))
            .andExpect(jsonPath("$.data.content[0].productId").value(100))
            .andExpect(jsonPath("$.data.content[0].productName").value("에티오피아 예가체프 G1"))
            .andExpect(jsonPath("$.data.content[0].productOptionId").value(1))
            .andExpect(jsonPath("$.data.content[0].optionCode").value("ETH-HD-200"))
            .andExpect(jsonPath("$.data.content[0].optionName").value("핸드드립 200g"))
            .andExpect(jsonPath("$.data.content[0].price").value(21000))
            .andExpect(jsonPath("$.data.content[0].stockQuantity").value(50))
            .andExpect(jsonPath("$.data.content[1].productOptionId").value(2))
            .andExpect(jsonPath("$.data.content[1].stockQuantity").value(30))
            .andExpect(jsonPath("$.data.pageable.pageNumber").value(page))
            .andExpect(jsonPath("$.data.pageable.pageSize").value(size))
            .andExpect(jsonPath("$.data.pageable.totalElements").value(45))
            .andExpect(jsonPath("$.data.pageable.totalPages").value(5))
    }

    @Test
    @DisplayName("GET /api/inventories - 파라미터 없이 조회 시 기본값(page=1, size=10)이 적용되어야 한다")
    fun `파라미터 없이 조회 시 기본값이 적용되어야 한다`() {
        // Given
        val defaultPage = 1
        val defaultSize = 10
        val mockUseCaseResult = GetInventoriesUseCase.InventoriesUseCaseResult(
            inventories = emptyList(),
            totalElements = 0L
        )

        every { getInventoriesUseCase.getInventories(defaultPage, defaultSize) } returns mockUseCaseResult

        // When & Then
        mockMvc.perform(
            get("/api/inventories")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.pageable.pageNumber").value(defaultPage))
            .andExpect(jsonPath("$.data.pageable.pageSize").value(defaultSize))
    }

    @Test
    @DisplayName("GET /api/inventories - 재고가 없을 경우 빈 배열을 반환해야 한다")
    fun `재고가 없을 경우 빈 배열을 반환해야 한다`() {
        // Given
        val page = 1
        val size = 10
        val mockUseCaseResult = GetInventoriesUseCase.InventoriesUseCaseResult(
            inventories = emptyList(),
            totalElements = 0L
        )

        every { getInventoriesUseCase.getInventories(page, size) } returns mockUseCaseResult

        // When & Then
        mockMvc.perform(
            get("/api/inventories")
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
    @DisplayName("GET /api/inventories - UseCase를 정확한 파라미터로 호출해야 한다")
    fun `UseCase를 정확한 파라미터로 호출해야 한다`() {
        // Given
        val page = 2
        val size = 15
        val mockUseCaseResult = GetInventoriesUseCase.InventoriesUseCaseResult(
            inventories = emptyList(),
            totalElements = 0L
        )

        every { getInventoriesUseCase.getInventories(page, size) } returns mockUseCaseResult

        // When
        mockMvc.perform(
            get("/api/inventories")
                .param("page", page.toString())
                .param("size", size.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)

        // Then: MockK의 every에서 정확한 파라미터로 호출되었는지 이미 검증됨
        // 만약 다른 파라미터로 호출되었다면 every 매칭이 실패하고 에러 발생
    }
}
