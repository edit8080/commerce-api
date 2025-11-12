package com.beanbliss.domain.product.repository

import com.beanbliss.common.util.SortUtils
import com.beanbliss.domain.product.entity.ProductEntity
import com.beanbliss.domain.product.entity.ProductOptionEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
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
     * 활성 옵션이 있는 상품 목록 조회 (Fetch Join + 페이징)
     *
     * [성능 최적화]:
     * - LEFT JOIN FETCH로 Product와 ProductOption을 한 번의 쿼리로 조회
     * - Pagination과 Fetch Join을 함께 사용
     * - Hibernate가 메모리에서 페이징 수행 (경고 발생 가능하지만 N+1보다 효율적)
     */
    @Query("""
        SELECT p
        FROM ProductEntity p
        LEFT JOIN FETCH p.productOptions po
        WHERE po.isActive = true
    """)
    fun findActiveProductsWithPagination(pageable: Pageable): Page<ProductEntity>

    /**
     * 특정 상품 ID들의 상품과 활성 옵션을 Fetch Join으로 조회
     *
     * [성능 최적화]:
     * - LEFT JOIN FETCH를 사용하여 한 번의 쿼리로 Product와 ProductOption 조회
     * - N+1 문제 완전 해결
     *
     * @param productIds 상품 ID 목록
     * @return 상품과 옵션이 포함된 엔티티 목록
     */
    @Query("""
        SELECT DISTINCT p
        FROM ProductEntity p
        LEFT JOIN FETCH p.productOptions po
        WHERE p.id IN :productIds AND (po.isActive = true OR po.isActive IS NULL)
        ORDER BY p.id ASC
    """)
    fun findByIdsWithActiveOptions(@Param("productIds") productIds: List<Long>): List<ProductEntity>
}

/**
 * [책임]: ProductRepository 인터페이스 구현체
 * - ProductJpaRepository를 활용하여 실제 DB 접근
 * - Fetch Join으로 N+1 문제 해결
 * - 활성 옵션이 있는 상품만 조회
 * - 재고 정보는 조회하지 않음 (도메인 경계 준수, Service 계층에서 처리)
 */
@Repository
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository
) : ProductRepository {

    override fun findActiveProducts(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): List<ProductWithOptions> {
        // 1. Create Sort object using SortUtils
        val sort = SortUtils.createSort(sortBy, sortDirection)

        // 2. Fetch Join + Pagination을 한 번에 수행
        val pageRequest = PageRequest.of(page - 1, size, sort)
        val products = productJpaRepository.findActiveProductsWithPagination(pageRequest).content

        // 3. 빈 목록인 경우 조기 반환
        if (products.isEmpty()) {
            return emptyList()
        }

        // 4. ProductWithOptions로 변환
        return products.map { product ->
            // Entity의 연관관계를 통해 이미 로드된 옵션들 사용 (Fetch Join으로 로드됨)
            val options = product.productOptions
                .filter { it.isActive }
                .sortedWith(compareBy({ it.weightGrams }, { it.grindType }))
                .map { option ->
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
        // Fetch Join으로 Product와 ProductOption을 한 번에 조회
        val productsWithOptions = productJpaRepository.findByIdsWithActiveOptions(listOf(productId))

        if (productsWithOptions.isEmpty()) {
            return null
        }

        val product = productsWithOptions[0]

        // 활성 옵션만 필터링하고 정렬
        val options = product.productOptions
            .filter { it.isActive }
            .sortedWith(compareBy({ it.weightGrams }, { it.grindType }))
            .map { option ->
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
}
