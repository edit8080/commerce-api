package com.beanbliss.domain.inventory.usecase

import java.time.LocalDateTime

/**
 * [책임]: INVENTORY + PRODUCT UseCase 계층의 조합 DTO
 *
 * [설계 변경]:
 * - Repository 계층에서 UseCase 계층으로 이동
 * - Repository의 JOIN 제거, UseCase에서 데이터 조합
 *
 * [사용처]:
 * - GetInventoriesUseCase에서 INVENTORY + PRODUCT 도메인 데이터 조합
 */
data class InventoryDetail(
    val inventoryId: Long,          // 재고 ID (INVENTORY)
    val productId: Long,            // 상품 ID (PRODUCT)
    val productName: String,        // 상품명 (PRODUCT)
    val productOptionId: Long,      // 상품 옵션 ID (INVENTORY)
    val optionCode: String,         // 옵션 코드 (PRODUCT_OPTION)
    val optionName: String,         // 옵션명 (조합)
    val price: Int,                 // 가격 (PRODUCT_OPTION)
    val stockQuantity: Int,         // 재고 수량 (INVENTORY)
    val createdAt: LocalDateTime    // 재고 등록 시각 (INVENTORY)
)
