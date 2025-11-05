package com.beanbliss.domain.product.controller

import com.beanbliss.common.dto.ApiResponse
import com.beanbliss.common.pagination.PageCalculator
import com.beanbliss.domain.product.dto.PopularProductsResponse
import com.beanbliss.domain.product.dto.ProductListResponse
import com.beanbliss.domain.product.dto.ProductResponse
import com.beanbliss.domain.product.service.ProductService
import com.beanbliss.domain.product.usecase.GetPopularProductsUseCase
import com.beanbliss.domain.product.usecase.GetProductDetailUseCase
import com.beanbliss.domain.product.usecase.GetProductsUseCase
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

/**
 * 상품 관련 API 엔드포인트를 제공하는 Controller
 *
 * [책임]:
 * 1. HTTP 요청을 받아 파라미터를 추출
 * 2. UseCase 계층에 비즈니스 로직 위임
 * 3. 결과를 HTTP 응답으로 변환
 *
 * [참고]:
 * - 페이징 파라미터 검증은 PageCalculator에서 수행
 * - 인기 상품 파라미터 검증은 Jakarta Validator 사용
 * - 상품 목록 조회는 GetProductsUseCase 사용 (멀티 도메인 오케스트레이션)
 * - 상품 상세 조회는 GetProductDetailUseCase 사용 (멀티 도메인 오케스트레이션)
 */
@Validated
@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService,
    private val getProductsUseCase: GetProductsUseCase,
    private val getProductDetailUseCase: GetProductDetailUseCase,
    private val getPopularProductsUseCase: GetPopularProductsUseCase
) {

    /**
     * 상품 목록 조회 API
     *
     * @param page 페이지 번호 (1부터 시작, 기본값: 1)
     * @param size 페이지 크기 (기본값: 10, 최대: 100)
     * @return 상품 목록과 페이징 정보
     */
    @GetMapping
    fun getProducts(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ApiResponse<ProductListResponse> {
        // 파라미터 검증 (PageCalculator에 위임)
        PageCalculator.validatePageParameters(page, size)

        // UseCase 호출 (ProductService + InventoryService 오케스트레이션)
        val result = getProductsUseCase.getProducts(page, size)

        // 응답 반환
        return ApiResponse(data = result)
    }

    /**
     * 상품 상세 조회 API
     *
     * @param productId 상품 ID
     * @return 상품 상세 정보 (옵션 포함, 가용 재고 계산됨)
     * @throws ResourceNotFoundException 상품이 존재하지 않거나 활성 옵션이 없는 경우
     */
    @GetMapping("/{productId}")
    fun getProductDetail(
        @PathVariable productId: Long
    ): ApiResponse<ProductResponse> {
        // UseCase 호출 (ProductService + InventoryService 오케스트레이션)
        val result = getProductDetailUseCase.getProductDetail(productId)

        // 응답 반환
        return ApiResponse(data = result)
    }

    /**
     * 인기 상품 목록 조회 API
     *
     * @param period 조회 기간 (일 단위, 1~90일, 기본값: 7)
     * @param limit 조회할 상품 개수 (1~50개, 기본값: 10)
     * @return 인기 상품 목록 (주문 수 기준 내림차순)
     */
    @GetMapping("/popular")
    fun getPopularProducts(
        @RequestParam(defaultValue = "7") @Min(value = 1, message = "period는 1 이상 90 이하여야 합니다.") @Max(value = 90, message = "period는 1 이상 90 이하여야 합니다.") period: Int,
        @RequestParam(defaultValue = "10") @Min(value = 1, message = "limit는 1 이상 50 이하여야 합니다.") @Max(value = 50, message = "limit는 1 이상 50 이하여야 합니다.") limit: Int
    ): ApiResponse<PopularProductsResponse> {
        // UseCase 호출
        val result = getPopularProductsUseCase.getPopularProducts(period, limit)

        // 응답 반환
        return ApiResponse(data = result)
    }
}
