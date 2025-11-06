package com.beanbliss.domain.product.service

import com.beanbliss.domain.order.exception.ProductOptionInactiveException
import com.beanbliss.domain.product.repository.ProductOptionDetail
import com.beanbliss.domain.product.repository.ProductOptionRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * ProductService의 상품 옵션 활성 여부 검증 비즈니스 로직을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. 모든 옵션이 활성 상태일 때 정상 처리되는가?
 * 2. 비활성 옵션이 포함되어 있을 때 ProductOptionInactiveException이 발생하는가?
 * 3. 존재하지 않는 옵션 ID가 포함되어 있을 때 ProductOptionInactiveException이 발생하는가?
 * 4. 여러 옵션 중 첫 번째 비활성 옵션에서 즉시 예외가 발생하는가?
 *
 * [관련 UseCase]:
 * - ReserveOrderUseCase
 */
@DisplayName("상품 옵션 활성 여부 검증 Service 테스트")
class ProductOptionValidateServiceTest {

    // Mock 객체 (Repository Interface에 의존)
    private val productRepository: com.beanbliss.domain.product.repository.ProductRepository = mockk()
    private val productOptionRepository: ProductOptionRepository = mockk()

    // 테스트 대상 (Service 인터페이스로 선언)
    private lateinit var productService: ProductService

    @BeforeEach
    fun setUp() {
        productService = ProductServiceImpl(productRepository, productOptionRepository)
    }

    @Test
    @DisplayName("모든 옵션이 활성 상태일 때 정상 처리되어야 한다 (예외 발생 안함)")
    fun `모든 옵션이 활성 상태일 때_정상 처리되어야 한다`() {
        // Given
        val optionIds = listOf(1L, 2L, 3L)

        val activeOption1 = createMockProductOption(1L, true)
        val activeOption2 = createMockProductOption(2L, true)
        val activeOption3 = createMockProductOption(3L, true)

        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns activeOption1
        every { productOptionRepository.findActiveOptionWithProduct(2L) } returns activeOption2
        every { productOptionRepository.findActiveOptionWithProduct(3L) } returns activeOption3

        // When & Then
        // [비즈니스 로직 검증]: 모든 옵션이 활성 상태이므로 예외가 발생하지 않아야 함
        assertDoesNotThrow {
            productService.validateProductOptionsActive(optionIds)
        }

        // [Repository 호출 검증]: 모든 옵션에 대해 findActiveOptionWithProduct가 호출되어야 함
        verify(exactly = 1) { productOptionRepository.findActiveOptionWithProduct(1L) }
        verify(exactly = 1) { productOptionRepository.findActiveOptionWithProduct(2L) }
        verify(exactly = 1) { productOptionRepository.findActiveOptionWithProduct(3L) }
    }

    @Test
    @DisplayName("비활성 옵션(isActive = false)이 포함되어 있을 때 ProductOptionInactiveException이 발생해야 한다")
    fun `비활성 옵션이 포함되어 있을 때_ProductOptionInactiveException이 발생해야 한다`() {
        // Given
        val optionIds = listOf(1L, 2L)

        val activeOption = createMockProductOption(1L, true)
        val inactiveOption = createMockProductOption(2L, false) // 비활성

        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns activeOption
        every { productOptionRepository.findActiveOptionWithProduct(2L) } returns inactiveOption

        // When & Then
        // [핵심 비즈니스 규칙 검증]: 비활성 옵션이 포함되어 있으면 예외 발생
        val exception = assertThrows<ProductOptionInactiveException> {
            productService.validateProductOptionsActive(optionIds)
        }

        assertTrue(exception.message!!.contains("비활성화된 상품 옵션이 포함되어 있습니다"))
        assertTrue(exception.message!!.contains("2"))

        // [Repository 호출 검증]: 비활성 옵션을 발견할 때까지만 호출됨
        verify(exactly = 1) { productOptionRepository.findActiveOptionWithProduct(1L) }
        verify(exactly = 1) { productOptionRepository.findActiveOptionWithProduct(2L) }
    }

