package com.beanbliss.domain.product.service

import com.beanbliss.common.exception.ResourceNotFoundException
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

        // 2-3. Map 기반 매칭
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

    override fun getProductDetail(productId: Long): ProductResponse {
        // 1. Repository에서 상품 상세 조회 (활성 옵션 포함, Repository에서 정렬됨)
        val product = productRepository.findByIdWithOptions(productId)
            ?: throw ResourceNotFoundException("상품 ID: $productId 의 상품을 찾을 수 없습니다.")

        // 2. 활성 옵션이 없는 경우 예외 처리
        if (product.options.isEmpty()) {
            throw ResourceNotFoundException("상품 ID: $productId 의 활성 옵션이 없습니다.")
        }

        // 3. 각 옵션의 가용 재고 계산 (Batch 조회로 N+1 문제 해결)
        // 3-1. 모든 optionId 수집
        val optionIds = product.options.map { it.optionId }

        // 3-2. 한 번의 쿼리로 모든 재고 조회
        val stockMap = inventoryRepository.calculateAvailableStockBatch(optionIds)

        // 3-3. Map 기반 매칭 (Repository의 정렬 순서 유지)
        val optionsWithStock = product.options.map { option ->
            option.copy(availableStock = stockMap[option.optionId] ?: 0)
        }

        // 4. 응답 조립
        return product.copy(options = optionsWithStock)
    }
}
