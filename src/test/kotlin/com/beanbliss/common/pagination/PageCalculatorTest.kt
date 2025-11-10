package com.beanbliss.common.pagination

import com.beanbliss.common.exception.InvalidParameterException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows

/**
 * [TDD Red 단계]
 * PageCalculator 유틸리티의 페이징 계산 로직을 검증하는 테스트
 *
 * 검증 목표:
 * 1. 전체 페이지 수가 올바르게 계산되는가?
 * 2. 엣지 케이스를 올바르게 처리하는가? (0개 데이터, 나누어 떨어지는 경우 등)
 * 3. 잘못된 입력에 대해 예외를 발생시키는가?
 */
@DisplayName("페이징 계산 유틸리티 테스트")
class PageCalculatorTest {

    @Test
    @DisplayName("전체 페이지 수 계산: 나누어 떨어지지 않는 경우 올림 처리")
    fun `전체 페이지 수 계산 - 나누어 떨어지지 않는 경우`() {
        // Given: 총 25개의 데이터, 페이지 크기 10
        val totalElements = 25L
        val pageSize = 10

        // When
        val totalPages = PageCalculator.calculateTotalPages(totalElements, pageSize)

        // Then: 25 / 10 = 2.5 -> 올림 3
        assertEquals(3, totalPages, "25개 데이터를 10개씩 나누면 3페이지여야 함")
    }

    @Test
    @DisplayName("전체 페이지 수 계산: 나누어 떨어지는 경우")
    fun `전체 페이지 수 계산 - 나누어 떨어지는 경우`() {
        // Given: 총 20개의 데이터, 페이지 크기 10
        val totalElements = 20L
        val pageSize = 10

        // When
        val totalPages = PageCalculator.calculateTotalPages(totalElements, pageSize)

        // Then: 20 / 10 = 2
        assertEquals(2, totalPages, "20개 데이터를 10개씩 나누면 정확히 2페이지여야 함")
    }

    @Test
    @DisplayName("전체 페이지 수 계산: 데이터가 없는 경우 0 반환")
    fun `전체 페이지 수 계산 - 데이터가 없는 경우`() {
        // Given: 데이터 없음
        val totalElements = 0L
        val pageSize = 10

        // When
        val totalPages = PageCalculator.calculateTotalPages(totalElements, pageSize)

        // Then
        assertEquals(0, totalPages, "데이터가 없으면 0페이지여야 함")
    }

    @Test
    @DisplayName("전체 페이지 수 계산: 데이터가 페이지 크기보다 작은 경우")
    fun `전체 페이지 수 계산 - 데이터가 페이지 크기보다 작은 경우`() {
        // Given: 총 5개의 데이터, 페이지 크기 10
        val totalElements = 5L
        val pageSize = 10

        // When
        val totalPages = PageCalculator.calculateTotalPages(totalElements, pageSize)

        // Then: 5 / 10 = 0.5 -> 올림 1
        assertEquals(1, totalPages, "5개 데이터를 10개 크기로 나누면 1페이지여야 함")
    }

    @Test
    @DisplayName("전체 페이지 수 계산: 데이터가 정확히 1개인 경우")
    fun `전체 페이지 수 계산 - 데이터가 1개인 경우`() {
        // Given
        val totalElements = 1L
        val pageSize = 10

        // When
        val totalPages = PageCalculator.calculateTotalPages(totalElements, pageSize)

        // Then
        assertEquals(1, totalPages, "데이터가 1개면 1페이지여야 함")
    }

    @Test
    @DisplayName("전체 페이지 수 계산: pageSize가 1인 경우")
    fun `전체 페이지 수 계산 - pageSize가 1인 경우`() {
        // Given
        val totalElements = 5L
        val pageSize = 1

        // When
        val totalPages = PageCalculator.calculateTotalPages(totalElements, pageSize)

        // Then: 5 / 1 = 5
        assertEquals(5, totalPages, "페이지 크기가 1이면 데이터 개수만큼 페이지가 있어야 함")
    }

