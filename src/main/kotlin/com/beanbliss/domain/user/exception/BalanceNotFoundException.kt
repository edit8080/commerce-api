package com.beanbliss.domain.user.exception

/**
 * 잔액 정보를 찾을 수 없을 때 발생하는 예외
 */
class BalanceNotFoundException(message: String) : RuntimeException(message)
