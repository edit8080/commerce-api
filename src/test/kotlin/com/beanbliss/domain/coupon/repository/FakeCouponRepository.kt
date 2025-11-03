package com.beanbliss.domain.coupon.repository

import com.beanbliss.domain.coupon.entity.CouponEntity
import java.time.LocalDateTime

/**
 * 테스트용 In-Memory CouponRepository 구현체
 *
 * [특징]:
 * - 실제 DB 없이 메모리 기반으로 동작
 * - Repository 인터페이스의 계약을 준수
 * - 테스트 격리를 위해 각 테스트마다 새 인스턴스 생성
 */
class FakeCouponRepository : CouponRepository {

    // 쿠폰 데이터 저장소
    private val coupons = mutableListOf<Coupon>()

    // 쿠폰 티켓 데이터 저장소 (couponId -> List of Tickets)
    private val tickets = mutableMapOf<Long, MutableList<CouponTicket>>()

    override fun findAllCoupons(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): List<CouponWithQuantity> {
        // 1. 정렬
        val sorted = when (sortBy) {
            "created_at" -> {
                if (sortDirection == "DESC") {
                    coupons.sortedByDescending { it.createdAt }
                } else {
                    coupons.sortedBy { it.createdAt }
                }
            }
            "name" -> {
                if (sortDirection == "DESC") {
                    coupons.sortedByDescending { it.name }
                } else {
                    coupons.sortedBy { it.name }
                }
            }
            else -> coupons // 기본: 정렬 없음
        }

        // 2. 페이징 (1-based index)
        val startIndex = (page - 1) * size
        val endIndex = minOf(startIndex + size, sorted.size)

        if (startIndex >= sorted.size) {
            return emptyList()
        }

        val pagedCoupons = sorted.subList(startIndex, endIndex)

        // 3. CouponWithQuantity로 변환 (remainingQuantity 계산)
        return pagedCoupons.map { coupon ->
            val remainingQuantity = calculateRemainingQuantity(coupon.id)

            CouponWithQuantity(
                id = coupon.id,
                name = coupon.name,
                discountType = coupon.discountType,
                discountValue = coupon.discountValue,
                minOrderAmount = coupon.minOrderAmount,
                maxDiscountAmount = coupon.maxDiscountAmount,
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
        return coupons.size.toLong()
    }

    override fun findById(couponId: Long): CouponEntity? {
        val coupon = coupons.find { it.id == couponId } ?: return null

        return CouponEntity(
            id = coupon.id,
            name = coupon.name,
            discountType = coupon.discountType,
            discountValue = coupon.discountValue,
            minOrderAmount = coupon.minOrderAmount,
            maxDiscountAmount = coupon.maxDiscountAmount,
            totalQuantity = coupon.totalQuantity,
            validFrom = coupon.validFrom,
            validUntil = coupon.validUntil,
            createdAt = coupon.createdAt,
            updatedAt = coupon.updatedAt
        )
    }

    // === Test Helper Methods ===

    /**
     * 테스트용: 쿠폰 추가
     */
    fun addCoupon(coupon: Coupon) {
        coupons.add(coupon)
    }

    /**
     * 테스트용: 쿠폰 티켓 추가
     */
    fun addTicket(couponId: Long, status: String) {
        val ticketList = tickets.getOrPut(couponId) { mutableListOf() }
        ticketList.add(CouponTicket(couponId = couponId, status = status))
    }

    /**
     * 테스트용: 모든 데이터 삭제
     */
    fun clear() {
        coupons.clear()
        tickets.clear()
    }

    // === Private Helper Methods ===

    /**
     * 남은 수량 계산: status = 'AVAILABLE'인 티켓 개수
     */
    private fun calculateRemainingQuantity(couponId: Long): Int {
        val couponTickets = tickets[couponId] ?: return 0
        return couponTickets.count { it.status == "AVAILABLE" }
    }

    // === Internal Data Classes ===

    /**
     * 테스트용 내부 쿠폰 데이터 모델
     */
    data class Coupon(
        val id: Long,
        val name: String,
        val discountType: String,
        val discountValue: Int,
        val minOrderAmount: Int,
        val maxDiscountAmount: Int,
        val totalQuantity: Int,
        val validFrom: LocalDateTime,
        val validUntil: LocalDateTime,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime
    )

    /**
     * 테스트용 내부 쿠폰 티켓 데이터 모델
     */
    data class CouponTicket(
        val couponId: Long,
        val status: String // "AVAILABLE", "ISSUED", "USED", "EXPIRED"
    )
}
