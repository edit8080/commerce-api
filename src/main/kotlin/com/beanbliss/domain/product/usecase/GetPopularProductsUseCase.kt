package com.beanbliss.domain.product.usecase

import com.beanbliss.domain.order.service.OrderService
import com.beanbliss.domain.product.service.ProductService
import com.beanbliss.domain.product.service.ProductOptionService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 인기 상품 조회 UseCase
 *
 * [책임]:
 * - OrderService, ProductOptionService, ProductService를 오케스트레이션
 * - 주문 수량 데이터와 상품 정보 병합
 * - 상품 옵션별 주문 수량을 상품별로 집계
 * - 데이터 정합성 검증
 *
 * [설계 변경]:
 * - Repository 간 JOIN 제거
 * - UseCase 계층에서 여러 도메인 Service 조율
 * - 상품 옵션별 주문 수를 상품별로 집계
 *
 * [DIP 준수]:
 * - OrderService, ProductOptionService, ProductService에만 의존
 */
@Component
class GetPopularProductsUseCase(
    private val orderService: OrderService,
    private val productOptionService: ProductOptionService,
    private val productService: ProductService
) {

    /**
     * 인기 상품 정보 (도메인 데이터)
     */
    data class PopularProduct(
        val productId: Long,
        val productName: String,
        val brand: String,
        val totalOrderCount: Int,
        val description: String
    )

    /**
     * 지정된 기간 동안 가장 많이 주문된 상품 조회
     *
     * [오케스트레이션 흐름]:
     * 1. ORDER 도메인: 상품 옵션별 주문 수량 조회
     * 2. PRODUCT 도메인: 상품 옵션 정보 Batch 조회 (product_id 포함)
     * 3. 집계: 상품 옵션별 수량을 상품별로 집계
     * 4. PRODUCT 도메인: 상품 기본 정보 조회
     * 5. 데이터 조합
     *
     * @param period 조회 기간 (일 단위, 1~90일)
     * @param limit 조회할 상품 개수 (1~50개)
     * @return 인기 상품 목록 (주문 수 포함)
     * @throws IllegalStateException 데이터 정합성 오류 발생 시
     */
    @Transactional(readOnly = true)
    fun getPopularProducts(period: Int, limit: Int): List<PopularProduct> {
        // 1. ORDER 도메인: 상품 옵션별 주문 수량 조회
        // limit을 크게 조회 (옵션별이므로 상품별로 집계 시 개수가 줄어듦)
        val optionOrderCounts = orderService.getTopOrderedProductOptions(period, limit * 10)

        // 2. 빈 목록인 경우 조기 반환
        if (optionOrderCounts.isEmpty()) {
            return emptyList()
        }

        // 3. PRODUCT 도메인: 상품 옵션 정보 Batch 조회 (product_id 포함)
        val optionIds = optionOrderCounts.map { it.productOptionId }
        val productOptions = productOptionService.getOptionsBatch(optionIds)

        // 4. 상품별로 주문 수량 집계
        val productOrderCounts = optionOrderCounts
            .mapNotNull { orderCount ->
                productOptions[orderCount.productOptionId]?.let { option ->
                    option.productId to orderCount.totalOrderCount
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, counts) -> counts.sum() }
            .entries
            .sortedByDescending { it.value }
            .take(limit)

        // 5. 빈 목록인 경우 조기 반환
        if (productOrderCounts.isEmpty()) {
            return emptyList()
        }

        // 6. PRODUCT 도메인: 상품 기본 정보 조회
        val productIds = productOrderCounts.map { it.key }
        val productInfos = productService.getProductsByIds(productIds)

        // 7. 데이터 정합성 검증
        validateDataConsistency(productIds, productInfos.map { it.productId })

        // 8. 결과 병합 및 정렬 순서 유지
        val productInfoMap = productInfos.associateBy { it.productId }
        return productOrderCounts.mapNotNull { (productId, totalCount) ->
            productInfoMap[productId]?.let { productInfo ->
                PopularProduct(
                    productId = productInfo.productId,
                    productName = productInfo.productName,
                    brand = productInfo.brand,
                    totalOrderCount = totalCount,
                    description = productInfo.description
                )
            }
        }
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
