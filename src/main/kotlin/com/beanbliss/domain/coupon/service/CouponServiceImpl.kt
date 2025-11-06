package com.beanbliss.domain.coupon.service

import com.beanbliss.common.exception.ResourceNotFoundException
import com.beanbliss.domain.coupon.domain.DiscountType
import com.beanbliss.domain.coupon.dto.CouponValidationResult
import com.beanbliss.domain.coupon.dto.CreateCouponRequest
import com.beanbliss.domain.coupon.entity.CouponEntity
import com.beanbliss.domain.coupon.enums.UserCouponStatus
import com.beanbliss.domain.coupon.exception.CouponExpiredException
import com.beanbliss.domain.coupon.exception.CouponNotStartedException
import com.beanbliss.domain.coupon.exception.InvalidCouponException
import com.beanbliss.domain.coupon.repository.CouponRepository
import com.beanbliss.domain.coupon.repository.CouponWithQuantity
import com.beanbliss.domain.coupon.repository.UserCouponRepository
import com.beanbliss.domain.order.exception.UserCouponAlreadyUsedException
import com.beanbliss.domain.order.exception.UserCouponExpiredException
import com.beanbliss.domain.order.exception.UserCouponNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.min

/**
 * [책임]: 쿠폰 비즈니스 로직 구현
 * - 쿠폰 생성 및 비즈니스 규칙 검증
 * - 쿠폰 목록 조회
 * - 쿠폰 유효성 검증 및 사용 처리
 * - Repository 호출
 * - DTO 변환
 */