    @Test
    @DisplayName("존재하지 않는 옵션 ID(null 반환)가 포함되어 있을 때 ProductOptionInactiveException이 발생해야 한다")
    fun `존재하지 않는 옵션 ID가 포함되어 있을 때_ProductOptionInactiveException이 발생해야 한다`() {
        // Given
        val optionIds = listOf(999L) // 존재하지 않는 ID

        every { productOptionRepository.findActiveOptionWithProduct(999L) } returns null

        // When & Then
        // [비즈니스 로직 검증]: 존재하지 않는 옵션 ID는 비활성 옵션과 동일하게 예외 발생
        val exception = assertThrows<ProductOptionInactiveException> {
            productService.validateProductOptionsActive(optionIds)
        }

        assertTrue(exception.message!!.contains("비활성화된 상품 옵션이 포함되어 있습니다"))
        assertTrue(exception.message!!.contains("999"))

        // [Repository 호출 검증]: findActiveOptionWithProduct가 호출되어야 함
        verify(exactly = 1) { productOptionRepository.findActiveOptionWithProduct(999L) }
    }

    @Test
    @DisplayName("여러 옵션 중 첫 번째가 비활성일 때 즉시 예외가 발생하고 나머지 옵션은 검증하지 않아야 한다")
    fun `여러 옵션 중 첫 번째가 비활성일 때_즉시 예외가 발생하고_나머지 옵션은 검증하지 않아야 한다`() {
        // Given
        val optionIds = listOf(1L, 2L, 3L)

        val inactiveOption = createMockProductOption(1L, false) // 첫 번째가 비활성

        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns inactiveOption

        // When & Then
        // [비즈니스 로직 검증]: 첫 번째 옵션에서 예외가 발생하므로 나머지는 검증 안함
        val exception = assertThrows<ProductOptionInactiveException> {
            productService.validateProductOptionsActive(optionIds)
        }

        assertTrue(exception.message!!.contains("1"))

        // [Repository 호출 검증]: 첫 번째 옵션만 조회되고, 나머지는 호출되지 않아야 함
        verify(exactly = 1) { productOptionRepository.findActiveOptionWithProduct(1L) }
        verify(exactly = 0) { productOptionRepository.findActiveOptionWithProduct(2L) }
        verify(exactly = 0) { productOptionRepository.findActiveOptionWithProduct(3L) }
    }

    @Test
    @DisplayName("빈 리스트 검증 시 정상 처리되어야 한다 (경계값)")
    fun `빈 리스트 검증 시_정상 처리되어야 한다`() {
        // Given
        val optionIds = emptyList<Long>()

        // When & Then
        // [경계값 검증]: 빈 리스트는 검증할 옵션이 없으므로 예외가 발생하지 않아야 함
        assertDoesNotThrow {
            productService.validateProductOptionsActive(optionIds)
        }

        // [Repository 호출 검증]: 빈 리스트이므로 Repository는 호출되지 않아야 함
        verify(exactly = 0) { productOptionRepository.findActiveOptionWithProduct(any()) }
    }

    @Test
    @DisplayName("단일 옵션이 활성 상태일 때 정상 처리되어야 한다")
    fun `단일 옵션이 활성 상태일 때_정상 처리되어야 한다`() {
        // Given
        val optionIds = listOf(1L)
        val activeOption = createMockProductOption(1L, true)

        every { productOptionRepository.findActiveOptionWithProduct(1L) } returns activeOption

        // When & Then
        // [경계값 검증]: 단일 옵션도 정상 처리되어야 함
        assertDoesNotThrow {
            productService.validateProductOptionsActive(optionIds)
        }

        verify(exactly = 1) { productOptionRepository.findActiveOptionWithProduct(1L) }
    }

    // === Helper Method ===

    /**
     * 테스트용 Mock ProductOption 생성
     */
    private fun createMockProductOption(
        optionId: Long,
        isActive: Boolean
    ): ProductOptionDetail {
        return ProductOptionDetail(
            optionId = optionId,
            productId = 1L,
            productName = "에티오피아 예가체프 G1",
            optionCode = "ETH-HD-200",
            origin = "에티오피아",
            grindType = "홀빈",
            weightGrams = 200,
            price = 15000,
            isActive = isActive
        )
    }
}
