package com.beanbliss.domain.coupon.repository

import com.beanbliss.domain.coupon.entity.UserCouponEntity
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
class UserCouponRepositoryImpl : UserCouponRepository {

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
            status = "ISSUED",
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
