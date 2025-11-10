package com.beanbliss.domain.coupon.repository

import com.beanbliss.domain.coupon.entity.CouponEntity
import com.beanbliss.domain.coupon.entity.CouponTicketStatus
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal

/**
 * [책임]: Spring Data JPA를 활용한 Coupon 영속성 처리
 * Infrastructure Layer에 속하며, JPA 기술에 종속적
 */
interface CouponJpaRepository : JpaRepository<CouponEntity, Long>

/**
 * [책임]: CouponRepository 인터페이스 구현체
 * - CouponJpaRepository를 활용하여 실제 DB 접근
 * - COUPON_TICKET과 연계하여 remainingQuantity 계산
 */
@Repository
class CouponRepositoryImpl(
    private val couponJpaRepository: CouponJpaRepository,
    private val couponTicketJpaRepository: CouponTicketJpaRepository
) : CouponRepository {

    override fun findAllCoupons(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): List<CouponWithQuantity> {
        // 1. 정렬 설정
        val sort = when (sortBy) {
            "name" -> Sort.by(
                if (sortDirection == "DESC") Sort.Direction.DESC else Sort.Direction.ASC,
                "name"
            )
            else -> Sort.by(
                if (sortDirection == "DESC") Sort.Direction.DESC else Sort.Direction.ASC,
                "createdAt"
            )
        }

        // 2. 페이징 설정 (0-based index)
        val pageRequest = PageRequest.of(page - 1, size, sort)

        // 3. 쿠폰 조회
        val coupons = couponJpaRepository.findAll(pageRequest).content

        // 4. CouponWithQuantity로 변환 (remainingQuantity 계산)
        return coupons.map { coupon ->
            val remainingQuantity = couponTicketJpaRepository.countByCouponIdAndStatus(
                coupon.id,
                CouponTicketStatus.AVAILABLE
            ).toInt()

            CouponWithQuantity(
                id = coupon.id,
                name = coupon.name,
                discountType = coupon.discountType.name,
                discountValue = coupon.discountValue.toInt(),
                minOrderAmount = coupon.minOrderAmount.toInt(),
                maxDiscountAmount = coupon.maxDiscountAmount.toInt(),
                totalQuantity = coupon.totalQuantity,
                validFrom = coupon.validFrom,
                validUntil = coupon.validUntil,
                createdAt = coupon.createdAt,
                updatedAt = coupon.updatedAt,
                remainingQuantity = remainingQuantity
            )
        }
    }

    override fun countAllCoupons(): Long {
        return couponJpaRepository.count()
    }

    override fun findById(couponId: Long): CouponEntity? {
        return couponJpaRepository.findById(couponId).orElse(null)
    }

    override fun save(coupon: CouponEntity): CouponEntity {
        return couponJpaRepository.save(coupon)
    }
}
