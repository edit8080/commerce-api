package com.beanbliss.domain.coupon.repository

import com.beanbliss.domain.coupon.domain.DiscountType
import com.beanbliss.domain.coupon.entity.CouponEntity
import com.beanbliss.domain.coupon.entity.CouponTicketStatus
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * [책임]: 쿠폰 In-memory 저장소 구현
 * - ConcurrentHashMap을 사용하여 Thread-safe 보장
 * - COUPON과 COUPON_TICKET을 함께 관리
 * - remainingQuantity 계산 (COUPON_TICKET의 AVAILABLE 상태 카운트)
 *
 * [주의사항]:
 * - 애플리케이션 재시작 시 데이터 소실 (In-memory 특성)
 * - 실제 DB 사용 시 JPA 기반 구현체로 교체 필요
 */
@Repository
class CouponRepositoryImpl : CouponRepository {

    // Thread-safe한 In-memory 저장소
    private val coupons = ConcurrentHashMap<Long, CouponEntity>()

    // CouponTicket 저장소 (couponId -> List<CouponTicket>)
    private val tickets = ConcurrentHashMap<Long, MutableList<CouponTicket>>()

    init {
        // 초기 테스트 데이터 세팅
        initializeSampleData()
    }

    override fun findAllCoupons(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): List<CouponWithQuantity> {
        // 1. 모든 쿠폰 조회
        val allCoupons = coupons.values.toList()

        // 2. 정렬 적용
        val sorted = when (sortBy) {
            "created_at" -> {
                if (sortDirection == "DESC") {
                    allCoupons.sortedByDescending { it.createdAt }
                } else {
                    allCoupons.sortedBy { it.createdAt }
                }
            }
            "name" -> {
                if (sortDirection == "DESC") {
                    allCoupons.sortedByDescending { it.name }
                } else {
                    allCoupons.sortedBy { it.name }
                }
            }
            else -> allCoupons // 기본: 정렬 없음
        }

        // 3. 페이징 적용 (1-based index)
        val startIndex = (page - 1) * size
        val endIndex = minOf(startIndex + size, sorted.size)

        if (startIndex >= sorted.size) {
            return emptyList()
        }

        val pagedCoupons = sorted.subList(startIndex, endIndex)

        // 4. CouponWithQuantity로 변환 (remainingQuantity 계산)
        return pagedCoupons.map { coupon ->
            val remainingQuantity = calculateRemainingQuantity(coupon.id!!)

            CouponWithQuantity(
                id = coupon.id!!,
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
        return coupons[couponId]
    }

    override fun save(coupon: CouponEntity): CouponEntity {
        // ID가 null이면 새로운 ID 생성
        val savedCoupon = if (coupon.id == null) {
            val newId = (coupons.keys.maxOrNull() ?: 0L) + 1L
            coupon.copy(id = newId)
        } else {
            coupon
        }

        coupons[savedCoupon.id!!] = savedCoupon
        return savedCoupon
    }

    // === Private Helper Methods ===

    /**
     * 남은 수량 계산: status = 'AVAILABLE'인 티켓 개수
     */
    private fun calculateRemainingQuantity(couponId: Long): Int {
        val couponTickets = tickets[couponId] ?: return 0
        return couponTickets.count { it.status == CouponTicketStatus.AVAILABLE }
    }

    /**
     * 초기 테스트 데이터 세팅
     */
    private fun initializeSampleData() {
        val now = LocalDateTime.now()

        // 쿠폰 1: 오픈 기념 쿠폰 (발급 가능)
        val coupon1 = CouponEntity(
            id = 1L,
            name = "오픈 기념! 선착순 100명 10% 할인 쿠폰",
            discountType = DiscountType.PERCENTAGE,
            discountValue = BigDecimal("10"),
            minOrderAmount = BigDecimal("10000"),
            maxDiscountAmount = BigDecimal("5000"),
            totalQuantity = 100,
            validFrom = now.minusDays(1),
            validUntil = now.plusDays(30),
            createdAt = now.minusDays(2)
        )
        coupons[1L] = coupon1

        // 쿠폰 1의 티켓 (AVAILABLE: 47개, ISSUED: 53개)
        val tickets1 = mutableListOf<CouponTicket>()
        repeat(47) { tickets1.add(CouponTicket(couponId = 1L, status = CouponTicketStatus.AVAILABLE)) }
        repeat(53) { tickets1.add(CouponTicket(couponId = 1L, status = CouponTicketStatus.ISSUED)) }
        tickets[1L] = tickets1

        // 쿠폰 2: 신규 회원 쿠폰 (수량 소진)
        val coupon2 = CouponEntity(
            id = 2L,
            name = "신규 회원 5000원 할인 쿠폰",
            discountType = DiscountType.FIXED_AMOUNT,
            discountValue = BigDecimal("5000"),
            minOrderAmount = BigDecimal("30000"),
            maxDiscountAmount = BigDecimal("5000"),
            totalQuantity = 500,
            validFrom = now.minusDays(1),
            validUntil = now.plusDays(60),
            createdAt = now.minusDays(1)
        )
        coupons[2L] = coupon2

        // 쿠폰 2의 티켓 (모두 ISSUED)
        val tickets2 = mutableListOf<CouponTicket>()
        repeat(500) { tickets2.add(CouponTicket(couponId = 2L, status = CouponTicketStatus.ISSUED)) }
        tickets[2L] = tickets2

        // 쿠폰 3: 만료된 쿠폰
        val coupon3 = CouponEntity(
            id = 3L,
            name = "만료된 쿠폰",
            discountType = DiscountType.PERCENTAGE,
            discountValue = BigDecimal("15"),
            minOrderAmount = BigDecimal("20000"),
            maxDiscountAmount = BigDecimal("10000"),
            totalQuantity = 100,
            validFrom = now.minusDays(60),
            validUntil = now.minusDays(30),
            createdAt = now.minusDays(60)
        )
        coupons[3L] = coupon3

        // 쿠폰 3의 티켓 (모두 AVAILABLE이지만 만료됨)
        val tickets3 = mutableListOf<CouponTicket>()
        repeat(100) { tickets3.add(CouponTicket(couponId = 3L, status = CouponTicketStatus.AVAILABLE)) }
        tickets[3L] = tickets3
    }

    // === Internal Data Class ===

    /**
     * 쿠폰 티켓 데이터 모델
     */
    private data class CouponTicket(
        val couponId: Long,
        val status: CouponTicketStatus // AVAILABLE, ISSUED, EXPIRED
    )
}
