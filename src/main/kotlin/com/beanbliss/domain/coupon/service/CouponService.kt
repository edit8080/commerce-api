package com.beanbliss.domain.coupon.service

import com.beanbliss.domain.coupon.dto.CouponValidationResult
import com.beanbliss.domain.coupon.dto.CreateCouponRequest
import java.time.LocalDateTime

/**
 * [책임]: 쿠폰 비즈니스 로직의 계약 정의
 * Controller는 이 인터페이스에만 의존합니다 (DIP 준수)
 */
interface CouponService {
    /**
     * 쿠폰 정보 (Service DTO)
     */
    data class CouponInfo(
        val id: Long,
        val name: String,
        val discountType: String,
        val discountValue: Int,
        val minOrderAmount: Int,
        val maxDiscountAmount: Int,
        val totalQuantity: Int,
        val validFrom: LocalDateTime,
        val validUntil: LocalDateTime,
        val createdAt: LocalDateTime
    )

    /**
     * 쿠폰 생성
     *
     * [책임]:
     * - 비즈니스 규칙 검증 (정액 할인에 최대 할인 금액 설정 불가)
     * - CouponEntity 생성 및 저장
     *
     * @param request 쿠폰 생성 요청
     * @return 생성된 쿠폰 정보
     * @throws InvalidCouponException 비즈니스 규칙 위반 시
     */
    fun createCoupon(request: CreateCouponRequest): CouponInfo

    /**
     * 쿠폰 목록 조회 결과 (도메인 데이터)
     */
    data class CouponsResult(
        val coupons: List<CouponWithAvailability>,
        val totalCount: Long
    )

    /**
     * 쿠폰 정보 + 발급 가능 여부 (도메인 데이터)
     */
    data class CouponWithAvailability(
        val couponId: Long,
        val name: String,
        val discountType: String,
        val discountValue: Int,
        val minOrderAmount: Int,
        val maxDiscountAmount: Int,
        val remainingQuantity: Int,
        val totalQuantity: Int,
        val validFrom: LocalDateTime,
        val validUntil: LocalDateTime,
        val isIssuable: Boolean
    )

    /**
     * 쿠폰 목록 조회
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 쿠폰 목록 + 총 개수
     */
    fun getCoupons(page: Int, size: Int): CouponsResult

    /**
     * 유효한 쿠폰 조회
     *
     * @param couponId 쿠폰 ID
     * @return 쿠폰 정보
     * @throws ResourceNotFoundException 쿠폰을 찾을 수 없는 경우
     * @throws CouponNotStartedException 쿠폰 유효 기간이 시작되지 않은 경우
     * @throws CouponExpiredException 쿠폰 유효 기간이 만료된 경우
     */
    fun getValidCoupon(couponId: Long): CouponInfo

    /**
     * 쿠폰 유효성 검증 및 조회
     *
     * [비즈니스 규칙]:
     * - 사용자 쿠폰 존재 및 소유권 확인
     * - 쿠폰 상태 확인 (ISSUED)
     * - 쿠폰 유효 기간 확인
     *
     * @param userId 사용자 ID
     * @param userCouponId 사용자 쿠폰 ID
     * @return 검증된 쿠폰 정보
     * @throws UserCouponNotFoundException 쿠폰을 찾을 수 없거나 소유권이 없는 경우
     * @throws UserCouponAlreadyUsedException 이미 사용된 쿠폰인 경우
     * @throws UserCouponExpiredException 쿠폰이 만료된 경우
     */
    fun validateAndGetCoupon(userId: Long, userCouponId: Long): CouponValidationResult

    /**
     * 쿠폰 사용 처리
     *
     * [비즈니스 규칙]:
     * - 쿠폰 상태를 USED로 변경
     * - 사용 주문 ID 및 사용 시각 기록
     *
     * @param userCouponId 사용자 쿠폰 ID
     * @param orderId 주문 ID
     */
    fun markCouponAsUsed(userCouponId: Long, orderId: Long)

    /**
     * 쿠폰 할인 금액 계산
     *
     * [비즈니스 로직]:
     * - PERCENTAGE: (원가 * 할인율 / 100), 최대 할인 금액 제한 적용
     * - FIXED_AMOUNT: 할인 금액 그대로 반환
     *
     * @param coupon 쿠폰 정보
     * @param originalAmount 원래 주문 금액
     * @return 할인 금액
     */
    fun calculateDiscount(coupon: CouponInfo, originalAmount: Int): Int
}
