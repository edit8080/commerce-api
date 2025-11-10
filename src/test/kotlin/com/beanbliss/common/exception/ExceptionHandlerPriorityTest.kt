package com.beanbliss.common.exception

import com.beanbliss.common.dto.ErrorResponse
import com.beanbliss.domain.inventory.exception.InsufficientStockException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * ExceptionHandler 우선순위 및 충돌 방지를 검증하는 통합 테스트
 *
 * 검증 목표:
 * 1. 도메인별 핸들러가 자신의 예외를 올바르게 처리하는가?
 * 2. CommonExceptionHandler의 Exception 핸들러가 다른 핸들러와 충돌하지 않는가?
 * 3. @Order 우선순위가 올바르게 적용되는가?
 */
@WebMvcTest(TestController::class)
@Import(CommonExceptionHandler::class, com.beanbliss.domain.inventory.exception.InventoryExceptionHandler::class)
@DisplayName("ExceptionHandler 우선순위 테스트")
class ExceptionHandlerPriorityTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    @DisplayName("InsufficientStockException은 InventoryExceptionHandler에서 처리되어야 함")
    fun `InsufficientStockException은 도메인 핸들러에서 처리`() {
        // When & Then
        mockMvc.perform(get("/test/insufficient-stock"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"))
            .andExpect(jsonPath("$.message").value("재고가 부족합니다"))
    }

    @Test
    @DisplayName("InvalidParameterException은 CommonExceptionHandler에서 처리되어야 함")
    fun `InvalidParameterException은 공통 핸들러에서 처리`() {
        // When & Then
        mockMvc.perform(get("/test/invalid-parameter"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
            .andExpect(jsonPath("$.message").value("잘못된 파라미터입니다"))
    }

    @Test
    @DisplayName("ResourceNotFoundException은 CommonExceptionHandler에서 처리되어야 함")
    fun `ResourceNotFoundException은 공통 핸들러에서 처리`() {
        // When & Then
        mockMvc.perform(get("/test/resource-not-found"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("리소스를 찾을 수 없습니다"))
    }

    @Test
    @DisplayName("처리되지 않은 예외는 CommonExceptionHandler의 Exception 핸들러로 fallback")
    fun `처리되지 않은 예외는 공통 Exception 핸들러로 fallback`() {
        // When & Then
        mockMvc.perform(get("/test/unknown-exception"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
            .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."))
    }
}

/**
 * 테스트용 Controller - 다양한 예외를 발생시킴
 */
@RestController
class TestController {

    @GetMapping("/test/insufficient-stock")
    fun insufficientStock() {
        throw InsufficientStockException("재고가 부족합니다")
    }

    @GetMapping("/test/invalid-parameter")
    fun invalidParameter() {
        throw InvalidParameterException("잘못된 파라미터입니다")
    }

    @GetMapping("/test/resource-not-found")
    fun resourceNotFound() {
        throw ResourceNotFoundException("리소스를 찾을 수 없습니다")
    }

    @GetMapping("/test/unknown-exception")
    fun unknownException() {
        throw RuntimeException("알 수 없는 오류")
    }
}
