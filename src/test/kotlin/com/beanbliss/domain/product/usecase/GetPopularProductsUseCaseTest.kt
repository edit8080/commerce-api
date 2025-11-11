package com.beanbliss.domain.product.usecase

import com.beanbliss.domain.order.repository.ProductOptionOrderCount
import com.beanbliss.domain.order.service.OrderService
import com.beanbliss.domain.product.repository.ProductBasicInfo
import com.beanbliss.domain.product.repository.ProductOptionDetail
import com.beanbliss.domain.product.service.ProductService
import com.beanbliss.domain.product.service.ProductOptionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * GetPopularProductsUseCaseTest의 비즈니스 로직과 책임 분산을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. UseCase는 OrderService, ProductOptionService, ProductService를 올바르게 오케스트레이션하는가?
 * 2. 상품 옵션별 주문 수량이 상품별로 올바르게 집계되는가?
 * 3. 주문 수량 데이터와 상품 정보가 올바르게 병합되는가?
 * 4. 정렬 순서가 유지되는가?
 * 5. 데이터 정합성 예외 상황이 올바르게 처리되는가?
 */
@DisplayName("인기 상품 조회 UseCase 테스트")
class GetPopularProductsUseCaseTest {

    private val orderService: OrderService = mockk()
    private val productOptionService: ProductOptionService = mockk()
    private val productService: ProductService = mockk()
    private val useCase = GetPopularProductsUseCase(orderService, productOptionService, productService)

    @Test
    @DisplayName("인기 상품 조회 성공 시 OrderService, ProductOptionService, ProductService가 순서대로 호출되어야 한다")
    fun `인기 상품 조회 성공 시 OrderService, ProductOptionService, ProductService가 순서대로 호출되어야 한다`() {
        // Given
        val period = 7
        val limit = 10

        // ORDER 도메인: 상품 옵션별 주문 수량 (옵션 1, 2는 상품 1, 옵션 3은 상품 2)
        val optionOrderCounts = listOf(
            ProductOptionOrderCount(productOptionId = 1L, totalOrderCount = 100),
            ProductOptionOrderCount(productOptionId = 2L, totalOrderCount = 50),
            ProductOptionOrderCount(productOptionId = 3L, totalOrderCount = 120)
        )

        // PRODUCT 도메인: 상품 옵션 정보 (어느 상품에 속하는지 포함)
        val productOptions = mapOf(
            1L to ProductOptionDetail(optionId = 1L, productId = 1L, productName = "에티오피아 예가체프 G1",
                optionCode = "ETH-001-W", origin = "에티오피아", grindType = "원두", weightGrams = 200, price = 15000, isActive = true),
            2L to ProductOptionDetail(optionId = 2L, productId = 1L, productName = "에티오피아 예가체프 G1",
                optionCode = "ETH-001-G", origin = "에티오피아", grindType = "분쇄", weightGrams = 200, price = 15000, isActive = true),
            3L to ProductOptionDetail(optionId = 3L, productId = 2L, productName = "콜롬비아 수프리모",
                optionCode = "COL-002-W", origin = "콜롬비아", grindType = "원두", weightGrams = 200, price = 13000, isActive = true)
        )

        val productInfos = listOf(
            ProductBasicInfo(
                productId = 1L,
                productName = "에티오피아 예가체프 G1",
                brand = "Bean Bliss",
                description = "플로럴하고 과일향이 풍부한 에티오피아 대표 원두"
            ),
            ProductBasicInfo(
                productId = 2L,
                productName = "콜롬비아 수프리모",
                brand = "Bean Bliss",
                description = "부드러운 맛과 균형잡힌 바디감"
            )
        )

        every { orderService.getTopOrderedProductOptions(period, limit * 10) } returns optionOrderCounts
        every { productOptionService.getOptionsBatch(listOf(1L, 2L, 3L)) } returns productOptions
        every { productService.getProductsByIds(listOf(1L, 2L)) } returns productInfos

        // When
        val result = useCase.getPopularProducts(period, limit)

        // Then
        // [TDD 검증 목표 1]: OrderService가 올바른 파라미터로 먼저 호출되었는가?
        verify(exactly = 1) { orderService.getTopOrderedProductOptions(period, limit * 10) }

        // [TDD 검증 목표 2]: ProductOptionService가 올바른 옵션 IDs로 호출되었는가?
        verify(exactly = 1) { productOptionService.getOptionsBatch(listOf(1L, 2L, 3L)) }

        // [TDD 검증 목표 3]: ProductService가 올바른 productIds로 호출되었는가?
        verify(exactly = 1) { productService.getProductsByIds(listOf(1L, 2L)) }

        // [TDD 검증 목표 4]: 결과가 올바르게 집계되고 병합되었는가?
        // 상품 1: 옵션1(100) + 옵션2(50) = 150
        // 상품 2: 옵션3(120) = 120
        assertEquals(2, result.size)
        assertEquals(1L, result[0].productId)
        assertEquals(150, result[0].totalOrderCount)
        assertEquals("에티오피아 예가체프 G1", result[0].productName)
        assertEquals("Bean Bliss", result[0].brand)
        assertEquals(2L, result[1].productId)
        assertEquals(120, result[1].totalOrderCount)
    }

