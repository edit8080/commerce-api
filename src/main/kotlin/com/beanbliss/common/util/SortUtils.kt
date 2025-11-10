package com.beanbliss.common.util

import org.springframework.data.domain.Sort

/**
 * Sort 객체 생성 유틸리티
 *
 * [책임]:
 * - API 파라미터(snake_case)를 Entity 필드명(camelCase)으로 변환
 * - Sort.Direction을 기반으로 Sort 객체 생성
 * - 정렬 로직의 중앙 관리
 */
object SortUtils {

    /**
     * Sort 객체 생성
     *
     * @param sortBy 정렬 기준 필드 (snake_case, 예: "created_at")
     * @param sortDirection 정렬 방향 ("ASC" or "DESC")
     * @return Sort 객체
     *
     * @example
     * val sort = SortUtils.createSort("created_at", "DESC")
     * // Result: Sort by createdAt DESC
     */
    fun createSort(sortBy: String, sortDirection: String): Sort {
        val direction = if (sortDirection.uppercase() == "DESC") {
            Sort.Direction.DESC
        } else {
            Sort.Direction.ASC
        }
        return Sort.by(direction, sortBy)
    }
}
