package com.beanbliss.domain.product.repository

import com.beanbliss.common.util.SortUtils
import com.beanbliss.domain.product.entity.ProductEntity
import com.beanbliss.domain.product.entity.ProductOptionEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * [책임]: Spring Data JPA를 활용한 Product 영속성 처리
 * Infrastructure Layer에 속하며, JPA 기술에 종속적
 */
interface ProductJpaRepository : JpaRepository<ProductEntity, Long> {
    /**
     * 활성 옵션이 있는 상품 ID 목록 조회
     */
    @Query("""
        SELECT DISTINCT p.id
        FROM ProductEntity p
        INNER JOIN ProductOptionEntity po ON po.productId = p.id
        WHERE po.isActive = true
    """)
    fun findProductIdsWithActiveOptions(): List<Long>

    /**
     * 활성 옵션이 있는 상품 수 조회
     */
    @Query("""
        SELECT COUNT(DISTINCT p.id)
        FROM ProductEntity p
        INNER JOIN ProductOptionEntity po ON po.productId = p.id
        WHERE po.isActive = true
    """)
    fun countProductsWithActiveOptions(): Long

    /**
     * 활성 옵션이 있는 상품 목록 조회 (DB 레벨 정렬 및 페이징)
     */
    @Query("""
        SELECT DISTINCT p
        FROM ProductEntity p
        INNER JOIN ProductOptionEntity po ON po.productId = p.id
        WHERE po.isActive = true
    """)
    fun findActiveProductsWithPagination(pageable: Pageable): Page<ProductEntity>
}

/**
 * [책임]: ProductRepository 인터페이스 구현체
 * - ProductJpaRepository를 활용하여 실제 DB 접근
 * - 활성 옵션이 있는 상품만 조회
 * - 재고 정보는 조회하지 않음 (도메인 경계 준수, Service 계층에서 처리)
 */
@Repository
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
    private val productOptionJpaRepository: ProductOptionJpaRepository
) : ProductRepository {

    override fun findActiveProducts(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): List<ProductWithOptions> {
        // 1. Create Sort object using SortUtils
        val sort = SortUtils.createSort(sortBy, sortDirection)

        // 2. DB 레벨에서 정렬 및 페이징 적용하여 상품 조회
        val pageRequest = org.springframework.data.domain.PageRequest.of(page - 1, size, sort)
        val products = productJpaRepository.findActiveProductsWithPagination(pageRequest).content

        // 3. ProductWithOptions로 변환 (옵션 포함)
        return products.map { product ->
            val options = getActiveOptionsForProduct(product.id)
            ProductWithOptions(
                productId = product.id,
                name = product.name,
                description = product.description,
                brand = product.brand,
                createdAt = product.createdAt,
                options = options
            )
        }
    }

    override fun countActiveProducts(): Long {
        return productJpaRepository.countProductsWithActiveOptions()
    }

    override fun findByIdWithOptions(productId: Long): ProductWithOptions? {
        val product = productJpaRepository.findById(productId).orElse(null) ?: return null

        val options = getActiveOptionsForProduct(productId)
        return ProductWithOptions(
            productId = product.id,
            name = product.name,
            description = product.description,
            brand = product.brand,
            createdAt = product.createdAt,
            options = options
        )
    }

    override fun findBasicInfoByIds(productIds: List<Long>): List<ProductBasicInfo> {
        if (productIds.isEmpty()) {
            return emptyList()
        }

        return productJpaRepository.findAllById(productIds).map { product ->
            ProductBasicInfo(
                productId = product.id,
                productName = product.name,
                brand = product.brand,
                description = product.description
            )
        }
    }

    /**
     * 상품 ID로 활성 옵션 목록 조회 (DB 레벨 정렬)
     *
     * [설계 변경]:
     * - 재고 정보는 조회하지 않음 (availableStock = null)
     * - Service 계층에서 InventoryRepository를 사용하여 별도 조회
     * - 도메인 경계 준수: Product Repository는 Inventory에 의존하지 않음
     * - 정렬은 DB 레벨에서 수행 (메모리 정렬 제거)
     */
    private fun getActiveOptionsForProduct(productId: Long): List<ProductOptionInfo> {
        val options = productOptionJpaRepository.findByProductIdAndIsActiveTrue(productId)

        return options.map { option ->
            ProductOptionInfo(
                optionId = option.id,
                optionCode = option.optionCode,
                origin = option.origin,
                grindType = option.grindType,
                weightGrams = option.weightGrams,
                price = option.price.toInt(),
                availableStock = null  // Repository 계층에서는 null 반환
            )
        }
    }
}
