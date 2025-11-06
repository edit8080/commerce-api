package com.beanbliss.domain.product.service

import com.beanbliss.common.exception.ResourceNotFoundException
import com.beanbliss.domain.order.exception.ProductOptionInactiveException
import com.beanbliss.domain.product.dto.ProductBasicInfo
import com.beanbliss.domain.product.dto.ProductResponse
import com.beanbliss.domain.product.repository.ProductOptionRepository
import com.beanbliss.domain.product.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [책임]: 상품 비즈니스 로직 구현
 * - Repository 조회 결과 조율
 * - 응답 DTO 조립
 *
 * [SRP 준수]: Product 도메인만 담당 (Inventory 도메인은 InventoryService가 담당)
 */
@Service
@Transactional(readOnly = true)
class ProductService(
    private val productRepository: ProductRepository,
    private val productOptionRepository: ProductOptionRepository
) {

    fun getActiveProducts(
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

    fun countActiveProducts(): Long {
        // 활성 옵션이 있는 상품의 총 개수 반환
        return productRepository.countActiveProducts()
    }

    fun getProductWithOptions(productId: Long): ProductResponse {
        // 1. Repository에서 상품 상세 조회 (활성 옵션 포함, Repository에서 정렬됨)
        val product = productRepository.findByIdWithOptions(productId)
            ?: throw ResourceNotFoundException("상품 ID: $productId 의 상품을 찾을 수 없습니다.")

        // 2. 활성 옵션이 없는 경우 예외 처리
        if (product.options.isEmpty()) {
            throw ResourceNotFoundException("상품 ID: $productId 의 활성 옵션이 없습니다.")
        }

        // 3. 응답 반환 (재고는 0으로 초기화된 상태)
        return product
    }

    fun getProductsByIds(productIds: List<Long>): List<ProductBasicInfo> {
        // 빈 목록인 경우 조기 반환
        if (productIds.isEmpty()) {
            return emptyList()
        }

        // Repository를 통해 상품 기본 정보 조회
        return productRepository.findBasicInfoByIds(productIds)
    }

    /**
     * 상품 옵션들의 활성 여부 검증
     *
     * [비즈니스 규칙]:
     * - 모든 옵션이 존재하고 활성 상태(is_active = true)여야 합니다.
     * - 하나라도 비활성 상태이면 예외 발생
     *
     * @param optionIds 검증할 상품 옵션 ID 목록
     * @throws ProductOptionInactiveException 비활성화된 상품 옵션이 포함된 경우
     */
    fun validateProductOptionsActive(optionIds: List<Long>) {
        // 각 옵션의 활성 여부 검증
        optionIds.forEach { optionId ->
            val productOption = productOptionRepository.findActiveOptionWithProduct(optionId)
            if (productOption == null || !productOption.isActive) {
                throw ProductOptionInactiveException("비활성화된 상품 옵션이 포함되어 있습니다. (옵션 ID: $optionId)")
            }
        }
    }
}
