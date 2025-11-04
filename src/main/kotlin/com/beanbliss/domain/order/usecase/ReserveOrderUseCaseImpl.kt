package com.beanbliss.domain.order.usecase

import com.beanbliss.domain.cart.repository.CartItemRepository
import com.beanbliss.domain.inventory.entity.InventoryReservationEntity
import com.beanbliss.domain.inventory.entity.InventoryReservationStatus
import com.beanbliss.domain.inventory.repository.InventoryRepository
import com.beanbliss.domain.inventory.repository.InventoryReservationRepository
import com.beanbliss.domain.order.dto.InventoryReservationItemResponse
import com.beanbliss.domain.order.dto.ReserveOrderResponse
import com.beanbliss.domain.order.exception.CartEmptyException
import com.beanbliss.domain.order.exception.DuplicateReservationException
import com.beanbliss.domain.order.exception.InsufficientAvailableStockException
import com.beanbliss.domain.order.exception.ProductOptionInactiveException
import com.beanbliss.domain.product.repository.ProductOptionRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * [책임]: 주문 예약 UseCase 구현
 * - 여러 도메인 Repository 조율
 * - 복잡한 비즈니스 트랜잭션 처리
 */
@Component
class ReserveOrderUseCaseImpl(
    private val cartItemRepository: CartItemRepository,
    private val productOptionRepository: ProductOptionRepository,
    private val inventoryRepository: InventoryRepository,
    private val inventoryReservationRepository: InventoryReservationRepository
) : ReserveOrderUseCase {

    @Transactional
    override fun reserveOrder(userId: Long): ReserveOrderResponse {
        // 1. 장바구니 조회 및 검증
        val cartItems = cartItemRepository.findByUserId(userId)
        if (cartItems.isEmpty()) {
            throw CartEmptyException("장바구니가 비어 있습니다.")
        }

        // 각 상품 옵션의 활성 여부 검증
        cartItems.forEach { cartItem ->
            val productOption = productOptionRepository.findActiveOptionWithProduct(cartItem.productOptionId)
            if (productOption == null || !productOption.isActive) {
                throw ProductOptionInactiveException("비활성화된 상품 옵션이 포함되어 있습니다.")
            }
        }

        // 2. 중복 예약 방지
        val activeReservationCount = inventoryReservationRepository.countActiveReservations(userId)
        if (activeReservationCount > 0) {
            throw DuplicateReservationException("이미 진행 중인 주문 예약이 있습니다.")
        }

        // 3. 가용 재고 계산 및 예약 생성
        val now = LocalDateTime.now()
        val expiresAt = now.plusMinutes(30)
        val reservations = mutableListOf<InventoryReservationItemResponse>()

        cartItems.forEach { cartItem ->
            // 가용 재고 조회
            val availableStock = inventoryRepository.calculateAvailableStock(cartItem.productOptionId)

            // 재고 충분성 검증
            if (availableStock < cartItem.quantity) {
                throw InsufficientAvailableStockException(
                    "가용 재고가 부족합니다. 상품 옵션 ID: ${cartItem.productOptionId}"
                )
            }

            // 예약 생성
            val reservation = InventoryReservationEntity(
                id = 0L,
                productOptionId = cartItem.productOptionId,
                userId = userId,
                quantity = cartItem.quantity,
                status = InventoryReservationStatus.RESERVED,
                reservedAt = now,
                expiresAt = expiresAt,
                updatedAt = now
            )

            val savedReservation = inventoryReservationRepository.save(reservation)

            // 응답 DTO 변환
            val reservationResponse = InventoryReservationItemResponse(
                reservationId = savedReservation.id,
                productOptionId = savedReservation.productOptionId,
                productName = cartItem.productName,
                optionCode = cartItem.optionCode,
                quantity = savedReservation.quantity,
                status = savedReservation.status,
                availableStock = availableStock - cartItem.quantity,
                reservedAt = savedReservation.reservedAt,
                expiresAt = savedReservation.expiresAt
            )

            reservations.add(reservationResponse)
        }

        return ReserveOrderResponse(reservations)
    }
}
