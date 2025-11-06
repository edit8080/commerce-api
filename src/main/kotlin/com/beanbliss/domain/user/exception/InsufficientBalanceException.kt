package com.beanbliss.domain.user.exception

/**
 * 잔액이 부족할 때 발생하는 예외
 */
class InsufficientBalanceException(message: String) : RuntimeException(message)