@Service
@Transactional(readOnly = true)
class CouponServiceImpl(
    private val couponRepository: CouponRepository,
    private val userCouponRepository: UserCouponRepository
) : CouponService {

    @Transactional
    override fun createCoupon(request: CreateCouponRequest): CouponService.CouponInfo {
        // 1. 비즈니스 규칙 검증
        validateMaxDiscountAmountRule(request)

        // 2. CouponEntity 생성
        val couponEntity = createCouponEntity(request)

        // 3. Repository에 저장
        val savedEntity = couponRepository.save(couponEntity)

        // 4. Entity → Service DTO 변환
        return CouponService.CouponInfo(
            id = savedEntity.id!!,
            name = savedEntity.name,
            discountType = savedEntity.discountType,
            discountValue = savedEntity.discountValue,
            minOrderAmount = savedEntity.minOrderAmount,
            maxDiscountAmount = savedEntity.maxDiscountAmount,
            totalQuantity = savedEntity.totalQuantity,
            validFrom = savedEntity.validFrom,
            validUntil = savedEntity.validUntil,
            createdAt = savedEntity.createdAt
        )
    }

    /**
     * 최대 할인 금액 규칙 검증
     *
     * [책임]:
     * - 할인 타입에 따른 최대 할인 금액 설정 가능 여부 검증
     * - 정액 할인(FIXED_AMOUNT)에는 최대 할인 금액을 설정할 수 없음
     * - 정률 할인(PERCENTAGE)에만 최대 할인 금액 설정 가능
     */
    private fun validateMaxDiscountAmountRule(request: CreateCouponRequest) {
        // 정액 할인에 최대 할인 금액 설정 불가 검증
        if (request.discountType == DiscountType.FIXED_AMOUNT && request.maxDiscountAmount != null) {
            throw InvalidCouponException("정액 할인에는 최대 할인 금액을 설정할 수 없습니다.")
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

    override fun getCoupons(page: Int, size: Int): CouponService.CouponsResult {
        // 1. Repository에서 쿠폰 목록 조회 (정렬: created_at DESC)
        val coupons = couponRepository.findAllCoupons(
            page = page,
            size = size,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // 2. Repository에서 전체 쿠폰 개수 조회
        val totalCount = couponRepository.countAllCoupons()

        // 3. CouponWithQuantity -> 도메인 데이터로 변환 및 isIssuable 계산
        val now = LocalDateTime.now()
        val couponData = coupons.map { coupon ->
            toCouponWithAvailability(coupon, now)
        }

        // 4. 도메인 데이터 반환
        return CouponService.CouponsResult(
            coupons = couponData,
            totalCount = totalCount
        )
    }

    override fun getValidCoupon(couponId: Long): CouponService.CouponInfo {
        // 1. 쿠폰 조회
        val coupon = couponRepository.findById(couponId)
            ?: throw ResourceNotFoundException("쿠폰을 찾을 수 없습니다.")

        // 2. 유효 기간 검증
        val now = LocalDateTime.now()
        if (now.isBefore(coupon.validFrom)) {
            throw CouponNotStartedException("아직 사용할 수 없는 쿠폰입니다.")
        }
        if (now.isAfter(coupon.validUntil)) {
            throw CouponExpiredException("유효기간이 만료된 쿠폰입니다.")
        }

        // 3. Entity → Service DTO 변환
        return CouponService.CouponInfo(
            id = coupon.id!!,
            name = coupon.name,
            discountType = coupon.discountType,
            discountValue = coupon.discountValue,
            minOrderAmount = coupon.minOrderAmount,
            maxDiscountAmount = coupon.maxDiscountAmount,
            totalQuantity = coupon.totalQuantity,
            validFrom = coupon.validFrom,
            validUntil = coupon.validUntil,
            createdAt = coupon.createdAt
        )
    }

    override fun validateAndGetCoupon(userId: Long, userCouponId: Long): CouponValidationResult {
        // 1. 사용자 쿠폰 조회
        val userCoupon = userCouponRepository.findById(userCouponId)
            ?: throw UserCouponNotFoundException("사용자 쿠폰을 찾을 수 없습니다.")

        // 2. 소유권 확인
        if (userCoupon.userId != userId) {
            throw UserCouponNotFoundException("해당 쿠폰에 대한 권한이 없습니다.")
        }

        // 3. 상태 확인
        if (userCoupon.status != UserCouponStatus.ISSUED) {
            throw UserCouponAlreadyUsedException("이미 사용된 쿠폰입니다.")
        }

        // 4. 쿠폰 정보 조회
        val coupon = couponRepository.findById(userCoupon.couponId)
            ?: throw UserCouponNotFoundException("쿠폰 정보를 찾을 수 없습니다.")

        // 5. 유효 기간 확인
        val now = LocalDateTime.now()
        if (now < coupon.validFrom || now > coupon.validUntil) {
            throw UserCouponExpiredException("쿠폰이 만료되었습니다.")
        }

        // 6. Entity → Service DTO 변환
        val couponInfo = CouponService.CouponInfo(
            id = coupon.id!!,
            name = coupon.name,
            discountType = coupon.discountType,
            discountValue = coupon.discountValue,
            minOrderAmount = coupon.minOrderAmount,
            maxDiscountAmount = coupon.maxDiscountAmount,
            totalQuantity = coupon.totalQuantity,
            validFrom = coupon.validFrom,
            validUntil = coupon.validUntil,
            createdAt = coupon.createdAt
        )

        return CouponValidationResult(couponInfo)
    }

    @Transactional
    override fun markCouponAsUsed(userCouponId: Long, orderId: Long) {
        // 1. 사용자 쿠폰 조회
        val userCoupon = userCouponRepository.findById(userCouponId)
            ?: throw UserCouponNotFoundException("사용자 쿠폰을 찾을 수 없습니다.")

        // 2. 쿠폰 상태를 USED로 변경
        val now = LocalDateTime.now()
        val updatedCoupon = userCoupon.copy(
            status = UserCouponStatus.USED,
            usedOrderId = orderId,
            usedAt = now,
            updatedAt = now
        )

        // 3. 저장
        userCouponRepository.save(updatedCoupon.userId, updatedCoupon.couponId)
    }

    override fun calculateDiscount(coupon: CouponService.CouponInfo, originalAmount: Int): Int {
        // 할인 금액 계산 로직
        return when (coupon.discountType) {
            "PERCENTAGE" -> {
                val calculated = (originalAmount * coupon.discountValue) / 100
                // maxDiscountAmount가 0이면 제한 없음 (Int.MAX_VALUE로 처리)
                val maxDiscount = if (coupon.maxDiscountAmount > 0) coupon.maxDiscountAmount else Int.MAX_VALUE
                min(calculated, maxDiscount)
            }
            "FIXED_AMOUNT" -> coupon.discountValue
            else -> 0
        }
    }

    /**
     * CouponWithQuantity를 도메인 데이터로 변환하고 isIssuable 계산
     *
     * isIssuable 조건:
     * - 현재 시각이 유효 기간 내 (now BETWEEN validFrom AND validUntil)
     * - 남은 수량이 1개 이상 (remainingQuantity > 0)
     */
    private fun toCouponWithAvailability(coupon: CouponWithQuantity, now: LocalDateTime): CouponService.CouponWithAvailability {
        val isInValidPeriod = now.isAfter(coupon.validFrom) && now.isBefore(coupon.validUntil)
        val hasRemainingQuantity = coupon.remainingQuantity > 0
        val isIssuable = isInValidPeriod && hasRemainingQuantity

        return CouponService.CouponWithAvailability(
            couponId = coupon.id,
            name = coupon.name,
            discountType = coupon.discountType,
            discountValue = coupon.discountValue,
            minOrderAmount = coupon.minOrderAmount,
            maxDiscountAmount = coupon.maxDiscountAmount,
            remainingQuantity = coupon.remainingQuantity,
            totalQuantity = coupon.totalQuantity,
            validFrom = coupon.validFrom,
            validUntil = coupon.validUntil,
            isIssuable = isIssuable
        )
    }
}
