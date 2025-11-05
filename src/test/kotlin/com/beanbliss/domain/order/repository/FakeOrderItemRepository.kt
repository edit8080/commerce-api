package com.beanbliss.domain.order.repository

import com.beanbliss.domain.order.dto.ProductOrderCount
import com.beanbliss.domain.order.entity.OrderItemEntity
import java.time.LocalDateTime

/**
 * 테스트용 In-Memory OrderItemRepository 구현체
 *
 * [특징]:
 * - 실제 DB 없이 메모리 기반으로 동작
 * - Repository 인터페이스의 계약을 준수
 * - 테스트 격리를 위해 각 테스트마다 새 인스턴스 생성
 */
class FakeOrderItemRepository : OrderItemRepository {

    private val orderItems = mutableMapOf<Long, OrderItemEntity>()
    private var nextId = 1L

    // 테스트를 위한 추가 데이터 저장소
    // orderId -> createdAt 매핑 (ORDER 테이블의 ordered_at)
    private val orderCreatedAtMap = mutableMapOf<Long, LocalDateTime>()
    // productOptionId -> productId 매핑 (PRODUCT_OPTION 테이블의 product_id)
    private val productOptionToProductMap = mutableMapOf<Long, Long>()

    override fun saveAll(orderItems: List<OrderItemEntity>): List<OrderItemEntity> {
        return orderItems.map { orderItem ->
            val id = if (orderItem.id == 0L) nextId++ else orderItem.id
            val savedOrderItem = orderItem.copy(id = id)
            this.orderItems[id] = savedOrderItem
            savedOrderItem
        }
    }

    override fun findTopOrderedProducts(startDate: LocalDateTime, limit: Int): List<ProductOrderCount> {
        // 1. startDate 이후의 주문 항목만 필터링 (orderId를 통해 createdAt 확인)
        val filteredItems = orderItems.values.filter { orderItem ->
            val createdAt = orderCreatedAtMap[orderItem.orderId]
            createdAt != null && createdAt >= startDate
        }

        // 2. productOptionId를 productId로 변환하고 그룹화
        val itemsWithProductId = filteredItems.mapNotNull { orderItem ->
            val productId = productOptionToProductMap[orderItem.productOptionId]
            productId?.let { orderItem to it }
        }

        // 3. productId별로 그룹화하고 수량 합산
        val productOrderCounts = itemsWithProductId
            .groupBy { it.second } // productId로 그룹화
            .map { (productId, items) ->
                ProductOrderCount(
                    productId = productId,
                    totalOrderCount = items.sumOf { it.first.quantity }
                )
            }
            .sortedByDescending { it.totalOrderCount } // 주문 수량 내림차순 정렬
            .take(limit) // 상위 limit개만 반환

        return productOrderCounts
    }

    // === Test Helper Methods ===

    /**
     * 테스트용: 모든 데이터 삭제
     */
    fun clear() {
        orderItems.clear()
        orderCreatedAtMap.clear()
        productOptionToProductMap.clear()
        nextId = 1L
    }

    /**
     * 테스트용: 주문 생성 시각 설정
     */
    fun setOrderCreatedAt(orderId: Long, createdAt: LocalDateTime) {
        orderCreatedAtMap[orderId] = createdAt
    }

    /**
     * 테스트용: 상품 옵션 ID -> 상품 ID 매핑 설정
     */
    fun setProductOptionToProduct(productOptionId: Long, productId: Long) {
        productOptionToProductMap[productOptionId] = productId
    }

    /**
     * 테스트용: ID로 주문 항목 조회
     */
    fun findById(id: Long): OrderItemEntity? {
        return orderItems[id]
    }

    /**
     * 테스트용: 모든 주문 항목 조회
     */
    fun findAll(): List<OrderItemEntity> {
        return orderItems.values.toList()
    }

    /**
     * 테스트용: 주문 ID로 주문 항목 목록 조회
     */
    fun findByOrderId(orderId: Long): List<OrderItemEntity> {
        return orderItems.values.filter { it.orderId == orderId }
    }
}
