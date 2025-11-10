package com.beanbliss.domain.product.usecase

import com.beanbliss.domain.order.repository.ProductOrderCount
import com.beanbliss.domain.order.service.OrderService
import com.beanbliss.domain.product.repository.ProductBasicInfo
import com.beanbliss.domain.product.service.ProductService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * GetPopularProductsUseCase의 비즈니스 로직과 책임 분산을 검증하는 테스트
 *
 * [검증 목표]:
 * 1. UseCase는 OrderService와 ProductService를 올바르게 오케스트레이션하는가?
 * 2. 주문 수량 데이터와 상품 정보가 올바르게 병합되는가?
 * 3. 정렬 순서가 유지되는가?
 * 4. 데이터 정합성 예외 상황이 올바르게 처리되는가?
 */
@DisplayName("인기 상품 조회 UseCase 테스트")
class GetPopularProductsUseCaseTest {

    private val orderService: OrderService = mockk()
    private val productService: ProductService = mockk()
    private val useCase = GetPopularProductsUseCase(orderService, productService)

    @Test
    @DisplayName("인기 상품 조회 성공 시 OrderService와 ProductService가 순서대로 호출되어야 한다")
    fun `인기 상품 조회 성공 시 OrderService와 ProductService가 순서대로 호출되어야 한다`() {
        // Given
        val period = 7
        val limit = 10

        val orderCounts = listOf(
            ProductOrderCount(productId = 1L, totalOrderCount = 150),
            ProductOrderCount(productId = 2L, totalOrderCount = 120)
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

        every { orderService.getTopOrderedProducts(period, limit) } returns orderCounts
        every { productService.getProductsByIds(listOf(1L, 2L)) } returns productInfos

        // When
        val result = useCase.getPopularProducts(period, limit)

        // Then
        // [TDD 검증 목표 1]: OrderService가 올바른 파라미터로 먼저 호출되었는가?
        verify(exactly = 1) { orderService.getTopOrderedProducts(period, limit) }

        // [TDD 검증 목표 2]: ProductService가 올바른 productIds로 호출되었는가?
        verify(exactly = 1) { productService.getProductsByIds(listOf(1L, 2L)) }

        // [TDD 검증 목표 3]: 결과가 올바르게 병합되었는가?
        assertEquals(2, result.size)
        assertEquals(1L, result[0].productId)
        assertEquals(150, result[0].totalOrderCount)
        assertEquals("에티오피아 예가체프 G1", result[0].productName)
        assertEquals("Bean Bliss", result[0].brand)
    }

    @Test
    @DisplayName("정렬 순서가 유지되어야 한다 (주문 수 내림차순)")
    fun `정렬 순서가 유지되어야 한다`() {
        // Given
        val period = 7
        val limit = 3

        val orderCounts = listOf(
            ProductOrderCount(productId = 1L, totalOrderCount = 150),
            ProductOrderCount(productId = 2L, totalOrderCount = 120),
            ProductOrderCount(productId = 3L, totalOrderCount = 100)
        )

        val productInfos = listOf(
            ProductBasicInfo(1L, "상품1", "브랜드1", "설명1"),
            ProductBasicInfo(2L, "상품2", "브랜드2", "설명2"),
            ProductBasicInfo(3L, "상품3", "브랜드3", "설명3")
        )

        every { orderService.getTopOrderedProducts(period, limit) } returns orderCounts
        every { productService.getProductsByIds(any()) } returns productInfos

        // When
        val result = useCase.getPopularProducts(period, limit)

        // Then
        // [TDD 검증 목표 4]: 주문 수 내림차순이 유지되는가?
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

        every { orderService.getTopOrderedProducts(period, limit) } returns emptyList()

        // When
        val result = useCase.getPopularProducts(period, limit)

        // Then
        // [TDD 검증 목표 6]: 빈 결과가 반환되는가?
        assertEquals(0, result.size)

        // [TDD 검증 목표 7]: ProductService는 호출되지 않아야 한다 (빈 목록이므로)
        verify(exactly = 1) { orderService.getTopOrderedProducts(period, limit) }
        verify(exactly = 0) { productService.getProductsByIds(any()) }
    }

    @Test
    @DisplayName("ProductService에서 일부 상품 정보가 누락되면 데이터 정합성 오류로 예외가 발생해야 한다")
    fun `ProductService에서 일부 상품 정보가 누락되면 데이터 정합성 오류로 예외가 발생해야 한다`() {
        // Given
        val period = 7
        val limit = 10

        val orderCounts = listOf(
            ProductOrderCount(productId = 1L, totalOrderCount = 150),
            ProductOrderCount(productId = 2L, totalOrderCount = 120),
            ProductOrderCount(productId = 3L, totalOrderCount = 100)
        )

        // ProductService에서 상품 2번 정보 누락 (데이터 정합성 문제)
        val productInfos = listOf(
            ProductBasicInfo(1L, "상품1", "브랜드1", "설명1"),
            ProductBasicInfo(3L, "상품3", "브랜드3", "설명3")
        )

        every { orderService.getTopOrderedProducts(period, limit) } returns orderCounts
        every { productService.getProductsByIds(listOf(1L, 2L, 3L)) } returns productInfos

        // When & Then
        // [TDD 검증 목표 8]: 데이터 정합성 오류 시 예외 발생
        val exception = assertThrows(IllegalStateException::class.java) {
            useCase.getPopularProducts(period, limit)
        }

        assertEquals("주문 수량 데이터와 상품 정보 간 불일치가 발생했습니다. 누락된 상품 ID: [2]", exception.message)
    }
}
