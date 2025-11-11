package com.beanbliss.domain.product.repository

/**
 * [책임]: 상품 옵션 영속성 계층의 '계약' 정의.
 * Service는 이 인터페이스에만 의존합니다. (DIP 준수)
 *
 * [주요 기능]:
 * - 활성 상태의 상품 옵션 조회
 */
interface ProductOptionRepository {
    /**
     * 상품 옵션 ID로 활성 상태의 옵션 조회 (PRODUCT와 JOIN)
     *
     * [조회 조건]:
     * - product_option.id = productOptionId
     * - product_option.is_active = true
     * - PRODUCT 정보 포함 (JOIN)
     *
     * @param productOptionId 상품 옵션 ID
     * @return 활성 상태의 상품 옵션 정보 (없으면 null)
     */
    fun findActiveOptionWithProduct(productOptionId: Long): ProductOptionDetail?

    /**
     * 여러 상품 옵션 ID로 활성 상태의 옵션 일괄 조회 (PRODUCT와 JOIN)
     *
     * [성능 최적화]:
     * - N+1 문제 방지: WHERE product_option.id IN (...) 사용
     * - 단일 쿼리로 모든 옵션 조회
     * - PRODUCT 정보 포함 (JOIN)
     *
     * [조회 조건]:
     * - product_option.id IN optionIds
     * - product_option.is_active = true
     *
     * @param optionIds 상품 옵션 ID 리스트
     * @return 활성 상태의 상품 옵션 정보 리스트 (존재하는 것만 반환)
     */
    fun findByIdsBatch(optionIds: List<Long>): List<ProductOptionDetail>
}

/**
 * 상품 옵션 상세 정보 (PRODUCT와 JOIN된 결과)
 */
data class ProductOptionDetail(
    val optionId: Long,
    val productId: Long,
    val productName: String,
    val optionCode: String,
    val origin: String,
    val grindType: String,
    val weightGrams: Int,
    val price: Int,
    val isActive: Boolean
)
