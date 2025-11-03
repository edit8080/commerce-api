package com.beanbliss.domain.product.repository

import com.beanbliss.domain.product.dto.ProductOptionResponse
import com.beanbliss.domain.product.dto.ProductResponse
import com.beanbliss.domain.product.entity.ProductEntity
import com.beanbliss.domain.product.entity.ProductOptionEntity
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * [책임]: 상품 In-memory 저장소 구현
 * - ConcurrentHashMap을 사용하여 Thread-safe 보장
 * - PRODUCT와 PRODUCT_OPTION을 함께 관리
 * - 활성 옵션(is_active=true)이 있는 상품만 조회
 *
 * [주의사항]:
 * - 애플리케이션 재시작 시 데이터 소실 (In-memory 특성)
 * - 실제 DB 사용 시 JPA 기반 구현체로 교체 필요
 */
@Repository
class ProductRepositoryImpl : ProductRepository {

    // Thread-safe한 In-memory 저장소
    private val products = ConcurrentHashMap<Long, ProductEntity>()
    private val productOptions = ConcurrentHashMap<Long, ProductOptionEntity>()

    // Inventory 정보 (product_option_id -> stock_quantity)
    // 실제로는 InventoryRepository에서 관리하지만, ProductOptionResponse에 availableStock이 필요하므로 임시 저장
    private val inventoryStock = ConcurrentHashMap<Long, Int>()

    override fun findActiveProducts(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): List<ProductResponse> {
        // 1. 활성 옵션이 있는 상품 ID 목록 추출
        val productIdsWithActiveOptions = productOptions.values
            .filter { it.isActive }
            .map { it.productId }
            .toSet()

        // 2. 해당 상품들 조회
        val activeProducts = products.values
            .filter { productIdsWithActiveOptions.contains(it.id) }

        // 3. 정렬 적용
        val sorted = when (sortBy) {
            "created_at" -> if (sortDirection == "DESC") {
                activeProducts.sortedByDescending { it.createdAt }
            } else {
                activeProducts.sortedBy { it.createdAt }
            }
            "name" -> if (sortDirection == "DESC") {
                activeProducts.sortedByDescending { it.name }
            } else {
                activeProducts.sortedBy { it.name }
            }
            else -> activeProducts.sortedBy { it.createdAt }
        }

        // 4. 페이징 적용
        val offset = (page - 1) * size
        val paged = sorted.drop(offset).take(size)

        // 5. ProductResponse로 변환 (옵션 포함)
        return paged.map { product ->
            val options = getActiveOptionsForProduct(product.id)
            product.toResponse(options)
        }
    }

    override fun countActiveProducts(): Long {
        // 활성 옵션이 있는 상품 ID 목록 추출
        val productIdsWithActiveOptions = productOptions.values
            .filter { it.isActive }
            .map { it.productId }
            .toSet()

        return productIdsWithActiveOptions.size.toLong()
    }

    override fun findByIdWithOptions(productId: Long): ProductResponse? {
        val product = products[productId] ?: return null

        val options = getActiveOptionsForProduct(productId)
        return product.toResponse(options)
    }

    /**
     * 상품 ID로 활성 옵션 목록 조회 (정렬 포함)
     */
    private fun getActiveOptionsForProduct(productId: Long): List<ProductOptionResponse> {
        return productOptions.values
            .filter { it.productId == productId && it.isActive }
            .sortedWith(compareBy({ it.weightGrams }, { it.grindType })) // 용량 → 분쇄 타입 순 정렬
            .map { option ->
                ProductOptionResponse(
                    optionId = option.id,
                    optionCode = option.optionCode,
                    origin = option.origin,
                    grindType = option.grindType,
                    weightGrams = option.weightGrams,
                    price = option.price,
                    availableStock = inventoryStock[option.id] ?: 0 // Inventory 조회
                )
            }
    }

    /**
     * 테스트용 헬퍼 메서드: 상품 추가
     */
    fun addProduct(entity: ProductEntity) {
        products[entity.id] = entity
    }

    /**
     * 테스트용 헬퍼 메서드: 상품 옵션 추가
     */
    fun addProductOption(entity: ProductOptionEntity) {
        productOptions[entity.id] = entity
    }

    /**
     * 테스트용 헬퍼 메서드: 재고 정보 추가
     */
    fun addInventory(productOptionId: Long, stock: Int) {
        inventoryStock[productOptionId] = stock
    }

    /**
     * 테스트용 헬퍼 메서드: 모든 데이터 삭제
     */
    fun clear() {
        products.clear()
        productOptions.clear()
        inventoryStock.clear()
    }
}
