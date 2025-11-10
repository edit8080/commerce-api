package com.beanbliss.common.dto

/**
 * 페이징 정보 공통 응답
 *
 * [책임]: 페이징 처리된 데이터의 메타 정보 제공
 * - pageNumber: 현재 페이지 번호 (1부터 시작)
 * - pageSize: 페이지 크기
 * - totalElements: 전체 데이터 개수
 * - totalPages: 전체 페이지 수
 */
data class PageableResponse(
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int
)
