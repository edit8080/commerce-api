package com.beanbliss.domain.cart.controller

import com.beanbliss.domain.cart.dto.AddToCartRequest
import com.beanbliss.domain.cart.dto.CartItemResponse
import com.beanbliss.domain.cart.usecase.AddToCartUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * [책임]: 장바구니 API 엔드포인트 제공
 * - 클라이언트 요청의 유효성 검사 (@Valid)
 * - UseCase 계층에 위임
 * - Repository JOIN DTO → Presentation DTO 변환
 * - 적절한 HTTP 상태 코드 반환 (201 Created, 200 OK)
 *
 * [주요 API]:
 * - POST /api/cart/items: 장바구니에 상품 추가
 */
@RestController
@RequestMapping("/api/cart")
class CartController(
    // DIP 준수: UseCase에 의존
    private val addToCartUseCase: AddToCartUseCase
) {

    /**
     * 장바구니에 상품 추가
     *
     * [HTTP Status]:
     * - 201 Created: 신규 장바구니 아이템 추가
     * - 200 OK: 기존 장바구니 아이템의 수량 증가
     * - 400 Bad Request: 유효성 검사 실패 또는 최대 수량 초과
     * - 404 Not Found: 사용자 또는 상품 옵션 없음
     */
    @PostMapping("/items")
    fun addToCart(
        @Valid @RequestBody request: AddToCartRequest
    ): ResponseEntity<Map<String, CartItemResponse>> {

        // 1. UseCase 계층에 위임
        val result = addToCartUseCase.addToCart(request)

        // 2. Repository JOIN DTO → Presentation DTO 변환 (Controller 책임)
        val cartItemResponse = CartItemResponse(
            cartItemId = result.cartItem.cartItemId,
            productOptionId = result.cartItem.productOptionId,
            productName = result.cartItem.productName,
            optionCode = result.cartItem.optionCode,
            origin = result.cartItem.origin,
            grindType = result.cartItem.grindType,
            weightGrams = result.cartItem.weightGrams,
            price = result.cartItem.price,
            quantity = result.cartItem.quantity,
            totalPrice = result.cartItem.totalPrice,
            createdAt = result.cartItem.createdAt,
            updatedAt = result.cartItem.updatedAt
        )

        // 3. 응답 래핑: {"data": {...}}
        val response = mapOf("data" to cartItemResponse)

        // 4. 신규 추가 vs 수량 증가에 따라 HTTP 상태 코드 결정
        return if (result.isNewItem) {
            ResponseEntity.status(HttpStatus.CREATED).body(response) // 201 Created
        } else {
            ResponseEntity.ok(response) // 200 OK
        }
    }
}
