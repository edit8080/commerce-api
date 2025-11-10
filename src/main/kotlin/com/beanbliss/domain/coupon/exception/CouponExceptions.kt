package com.beanbliss.domain.coupon.exception

/**
 * 쿠폰이 유효기간 이전일 때 발생하는 예외
 *
 * [사용 시나리오]:
 * - validFrom > 현재 시간
 */
class CouponNotStartedException(message: String) : RuntimeException(message)

/**
 * 쿠폰이 유효기간이 만료되었을 때 발생하는 예외
 *
 * [사용 시나리오]:
 * - validUntil < 현재 시간
 */
class CouponExpiredException(message: String) : RuntimeException(message)

/**
 * 이미 발급받은 쿠폰을 재발급 요청할 때 발생하는 예외
 *
 * [사용 시나리오]:
 * - 동일 사용자가 동일 쿠폰을 중복 발급 시도
 */
class CouponAlreadyIssuedException(message: String) : RuntimeException(message)

/**
 * 쿠폰 재고가 부족할 때 발생하는 예외
 *
 * [사용 시나리오]:
 * - AVAILABLE 상태의 티켓이 없을 때
 */
class CouponOutOfStockException(message: String) : RuntimeException(message)

/**
 * 쿠폰 생성 시 비즈니스 규칙 검증 실패 시 발생하는 예외
 *
 * [사용 시나리오]:
 * - 할인값이 유효 범위를 벗어난 경우 (정률: 1~100, 정액: 1 이상)
 * - 유효 기간이 잘못된 경우 (validFrom >= validUntil)
 * - 발급 수량이 유효 범위를 벗어난 경우 (1 ~ 10,000)
 * - 정액 할인에 최대 할인 금액이 설정된 경우
 *
 * [참고]:
 * - 할인 타입 검증은 DTO의 enum 타입으로 처리되며,
 *   잘못된 값은 Spring의 HttpMessageNotReadableException으로 처리됨
 */
class InvalidCouponException(message: String) : RuntimeException(message)
