package com.beanbliss.domain.coupon.usecase

import com.beanbliss.domain.coupon.dto.CreateCouponRequest
import com.beanbliss.domain.coupon.service.CouponService
import com.beanbliss.domain.coupon.service.CouponTicketService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 쿠폰 생성 UseCase
 *
 * [책임]: CouponService와 CouponTicketService를 조율하여 쿠폰 생성 트랜잭션을 완성합니다.
 * - Service 계층의 비즈니스 로직을 조율
 * - 단일 트랜잭션 내에서 쿠폰과 티켓 생성을 원자적으로 처리
 */
@Component
class CreateCouponUseCase(
    private val couponService: CouponService,
    private val couponTicketService: CouponTicketService
) {

    @Transactional
    fun createCoupon(request: CreateCouponRequest): CouponService.CouponInfo {
        // 1. CouponService를 통한 쿠폰 생성 (비즈니스 규칙 검증 포함) (Service DTO 반환)
        val savedCoupon = couponService.createCoupon(request)

        // 2. CouponTicketService를 통한 티켓 일괄 생성
        couponTicketService.createTickets(savedCoupon.id, request.totalQuantity)

        // 3. Service DTO 반환
        return savedCoupon
    }
}
