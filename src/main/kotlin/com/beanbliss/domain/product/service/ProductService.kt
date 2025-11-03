package com.beanbliss.domain.product.service

import com.beanbliss.domain.product.dto.ProductListResponse
import com.beanbliss.domain.product.dto.ProductResponse

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

    /**
     * 상품 상세 조회
     *
     * @param productId 상품 ID
     * @return 상품 상세 정보 (옵션 포함, 가용 재고 계산됨)
     * @throws ResourceNotFoundException 상품이 존재하지 않거나 활성 옵션이 없는 경우
     */
    fun getProductDetail(productId: Long): ProductResponse
}
