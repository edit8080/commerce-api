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
