package com.beanbliss.domain.coupon.controller

import com.beanbliss.common.pagination.PageCalculator
import com.beanbliss.domain.coupon.dto.CouponListResponse
import com.beanbliss.domain.coupon.dto.CreateCouponRequest
import com.beanbliss.domain.coupon.dto.CreateCouponResponse
import com.beanbliss.domain.coupon.dto.IssueCouponRequest
import com.beanbliss.domain.coupon.dto.IssueCouponResponse
import com.beanbliss.domain.coupon.service.CouponIssueUseCase
import com.beanbliss.domain.coupon.service.CouponService
import com.beanbliss.domain.coupon.usecase.CreateCouponUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 쿠폰 관련 API 엔드포인트를 제공하는 Controller
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
@RequestMapping("/api/coupons")
@Tag(name = "쿠폰 관리", description = "쿠폰 생성, 조회, 발급 API")
class CouponController(
    private val couponService: CouponService,
    private val couponIssueUseCase: CouponIssueUseCase,
    private val createCouponUseCase: CreateCouponUseCase
) {

    /**
     * 선착순 쿠폰 생성 API
     *
     * [기능]:
     * - 쿠폰 마스터 데이터 생성 (COUPON 테이블)
     * - totalQuantity 개수만큼 COUPON_TICKET 사전 생성 (AVAILABLE 상태)
     *
     * @param request 쿠폰 생성 요청 정보
     * @return 201 Created - 생성된 쿠폰 정보
     * @throws InvalidCouponException 비즈니스 규칙 검증 실패 시
     *   - 할인값이 유효 범위를 벗어난 경우
     *   - 유효 기간이 잘못된 경우
     *   - 발급 수량이 유효 범위를 벗어난 경우
     *   - 정액 할인에 최대 할인 금액이 설정된 경우
     */
    @PostMapping
    @Operation(summary = "쿠폰 생성", description = "선착순 쿠폰 생성 및 초기화")
    fun createCoupon(
        @Valid @RequestBody request: CreateCouponRequest
    ): ResponseEntity<CreateCouponResponse> {
        // 1. UseCase 호출 (Service DTO 반환)
        val couponInfo = createCouponUseCase.createCoupon(request)

        // 2. Service DTO → Response DTO 변환 (Controller 책임)
        val response = CreateCouponResponse(
            couponId = couponInfo.id,
            name = couponInfo.name,
            discountType = com.beanbliss.domain.coupon.domain.DiscountType.valueOf(couponInfo.discountType),
            discountValue = couponInfo.discountValue,
            minOrderAmount = couponInfo.minOrderAmount,
            maxDiscountAmount = couponInfo.maxDiscountAmount,
            totalQuantity = couponInfo.totalQuantity,
            validFrom = couponInfo.validFrom,
            validUntil = couponInfo.validUntil,
            createdAt = couponInfo.createdAt
        )

        // 3. HTTP 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * 쿠폰 목록 조회 API
     *
     * @param page 페이지 번호 (1부터 시작, 기본값: 1)
     * @param size 페이지 크기 (기본값: 10, 최대: 100)
     * @return 쿠폰 목록과 페이징 정보
     * @throws InvalidParameterException 페이징 파라미터가 유효하지 않을 경우
     */
    @GetMapping
    @Operation(summary = "쿠폰 목록 조회", description = "사용 가능한 쿠폰 목록 조회 (페이지네이션)")
    fun getCoupons(
        @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
        @RequestParam(defaultValue = "1") page: Int,
        @Parameter(description = "페이지 크기 (1-100)", example = "10")
        @RequestParam(defaultValue = "10") size: Int
    ): CouponListResponse {
        // 1. 파라미터 검증 (PageCalculator에 위임)
        PageCalculator.validatePageParameters(page, size)

        // 2. Service 호출
        val result = couponService.getCoupons(page, size)

        // 3. 도메인 데이터 → DTO 변환 (Controller 책임)
        val totalPages = PageCalculator.calculateTotalPages(result.totalCount, size)
        val couponResponses = result.coupons.map { coupon ->
            com.beanbliss.domain.coupon.dto.CouponResponse(
                couponId = coupon.couponId,
                name = coupon.name,
                discountType = coupon.discountType,
                discountValue = coupon.discountValue,
                minOrderAmount = coupon.minOrderAmount,
                maxDiscountAmount = coupon.maxDiscountAmount,
                remainingQuantity = coupon.remainingQuantity,
                totalQuantity = coupon.totalQuantity,
                validFrom = coupon.validFrom,
                validUntil = coupon.validUntil,
                isIssuable = coupon.isIssuable
            )
        }

        val pageable = com.beanbliss.common.dto.PageableResponse(
            pageNumber = page,
            pageSize = size,
            totalElements = result.totalCount,
            totalPages = totalPages
        )

        // 4. 응답 반환
        return CouponListResponse(
            data = com.beanbliss.domain.coupon.dto.CouponListData(
                content = couponResponses,
                pageable = pageable
            )
        )
    }

    /**
     * 쿠폰 발급 API
     *
     * @param couponId 발급할 쿠폰 ID
     * @param request 발급 요청 (userId 포함)
     * @return 발급된 쿠폰 정보
     * @throws ResourceNotFoundException 쿠폰을 찾을 수 없는 경우
     * @throws IllegalStateException 유효기간 만료, 재고 부족, 중복 발급 등의 경우
     */
    @PostMapping("/{couponId}/issue")
    @Operation(summary = "쿠폰 발급", description = "사용자에게 쿠폰 발급")
    fun issueCoupon(
        @Parameter(description = "쿠폰 ID", example = "1")
        @PathVariable couponId: Long,
        @Valid @RequestBody request: IssueCouponRequest
    ): IssueCouponResponse {
        return couponIssueUseCase.issueCoupon(couponId, request.userId)
    }
}
