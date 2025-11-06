package com.beanbliss.domain.product.service

import com.beanbliss.domain.product.repository.ProductRepository
import com.beanbliss.domain.product.repository.ProductOptionRepository
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
 * 상품 + 옵션 조회 Service의 비즈니스 로직을 검증하는 테스트
 *
 * [검증 목표]:
 * - ProductService의 예외 처리 비즈니스 규칙 검증
 *
 * [비즈니스 로직]:
 * 1. 존재하지 않는 상품 ID 조회 시 적절한 예외가 발생하는가?
 * 2. 활성 옵션이 없는 상품 조회 시 적절한 예외가 발생하는가?
 *
 * [관련 API]:
 * - GET /api/products/{productId}
 */
@DisplayName("상품 + 옵션 조회 Service 테스트")
class ProductDetailServiceTest {

    // Mock 객체 (Repository Interface에 의존)
    private val productRepository: ProductRepository = mockk()
    private val productOptionRepository: ProductOptionRepository = mockk()

    // 테스트 대상 (Service 인터페이스로 선언)
    private lateinit var productService: ProductService

    @BeforeEach
    fun setUp() {
        productService = ProductServiceImpl(productRepository, productOptionRepository)
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID 조회 시 ResourceNotFoundException이 발생해야 한다")
    fun `존재하지 않는 상품 ID 조회 시 ResourceNotFoundException이 발생해야 한다`() {
        // Given
        val nonExistentProductId = 999L

        every { productRepository.findByIdWithOptions(nonExistentProductId) } returns null

        // When & Then
        // [비즈니스 로직 검증]: 존재하지 않는 상품 ID 조회 시 적절한 예외가 발생하는가?
        val exception = assertThrows(ResourceNotFoundException::class.java) {
            productService.getProductWithOptions(nonExistentProductId)
        }

        assertTrue(exception.message?.contains("$nonExistentProductId") ?: false,
            "예외 메시지에 상품 ID가 포함되어야 함")

        // [Repository 호출 검증]: findByIdWithOptions가 호출되어야 함
        verify(exactly = 1) { productRepository.findByIdWithOptions(nonExistentProductId) }
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
        // [비즈니스 로직 검증]: 활성 옵션이 없는 경우 적절한 예외가 발생하는가?
        val exception = assertThrows(ResourceNotFoundException::class.java) {
            productService.getProductWithOptions(productId)
        }

        assertTrue(exception.message?.contains("옵션") ?: false,
            "예외 메시지에 '옵션' 관련 내용이 포함되어야 함")

        // [Repository 호출 검증]: findByIdWithOptions가 호출되어야 함
        verify(exactly = 1) { productRepository.findByIdWithOptions(productId) }
    }
}
