package com.beanbliss.domain.product.repository

import com.beanbliss.domain.product.entity.ProductOptionEntity
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * [책임]: 상품 옵션 In-memory 저장소 구현
 * - ConcurrentHashMap을 사용하여 Thread-safe 보장
 * - 활성 상태(is_active=true)의 옵션만 조회 가능
 *
 * [주의사항]:
 * - 애플리케이션 재시작 시 데이터 소실 (In-memory 특성)
 * - 실제 DB 사용 시 JPA 기반 구현체로 교체 필요
 */
@Repository
class ProductOptionRepositoryImpl : ProductOptionRepository {

    // Thread-safe한 In-memory 저장소
    private val productOptions = ConcurrentHashMap<Long, ProductOptionEntity>()

    override fun findActiveOptionWithProduct(productOptionId: Long): ProductOptionDetail? {
        return productOptions[productOptionId]
            ?.takeIf { it.isActive } // is_active = true인 것만 반환
            ?.toDetail()
    }

    /**
     * 테스트용 헬퍼 메서드: 상품 옵션 추가
     */
    fun add(entity: ProductOptionEntity) {
        productOptions[entity.id] = entity
    }

    /**
     * 테스트용 헬퍼 메서드: 모든 데이터 삭제
     */
    fun clear() {
        productOptions.clear()
    }
}
