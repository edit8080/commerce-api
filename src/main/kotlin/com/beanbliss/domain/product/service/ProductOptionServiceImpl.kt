package com.beanbliss.domain.product.service

import com.beanbliss.domain.product.repository.ProductOptionDetail
import com.beanbliss.domain.product.repository.ProductOptionRepository
import com.beanbliss.common.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [책임]: 상품 옵션 관리 기능 구현
 *
 * [주요 기능]:
 * - 상품 옵션 존재 여부 확인
 * - 활성 상품 옵션 조회
 *
 * [DIP 준수]:
 * - ProductOptionRepository Interface에만 의존
 *
 * [트랜잭션]:
 * - 읽기 전용 트랜잭션 사용
 */
@Service
@Transactional(readOnly = true)
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

    /**
     * 활성 상태의 상품 옵션 조회 (PRODUCT와 JOIN)
     *
     * [비즈니스 규칙]:
     * - 활성 옵션만 조회 가능
     * - 비활성 또는 존재하지 않는 옵션은 ResourceNotFoundException 발생
     *
     * @param productOptionId 상품 옵션 ID
     * @return 활성 상태의 상품 옵션 상세 정보
     * @throws ResourceNotFoundException 상품 옵션이 없거나 비활성 상태인 경우
     */
    override fun getActiveOptionWithProduct(productOptionId: Long): ProductOptionDetail {
        return productOptionRepository.findActiveOptionWithProduct(productOptionId)
            ?: throw ResourceNotFoundException("상품 옵션 ID: ${productOptionId}를 찾을 수 없습니다.")
    }
}
