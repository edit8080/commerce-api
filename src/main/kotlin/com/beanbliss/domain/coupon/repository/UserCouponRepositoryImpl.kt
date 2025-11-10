package com.beanbliss.domain.coupon.repository

import com.beanbliss.domain.coupon.entity.UserCouponEntity
import com.beanbliss.domain.coupon.enums.UserCouponStatus
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * [책임]: UserCouponRepository의 In-memory 구현
 * - 사용자별 발급된 쿠폰 관리
 * - 중복 발급 방지
 */
@Repository
class UserCouponRepositoryImpl(
    private val couponRepository: CouponRepository
) : UserCouponRepository {

    private val userCoupons = ConcurrentHashMap<Long, UserCouponEntity>()
    private val idGenerator = AtomicLong(1)

    init {
        initializeSampleData()
    }

    /**
     * 샘플 데이터 초기화
     * - 사용자 100번이 쿠폰 3번을 이미 발급받은 상태 (중복 발급 방지 테스트용)
     */
    private fun initializeSampleData() {
        val now = LocalDateTime.now()

        // 사용자 100번이 쿠폰 3번을 이미 발급받음
        val userCouponId = idGenerator.getAndIncrement()
        userCoupons[userCouponId] = UserCouponEntity(
            id = userCouponId,
            userId = 100L,
            couponId = 3L,
            status = UserCouponStatus.ISSUED,
            usedOrderId = null,
            usedAt = null,
            createdAt = now.minusHours(2),
            updatedAt = now.minusHours(2)
        )
    }

    override fun existsByUserIdAndCouponId(userId: Long, couponId: Long): Boolean {
        return userCoupons.values.any {
            it.userId == userId && it.couponId == couponId
        }
    }

    override fun save(userId: Long, couponId: Long): UserCouponEntity {
        val now = LocalDateTime.now()
        val newUserCoupon = UserCouponEntity(
            id = idGenerator.getAndIncrement(),
            userId = userId,
            couponId = couponId,
            status = UserCouponStatus.ISSUED,
            usedOrderId = null,
            usedAt = null,
            createdAt = now,
            updatedAt = now
        )

        userCoupons[newUserCoupon.id] = newUserCoupon
        return newUserCoupon
    }

    override fun findById(id: Long): UserCouponEntity? {
        return userCoupons[id]
    }

    override fun findAllByUserId(userId: Long): List<UserCouponEntity> {
        return userCoupons.values
            .filter { it.userId == userId }
            .sortedByDescending { it.createdAt }
    }

    override fun findByUserIdWithPaging(userId: Long, page: Int, size: Int, now: LocalDateTime): List<UserCouponWithCoupon> {
        // 1. userId로 필터링 및 Coupon과 JOIN
        val userCouponsWithCoupon = userCoupons.values
            .filter { it.userId == userId }
            .mapNotNull { userCoupon ->
                val couponEntity = couponRepository.findById(userCoupon.couponId) ?: return@mapNotNull null

                // 2. isAvailable 계산 (쿼리 레벨 로직)
                val isAvailable = userCoupon.status == UserCouponStatus.ISSUED &&
                        !now.isBefore(couponEntity.validFrom) &&
                        !now.isAfter(couponEntity.validUntil)

                UserCouponWithCoupon(
                    userCouponId = userCoupon.id,
                    userId = userCoupon.userId,
                    couponId = userCoupon.couponId,
                    status = userCoupon.status,
                    usedOrderId = userCoupon.usedOrderId,
                    usedAt = userCoupon.usedAt,
                    issuedAt = userCoupon.createdAt,
                    couponName = couponEntity.name,
                    discountType = couponEntity.discountType,
                    discountValue = couponEntity.discountValue,
                    minOrderAmount = couponEntity.minOrderAmount,
                    maxDiscountAmount = couponEntity.maxDiscountAmount,
                    validFrom = couponEntity.validFrom,
                    validUntil = couponEntity.validUntil,
                    isAvailable = isAvailable
                )
            }

        // 3. 정렬: isAvailable DESC, issuedAt DESC (쿼리 레벨 정렬)
        val sorted = userCouponsWithCoupon
            .sortedWith(compareByDescending<UserCouponWithCoupon> { it.isAvailable }
                .thenByDescending { it.issuedAt })

        // 4. 페이징 적용 (1-based index)
        val offset = (page - 1) * size
        return sorted.drop(offset).take(size)
    }

    override fun countByUserId(userId: Long): Long {
        return userCoupons.values.count { it.userId == userId }.toLong()
    }
}
