package com.beanbliss.common.exception

/**
 * 요청한 리소스를 찾을 수 없을 때 발생하는 예외
 *
 * [사용 시나리오]:
 * - 존재하지 않는 엔티티 조회
 * - 삭제된 데이터 접근
 */
class ResourceNotFoundException(message: String) : RuntimeException(message)

/**
 * 요청 파라미터가 유효하지 않을 때 발생하는 예외
 *
 * [사용 시나리오]:
 * - 페이징 파라미터 검증 실패
 * - 잘못된 형식의 입력값
 */
class InvalidParameterException(message: String) : RuntimeException(message)

/**
 * 페이지 번호가 유효하지 않을 때 발생하는 예외
 *
 * [사용 시나리오]:
 * - 페이지 번호가 1 미만인 경우
 */
class InvalidPageNumberException(message: String) : RuntimeException(message)

/**
 * 페이지 크기가 유효하지 않을 때 발생하는 예외
 *
 * [사용 시나리오]:
 * - 페이지 크기가 1 미만이거나 100 초과인 경우
 */
class InvalidPageSizeException(message: String) : RuntimeException(message)
