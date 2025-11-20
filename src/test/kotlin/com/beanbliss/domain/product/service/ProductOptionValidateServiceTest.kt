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
        productService = ProductService(productRepository, productOptionRepository)
    }

    @Test
    @DisplayName("모든 옵션이 활성 상태일 때 정상 처리되어야 한다 (예외 발생 안함)")
    fun `모든 옵션이 활성 상태일 때_정상 처리되어야 한다`() {
        // Given
        val optionIds = listOf(1L, 2L, 3L)

        val activeOption1 = createMockProductOption(1L, true)
        val activeOption2 = createMockProductOption(2L, true)
        val activeOption3 = createMockProductOption(3L, true)

        every { productOptionRepository.findByIdsBatch(optionIds) } returns listOf(
            activeOption1,
            activeOption2,
            activeOption3
        )

        // When & Then
        // [비즈니스 로직 검증]: 모든 옵션이 활성 상태이므로 예외가 발생하지 않아야 함
        assertDoesNotThrow {
            productService.validateProductOptionsActive(optionIds)
        }

        // [Repository 호출 검증]: Batch 조회가 정확히 1번 호출되어야 함 (N+1 문제 해결)
        verify(exactly = 1) { productOptionRepository.findByIdsBatch(optionIds) }
    }

    @Test
    @DisplayName("비활성 옵션(isActive = false)이 포함되어 있을 때 ProductOptionInactiveException이 발생해야 한다")
    fun `비활성 옵션이 포함되어 있을 때_ProductOptionInactiveException이 발생해야 한다`() {
        // Given
        val optionIds = listOf(1L, 2L)

        val activeOption = createMockProductOption(1L, true)
        // Batch 조회에서 2L은 반환되지 않음 (비활성 또는 미존재로 간주)

        every { productOptionRepository.findByIdsBatch(optionIds) } returns listOf(
            activeOption
            // 2L은 활성 상태의 옵션에 포함되지 않음 → 검증 실패
        )

        // When & Then
        // [핵심 비즈니스 규칙 검증]: 비활성 옵션이 포함되어 있으면 예외 발생
        val exception = assertThrows<ProductOptionInactiveException> {
            productService.validateProductOptionsActive(optionIds)
        }

        assertTrue(exception.message!!.contains("비활성화된 상품 옵션이 포함되어 있습니다"))
        assertTrue(exception.message!!.contains("2"))

        // [Repository 호출 검증]: Batch 조회가 정확히 1번 호출됨 (N+1 문제 해결)
        verify(exactly = 1) { productOptionRepository.findByIdsBatch(optionIds) }
    }

    @Test
    @DisplayName("존재하지 않는 옵션 ID(null 반환)가 포함되어 있을 때 ProductOptionInactiveException이 발생해야 한다")
    fun `존재하지 않는 옵션 ID가 포함되어 있을 때_ProductOptionInactiveException이 발생해야 한다`() {
        // Given
        val optionIds = listOf(999L) // 존재하지 않는 ID

        every { productOptionRepository.findByIdsBatch(optionIds) } returns emptyList()

        // When & Then
        // [비즈니스 로직 검증]: 존재하지 않는 옵션 ID는 비활성 옵션과 동일하게 예외 발생
        val exception = assertThrows<ProductOptionInactiveException> {
            productService.validateProductOptionsActive(optionIds)
        }

        assertTrue(exception.message!!.contains("비활성화된 상품 옵션이 포함되어 있습니다"))
        assertTrue(exception.message!!.contains("999"))

        // [Repository 호출 검증]: Batch 조회가 정확히 1번 호출됨
        verify(exactly = 1) { productOptionRepository.findByIdsBatch(optionIds) }
    }

    @Test
    @DisplayName("여러 옵션 중 첫 번째가 비활성일 때 즉시 예외가 발생해야 한다")
    fun `여러 옵션 중 첫 번째가 비활성일 때_즉시 예외가_발생해야_한다`() {
        // Given
        val optionIds = listOf(1L, 2L, 3L)

        // Batch 조회에서 1L은 반환되지 않음 (비활성 또는 미존재)
        val activeOption2 = createMockProductOption(2L, true)
        val activeOption3 = createMockProductOption(3L, true)

        every { productOptionRepository.findByIdsBatch(optionIds) } returns listOf(
            activeOption2,
            activeOption3
            // 1L은 활성 상태의 옵션에 포함되지 않음 → 검증 실패
        )

        // When & Then
        // [비즈니스 로직 검증]: 첫 번째 옵션이 비활성이므로 예외 발생
        val exception = assertThrows<ProductOptionInactiveException> {
            productService.validateProductOptionsActive(optionIds)
        }

        assertTrue(exception.message!!.contains("1"))

        // [Repository 호출 검증]: Batch 조회가 정확히 1번 호출됨 (N+1 문제 해결)
        verify(exactly = 1) { productOptionRepository.findByIdsBatch(optionIds) }
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

        // [Repository 호출 검증]: 빈 리스트이므로 조기 반환되어 Repository는 호출되지 않아야 함
        verify(exactly = 0) { productOptionRepository.findByIdsBatch(any()) }
    }

    @Test
    @DisplayName("단일 옵션이 활성 상태일 때 정상 처리되어야 한다")
    fun `단일 옵션이 활성 상태일 때_정상 처리되어야 한다`() {
        // Given
        val optionIds = listOf(1L)
        val activeOption = createMockProductOption(1L, true)

        every { productOptionRepository.findByIdsBatch(optionIds) } returns listOf(activeOption)

        // When & Then
        // [경계값 검증]: 단일 옵션도 정상 처리되어야 함
        assertDoesNotThrow {
            productService.validateProductOptionsActive(optionIds)
        }

        verify(exactly = 1) { productOptionRepository.findByIdsBatch(optionIds) }
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
