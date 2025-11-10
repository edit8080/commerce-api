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
interface ProductOptionJpaRepository : JpaRepository<ProductOptionEntity, Long> {
    /**
     * 상품 ID와 활성 상태로 옵션 조회 (DB 레벨 정렬)
     */
    @Query("""
        SELECT po
        FROM ProductOptionEntity po
        WHERE po.productId = :productId AND po.isActive = true
        ORDER BY po.weightGrams ASC, po.grindType ASC
    """)
    fun findByProductIdAndIsActiveTrue(@Param("productId") productId: Long): List<ProductOptionEntity>

    /**
     * 상품 옵션 ID로 활성 상태의 옵션 조회 (PRODUCT와 INNER JOIN)
     * N+1 문제 방지를 위한 단일 쿼리
     *
     * @param productOptionId 상품 옵션 ID
     * @return Array<Any> = [ProductOptionEntity, ProductEntity] (없으면 null)
     */
    @Query("""
        SELECT po, p
        FROM ProductOptionEntity po
        INNER JOIN ProductEntity p ON po.productId = p.id
        WHERE po.id = :productOptionId AND po.isActive = true
    """)
    fun findActiveByIdWithProduct(@Param("productOptionId") productOptionId: Long): Array<Any>?
}

/**
 * [책임]: ProductOptionRepository 인터페이스 구현체
 * - ProductOptionJpaRepository를 활용하여 실제 DB 접근
 * - PRODUCT와 INNER JOIN하여 단일 쿼리로 조회
 */
@Repository
class ProductOptionRepositoryImpl(
    private val productOptionJpaRepository: ProductOptionJpaRepository
) : ProductOptionRepository {

    override fun findActiveOptionWithProduct(productOptionId: Long): ProductOptionDetail? {
        // PRODUCT_OPTION과 PRODUCT를 INNER JOIN하여 단일 쿼리로 조회
        val result = productOptionJpaRepository.findActiveByIdWithProduct(productOptionId)
            ?: return null

        // result = [ProductOptionEntity, ProductEntity]
        val optionEntity = result[0] as ProductOptionEntity
        val productEntity = result[1] as ProductEntity

        // ProductOptionDetail로 변환
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
