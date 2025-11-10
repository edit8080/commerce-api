package com.beanbliss.domain.coupon.repository

import com.beanbliss.domain.coupon.entity.CouponEntity
import com.beanbliss.domain.coupon.entity.UserCouponEntity
import com.beanbliss.domain.coupon.enums.UserCouponStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * [책임]: Spring Data JPA를 활용한 UserCoupon 영속성 처리
 * Infrastructure Layer에 속하며, JPA 기술에 종속적
 */
interface UserCouponJpaRepository : JpaRepository<UserCouponEntity, Long> {
    /**
     * 사용자가 특정 쿠폰을 이미 발급받았는지 확인
     */
    fun existsByUserIdAndCouponId(userId: Long, couponId: Long): Boolean

    /**
     * 사용자 ID로 모든 쿠폰 조회 (DB 레벨 정렬)
     */
    @Query("""
        SELECT uc
        FROM UserCouponEntity uc
        WHERE uc.userId = :userId
        ORDER BY uc.createdAt DESC
    """)
    fun findByUserId(@Param("userId") userId: Long): List<UserCouponEntity>

    /**
     * 사용자 ID로 쿠폰 개수 조회
     */
    fun countByUserId(userId: Long): Long

    /**
     * 사용자 쿠폰 목록 조회 (Coupon 정보 포함, DB 레벨 정렬 및 페이징)
     * - isAvailable 계산: (status == 'ISSUED') AND (validFrom <= now <= validUntil)
     * - 정렬: isAvailable DESC, createdAt DESC
     */
    @Query("""
        SELECT uc, c,
               CASE WHEN (uc.status = 'ISSUED' AND :now >= c.validFrom AND :now <= c.validUntil) THEN true ELSE false END as isAvailable
        FROM UserCouponEntity uc
        INNER JOIN CouponEntity c ON uc.couponId = c.id
        WHERE uc.userId = :userId
        ORDER BY
            CASE WHEN (uc.status = 'ISSUED' AND :now >= c.validFrom AND :now <= c.validUntil) THEN 0 ELSE 1 END,
            uc.createdAt DESC
    """)
    fun findByUserIdWithCoupon(
        @Param("userId") userId: Long,
        @Param("now") now: LocalDateTime,
        pageable: Pageable
    ): Page<Array<Any>>
}

/**
 * [책임]: UserCouponRepository 인터페이스 구현체
 * - UserCouponJpaRepository를 활용하여 실제 DB 접근
 */
@Repository
class UserCouponRepositoryImpl(
    private val userCouponJpaRepository: UserCouponJpaRepository
) : UserCouponRepository {

    override fun existsByUserIdAndCouponId(userId: Long, couponId: Long): Boolean {
        return userCouponJpaRepository.existsByUserIdAndCouponId(userId, couponId)
    }

    override fun save(userId: Long, couponId: Long): UserCouponEntity {
        val now = LocalDateTime.now()
        val newUserCoupon = UserCouponEntity(
            id = 0L,
            userId = userId,
            couponId = couponId,
            status = UserCouponStatus.ISSUED,
            usedOrderId = null,
            usedAt = null,
            createdAt = now,
            updatedAt = now
        )

        return userCouponJpaRepository.save(newUserCoupon)
    }

    override fun findById(id: Long): UserCouponEntity? {
        return userCouponJpaRepository.findById(id).orElse(null)
    }

    override fun findAllByUserId(userId: Long): List<UserCouponEntity> {
        return userCouponJpaRepository.findByUserId(userId)
    }

    override fun findByUserIdWithPaging(
        userId: Long,
        page: Int,
        size: Int,
        now: LocalDateTime
    ): List<UserCouponWithCoupon> {
        // 1. DB 레벨에서 정렬 및 페이징 적용하여 조회
        val pageRequest = PageRequest.of(page - 1, size)
        val results = userCouponJpaRepository.findByUserIdWithCoupon(userId, now, pageRequest).content

        // 2. UserCouponWithCoupon으로 변환
        return results.map { row ->
            val userCoupon = row[0] as UserCouponEntity
            val coupon = row[1] as CouponEntity
            val isAvailable = row[2] as Boolean

            UserCouponWithCoupon(
                userCouponId = userCoupon.id,
                userId = userCoupon.userId,
                couponId = userCoupon.couponId,
                status = userCoupon.status,
                usedOrderId = userCoupon.usedOrderId,
                usedAt = userCoupon.usedAt,
                issuedAt = userCoupon.createdAt,
                couponName = coupon.name,
                discountType = coupon.discountType.name,
                discountValue = coupon.discountValue.toInt(),
                minOrderAmount = coupon.minOrderAmount.toInt(),
                maxDiscountAmount = coupon.maxDiscountAmount.toInt(),
                validFrom = coupon.validFrom,
                validUntil = coupon.validUntil,
                isAvailable = isAvailable
            )
        }
    }

    override fun countByUserId(userId: Long): Long {
        return userCouponJpaRepository.countByUserId(userId)
    }
}
