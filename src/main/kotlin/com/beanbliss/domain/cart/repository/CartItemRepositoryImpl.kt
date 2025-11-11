package com.beanbliss.domain.cart.repository

import com.beanbliss.domain.cart.domain.CartItem
import com.beanbliss.domain.cart.entity.CartItemEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * [책임]: Spring Data JPA를 활용한 CartItem 영속성 처리
 * Infrastructure Layer에 속하며, JPA 기술에 종속적
 *
 * [설계 변경]:
 * - 도메인 간 JOIN 제거: CART_ITEM 테이블만 조회
 * - PRODUCT_OPTION, PRODUCT와의 JOIN 제거
 */
interface CartItemJpaRepository : JpaRepository<CartItemEntity, Long> {
    /**
     * 사용자 ID로 장바구니 아이템 조회 (CART_ITEM만)
     *
     * @param userId 사용자 ID
     * @return CartItemEntity 리스트
     */
    fun findByUserId(userId: Long): List<CartItemEntity>

    /**
     * 사용자 ID와 상품 옵션 ID로 장바구니 아이템 조회 (CART_ITEM만)
     *
     * @param userId 사용자 ID
     * @param productOptionId 상품 옵션 ID
     * @return CartItemEntity (없으면 null)
     */
    fun findByUserIdAndProductOptionId(userId: Long, productOptionId: Long): CartItemEntity?

    /**
     * 사용자의 모든 장바구니 아이템 삭제
     */
    @Modifying
    @Query("DELETE FROM CartItemEntity c WHERE c.userId = :userId")
    fun deleteByUserId(@Param("userId") userId: Long)
}

/**
 * [책임]: CartItemRepository 인터페이스 구현체
 * - CartItemJpaRepository를 활용하여 실제 DB 접근
 * - CART_ITEM 테이블만 조회 (도메인 간 JOIN 제거)
 * - Entity ↔ Domain Model 변환 담당
 */
@Repository
class CartItemRepositoryImpl(
    private val cartItemJpaRepository: CartItemJpaRepository
) : CartItemRepository {

    override fun findByUserId(userId: Long): List<CartItem> {
        // CART_ITEM만 조회 (JOIN 제거)
        val entities = cartItemJpaRepository.findByUserId(userId)
        return entities.map { it.toDomain() }
    }

    override fun findByUserIdAndProductOptionId(userId: Long, productOptionId: Long): CartItem? {
        // CART_ITEM만 조회 (JOIN 제거)
        val entity = cartItemJpaRepository.findByUserIdAndProductOptionId(userId, productOptionId)
            ?: return null
        return entity.toDomain()
    }

    override fun findById(cartItemId: Long): CartItem? {
        val entity = cartItemJpaRepository.findById(cartItemId).orElse(null)
            ?: return null
        return entity.toDomain()
    }

    override fun save(cartItem: CartItem): CartItem {
        val entity = cartItem.toEntity()
        val savedEntity = cartItemJpaRepository.save(entity)
        return savedEntity.toDomain()
    }

    @Transactional
    override fun deleteByUserId(userId: Long) {
        cartItemJpaRepository.deleteByUserId(userId)
    }
}

/**
 * CartItemEntity ↔ CartItem 변환 확장 함수
 */
private fun CartItemEntity.toDomain(): CartItem {
    return CartItem(
        id = this.id,
        userId = this.userId,
        productOptionId = this.productOptionId,
        quantity = this.quantity,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

private fun CartItem.toEntity(): CartItemEntity {
    return CartItemEntity(
        id = this.id,
        userId = this.userId,
        productOptionId = this.productOptionId,
        quantity = this.quantity,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
