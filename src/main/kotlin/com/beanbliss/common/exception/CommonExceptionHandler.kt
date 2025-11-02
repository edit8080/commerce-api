package com.beanbliss.common.exception

import com.beanbliss.common.dto.ErrorResponse
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.MethodArgumentNotValidException

/**
 * 공통 예외 처리 핸들러
 *
 * [책임]:
 * - 도메인에 속하지 않는 공통 예외 처리
 * - ResourceNotFoundException, InvalidParameterException 등
 *
 * [우선순위]:
 * - @Order(100)으로 낮은 우선순위 설정
 * - 도메인별 핸들러(예: InventoryExceptionHandler)가 먼저 처리
 * - 처리되지 않은 예외만 이 핸들러로 fallback
 */
@ControllerAdvice
@Order(100)
class CommonExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(ex: ResourceNotFoundException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            code = "RESOURCE_NOT_FOUND",
            message = ex.message ?: "요청한 자원을 찾을 수 없습니다."
        )
        return ResponseEntity(response, HttpStatus.NOT_FOUND) // 404
    }

    @ExceptionHandler(InvalidParameterException::class)
    fun handleInvalidParameter(ex: InvalidParameterException): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            code = "INVALID_PARAMETER",
            message = ex.message ?: "요청 파라미터가 유효하지 않습니다."
        )
        return ResponseEntity(response, HttpStatus.BAD_REQUEST) // 400
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errorMessage = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
            ?: "유효성 검사에 실패했습니다."

        val response = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            code = "INVALID_INPUT",
            message = errorMessage
        )
        return ResponseEntity(response, HttpStatus.BAD_REQUEST) // 400
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            code = "INTERNAL_SERVER_ERROR",
            message = "서버 내부 오류가 발생했습니다."
        )
        return ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR) // 500
    }
}
