package com.beanbliss.domain.product.service

import com.beanbliss.common.exception.ResourceNotFoundException
import com.beanbliss.domain.inventory.service.InventoryService
import com.beanbliss.domain.product.dto.ProductResponse
import com.beanbliss.domain.product.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [책임]: 상품 비즈니스 로직 구현
 * - Repository 조회 결과 조율
 * - 상품 도메인 데이터만 처리
 * - 상품 상세 조회 시에는 InventoryService 협력 (재고 정보 포함)
 *
 * [SRP 준수]:
 * - Product 도메인 주 책임
 * - Inventory 도메인은 InventoryService가 담당
 */
@Service
@Transactional(readOnly = true)
class ProductServiceImpl(
    private val productRepository: ProductRepository,
    private val inventoryService: InventoryService
) : ProductService {

    override fun getActiveProducts(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): List<ProductResponse> {
        // Repository에서 활성 옵션이 있는 상품 조회 (정렬 조건 적용)
        // availableStock은 0으로 초기화된 상태로 반환 (UseCase에서 채움)
        return productRepository.findActiveProducts(
            page = page,
            size = size,
            sortBy = sortBy,
            sortDirection = sortDirection
        )
    }

    override fun countActiveProducts(): Long {
        // 활성 옵션이 있는 상품의 총 개수 반환
        return productRepository.countActiveProducts()
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

        // 3-2. InventoryService를 통해 한 번의 쿼리로 모든 재고 조회
        val stockMap = inventoryService.calculateAvailableStockBatch(optionIds)

        // 3-3. Map 기반 매칭 (Repository의 정렬 순서 유지)
        val optionsWithStock = product.options.map { option ->
            option.copy(availableStock = stockMap[option.optionId] ?: 0)
        }

        // 4. 응답 조립
        return product.copy(options = optionsWithStock)
    }

    override fun getProductsByIds(productIds: List<Long>): List<com.beanbliss.domain.product.dto.ProductBasicInfo> {
        // 빈 목록인 경우 조기 반환
        if (productIds.isEmpty()) {
            return emptyList()
        }

        // Repository를 통해 상품 기본 정보 조회
        return productRepository.findBasicInfoByIds(productIds)
    }
}
