package com.beanbliss.domain.inventory.service

import com.beanbliss.common.dto.PageableResponse
import com.beanbliss.common.exception.InvalidPageNumberException
import com.beanbliss.common.exception.InvalidPageSizeException
import com.beanbliss.common.pagination.PageCalculator
import com.beanbliss.domain.inventory.dto.InventoryListResponse
import com.beanbliss.domain.inventory.repository.InventoryRepository
import org.springframework.stereotype.Service

/**
 * [책임]: 재고 목록 조회 비즈니스 로직 구현
 *
 * [주요 기능]:
 * 1. 파라미터 유효성 검증
 * 2. Repository 호출 및 결과 조립
 * 3. 페이지 정보 계산
 *
 * [DIP 준수]:
 * - InventoryRepository Interface에만 의존
 */
@Service
class InventoryServiceImpl(
    private val inventoryRepository: InventoryRepository
) : InventoryService {

    override fun getInventories(page: Int, size: Int): InventoryListResponse {
        // 1. 파라미터 유효성 검증
        validatePageNumber(page)
        validatePageSize(size)

        // 2. 재고 목록 조회 (created_at DESC 정렬)
        val inventories = inventoryRepository.findAllWithProductInfo(
            page = page,
            size = size,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // 3. 전체 재고 개수 조회
        val totalElements = inventoryRepository.count()

        // 4. 전체 페이지 수 계산 (공통 유틸리티 사용)
        val totalPages = PageCalculator.calculateTotalPages(totalElements, size)

        // 5. 응답 데이터 조립
        return InventoryListResponse(
            content = inventories,
            pageable = PageableResponse(
                pageNumber = page,
                pageSize = size,
                totalElements = totalElements,
                totalPages = totalPages
            )
        )
    }

    /**
     * 페이지 번호 유효성 검증
     *
     * @throws InvalidPageNumberException 페이지 번호가 1 미만인 경우
     */
    private fun validatePageNumber(page: Int) {
        if (page < 1) {
            throw InvalidPageNumberException("페이지 번호는 1 이상이어야 합니다.")
        }
    }

    /**
     * 페이지 크기 유효성 검증
     *
     * @throws InvalidPageSizeException 페이지 크기가 1 미만이거나 100 초과인 경우
     */
    private fun validatePageSize(size: Int) {
        if (size < 1 || size > 100) {
            throw InvalidPageSizeException("페이지 크기는 1 이상 100 이하여야 합니다.")
        }
    }
}
