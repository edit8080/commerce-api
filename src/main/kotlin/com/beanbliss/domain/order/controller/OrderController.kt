package com.beanbliss.domain.order.controller

import com.beanbliss.domain.order.dto.ReserveOrderRequest
import com.beanbliss.domain.order.dto.ReserveOrderResponse
import com.beanbliss.domain.order.usecase.ReserveOrderUseCase
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * [책임]: 주문 관련 API 엔드포인트
 * - 주문 예약 (재고 예약)
 */
@RestController
@RequestMapping("/api/order")
class OrderController(
    private val reserveOrderUseCase: ReserveOrderUseCase
) {

    /**
     * 주문 예약 API (재고 예약)
     *
     * @param request 예약 요청 (사용자 ID)
     * @return 예약 결과 (data envelope 형태)
     */
    @PostMapping("/reserve")
    fun reserveOrder(
        @Valid @RequestBody request: ReserveOrderRequest
    ): ResponseEntity<Map<String, ReserveOrderResponse>> {
        // UseCase에 위임
        val result = reserveOrderUseCase.reserveOrder(request.userId)

        // data envelope 형태로 응답 반환
        return ResponseEntity.ok(mapOf("data" to result))
    }
}
