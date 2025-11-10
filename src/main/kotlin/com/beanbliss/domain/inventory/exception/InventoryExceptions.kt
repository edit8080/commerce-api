package com.beanbliss.domain.inventory.exception

/**
 * 재고가 부족할 때 발생하는 예외
 *
 * [사용 시나리오]:
 * - 주문 시 가용 재고보다 많은 수량 요청
 * - 재고 감소 시 재고 부족
 */
class InsufficientStockException(message: String) : RuntimeException(message)

/**
 * 최대 재고 수량을 초과할 때 발생하는 예외
 *
 * [사용 시나리오]:
 * - 재고 추가 시 최대 허용량(1,000,000개)을 초과하는 경우
 * - 악의적인 재고 추가 방지
 */
class MaxStockExceededException(message: String) : RuntimeException(message)
