package com.beanbliss.domain.coupon.repository

import com.beanbliss.common.util.SortUtils
import com.beanbliss.domain.coupon.entity.CouponEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * [책임]: Spring Data JPA를 활용한 Coupon 영속성 처리
 * Infrastructure Layer에 속하며, JPA 기술에 종속적
 */
interface CouponJpaRepository : JpaRepository<CouponEntity, Long> {
    /**
     * 쿠폰 목록 조회 (COUPON_TICKET과 LEFT JOIN하여 remainingQuantity 계산)
     * N+1 문제 방지를 위한 단일 쿼리 (DB 레벨 정렬 및 페이징)
     *
     * 계산식: COUNT(COUPON_TICKET) WHERE status = 'AVAILABLE' AND user_id IS NULL
     *
     * @return Page<Array<Any>> = [[CouponEntity, remainingQuantity: Long], ...]
     */
    @Query("""
        SELECT c, COUNT(ct)
        FROM CouponEntity c
        LEFT JOIN CouponTicketEntity ct ON ct.couponId = c.id
            AND ct.status = 'AVAILABLE'
            AND ct.userId IS NULL
        GROUP BY c.id, c.name, c.discountType, c.discountValue, c.minOrderAmount,
                 c.maxDiscountAmount, c.totalQuantity, c.validFrom, c.validUntil,
                 c.createdAt, c.updatedAt
    """)
    fun findAllCouponsWithRemainingQuantity(pageable: Pageable): Page<Array<Any>>
}

/**
 * [책임]: CouponRepository 인터페이스 구현체
 * - CouponJpaRepository를 활용하여 실제 DB 접근
 * - COUPON_TICKET과 LEFT JOIN하여 remainingQuantity 계산 (단일 쿼리)
 */
@Repository
class CouponRepositoryImpl(
    private val couponJpaRepository: CouponJpaRepository
) : CouponRepository {

    override fun findAllCoupons(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): List<CouponWithQuantity> {
        // 1. Create Sort object using SortUtils
        val sort = SortUtils.createSort(sortBy, sortDirection)

        // 2. DB 레벨에서 정렬 및 페이징 적용하여 조회
        val pageRequest = PageRequest.of(page - 1, size, sort)
        val results = couponJpaRepository.findAllCouponsWithRemainingQuantity(pageRequest).content

        // 3. CouponWithQuantity로 변환
        return results.map { row ->
            val coupon = row[0] as CouponEntity
            val remainingQuantity = (row[1] as Long).toInt()

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
