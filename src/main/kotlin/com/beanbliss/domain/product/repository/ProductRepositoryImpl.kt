package com.beanbliss.domain.product.repository

import com.beanbliss.domain.product.entity.ProductEntity
import com.beanbliss.domain.product.entity.ProductOptionEntity
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
}

/**
 * [책임]: ProductRepository 인터페이스 구현체
 * - ProductJpaRepository를 활용하여 실제 DB 접근
 * - 활성 옵션이 있는 상품만 조회
 */
@Repository
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
    private val productOptionJpaRepository: ProductOptionJpaRepository,
    private val inventoryJpaRepository: InventoryJpaRepository
) : ProductRepository {

    override fun findActiveProducts(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): List<ProductWithOptions> {
        // 1. 활성 옵션이 있는 상품 ID 목록 조회
        val productIdsWithActiveOptions = productJpaRepository.findProductIdsWithActiveOptions()

        // 2. 해당 상품들 조회 (페이징 적용)
        val products = if (productIdsWithActiveOptions.isEmpty()) {
            emptyList()
        } else {
            productJpaRepository.findAllById(productIdsWithActiveOptions)
                .sortedWith(compareBy { product ->
                    when (sortBy) {
                        "name" -> if (sortDirection == "DESC") -product.name.hashCode() else product.name.hashCode()
                        else -> if (sortDirection == "DESC") -product.createdAt.hashCode() else product.createdAt.hashCode()
                    }
                })
                .drop((page - 1) * size)
                .take(size)
        }

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
     * 상품 ID로 활성 옵션 목록 조회 (정렬 포함)
     */
    private fun getActiveOptionsForProduct(productId: Long): List<ProductOptionInfo> {
        val options = productOptionJpaRepository.findByProductIdAndIsActiveTrue(productId)
            .sortedWith(compareBy({ it.weightGrams }, { it.grindType }))

        // 재고 정보 조회 (Batch)
        val optionIds = options.map { it.id }
        val inventoryMap = if (optionIds.isNotEmpty()) {
            inventoryJpaRepository.findByProductOptionIdIn(optionIds)
                .associateBy({ it.productOptionId }, { it.stockQuantity })
        } else {
            emptyMap()
        }

        return options.map { option ->
            ProductOptionInfo(
                optionId = option.id,
                optionCode = option.optionCode,
                origin = option.origin,
                grindType = option.grindType,
                weightGrams = option.weightGrams,
                price = option.price.toInt(),
                availableStock = inventoryMap[option.id] ?: 0
            )
        }
    }
}

/**
 * [책임]: Spring Data JPA를 활용한 ProductOption 조회
 */
interface ProductOptionJpaRepository : JpaRepository<ProductOptionEntity, Long> {
    /**
     * 상품 ID와 활성 상태로 옵션 조회
     */
    fun findByProductIdAndIsActiveTrue(productId: Long): List<ProductOptionEntity>
}

/**
 * [책임]: Spring Data JPA를 활용한 Inventory 조회 (재고 정보용)
 */
interface InventoryJpaRepository : JpaRepository<com.beanbliss.domain.inventory.entity.InventoryEntity, Long> {
    /**
     * 여러 상품 옵션 ID로 재고 조회 (Batch)
     */
    fun findByProductOptionIdIn(productOptionIds: List<Long>): List<com.beanbliss.domain.inventory.entity.InventoryEntity>
}
