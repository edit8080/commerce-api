package com.beanbliss.domain.product.service

import com.beanbliss.domain.product.dto.ProductBasicInfo
import com.beanbliss.domain.product.dto.ProductListResponse
import com.beanbliss.domain.product.dto.ProductResponse

/**
 * [책임]: 상품 비즈니스 로직의 계약 정의
 * UseCase와 Controller는 이 인터페이스에만 의존합니다 (DIP 준수)
 *
 * [SRP 준수]: Product 도메인만 담당 (Inventory 도메인은 InventoryService가 담당)
 */
interface ProductService {
    /**
     * 활성 상품 목록 조회 (옵션 포함, 재고 제외)
     *
     * [책임]: 상품 도메인 데이터만 조회 (재고 정보는 InventoryService에서 조회)
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @param sortBy 정렬 기준 필드 (예: "created_at")
     * @param sortDirection 정렬 방향 ("ASC" 또는 "DESC")
     * @return 상품 목록 (옵션 포함, availableStock은 0으로 초기화됨)
     */
    fun getActiveProducts(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): List<ProductResponse>

    /**
     * 활성 상품 총 개수 조회
     *
     * [책임]: 페이징 계산을 위한 전체 상품 개수 반환
     *
     * @return 활성 옵션이 있는 상품의 총 개수
     */
    fun countActiveProducts(): Long

    /**
     * 상품 상세 조회
     *
     * @param productId 상품 ID
     * @return 상품 상세 정보 (옵션 포함, 가용 재고 계산됨)
     * @throws ResourceNotFoundException 상품이 존재하지 않거나 활성 옵션이 없는 경우
     */
    fun getProductDetail(productId: Long): ProductResponse

    /**
     * 여러 상품의 기본 정보 조회
     *
     * [목적]: 인기 상품 목록 조회 시 상품 기본 정보만 필요한 경우 사용
     *
     * @param productIds 조회할 상품 ID 목록
     * @return 상품 기본 정보 목록 (id, name, brand, description)
     */
    fun getProductsByIds(productIds: List<Long>): List<ProductBasicInfo>
}
