package com.beanbliss.domain.coupon.exception

import com.beanbliss.common.dto.ErrorResponse
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

/**
 * Coupon 도메인 예외 처리 핸들러
 *
 * [책임]:
 * - Coupon 도메인의 비즈니스 예외 처리
 * - 쿠폰 발급 관련 예외 처리
 *
 * [우선순위]:
 * - @Order(10)으로 높은 우선순위 설정
 * - CommonExceptionHandler(Order=100)보다 먼저 처리
 */
@ControllerAdvice
@Order(10)
class CouponExceptionHandler {

    @ExceptionHandler(CouponNotStartedException::class)
    fun handleCouponNotStarted(ex: CouponNotStartedException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            code = "COUPON_NOT_STARTED",
            message = ex.message ?: "아직 사용할 수 없는 쿠폰입니다."
        )
        return ResponseEntity(response, HttpStatus.BAD_REQUEST) // 400
    }

    @ExceptionHandler(CouponExpiredException::class)
    fun handleCouponExpired(ex: CouponExpiredException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            code = "COUPON_EXPIRED",
            message = ex.message ?: "유효기간이 만료된 쿠폰입니다."
        )
        return ResponseEntity(response, HttpStatus.BAD_REQUEST) // 400
    }

    @ExceptionHandler(CouponAlreadyIssuedException::class)
    fun handleCouponAlreadyIssued(ex: CouponAlreadyIssuedException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.CONFLICT.value(),
            code = "ALREADY_ISSUED",
            message = ex.message ?: "이미 발급받은 쿠폰입니다."
        )
        return ResponseEntity(response, HttpStatus.CONFLICT) // 409
    }

    @ExceptionHandler(CouponOutOfStockException::class)
    fun handleCouponOutOfStock(ex: CouponOutOfStockException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            code = "OUT_OF_STOCK",
            message = ex.message ?: "쿠폰 재고가 부족합니다."
        )
        return ResponseEntity(response, HttpStatus.BAD_REQUEST) // 400
    }
}
