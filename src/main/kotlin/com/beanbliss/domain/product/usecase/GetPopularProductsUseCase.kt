package com.beanbliss.domain.product.usecase

import com.beanbliss.domain.order.service.OrderService
import com.beanbliss.domain.product.dto.PopularProductInfo
import com.beanbliss.domain.product.dto.PopularProductsResponse
import com.beanbliss.domain.product.service.ProductService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 인기 상품 조회 UseCase
 *
 * [책임]:
 * - OrderService와 ProductService를 오케스트레이션
 * - 주문 수량 데이터와 상품 정보 병합
 * - 데이터 정합성 검증
 *
 * [DIP 준수]:
 * - OrderService, ProductService 인터페이스에만 의존
 */
@Component
class GetPopularProductsUseCase(
    private val orderService: OrderService,
    private val productService: ProductService
) {

    /**
     * 지정된 기간 동안 가장 많이 주문된 상품 조회
     *
     * @param period 조회 기간 (일 단위, 1~90일)
     * @param limit 조회할 상품 개수 (1~50개)
     * @return 인기 상품 목록 (주문 수 포함)
     * @throws IllegalStateException 데이터 정합성 오류 발생 시
     */
    @Transactional(readOnly = true)
    fun getPopularProducts(period: Int, limit: Int): PopularProductsResponse {
        // 1. OrderService를 통해 활성 상품 주문 수량 조회
        val orderCounts = orderService.getTopOrderedProducts(period, limit)

        // 2. 빈 목록인 경우 조기 반환
        if (orderCounts.isEmpty()) {
            return PopularProductsResponse(products = emptyList())
        }

        // 3. ProductService를 통해 상품 기본 정보 조회
        val productIds = orderCounts.map { it.productId }
        val productInfos = productService.getProductsByIds(productIds)

        // 4. 데이터 정합성 검증
        validateDataConsistency(productIds, productInfos.map { it.productId })

        // 5. 결과 병합 및 정렬 순서 유지
        val productInfoMap = productInfos.associateBy { it.productId }
        val products = orderCounts.mapNotNull { orderCount ->
            productInfoMap[orderCount.productId]?.let { productInfo ->
                PopularProductInfo(
                    productId = productInfo.productId,
                    productName = productInfo.productName,
                    brand = productInfo.brand,
                    totalOrderCount = orderCount.totalOrderCount,
                    description = productInfo.description
                )
            }
        }

        return PopularProductsResponse(products = products)
    }

    /**
     * 데이터 정합성 검증
     *
     * OrderService에서 반환한 productId와 ProductService에서 반환한 productId가 일치하는지 검증
     */
    private fun validateDataConsistency(expectedProductIds: List<Long>, actualProductIds: List<Long>) {
        val missingProductIds = expectedProductIds - actualProductIds.toSet()
        if (missingProductIds.isNotEmpty()) {
            throw IllegalStateException("주문 수량 데이터와 상품 정보 간 불일치가 발생했습니다. 누락된 상품 ID: $missingProductIds")
        }
    }
}
