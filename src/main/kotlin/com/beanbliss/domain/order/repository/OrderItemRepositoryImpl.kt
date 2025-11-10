package com.beanbliss.domain.order.repository

import com.beanbliss.domain.order.dto.ProductOrderCount
import com.beanbliss.domain.order.entity.OrderItemEntity
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * [책임]: OrderItem In-memory 저장소 구현
 * - ConcurrentHashMap을 사용하여 Thread-safe 보장
 * - ORDER_ITEM 테이블 관리
 *
 * [주의사항]:
 * - 애플리케이션 재시작 시 데이터 소실 (In-memory 특성)
 * - 실제 DB 사용 시 JPA 기반 구현체로 교체 필요
 */
@Repository
class OrderItemRepositoryImpl : OrderItemRepository {

    private val orderItems = ConcurrentHashMap<Long, OrderItemEntity>()
    private var nextId = 1L

    // 테스트를 위한 추가 데이터 저장소
    // orderId -> createdAt 매핑 (ORDER 테이블의 ordered_at)
    private val orderCreatedAtMap = ConcurrentHashMap<Long, LocalDateTime>()
    // productOptionId -> productId 매핑 (PRODUCT_OPTION 테이블의 product_id)
    private val productOptionToProductMap = ConcurrentHashMap<Long, Long>()

    override fun saveAll(orderItemEntities: List<OrderItemEntity>): List<OrderItemEntity> {
        return orderItemEntities.map { orderItem ->
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

    /**
     * 테스트용 헬퍼 메서드: 주문 생성 시각 설정
     */
    fun setOrderCreatedAt(orderId: Long, createdAt: LocalDateTime) {
        orderCreatedAtMap[orderId] = createdAt
    }

    /**
     * 테스트용 헬퍼 메서드: 상품 옵션 ID -> 상품 ID 매핑 설정
     */
    fun setProductOptionToProduct(productOptionId: Long, productId: Long) {
        productOptionToProductMap[productOptionId] = productId
    }

    /**
     * 테스트용 헬퍼 메서드: 모든 데이터 삭제
     */
    fun clear() {
        orderItems.clear()
        orderCreatedAtMap.clear()
        productOptionToProductMap.clear()
        nextId = 1L
    }
}
