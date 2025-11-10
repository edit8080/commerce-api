package com.beanbliss.domain.order.exception

/**
 * 장바구니가 비어있을 때 발생하는 예외
 */
class CartEmptyException(message: String) : RuntimeException(message)

/**
 * 비활성화된 상품 옵션이 포함된 경우 발생하는 예외
 */
class ProductOptionInactiveException(message: String) : RuntimeException(message)

/**
 * 이미 진행 중인 주문 예약이 있을 때 발생하는 예외
 */
class DuplicateReservationException(message: String) : RuntimeException(message)

/**
 * 가용 재고가 부족할 때 발생하는 예외
 */
class InsufficientAvailableStockException(message: String) : RuntimeException(message)

/**
 * 사용자 쿠폰을 찾을 수 없을 때 발생하는 예외
 */
class UserCouponNotFoundException(message: String) : RuntimeException(message)

/**
 * 쿠폰이 만료되었을 때 발생하는 예외
 */
class UserCouponExpiredException(message: String) : RuntimeException(message)

/**
 * 쿠폰이 이미 사용되었을 때 발생하는 예외
 */
class UserCouponAlreadyUsedException(message: String) : RuntimeException(message)

/**
 * 최소 주문 금액 미달로 쿠폰을 사용할 수 없을 때 발생하는 예외
 */
class InvalidCouponOrderAmountException(message: String) : RuntimeException(message)

/**
 * 사용자 잔액이 부족할 때 발생하는 예외
 */
class InsufficientBalanceException(message: String) : RuntimeException(message)

/**
 * 재고 예약을 찾을 수 없을 때 발생하는 예외
 */
class InventoryReservationNotFoundException(message: String) : RuntimeException(message)

/**
 * 재고 예약이 만료되었을 때 발생하는 예외
 */
class InventoryReservationExpiredException(message: String) : RuntimeException(message)
