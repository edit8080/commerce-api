package com.beanbliss.domain.product.repository

import com.beanbliss.domain.product.dto.ProductOptionResponse
import com.beanbliss.domain.product.dto.ProductResponse
import java.time.LocalDateTime

/**
 * Fake ProductRepository 구현체 (테스트용)
 *
 * [책임]:
 * 1. 활성 옵션만 필터링 (is_active = true)
 * 2. 활성 옵션이 있는 상품만 반환
 * 3. 지정된 정렬 기준으로 정렬
 * 4. 페이징 처리
 */
class FakeProductRepository : ProductRepository {

    // 테스트 데이터 저장소
    private val products = mutableListOf<Product>()

    /**
     * 테스트 데이터 추가용 헬퍼 메서드
     */
    fun addProduct(product: Product) {
        products.add(product)
    }

    /**
     * 테스트 데이터 초기화
     */
    fun clear() {
        products.clear()
    }

    override fun findActiveProducts(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): List<ProductResponse> {
        // 1. 각 상품의 활성 옵션만 필터링
        val productsWithActiveOptions = products.map { product ->
            val activeOptions = product.options.filter { it.isActive }
            product.copy(options = activeOptions)
        }

        // 2. 활성 옵션이 있는 상품만 필터링
        val productsWithOptions = productsWithActiveOptions.filter { it.options.isNotEmpty() }

        // 3. 정렬
        val sorted = when (sortBy) {
            "created_at" -> {
                if (sortDirection == "DESC") {
                    productsWithOptions.sortedByDescending { it.createdAt }
                } else {
                    productsWithOptions.sortedBy { it.createdAt }
                }
            }
            "name" -> {
                if (sortDirection == "DESC") {
                    productsWithOptions.sortedByDescending { it.name }
                } else {
                    productsWithOptions.sortedBy { it.name }
                }
            }
            else -> productsWithOptions // 기본: 정렬 없음
        }

        // 4. 페이징 (1-based index)
        val startIndex = (page - 1) * size
        val endIndex = minOf(startIndex + size, sorted.size)

        if (startIndex >= sorted.size) {
            return emptyList()
        }

        val pagedProducts = sorted.subList(startIndex, endIndex)

        // 5. ProductResponse로 변환
        return pagedProducts.map { it.toResponse() }
    }

    override fun countActiveProducts(): Long {
        // 활성 옵션이 있는 상품 수
        val productsWithActiveOptions = products.map { product ->
            val activeOptions = product.options.filter { it.isActive }
            product.copy(options = activeOptions)
        }

        return productsWithActiveOptions.count { it.options.isNotEmpty() }.toLong()
    }

    override fun findByIdWithOptions(productId: Long): ProductResponse? {
        // 1. 상품 ID로 조회
        val product = products.firstOrNull { it.productId == productId } ?: return null

        // 2. 활성 옵션만 필터링
        val activeOptions = product.options.filter { it.isActive }

        // 3. 활성 옵션이 없으면 null 반환 (활성 옵션이 없는 상품은 조회 불가)
        if (activeOptions.isEmpty()) {
            return null
        }

        // 4. 옵션 정렬: 용량(weightGrams) 오름차순 → 분쇄 타입(grindType) 오름차순
        val sortedOptions = activeOptions.sortedWith(
            compareBy<ProductOption> { it.weightGrams }.thenBy { it.grindType }
        )

        // 5. ProductResponse로 변환
        return product.copy(options = sortedOptions).toResponse()
    }

    /**
     * 테스트용 내부 데이터 모델
     */
    data class Product(
        val productId: Long,
        val name: String,
        val description: String,
        val brand: String,
        val createdAt: LocalDateTime,
        val options: List<ProductOption>
    ) {
        fun toResponse(): ProductResponse {
            return ProductResponse(
                productId = productId,
                name = name,
                description = description,
                brand = brand,
                createdAt = createdAt,
                options = options.map { it.toResponse() }
            )
        }
    }

    /**
     * 테스트용 내부 옵션 모델
     */
    data class ProductOption(
        val optionId: Long,
        val optionCode: String,
        val origin: String,
        val grindType: String,
        val weightGrams: Int,
        val price: Int,
        val isActive: Boolean
    ) {
        fun toResponse(): ProductOptionResponse {
            return ProductOptionResponse(
                optionId = optionId,
                optionCode = optionCode,
                origin = origin,
                grindType = grindType,
                weightGrams = weightGrams,
                price = price,
                availableStock = 0 // Service에서 계산하여 채워질 예정
            )
        }
    }
}
