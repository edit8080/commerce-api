package com.beanbliss.domain.product.repository

/**
 * 테스트용 In-Memory ProductOptionRepository 구현체
 *
 * [특징]:
 * - 실제 DB 없이 메모리 기반으로 동작
 * - Repository 인터페이스의 계약을 준수
 * - 테스트 격리를 위해 각 테스트마다 새 인스턴스 생성
 */
class FakeProductOptionRepository : ProductOptionRepository {

    private val productOptions = mutableMapOf<Long, ProductOptionDetail>()

    override fun findActiveOptionWithProduct(productOptionId: Long): ProductOptionDetail? {
        return productOptions[productOptionId]?.takeIf { it.isActive }
    }

    // === Test Helper Methods ===

    /**
     * 테스트용: 상품 옵션 추가
     */
    fun addProductOption(option: ProductOptionDetail) {
        productOptions[option.optionId] = option
    }

    /**
     * 테스트용: 모든 데이터 삭제
     */
    fun clear() {
        productOptions.clear()
    }
}
