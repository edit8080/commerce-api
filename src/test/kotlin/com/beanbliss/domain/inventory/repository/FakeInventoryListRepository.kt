package com.beanbliss.domain.inventory.repository

import com.beanbliss.domain.inventory.dto.InventoryResponse

/**
 * FakeInventoryListRepository 구현체 (테스트용)
 *
 * [책임]:
 * - 재고 목록 조회 기능 테스트를 위한 In-Memory 구현
 * - findAllWithProductInfo: 페이징, 정렬 지원
 * - count: 전체 재고 개수 반환
 */
class FakeInventoryListRepository : InventoryRepository {

    // 재고 목록 저장소
    private val inventories = mutableListOf<InventoryResponse>()

    /**
     * 테스트 데이터 추가용 헬퍼 메서드
     */
    fun addInventory(inventory: InventoryResponse) {
        inventories.add(inventory)
    }

    /**
     * 테스트 데이터 초기화
     */
    fun clear() {
        inventories.clear()
    }

    /**
     * 재고 목록 조회 (페이징, 정렬 지원)
     *
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @param sortBy 정렬 기준 (예: "created_at")
     * @param sortDirection 정렬 방향 ("ASC" 또는 "DESC")
     * @return 재고 목록
     */
    fun findAllWithProductInfo(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): List<InventoryResponse> {
        // 1. 정렬
        val sorted = when (sortBy) {
            "created_at" -> {
                if (sortDirection == "DESC") {
                    inventories.sortedByDescending { it.createdAt }
                } else {
                    inventories.sortedBy { it.createdAt }
                }
            }
            else -> inventories
        }

        // 2. 페이징 (offset 계산: (page - 1) * size)
        val offset = (page - 1) * size
        return sorted.drop(offset).take(size)
    }

    /**
     * 전체 재고 개수 조회
     *
     * @return 전체 재고 개수
     */
    fun count(): Long {
        return inventories.size.toLong()
    }

    // InventoryRepository 인터페이스의 기존 메서드 구현 (현재는 사용하지 않음)
    override fun calculateAvailableStock(productOptionId: Long): Int {
        // 목록 조회 테스트에서는 사용하지 않음
        return 0
    }

    override fun calculateAvailableStockBatch(productOptionIds: List<Long>): Map<Long, Int> {
        // 목록 조회 테스트에서는 사용하지 않음
        return emptyMap()
    }
}
