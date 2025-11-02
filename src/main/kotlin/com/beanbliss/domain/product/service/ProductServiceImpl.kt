package com.beanbliss.domain.product.service

import com.beanbliss.common.pagination.PageCalculator
import com.beanbliss.domain.inventory.repository.InventoryRepository
import com.beanbliss.domain.product.dto.PageableResponse
import com.beanbliss.domain.product.dto.ProductListResponse
import com.beanbliss.domain.product.dto.ProductResponse
import com.beanbliss.domain.product.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [책임]: 상품 비즈니스 로직 구현
 * - Repository 조회 결과 조율
 * - 가용 재고 계산 (InventoryRepository 사용)
 * - 응답 DTO 조립
 */
@Service
@Transactional(readOnly = true)
class ProductServiceImpl(
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository
) : ProductService {

    override fun getProducts(page: Int, size: Int): ProductListResponse {
        // 1. Repository에서 활성 옵션이 있는 상품 조회 (created_at DESC 정렬)
        val products = productRepository.findActiveProducts(
            page = page,
            size = size,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // 2. 각 옵션의 가용 재고 계산 (Batch 조회로 N+1 문제 해결)
        // 2-1. 모든 optionId 수집
        val allOptionIds = products.flatMap { it.options.map { option -> option.optionId } }

        // 2-2. 한 번의 쿼리로 모든 재고 조회
        val stockMap = inventoryRepository.calculateAvailableStockBatch(allOptionIds)

        // 2-3. Map 기반 매칭 (O(1) 시간 복잡도)
        val productsWithStock = products.map { product ->
            val optionsWithStock = product.options.map { option ->
                option.copy(availableStock = stockMap[option.optionId] ?: 0)
            }
            product.copy(options = optionsWithStock)
        }

        // 3. 페이징 정보 조립
        val totalElements = productRepository.countActiveProducts()
        val totalPages = PageCalculator.calculateTotalPages(totalElements, size)
        val pageable = PageableResponse(
            pageNumber = page,
            pageSize = size,
            totalElements = totalElements,
            totalPages = totalPages
        )

        // 4. 응답 조립
        return ProductListResponse(
            content = productsWithStock,
            pageable = pageable
        )
    }
}
