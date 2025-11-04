package com.beanbliss.domain.user.controller

import com.beanbliss.domain.user.dto.BalanceResponse
import com.beanbliss.domain.user.service.BalanceService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * [책임]: 사용자 관련 API 엔드포인트
 * - 잔액 조회
 */
@RestController
@RequestMapping("/api/users")
class UserController(
    private val balanceService: BalanceService
) {

    /**
     * 사용자 잔액 조회 API
     *
     * @param userId 조회할 사용자 ID
     * @return 잔액 정보 (data envelope 형태)
     */
    @GetMapping("/{userId}/balance")
    fun getBalance(@PathVariable userId: Long): ResponseEntity<Map<String, BalanceResponse>> {
        // Service 계층에 위임
        val balance = balanceService.getBalance(userId)

        // data envelope 형태로 응답 반환
        return ResponseEntity.ok(mapOf("data" to balance))
    }
}
