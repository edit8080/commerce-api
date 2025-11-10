package com.beanbliss.domain.inventory.usecase

import com.beanbliss.common.exception.ResourceNotFoundException
import com.beanbliss.domain.inventory.exception.MaxStockExceededException
import com.beanbliss.domain.inventory.service.InventoryService
import com.beanbliss.domain.product.service.ProductOptionService
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * 재고 추가 UseCase의 비즈니스 로직을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. ProductOptionService와 InventoryService를 올바르게 조율하는가?
 * 2. 상품 옵션 존재 여부 검증이 먼저 수행되는가?
 * 3. 재고 추가 후 현재 재고 수량을 올바르게 반환하는가?
 * 4. 예외 상황에서 올바른 예외를 전파하는가?
 * 5. 서비스 호출 순서가 올바른가? (ProductOptionService → InventoryService)
 *
 * [참고]:
 * - 트랜잭션 동작은 통합 테스트에서 검증합니다.
 *
 * [관련 API]:
 * - POST /api/inventories/{productOptionId}/add
 */
@DisplayName("재고 추가 UseCase 테스트")
class InventoryAddStockUseCaseTest {

    // Mock 객체
    private val productOptionService: ProductOptionService = mockk()
    private val inventoryService: InventoryService = mockk()

    // 테스트 대상
    private lateinit var inventoryAddStockUseCase: InventoryAddStockUseCase

    @BeforeEach
    fun setUp() {
        inventoryAddStockUseCase = InventoryAddStockUseCase(productOptionService, inventoryService)
    }

    @Test
    @DisplayName("재고 추가 성공 시 ProductOptionService와 InventoryService가 순차적으로 호출되어야 한다")
    fun `재고 추가 성공 시 ProductOptionService와 InventoryService가 순차적으로 호출되어야 한다`() {
        // Given
        val productOptionId = 1L
        val quantity = 50
        val expectedCurrentStock = 58

        every { productOptionService.existsById(productOptionId) } returns true
        every { inventoryService.addStock(productOptionId, quantity) } returns expectedCurrentStock

        // When
        val result = inventoryAddStockUseCase.addStock(productOptionId, quantity)

        // Then
        // [책임 검증 1]: ProductOptionService의 existsById가 먼저 호출되었는가?
        verify(exactly = 1) { productOptionService.existsById(productOptionId) }

        // [책임 검증 2]: InventoryService의 addStock이 호출되었는가?
        verify(exactly = 1) { inventoryService.addStock(productOptionId, quantity) }

        // [비즈니스 로직 검증]: 결과가 올바르게 반환되는가?
        assertEquals(productOptionId, result.productOptionId)
        assertEquals(expectedCurrentStock, result.currentStock)
    }

    @Test
    @DisplayName("재고 추가 성공 시 현재 재고 수량을 포함한 결과를 반환해야 한다")
    fun `재고 추가 성공 시 현재 재고 수량을 포함한 결과를 반환해야 한다`() {
        // Given
        val productOptionId = 2L
        val quantity = 100
        val expectedCurrentStock = 150

        every { productOptionService.existsById(productOptionId) } returns true
        every { inventoryService.addStock(productOptionId, quantity) } returns expectedCurrentStock

        // When
        val result = inventoryAddStockUseCase.addStock(productOptionId, quantity)

        // Then
        assertNotNull(result)
        assertEquals(productOptionId, result.productOptionId, "상품 옵션 ID가 일치해야 함")
        assertEquals(expectedCurrentStock, result.currentStock, "현재 재고 수량이 일치해야 함")
    }

