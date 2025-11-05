package com.beanbliss.domain.inventory.dto

import com.beanbliss.common.dto.PageableResponse

/**
 * 재고 목록 조회 응답 DTO
 *
 * [책임]: 재고 목록 + 페이징 정보를 클라이언트에 전달
 */
data class InventoryListResponse(
    val content: List<InventoryResponse>,  // 재고 목록
    val pageable: PageableResponse         // 페이징 정보
)
