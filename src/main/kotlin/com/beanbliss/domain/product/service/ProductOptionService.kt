package com.beanbliss.domain.product.service

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
}
