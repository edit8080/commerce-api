package com.beanbliss.common.pagination

import kotlin.math.ceil

/**
 * 페이징 계산 유틸리티
 *
 * [책임]: 페이징 관련 계산 로직을 캡슐화하여 재사용성을 높임
 * - 전체 페이지 수 계산
 * - 페이지 범위 검증
 *
 * [사용 예시]:
 * ```
 * val totalPages = PageCalculator.calculateTotalPages(totalElements = 25, pageSize = 10)
 * // totalPages = 3 (25 / 10 = 2.5 -> 올림 3)
 * ```
 */
object PageCalculator {

    /**
     * 전체 페이지 수 계산
     *
     * @param totalElements 전체 데이터 개수
     * @param pageSize 페이지 크기
     * @return 전체 페이지 수 (최소 0)
     * @throws IllegalArgumentException pageSize가 1 미만인 경우
     */
    fun calculateTotalPages(totalElements: Long, pageSize: Int): Int {
        require(pageSize > 0) { "페이지 크기는 1 이상이어야 합니다: $pageSize" }

        if (totalElements <= 0) {
            return 0
        }

        return ceil(totalElements.toDouble() / pageSize).toInt()
    }

    /**
     * 페이지 번호 유효성 검증
     *
     * @param page 페이지 번호 (1부터 시작)
     * @return 유효 여부
     */
    fun isValidPage(page: Int): Boolean {
        return page >= 1
    }

    /**
     * 페이지 크기 유효성 검증
     *
     * @param size 페이지 크기
     * @param maxSize 최대 페이지 크기 (기본값: 100)
     * @return 유효 여부
     */
    fun isValidPageSize(size: Int, maxSize: Int = 100): Boolean {
        return size in 1..maxSize
    }
}
