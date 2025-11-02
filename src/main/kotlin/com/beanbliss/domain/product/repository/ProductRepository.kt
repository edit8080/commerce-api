package com.beanbliss.domain.product.repository

import com.beanbliss.domain.product.dto.ProductResponse

/**
 * [책임]: 상품 영속성 계층의 계약 정의
 * Service는 이 인터페이스에만 의존합니다 (DIP 준수)
 *
 * Repository는 도메인에서 자주 사용되는 쿼리를 캡슐화합니다.
 * "활성 상품 조회"는 비즈니스에서 매우 일반적인 패턴이므로 명시적인 메서드로 제공합니다.
 */
interface ProductRepository {
    /**
     * 활성 상품 목록 조회 (활성 옵션 포함)
     *
     * - PRODUCT_OPTION.is_active = true인 옵션만 포함
     * - 활성 옵션이 있는 상품만 반환 (활성 옵션이 없으면 상품 자체 제외)
     * - 지정된 정렬 기준으로 정렬
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @param sortBy 정렬 필드 (예: "created_at", "name", "price")
     * @param sortDirection 정렬 방향 ("ASC" 또는 "DESC")
     * @return 활성 상품 목록 (옵션 포함)
     */
    fun findActiveProducts(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): List<ProductResponse>

    /**
     * 활성 상품 총 개수 조회
     *
     * @return 활성 옵션이 있는 상품 수
     */
    fun countActiveProducts(): Long
}
