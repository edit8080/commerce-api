package com.beanbliss.domain.user.exception

import org.springframework.core.annotation.Order
import org.springframework.web.bind.annotation.ControllerAdvice

/**
 * User 도메인 예외 처리 핸들러
 *
 * [책임]:
 * - User 도메인의 비즈니스 예외 처리
 *
 * [우선순위]:
 * - @Order(10)으로 높은 우선순위 설정
 * - CommonExceptionHandler(Order=100)보다 먼저 처리
 *
 * [참고]:
 * - 현재 User 도메인에는 별도 예외 핸들러가 없습니다.
 * - 필요 시 @ExceptionHandler 메서드를 추가하세요.
 */
@ControllerAdvice
@Order(10)
class UserExceptionHandler {
    // User 도메인 특화 예외 핸들러를 여기에 추가
}
