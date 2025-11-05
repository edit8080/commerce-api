package com.beanbliss.domain.user.controller

import com.beanbliss.domain.user.dto.BalanceResponse
import com.beanbliss.domain.user.dto.ChargeBalanceRequest
import com.beanbliss.domain.user.dto.ChargeBalanceResponse
import com.beanbliss.domain.user.usecase.ChargeBalanceUseCase
import com.beanbliss.domain.user.usecase.GetBalanceUseCase
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * [책임]: 사용자 관련 API 엔드포인트
 * - 잔액 조회
 * - 잔액 충전
 *
 * [아키텍처]:
 * - Controller → UseCase (멀티 도메인 오케스트레이션)
 */
@RestController
@RequestMapping("/api/users")
class UserController(
    private val getBalanceUseCase: GetBalanceUseCase,
    private val chargeBalanceUseCase: ChargeBalanceUseCase
) {

    /**
     * 사용자 잔액 조회 API
     *
     * @param userId 조회할 사용자 ID
     * @return 잔액 정보 (data envelope 형태)
     */
    @GetMapping("/{userId}/balance")
    fun getBalance(@PathVariable userId: Long): ResponseEntity<Map<String, BalanceResponse>> {
        // UseCase 계층에 위임
        val balance = getBalanceUseCase.getBalance(userId)

        // data envelope 형태로 응답 반환
        return ResponseEntity.ok(mapOf("data" to balance))
    }

    /**
     * 사용자 잔액 충전 API
     *
     * @param userId 사용자 ID
     * @param request 충전 요청 (충전 금액)
     * @return 충전 결과 (data envelope 형태)
     */
    @PostMapping("/{userId}/balance/charge")
    fun chargeBalance(
        @PathVariable userId: Long,
        @Valid @RequestBody request: ChargeBalanceRequest
    ): ResponseEntity<Map<String, ChargeBalanceResponse>> {
        // UseCase 계층에 위임
        val result = chargeBalanceUseCase.chargeBalance(userId, request.chargeAmount)

        // data envelope 형태로 응답 반환
        return ResponseEntity.ok(mapOf("data" to result))
    }
}
