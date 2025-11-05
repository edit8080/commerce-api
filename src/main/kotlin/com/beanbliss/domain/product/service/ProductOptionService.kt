package com.beanbliss.domain.product.service

import com.beanbliss.domain.product.repository.ProductOptionDetail

/**
 * [책임]: 상품 옵션 관리 기능의 계약 정의
 */
interface ProductOptionService {
    /**
     * 상품 옵션 존재 여부 확인
     *
     * @param productOptionId 상품 옵션 ID
     * @return 존재하면 true, 없으면 false
     */
    fun existsById(productOptionId: Long): Boolean

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
    fun getActiveOptionWithProduct(productOptionId: Long): ProductOptionDetail
}
