package com.beanbliss.domain.cart.repository

import com.beanbliss.domain.cart.entity.CartItemEntity
import com.beanbliss.domain.product.entity.ProductEntity
import com.beanbliss.domain.product.entity.ProductOptionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * [책임]: Spring Data JPA를 활용한 CartItem 영속성 처리
 * Infrastructure Layer에 속하며, JPA 기술에 종속적
 */
interface CartItemJpaRepository : JpaRepository<CartItemEntity, Long> {
    /**
     * 사용자 ID로 장바구니 아이템 조회 (PRODUCT_OPTION, PRODUCT와 JOIN)
     * N+1 문제 방지를 위한 단일 쿼리
     *
     * @return List<Array<Any>> = [[CartItemEntity, ProductOptionEntity, ProductEntity], ...]
     */
    @Query("""
        SELECT c, po, p
        FROM CartItemEntity c
        INNER JOIN ProductOptionEntity po ON c.productOptionId = po.id
        INNER JOIN ProductEntity p ON po.productId = p.id
        WHERE c.userId = :userId
    """)
    fun findByUserIdWithProductInfo(@Param("userId") userId: Long): List<Array<Any>>

    /**
     * 장바구니 아이템 ID로 조회 (PRODUCT_OPTION, PRODUCT와 JOIN)
     *
     * @return Array<Any> = [CartItemEntity, ProductOptionEntity, ProductEntity]
     */
    @Query("""
        SELECT c, po, p
        FROM CartItemEntity c
        INNER JOIN ProductOptionEntity po ON c.productOptionId = po.id
        INNER JOIN ProductEntity p ON po.productId = p.id
        WHERE c.id = :cartItemId
    """)
    fun findByIdWithProductInfo(@Param("cartItemId") cartItemId: Long): Array<Any>?

    /**
     * 사용자 ID와 상품 옵션 ID로 장바구니 아이템 조회 (PRODUCT_OPTION, PRODUCT와 JOIN)
     *
     * @return Array<Any> = [CartItemEntity, ProductOptionEntity, ProductEntity]
     */
    @Query("""
        SELECT c, po, p
        FROM CartItemEntity c
        INNER JOIN ProductOptionEntity po ON c.productOptionId = po.id
        INNER JOIN ProductEntity p ON po.productId = p.id
        WHERE c.userId = :userId AND c.productOptionId = :productOptionId
    """)
    fun findByUserIdAndProductOptionIdWithProductInfo(
        @Param("userId") userId: Long,
        @Param("productOptionId") productOptionId: Long
    ): Array<Any>?

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
 * - PRODUCT_OPTION, PRODUCT와 JOIN하여 상세 정보 제공 (단일 쿼리)
 */
@Repository
class CartItemRepositoryImpl(
    private val cartItemJpaRepository: CartItemJpaRepository
) : CartItemRepository {

    override fun findByUserId(userId: Long): List<CartItemDetail> {
        // CART_ITEM, PRODUCT_OPTION, PRODUCT를 JOIN하여 단일 쿼리로 조회
        val results = cartItemJpaRepository.findByUserIdWithProductInfo(userId)

        return results.map { row -> mapToCartItemDetail(row) }
    }

    override fun findByUserIdAndProductOptionId(
        userId: Long,
        productOptionId: Long
    ): CartItemDetail? {
        // CART_ITEM, PRODUCT_OPTION, PRODUCT를 JOIN하여 단일 쿼리로 조회
        val result = cartItemJpaRepository.findByUserIdAndProductOptionIdWithProductInfo(userId, productOptionId)
            ?: return null

        return mapToCartItemDetail(result)
    }

    override fun save(cartItem: CartItemDetail, userId: Long): CartItemDetail {
        val now = java.time.LocalDateTime.now()

        // 기존 아이템이 있는지 확인
        val existingEntity = if (cartItem.cartItemId != 0L) {
            cartItemJpaRepository.findById(cartItem.cartItemId).orElse(null)
        } else {
            null
        }

        val entity = CartItemEntity(
            id = cartItem.cartItemId,
            userId = userId,
            productOptionId = cartItem.productOptionId,
            quantity = cartItem.quantity,
            createdAt = existingEntity?.createdAt ?: now,
            updatedAt = now
        )

        val savedEntity = cartItemJpaRepository.save(entity)

        // PRODUCT_OPTION, PRODUCT와 JOIN하여 정보 조회 (단일 쿼리)
        val result = cartItemJpaRepository.findByIdWithProductInfo(savedEntity.id)
            ?: throw IllegalStateException("장바구니 아이템 저장 후 조회 실패: ${savedEntity.id}")

        return mapToCartItemDetail(result)
    }

    @Transactional
    override fun updateQuantity(cartItemId: Long, newQuantity: Int): CartItemDetail {
        val entity = cartItemJpaRepository.findById(cartItemId).orElseThrow {
            IllegalArgumentException("장바구니 아이템을 찾을 수 없습니다. ID: $cartItemId")
        }

        // 수량 업데이트
        val updatedEntity = CartItemEntity(
            id = entity.id,
            userId = entity.userId,
            productOptionId = entity.productOptionId,
            quantity = newQuantity,
            createdAt = entity.createdAt,
            updatedAt = java.time.LocalDateTime.now()
        )

        val savedEntity = cartItemJpaRepository.save(updatedEntity)

        // PRODUCT_OPTION, PRODUCT와 JOIN하여 정보 조회 (단일 쿼리)
        val result = cartItemJpaRepository.findByIdWithProductInfo(savedEntity.id)
            ?: throw IllegalStateException("장바구니 아이템 업데이트 후 조회 실패: ${savedEntity.id}")

        return mapToCartItemDetail(result)
    }

    /**
     * JPA 쿼리 결과를 CartItemDetail로 변환하는 헬퍼 메서드
     *
     * @param row Array<Any> = [CartItemEntity, ProductOptionEntity, ProductEntity]
     * @return CartItemDetail
     */
    private fun mapToCartItemDetail(row: Array<Any>): CartItemDetail {
        val cartItem = row[0] as CartItemEntity
        val productOption = row[1] as ProductOptionEntity
        val product = row[2] as ProductEntity

        return CartItemDetail(
            cartItemId = cartItem.id,
            productOptionId = cartItem.productOptionId,
            productName = product.name,
            optionCode = productOption.optionCode,
            origin = productOption.origin,
            grindType = productOption.grindType,
            weightGrams = productOption.weightGrams,
            price = productOption.price.toInt(),
            quantity = cartItem.quantity,
            totalPrice = productOption.price.toInt() * cartItem.quantity,
            createdAt = cartItem.createdAt,
            updatedAt = cartItem.updatedAt
        )
    }

    @Transactional
    override fun deleteByUserId(userId: Long) {
        cartItemJpaRepository.deleteByUserId(userId)
    }
}
