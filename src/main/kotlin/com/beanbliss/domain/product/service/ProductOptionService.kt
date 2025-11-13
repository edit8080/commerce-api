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
class ProductOptionService(
    private val productOptionRepository: ProductOptionRepository
) {

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
    fun existsById(productOptionId: Long): Boolean {
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
    fun getActiveOptionWithProduct(productOptionId: Long): ProductOptionDetail {
        return productOptionRepository.findActiveOptionWithProduct(productOptionId)
            ?: throw ResourceNotFoundException("상품 옵션 ID: ${productOptionId}를 찾을 수 없습니다.")
    }

    /**
     * 여러 상품 옵션 ID로 일괄 조회 (Batch 조회, PRODUCT와 JOIN)
     *
     * [성능 최적화]:
     * - N+1 문제 방지: 단일 쿼리로 모든 옵션 조회
     * - WHERE IN 절 사용
     *
     * [사용처]:
     * - UseCase 계층에서 여러 도메인 데이터 조합 시 사용
     * - 장바구니, 주문, 재고 등에서 상품 옵션 정보가 필요할 때
     *
     * @param optionIds 상품 옵션 ID 리스트
     * @return Map<옵션ID, 상품 옵션 상세 정보> (존재하는 것만 반환)
     */
    fun getOptionsBatch(optionIds: List<Long>): Map<Long, ProductOptionDetail> {
        if (optionIds.isEmpty()) {
            return emptyMap()
        }

        return productOptionRepository.findByIdsBatch(optionIds)
            .associateBy { it.optionId }
    }
}
