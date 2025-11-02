package com.beanbliss.domain.product.service

import com.beanbliss.domain.product.dto.ProductListResponse

/**
 * [책임]: 상품 비즈니스 로직의 계약 정의
 * Controller는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface ProductService {
    /**
     * 상품 목록 조회 (페이징)
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 상품 목록 응답 (상품, 옵션, 페이징 정보 포함)
     */
    fun getProducts(page: Int, size: Int): ProductListResponse
}
