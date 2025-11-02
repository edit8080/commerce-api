package com.beanbliss.domain.inventory.exception

/**
 * 재고가 부족할 때 발생하는 예외
 *
 * [사용 시나리오]:
 * - 주문 시 가용 재고보다 많은 수량 요청
 * - 재고 감소 시 재고 부족
 */
class InsufficientStockException(message: String) : RuntimeException(message)
