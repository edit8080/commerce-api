package com.beanbliss.domain.coupon.usecase

import com.beanbliss.domain.coupon.domain.DiscountType
import com.beanbliss.domain.coupon.dto.CreateCouponRequest
import com.beanbliss.domain.coupon.dto.CreateCouponResponse
import com.beanbliss.domain.coupon.entity.CouponEntity
import com.beanbliss.domain.coupon.entity.CouponTicketEntity
import com.beanbliss.domain.coupon.exception.InvalidCouponException
import com.beanbliss.domain.coupon.repository.CouponRepository
import com.beanbliss.domain.coupon.repository.CouponTicketRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 쿠폰 생성 UseCase
 *
 * [책임]: CouponService와 CouponTicketService를 조율하여 쿠폰 생성 트랜잭션을 완성합니다.
 * - 비즈니스 규칙 검증
 * - 쿠폰 엔티티 생성 및 저장
 * - 쿠폰 티켓 일괄 생성 및 저장
 */
@Component
class CreateCouponUseCase(
    private val couponRepository: CouponRepository,
    private val couponTicketRepository: CouponTicketRepository
) {

    @Transactional
    fun execute(request: CreateCouponRequest): CreateCouponResponse {
        // 1. 비즈니스 규칙 검증
        validateCouponRules(request)

        // 2. Coupon 엔티티 생성 및 저장
        val couponEntity = createCouponEntity(request)
        val savedCoupon = couponRepository.save(couponEntity)

        // 3. CouponTicket 엔티티 일괄 생성 및 저장
        val tickets = createCouponTickets(savedCoupon.id!!, request.totalQuantity)
        couponTicketRepository.saveAll(tickets)

        // 4. 응답 DTO 반환
        return CreateCouponResponse(
            couponId = savedCoupon.id,
            name = savedCoupon.name,
            discountType = DiscountType.valueOf(savedCoupon.discountType),
            discountValue = savedCoupon.discountValue,
            minOrderAmount = savedCoupon.minOrderAmount,
            maxDiscountAmount = savedCoupon.maxDiscountAmount,
            totalQuantity = savedCoupon.totalQuantity,
            validFrom = savedCoupon.validFrom,
            validUntil = savedCoupon.validUntil,
            createdAt = savedCoupon.createdAt
        )
    }

    /**
     * 쿠폰 생성 비즈니스 규칙 검증
     */
    private fun validateCouponRules(request: CreateCouponRequest) {
        // 1. 할인값 검증
        when (request.discountType) {
            DiscountType.PERCENTAGE -> {
                if (request.discountValue < 1 || request.discountValue > 100) {
                    throw InvalidCouponException("정률 할인의 할인값은 1 이상 100 이하여야 합니다. 현재값: ${request.discountValue}")
                }
            }
            DiscountType.FIXED_AMOUNT -> {
                if (request.discountValue < 1) {
                    throw InvalidCouponException("정액 할인의 할인값은 1 이상이어야 합니다. 현재값: ${request.discountValue}")
                }
            }
        }

        // 2. 유효 기간 검증
        if (!request.validFrom.isBefore(request.validUntil)) {
            throw InvalidCouponException("유효 시작 일시는 유효 종료 일시보다 이전이어야 합니다. validFrom: ${request.validFrom}, validUntil: ${request.validUntil}")
        }

        // 3. 정액 할인에 최대 할인 금액 설정 불가 검증
        if (request.discountType == DiscountType.FIXED_AMOUNT && request.maxDiscountAmount != null) {
            throw InvalidCouponException("정액 할인에는 최대 할인 금액을 설정할 수 없습니다.")
        }

        // 4. 발급 수량 검증
        if (request.totalQuantity < 1 || request.totalQuantity > 10000) {
            throw InvalidCouponException("발급 수량은 1 이상 10,000 이하여야 합니다. 현재값: ${request.totalQuantity}")
        }
    }

    /**
     * CouponEntity 생성
     */
    private fun createCouponEntity(request: CreateCouponRequest): CouponEntity {
        val now = LocalDateTime.now()
        return CouponEntity(
            id = null, // Auto-generated
            name = request.name,
            discountType = request.discountType.name,
            discountValue = request.discountValue,
            minOrderAmount = request.minOrderAmount,
            maxDiscountAmount = request.maxDiscountAmount ?: 0,
            totalQuantity = request.totalQuantity,
            validFrom = request.validFrom,
            validUntil = request.validUntil,
            createdAt = now,
            updatedAt = now
        )
    }

    /**
     * CouponTicket 엔티티 일괄 생성
     * totalQuantity 개수만큼 AVAILABLE 상태의 티켓을 생성합니다.
     */
    private fun createCouponTickets(couponId: Long, totalQuantity: Int): List<CouponTicketEntity> {
        return (1..totalQuantity).map {
            CouponTicketEntity(
                id = null, // Auto-generated
                couponId = couponId,
                status = "AVAILABLE",
                userId = null, // 발급 전이므로 null
                userCouponId = null, // 발급 전이므로 null
                issuedAt = null,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        }
    }
}
