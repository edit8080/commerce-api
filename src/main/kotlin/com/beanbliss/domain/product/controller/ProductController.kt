package com.beanbliss.domain.product.controller

import com.beanbliss.common.dto.ApiResponse
import com.beanbliss.common.pagination.PageCalculator
import com.beanbliss.domain.product.dto.ProductListResponse
import com.beanbliss.domain.product.service.ProductService
import org.springframework.web.bind.annotation.*

/**
 * 상품 관련 API 엔드포인트를 제공하는 Controller
 *
 * [책임]:
 * 1. HTTP 요청을 받아 파라미터를 추출
 * 2. Service 계층에 비즈니스 로직 위임
 * 3. Service 결과를 HTTP 응답으로 변환
 *
 * [참고]:
 * - 페이징 파라미터 검증은 PageCalculator에서 수행
 */
@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService
) {

    /**
     * 상품 목록 조회 API
     *
     * @param page 페이지 번호 (1부터 시작, 기본값: 1)
     * @param size 페이지 크기 (기본값: 20, 최대: 100)
     * @return 상품 목록과 페이징 정보
     */
    @GetMapping
    fun getProducts(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<ProductListResponse> {
        // 파라미터 검증 (PageCalculator에 위임)
        PageCalculator.validatePageParameters(page, size)

        // Service 호출
        val result = productService.getProducts(page, size)

        // 응답 반환
        return ApiResponse(data = result)
    }
}
