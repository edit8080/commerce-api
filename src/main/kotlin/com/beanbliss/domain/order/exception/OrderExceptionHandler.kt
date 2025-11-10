package com.beanbliss.domain.order.exception

import com.beanbliss.common.dto.ErrorResponse
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

/**
 * Order 도메인 예외 처리 핸들러
 *
 * [책임]:
 * - Order 도메인의 비즈니스 예외 처리
 * - 주문 예약 관련 예외 처리
 *
 * [우선순위]:
 * - @Order(10)으로 높은 우선순위 설정
 * - CommonExceptionHandler(Order=100)보다 먼저 처리
 */
@ControllerAdvice
@Order(10)
class OrderExceptionHandler {

    @ExceptionHandler(CartEmptyException::class)
    fun handleCartEmpty(ex: CartEmptyException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            code = "CART_EMPTY",
            message = ex.message ?: "장바구니가 비어 있습니다."
        )
        return ResponseEntity(response, HttpStatus.BAD_REQUEST) // 400
    }

    @ExceptionHandler(ProductOptionInactiveException::class)
    fun handleProductOptionInactive(ex: ProductOptionInactiveException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            code = "PRODUCT_OPTION_INACTIVE",
            message = ex.message ?: "비활성화된 상품 옵션이 포함되어 있습니다."
        )
        return ResponseEntity(response, HttpStatus.BAD_REQUEST) // 400
    }

    @ExceptionHandler(DuplicateReservationException::class)
    fun handleDuplicateReservation(ex: DuplicateReservationException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.CONFLICT.value(),
            code = "DUPLICATE_RESERVATION",
            message = ex.message ?: "이미 진행 중인 주문 예약이 있습니다."
        )
        return ResponseEntity(response, HttpStatus.CONFLICT) // 409
    }

    @ExceptionHandler(InsufficientAvailableStockException::class)
    fun handleInsufficientAvailableStock(ex: InsufficientAvailableStockException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.CONFLICT.value(),
            code = "INSUFFICIENT_AVAILABLE_STOCK",
            message = ex.message ?: "가용 재고가 부족합니다."
        )
        return ResponseEntity(response, HttpStatus.CONFLICT) // 409
    }

    @ExceptionHandler(UserCouponNotFoundException::class)
    fun handleUserCouponNotFound(ex: UserCouponNotFoundException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            code = "USER_COUPON_NOT_FOUND",
            message = ex.message ?: "사용자 쿠폰을 찾을 수 없습니다."
        )
        return ResponseEntity(response, HttpStatus.NOT_FOUND) // 404
    }

    @ExceptionHandler(UserCouponExpiredException::class)
    fun handleUserCouponExpired(ex: UserCouponExpiredException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            code = "USER_COUPON_EXPIRED",
            message = ex.message ?: "쿠폰이 만료되었습니다."
        )
        return ResponseEntity(response, HttpStatus.BAD_REQUEST) // 400
    }

    @ExceptionHandler(UserCouponAlreadyUsedException::class)
    fun handleUserCouponAlreadyUsed(ex: UserCouponAlreadyUsedException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            code = "USER_COUPON_ALREADY_USED",
            message = ex.message ?: "이미 사용된 쿠폰입니다."
        )
        return ResponseEntity(response, HttpStatus.BAD_REQUEST) // 400
    }

    @ExceptionHandler(InvalidCouponOrderAmountException::class)
    fun handleInvalidCouponOrderAmount(ex: InvalidCouponOrderAmountException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            code = "INVALID_COUPON_ORDER_AMOUNT",
            message = ex.message ?: "최소 주문 금액 미달로 쿠폰을 사용할 수 없습니다."
        )
        return ResponseEntity(response, HttpStatus.BAD_REQUEST) // 400
    }

    @ExceptionHandler(InsufficientBalanceException::class)
    fun handleInsufficientBalance(ex: InsufficientBalanceException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.CONFLICT.value(),
            code = "INSUFFICIENT_BALANCE",
            message = ex.message ?: "사용자 잔액이 부족합니다."
        )
        return ResponseEntity(response, HttpStatus.CONFLICT) // 409
    }

    @ExceptionHandler(InventoryReservationNotFoundException::class)
    fun handleInventoryReservationNotFound(ex: InventoryReservationNotFoundException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            code = "INVENTORY_RESERVATION_NOT_FOUND",
            message = ex.message ?: "재고 예약을 찾을 수 없습니다."
        )
        return ResponseEntity(response, HttpStatus.NOT_FOUND) // 404
    }

    @ExceptionHandler(InventoryReservationExpiredException::class)
    fun handleInventoryReservationExpired(ex: InventoryReservationExpiredException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            code = "INVENTORY_RESERVATION_EXPIRED",
            message = ex.message ?: "재고 예약이 만료되었습니다."
        )
        return ResponseEntity(response, HttpStatus.BAD_REQUEST) // 400
    }
}
