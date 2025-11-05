package com.beanbliss.domain.inventory.domain

import com.beanbliss.domain.inventory.exception.MaxStockExceededException

/**
 * 재고 도메인 모델
 *
 * [책임]:
 * - 재고 수량의 상태 관리
 * - 재고 추가/감소 비즈니스 규칙 수행
 * - 최대 재고 수량 검증
 *
 * SOLID - SRP: 재고 상태 변경에 대한 책임만 가짐
 */
data class Inventory(
    val productOptionId: Long,  // 상품 옵션 ID (SKU 기반)
    var stockQuantity: Int       // 재고 수량
) {
    companion object {
        const val MAX_STOCK_QUANTITY = 1_000_000 // 최대 재고 수량 (백만 개)
    }

    /**
     * 재고를 추가하는 비즈니스 로직
     *
     * @param quantity 추가할 재고 수량
     * @return 추가 후 현재 재고 수량
     * @throws IllegalArgumentException 추가 수량이 1 미만인 경우
     * @throws MaxStockExceededException 최대 재고 수량을 초과하는 경우
     */
    fun addStock(quantity: Int): Int {
        require(quantity > 0) { "추가 수량은 1 이상이어야 합니다." }

        // 최대 재고 수량 검증
        val newStock = this.stockQuantity + quantity
        if (newStock > MAX_STOCK_QUANTITY) {
            throw MaxStockExceededException(
                "재고 추가 후 총 수량이 최대 허용량(${MAX_STOCK_QUANTITY}개)을 초과합니다. " +
                "현재: ${this.stockQuantity}, 추가 요청: $quantity"
            )
        }

        this.stockQuantity = newStock
        return this.stockQuantity // 현재 재고 반환
    }
}