    @Test
    @DisplayName("전체 페이지 수 계산: pageSize가 0 이하인 경우 예외 발생")
    fun `전체 페이지 수 계산 - pageSize가 0 이하인 경우 예외`() {
        // Given
        val totalElements = 10L
        val invalidPageSize = 0

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            PageCalculator.calculateTotalPages(totalElements, invalidPageSize)
        }
        assertTrue(exception.message!!.contains("페이지 크기는 1 이상이어야 합니다"))
    }

    @Test
    @DisplayName("전체 페이지 수 계산: pageSize가 음수인 경우 예외 발생")
    fun `전체 페이지 수 계산 - pageSize가 음수인 경우 예외`() {
        // Given
        val totalElements = 10L
        val invalidPageSize = -5

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            PageCalculator.calculateTotalPages(totalElements, invalidPageSize)
        }
        assertTrue(exception.message!!.contains("페이지 크기는 1 이상이어야 합니다"))
    }

    @Test
    @DisplayName("페이지 번호 유효성 검증: 1 이상이면 유효")
    fun `페이지 번호 유효성 검증 - 유효한 페이지 번호`() {
        assertTrue(PageCalculator.isValidPage(1), "페이지 1은 유효해야 함")
        assertTrue(PageCalculator.isValidPage(10), "페이지 10은 유효해야 함")
        assertTrue(PageCalculator.isValidPage(100), "페이지 100은 유효해야 함")
    }

    @Test
    @DisplayName("페이지 번호 유효성 검증: 0 이하면 무효")
    fun `페이지 번호 유효성 검증 - 무효한 페이지 번호`() {
        assertFalse(PageCalculator.isValidPage(0), "페이지 0은 무효해야 함")
        assertFalse(PageCalculator.isValidPage(-1), "음수 페이지는 무효해야 함")
    }

    @Test
    @DisplayName("페이지 크기 유효성 검증: 1 이상 100 이하면 유효")
    fun `페이지 크기 유효성 검증 - 유효한 페이지 크기`() {
        assertTrue(PageCalculator.isValidPageSize(1), "페이지 크기 1은 유효해야 함")
        assertTrue(PageCalculator.isValidPageSize(10), "페이지 크기 10은 유효해야 함")
        assertTrue(PageCalculator.isValidPageSize(100), "페이지 크기 100은 유효해야 함")
    }

    @Test
    @DisplayName("페이지 크기 유효성 검증: 0 이하 또는 100 초과면 무효")
    fun `페이지 크기 유효성 검증 - 무효한 페이지 크기`() {
        assertFalse(PageCalculator.isValidPageSize(0), "페이지 크기 0은 무효해야 함")
        assertFalse(PageCalculator.isValidPageSize(-1), "음수 페이지 크기는 무효해야 함")
        assertFalse(PageCalculator.isValidPageSize(101), "페이지 크기 101은 무효해야 함 (기본 최대값 100)")
    }

    @Test
    @DisplayName("페이지 크기 유효성 검증: 커스텀 최대값 적용")
    fun `페이지 크기 유효성 검증 - 커스텀 최대값`() {
        // 최대 크기를 50으로 설정
        assertTrue(PageCalculator.isValidPageSize(50, maxSize = 50), "최대값이 50이면 50은 유효해야 함")
        assertFalse(PageCalculator.isValidPageSize(51, maxSize = 50), "최대값이 50이면 51은 무효해야 함")
    }

    @Test
    @DisplayName("페이징 파라미터 검증: 유효한 파라미터는 예외를 발생시키지 않음")
    fun `페이징 파라미터 검증 - 유효한 파라미터`() {
        // Given
        val page = 1
        val size = 20

        // When & Then: 예외가 발생하지 않아야 함
        assertDoesNotThrow {
            PageCalculator.validatePageParameters(page, size)
        }
    }

    @Test
    @DisplayName("페이징 파라미터 검증: page가 1 미만이면 InvalidParameterException 발생")
    fun `페이징 파라미터 검증 - page가 1 미만`() {
        // Given
        val invalidPage = 0
        val size = 20

        // When & Then
        val exception = assertThrows<InvalidParameterException> {
            PageCalculator.validatePageParameters(invalidPage, size)
        }
        assertTrue(exception.message!!.contains("페이지 번호는 1 이상이어야 합니다"))
        assertTrue(exception.message!!.contains("입력값: 0"))
    }

    @Test
    @DisplayName("페이징 파라미터 검증: page가 음수이면 InvalidParameterException 발생")
    fun `페이징 파라미터 검증 - page가 음수`() {
        // Given
        val invalidPage = -1
        val size = 20

        // When & Then
        val exception = assertThrows<InvalidParameterException> {
            PageCalculator.validatePageParameters(invalidPage, size)
        }
        assertTrue(exception.message!!.contains("페이지 번호는 1 이상이어야 합니다"))
        assertTrue(exception.message!!.contains("입력값: -1"))
    }

    @Test
    @DisplayName("페이징 파라미터 검증: size가 1 미만이면 InvalidParameterException 발생")
    fun `페이징 파라미터 검증 - size가 1 미만`() {
        // Given
        val page = 1
        val invalidSize = 0

        // When & Then
        val exception = assertThrows<InvalidParameterException> {
            PageCalculator.validatePageParameters(page, invalidSize)
        }
        assertTrue(exception.message!!.contains("페이지 크기는 1 이상이어야 합니다"))
        assertTrue(exception.message!!.contains("입력값: 0"))
    }

    @Test
    @DisplayName("페이징 파라미터 검증: size가 최대값 초과이면 InvalidParameterException 발생")
    fun `페이징 파라미터 검증 - size가 최대값 초과`() {
        // Given
        val page = 1
        val invalidSize = 101

        // When & Then
        val exception = assertThrows<InvalidParameterException> {
            PageCalculator.validatePageParameters(page, invalidSize)
        }
        assertTrue(exception.message!!.contains("페이지 크기는 100 이하여야 합니다"))
        assertTrue(exception.message!!.contains("입력값: 101"))
    }

    @Test
    @DisplayName("페이징 파라미터 검증: 커스텀 최대값 적용")
    fun `페이징 파라미터 검증 - 커스텀 최대값`() {
        // Given
        val page = 1
        val size = 51
        val maxSize = 50

        // When & Then
        val exception = assertThrows<InvalidParameterException> {
            PageCalculator.validatePageParameters(page, size, maxSize)
        }
        assertTrue(exception.message!!.contains("페이지 크기는 50 이하여야 합니다"))
        assertTrue(exception.message!!.contains("입력값: 51"))
    }
}
