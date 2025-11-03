package com.beanbliss.domain.coupon.repository

import com.beanbliss.domain.coupon.entity.UserCouponEntity
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * [책임]: UserCouponRepository의 In-memory Fake 구현
 * - 단위 테스트를 위한 빠른 실행 환경 제공
 * - 동시성 제어를 위해 ConcurrentHashMap 사용
 */
class FakeUserCouponRepository : UserCouponRepository {

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
            status = "ISSUED",
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
}
