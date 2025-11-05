package com.beanbliss.domain.product.service

import com.beanbliss.domain.product.repository.ProductOptionRepository
import org.springframework.stereotype.Service

/**
 * [책임]: 상품 옵션 관리 기능 구현
 *
 * [주요 기능]:
 * - 상품 옵션 존재 여부 확인
 *
 * [DIP 준수]:
 * - ProductOptionRepository Interface에만 의존
 */
@Service
class ProductOptionServiceImpl(
    private val productOptionRepository: ProductOptionRepository
) : ProductOptionService {

    /**
     * 상품 옵션 존재 여부 확인
     *
     * [구현 방식]:
     * - ProductOptionRepository.findActiveOptionWithProduct()를 사용
     * - 활성 상태의 옵션이 있으면 true, 없으면 false
     *
     * @param productOptionId 상품 옵션 ID
     * @return 존재하면 true, 없으면 false
     */
    override fun existsById(productOptionId: Long): Boolean {
        // 활성 상태의 옵션 조회 후 null 체크
        return productOptionRepository.findActiveOptionWithProduct(productOptionId) != null
    }
}
