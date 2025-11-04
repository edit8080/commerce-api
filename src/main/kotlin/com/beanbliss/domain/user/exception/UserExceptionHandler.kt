package com.beanbliss.domain.user.exception

import com.beanbliss.common.dto.ErrorResponse
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

/**
 * User 도메인 예외 처리 핸들러
 *
 * [책임]:
 * - User 도메인의 비즈니스 예외 처리
 * - 잔액 조회 관련 예외 처리
 *
 * [우선순위]:
 * - @Order(10)으로 높은 우선순위 설정
 * - CommonExceptionHandler(Order=100)보다 먼저 처리
 */
@ControllerAdvice
@Order(10)
class UserExceptionHandler {

    @ExceptionHandler(BalanceNotFoundException::class)
    fun handleBalanceNotFound(ex: BalanceNotFoundException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            code = "BALANCE_NOT_FOUND",
            message = ex.message ?: "잔액 정보를 찾을 수 없습니다."
        )
        return ResponseEntity(response, HttpStatus.NOT_FOUND) // 404
    }
}
