package com.beanbliss.domain.product.repository

import com.beanbliss.domain.product.entity.ProductEntity
import com.beanbliss.domain.product.entity.ProductOptionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * [책임]: Spring Data JPA를 활용한 ProductOption 영속성 처리
 * Infrastructure Layer에 속하며, JPA 기술에 종속적
 */
interface ProductOptionWithProductJpaRepository : JpaRepository<ProductOptionEntity, Long> {
    /**
     * 상품 옵션 ID로 활성 상태의 옵션 조회 (PRODUCT와 JOIN)
     *
     * @param productOptionId 상품 옵션 ID
     * @param isActive 활성 상태 (true)
     * @return 활성 상태의 상품 옵션 (없으면 null)
     */
    @Query("""
        SELECT po, p
        FROM ProductOptionEntity po
        INNER JOIN ProductEntity p ON po.productId = p.id
        WHERE po.id = :productOptionId AND po.isActive = :isActive
    """)
    fun findByIdAndIsActiveWithProduct(
        @Param("productOptionId") productOptionId: Long,
        @Param("isActive") isActive: Boolean
    ): List<Array<Any>>?
}

/**
 * [책임]: ProductOptionRepository 인터페이스 구현체
 * - ProductOptionWithProductJpaRepository를 활용하여 실제 DB 접근
 * - 활성 상태의 상품 옵션만 조회
 */
@Repository
class ProductOptionRepositoryImpl(
    private val productOptionJpaRepository: ProductOptionJpaRepository,
    private val productJpaRepository: org.springframework.data.jpa.repository.JpaRepository<ProductEntity, Long>
) : ProductOptionRepository {

    override fun findActiveOptionWithProduct(productOptionId: Long): ProductOptionDetail? {
        // 1. 상품 옵션 조회 (is_active = true 조건)
        val optionEntity = productOptionJpaRepository.findById(productOptionId).orElse(null)
            ?: return null

        // 활성 상태가 아니면 null 반환
        if (!optionEntity.isActive) {
            return null
        }

        // 2. 상품 정보 조회 (JOIN)
        val productEntity = productJpaRepository.findById(optionEntity.productId).orElse(null)
            ?: return null

        // 3. ProductOptionDetail로 변환
        return optionEntity.toDetail(productEntity.name)
    }
}

/**
 * ProductOptionEntity 확장 함수: ProductOptionDetail로 변환
 */
private fun ProductOptionEntity.toDetail(productName: String): ProductOptionDetail {
    return ProductOptionDetail(
        optionId = this.id,
        productId = this.productId,
        productName = productName,
        optionCode = this.optionCode,
        origin = this.origin,
        grindType = this.grindType,
        weightGrams = this.weightGrams,
        price = this.price.toInt(),
        isActive = this.isActive
    )
}
