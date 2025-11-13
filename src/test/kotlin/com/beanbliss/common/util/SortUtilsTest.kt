package com.beanbliss.common.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort

@DisplayName("SortUtils 테스트")
class SortUtilsTest {

    @Test
    @DisplayName("ASC 정렬 Sort 객체 생성")
    fun `createSort should create ASC sort`() {
        // Given
        val sortBy = "name"
        val sortDirection = "ASC"

        // When
        val sort = SortUtils.createSort(sortBy, sortDirection)

        // Then
        assertEquals(Sort.Direction.ASC, sort.toList()[0].direction)
        assertEquals("name", sort.toList()[0].property)
    }

    @Test
    @DisplayName("DESC 정렬 Sort 객체 생성")
    fun `createSort should create DESC sort`() {
        // Given
        val sortBy = "name"
        val sortDirection = "DESC"

        // When
        val sort = SortUtils.createSort(sortBy, sortDirection)

        // Then
        assertEquals(Sort.Direction.DESC, sort.toList()[0].direction)
        assertEquals("name", sort.toList()[0].property)
    }

    @Test
    @DisplayName("소문자 asc도 처리")
    fun `createSort should handle lowercase asc`() {
        // Given
        val sortBy = "name"
        val sortDirection = "asc"

        // When
        val sort = SortUtils.createSort(sortBy, sortDirection)

        // Then
        assertEquals(Sort.Direction.ASC, sort.toList()[0].direction)
    }

    @Test
    @DisplayName("소문자 desc도 처리")
    fun `createSort should handle lowercase desc`() {
        // Given
        val sortBy = "name"
        val sortDirection = "desc"

        // When
        val sort = SortUtils.createSort(sortBy, sortDirection)

        // Then
        assertEquals(Sort.Direction.DESC, sort.toList()[0].direction)
    }

    @Test
    @DisplayName("기본적으로 ASC 처리 (잘못된 direction)")
    fun `createSort should default to ASC for invalid direction`() {
        // Given
        val sortBy = "name"
        val sortDirection = "INVALID"

        // When
        val sort = SortUtils.createSort(sortBy, sortDirection)

        // Then
        assertEquals(Sort.Direction.ASC, sort.toList()[0].direction)
    }
}