    @Test
    @DisplayName("상품 옵션이 존재하지 않으면 ResourceNotFoundException이 발생하고 InventoryService는 호출되지 않아야 한다")
    fun `상품 옵션이 존재하지 않으면 ResourceNotFoundException이 발생하고 InventoryService는 호출되지 않아야 한다`() {
        // Given
        val productOptionId = 999L
        val quantity = 50

        every { productOptionService.existsById(productOptionId) } returns false

        // When & Then
        val exception = assertThrows<ResourceNotFoundException> {
            inventoryAddStockUseCase.addStock(productOptionId, quantity)
        }

        assertEquals("상품 옵션 ID: $productOptionId 을(를) 찾을 수 없습니다.", exception.message)

        // [책임 검증]: 상품 옵션이 없으면 InventoryService는 호출되지 않아야 함
        verify(exactly = 1) { productOptionService.existsById(productOptionId) }
        verify(exactly = 0) { inventoryService.addStock(any(), any()) }
    }

    @Test
    @DisplayName("재고 정보가 존재하지 않으면 InventoryService에서 발생한 ResourceNotFoundException을 전파해야 한다")
    fun `재고 정보가 존재하지 않으면 InventoryService에서 발생한 ResourceNotFoundException을 전파해야 한다`() {
        // Given
        val productOptionId = 1L
        val quantity = 50

        every { productOptionService.existsById(productOptionId) } returns true
        every { inventoryService.addStock(productOptionId, quantity) } throws
                ResourceNotFoundException("상품 옵션 ID: $productOptionId 의 재고 정보를 찾을 수 없습니다.")

        // When & Then
        val exception = assertThrows<ResourceNotFoundException> {
            inventoryAddStockUseCase.addStock(productOptionId, quantity)
        }

        assertTrue(exception.message!!.contains("재고 정보를 찾을 수 없습니다"))

        // [책임 검증]: 두 서비스 모두 호출되었는가?
        verify(exactly = 1) { productOptionService.existsById(productOptionId) }
        verify(exactly = 1) { inventoryService.addStock(productOptionId, quantity) }
    }

    @Test
    @DisplayName("최대 재고 수량을 초과하면 MaxStockExceededException을 전파해야 한다")
    fun `최대 재고 수량을 초과하면 MaxStockExceededException을 전파해야 한다`() {
        // Given
        val productOptionId = 1L
        val quantity = 999_999
        val currentStock = 10

        every { productOptionService.existsById(productOptionId) } returns true
        every { inventoryService.addStock(productOptionId, quantity) } throws
                MaxStockExceededException(
                    "재고 추가 후 총 수량이 최대 허용량(1,000,000개)을 초과합니다. " +
                            "현재: $currentStock, 추가 요청: $quantity"
                )

        // When & Then
        val exception = assertThrows<MaxStockExceededException> {
            inventoryAddStockUseCase.addStock(productOptionId, quantity)
        }

        assertTrue(exception.message!!.contains("최대 허용량"))
        assertTrue(exception.message!!.contains("1,000,000"))

        // [책임 검증]: 두 서비스 모두 호출되었는가?
        verify(exactly = 1) { productOptionService.existsById(productOptionId) }
        verify(exactly = 1) { inventoryService.addStock(productOptionId, quantity) }
    }

    @Test
    @DisplayName("ProductOptionService 호출이 InventoryService 호출보다 먼저 이루어져야 한다")
    fun `ProductOptionService 호출이 InventoryService 호출보다 먼저 이루어져야 한다`() {
        // Given
        val productOptionId = 1L
        val quantity = 50
        val callOrder = mutableListOf<String>()

        every { productOptionService.existsById(productOptionId) } answers {
            callOrder.add("ProductOptionService.existsById")
            true
        }
        every { inventoryService.addStock(productOptionId, quantity) } answers {
            callOrder.add("InventoryService.addStock")
            100
        }

        // When
        inventoryAddStockUseCase.addStock(productOptionId, quantity)

        // Then
        // [실행 순서 검증]: ProductOptionService가 먼저 호출되었는가?
        assertEquals(2, callOrder.size)
        assertEquals("ProductOptionService.existsById", callOrder[0])
        assertEquals("InventoryService.addStock", callOrder[1])
    }
}
