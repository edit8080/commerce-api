package com.beanbliss.domain.user.controller

import com.beanbliss.common.dto.ApiResponse
import com.beanbliss.domain.user.dto.BalanceResponse
import com.beanbliss.domain.user.dto.ChargeBalanceRequest
import com.beanbliss.domain.user.dto.ChargeBalanceResponse
import com.beanbliss.domain.user.usecase.ChargeBalanceUseCase
import com.beanbliss.domain.user.usecase.GetBalanceUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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
@Tag(name = "사용자 관리", description = "사용자 잔액 관리 API")
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
    @Operation(summary = "잔액 조회", description = "사용자의 현재 잔액 조회")
    fun getBalance(
        @Parameter(description = "사용자 ID", example = "1")
        @PathVariable userId: Long
    ): ApiResponse<BalanceResponse> {
        // 1. UseCase 계층에 위임 (Service DTO 반환)
        val balanceInfo = getBalanceUseCase.getBalance(userId)

        // 2. Service DTO → Response DTO 변환 (Controller 책임)
        val response = if (balanceInfo != null) {
            BalanceResponse(
                userId = balanceInfo.userId,
                amount = balanceInfo.amount,
                lastUpdatedAt = balanceInfo.updatedAt
            )
        } else {
            // 레코드 없으면 0원 반환
            BalanceResponse(
                userId = userId,
                amount = 0,
                lastUpdatedAt = null
            )
        }

        // 3. ApiResponse 형태로 응답 반환
        return ApiResponse(data = response)
    }

    /**
     * 사용자 잔액 충전 API
     *
     * @param userId 사용자 ID
     * @param request 충전 요청 (충전 금액)
     * @return 충전 결과 (data envelope 형태)
     */
    @PostMapping("/{userId}/balance/charge")
    @Operation(summary = "잔액 충전", description = "사용자 잔액을 충전")
    fun chargeBalance(
        @Parameter(description = "사용자 ID", example = "1")
        @PathVariable userId: Long,
        @Valid @RequestBody request: ChargeBalanceRequest
    ): ApiResponse<ChargeBalanceResponse> {
        // 1. UseCase 계층에 위임 (Service DTO 반환)
        val balanceInfo = chargeBalanceUseCase.chargeBalance(userId, request.chargeAmount)

        // 2. Service DTO → Response DTO 변환 (Controller 책임)
        val response = ChargeBalanceResponse(
            userId = balanceInfo.userId,
            currentBalance = balanceInfo.amount,
            chargedAt = balanceInfo.updatedAt
        )

        // 3. ApiResponse 형태로 응답 반환
        return ApiResponse(data = response)
    }
}
