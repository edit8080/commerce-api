package com.beanbliss.domain.inventory.service

import com.beanbliss.domain.cart.repository.CartItemDetail
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import com.beanbliss.domain.inventory.repository.InventoryReservationRepository
import com.beanbliss.domain.order.exception.InventoryReservationExpiredException
import com.beanbliss.domain.order.exception.InventoryReservationNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * [책임]: 재고 예약 비즈니스 로직 구현
 * - 재고 예약 검증 (존재 여부, 만료 여부)
 * - 재고 예약 확정 (RESERVED → CONFIRMED)
 *
 * [트랜잭션]: @Transactional을 통해 원자성 보장
 *
 * [DIP 준수]:
 * - InventoryReservationRepository Interface에만 의존
 */
@Service
@Transactional
class InventoryReservationService(
    private val inventoryReservationRepository: InventoryReservationRepository
) {

    fun validateReservations(userId: Long, cartItems: List<CartItemDetail>) {
        // 1. 사용자의 활성 재고 예약 조회
        val reservations = inventoryReservationRepository.findActiveReservationsByUserId(userId)

        // 2. 예약 존재 여부 확인
        if (reservations.isEmpty()) {
            throw InventoryReservationNotFoundException("재고 예약을 찾을 수 없습니다.")
        }

        // 3. 예약 만료 여부 확인
        val now = LocalDateTime.now()
        reservations.forEach { reservation ->
            if (reservation.expiresAt <= now) {
                throw InventoryReservationExpiredException("재고 예약이 만료되었습니다.")
            }
        }

        // 4. 예약 수량과 장바구니 수량 일치 확인은 생략 (간단한 버전)
        // TODO: 필요시 추가 검증 로직 구현
    }

    fun confirmReservations(userId: Long, cartItems: List<CartItemDetail>) {
        // 1. 사용자의 활성 재고 예약 조회
        val reservations = inventoryReservationRepository.findActiveReservationsByUserId(userId)

        // 2. 예약 상태를 CONFIRMED로 변경
        val now = LocalDateTime.now()
        reservations.forEach { reservation ->
            reservation.status = InventoryReservationStatus.CONFIRMED
            reservation.updatedAt = now
            inventoryReservationRepository.save(reservation)
        }
    }
}
