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
