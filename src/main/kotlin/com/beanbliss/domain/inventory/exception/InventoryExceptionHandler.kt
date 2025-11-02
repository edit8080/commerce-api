package com.beanbliss.domain.inventory.exception

import com.beanbliss.common.dto.ErrorResponse
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

/**
 * Inventory 도메인 예외 처리 핸들러
 *
 * [책임]:
 * - Inventory 도메인의 비즈니스 예외 처리
 * - InsufficientStockException 처리
 *
 * [우선순위]:
 * - @Order(10)으로 높은 우선순위 설정
 * - CommonExceptionHandler(Order=100)보다 먼저 처리
 */
@ControllerAdvice
@Order(10)
class InventoryExceptionHandler {

    @ExceptionHandler(InsufficientStockException::class)
    fun handleInsufficientStock(ex: InsufficientStockException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            code = "INSUFFICIENT_STOCK",
            message = ex.message ?: "재고가 부족합니다."
        )
        return ResponseEntity(response, HttpStatus.BAD_REQUEST) // 400
    }
}
