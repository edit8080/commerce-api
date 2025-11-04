package com.beanbliss.domain.coupon.repository

import com.beanbliss.domain.coupon.entity.UserCouponEntity
import com.beanbliss.domain.coupon.enums.UserCouponStatus
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * [책임]: UserCouponRepository의 In-memory Fake 구현
 * - 단위 테스트를 위한 빠른 실행 환경 제공
 * - 동시성 제어를 위해 ConcurrentHashMap 사용
 */
class FakeUserCouponRepository(
    private val couponRepository: FakeCouponRepository = FakeCouponRepository()
) : UserCouponRepository {

    private val userCoupons = ConcurrentHashMap<Long, UserCouponEntity>()
    private val idGenerator = AtomicLong(1)

    // 테스트 헬퍼: 사용자 쿠폰 추가
    fun addUserCoupon(userCoupon: UserCouponEntity) {
        userCoupons[userCoupon.id] = userCoupon
    }

    // 테스트 헬퍼: 모든 데이터 삭제
    fun clear() {
        userCoupons.clear()
        idGenerator.set(1)
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