    @Test
    @DisplayName("정렬 순서가 유지되어야 한다 (주문 수 내림차순)")
    fun `정렬 순서가 유지되어야 한다`() {
        // Given
        val period = 7
        val limit = 3

        // 상품 옵션별 주문 수량 (3개 상품, 각 1개 옵션)
        val optionOrderCounts = listOf(
            ProductOptionOrderCount(productOptionId = 1L, totalOrderCount = 150),
            ProductOptionOrderCount(productOptionId = 2L, totalOrderCount = 120),
            ProductOptionOrderCount(productOptionId = 3L, totalOrderCount = 100)
        )

        val productOptions = mapOf(
            1L to ProductOptionDetail(1L, 1L, "상품1", "OPT-1", "origin1", "원두", 200, 10000, true),
            2L to ProductOptionDetail(2L, 2L, "상품2", "OPT-2", "origin2", "원두", 200, 10000, true),
            3L to ProductOptionDetail(3L, 3L, "상품3", "OPT-3", "origin3", "원두", 200, 10000, true)
        )

        val productInfos = listOf(
            ProductBasicInfo(1L, "상품1", "브랜드1", "설명1"),
            ProductBasicInfo(2L, "상품2", "브랜드2", "설명2"),
            ProductBasicInfo(3L, "상품3", "브랜드3", "설명3")
        )

        every { orderService.getTopOrderedProductOptions(period, limit * 10) } returns optionOrderCounts
        every { productOptionService.getOptionsBatch(listOf(1L, 2L, 3L)) } returns productOptions
        every { productService.getProductsByIds(any()) } returns productInfos

        // When
        val result = useCase.getPopularProducts(period, limit)

        // Then
        // [TDD 검증 목표 5]: 주문 수 내림차순이 유지되는가?
        assertEquals(150, result[0].totalOrderCount)
        assertEquals(120, result[1].totalOrderCount)
        assertEquals(100, result[2].totalOrderCount)
    }

    @Test
    @DisplayName("주문 수량 데이터가 비어있을 경우 빈 목록을 반환해야 한다")
    fun `주문 수량 데이터가 비어있을 경우 빈 목록을 반환해야 한다`() {
        // Given
        val period = 7
        val limit = 10

        every { orderService.getTopOrderedProductOptions(period, limit * 10) } returns emptyList()

        // When
        val result = useCase.getPopularProducts(period, limit)

        // Then
        // [TDD 검증 목표 6]: 빈 결과가 반환되는가?
        assertEquals(0, result.size)

        // [TDD 검증 목표 7]: ProductOptionService와 ProductService는 호출되지 않아야 한다 (빈 목록이므로)
        verify(exactly = 1) { orderService.getTopOrderedProductOptions(period, limit * 10) }
        verify(exactly = 0) { productOptionService.getOptionsBatch(any()) }
        verify(exactly = 0) { productService.getProductsByIds(any()) }
    }

    @Test
    @DisplayName("ProductService에서 일부 상품 정보가 누락되면 데이터 정합성 오류로 예외가 발생해야 한다")
    fun `ProductService에서 일부 상품 정보가 누락되면 데이터 정합성 오류로 예외가 발생해야 한다`() {
        // Given
        val period = 7
        val limit = 10

        // 상품 옵션별 주문 수량 (3개 상품, 각 1개 옵션)
        val optionOrderCounts = listOf(
            ProductOptionOrderCount(productOptionId = 1L, totalOrderCount = 150),
            ProductOptionOrderCount(productOptionId = 2L, totalOrderCount = 120),
            ProductOptionOrderCount(productOptionId = 3L, totalOrderCount = 100)
        )

        val productOptions = mapOf(
            1L to ProductOptionDetail(1L, 1L, "상품1", "OPT-1", "origin1", "원두", 200, 10000, true),
            2L to ProductOptionDetail(2L, 2L, "상품2", "OPT-2", "origin2", "원두", 200, 10000, true),
            3L to ProductOptionDetail(3L, 3L, "상품3", "OPT-3", "origin3", "원두", 200, 10000, true)
        )

        // ProductService에서 상품 2번 정보 누락 (데이터 정합성 문제)
        val productInfos = listOf(
            ProductBasicInfo(1L, "상품1", "브랜드1", "설명1"),
            ProductBasicInfo(3L, "상품3", "브랜드3", "설명3")
        )

        every { orderService.getTopOrderedProductOptions(period, limit * 10) } returns optionOrderCounts
        every { productOptionService.getOptionsBatch(listOf(1L, 2L, 3L)) } returns productOptions
        every { productService.getProductsByIds(listOf(1L, 2L, 3L)) } returns productInfos

        // When & Then
        // [TDD 검증 목표 8]: 데이터 정합성 오류 시 예외 발생
        val exception = assertThrows(IllegalStateException::class.java) {
            useCase.getPopularProducts(period, limit)
        }

        assertEquals("주문 수량 데이터와 상품 정보 간 불일치가 발생했습니다. 누락된 상품 ID: [2]", exception.message)
    }
}
