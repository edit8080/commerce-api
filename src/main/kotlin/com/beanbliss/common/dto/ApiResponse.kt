package com.beanbliss.common.dto

/**
 * API 성공 응답을 감싸는 공통 래퍼 클래스
 *
 * [책임]:
 * - 모든 API 성공 응답을 일관된 형식으로 제공
 * - { "data": { ... } } 구조로 응답
 *
 * [사용 예시]:
 * ```
 * return ApiResponse(data = ProductListResponse(...))
 * // { "data": { "content": [...], "pageable": {...} } }
 * ```
 */
data class ApiResponse<T>(
    val data: T
)

/**
 * API 에러 응답 DTO
 *
 * [책임]:
 * - 모든 API 에러 응답을 일관된 형식으로 제공
 * - HTTP 상태 코드, 에러 코드, 메시지 포함
 *
 * [사용 예시]:
 * ```
 * ErrorResponse(
 *     status = 400,
 *     code = "INVALID_PARAMETER",
 *     message = "페이지 번호는 1 이상이어야 합니다"
 * )
 * ```
 */
data class ErrorResponse(
    val status: Int,
    val code: String,
    val message: String
)
