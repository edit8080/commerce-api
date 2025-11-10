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
     * 사용자 ID로 장바구니 아이템 조회
     */
    fun findByUserId(userId: Long): List<CartItemEntity>

    /**
     * 사용자 ID와 상품 옵션 ID로 장바구니 아이템 조회
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
 * - PRODUCT_OPTION, PRODUCT와 JOIN하여 상세 정보 제공
 */
@Repository
class CartItemRepositoryImpl(
    private val cartItemJpaRepository: CartItemJpaRepository,
    private val productOptionJpaRepository: JpaRepository<ProductOptionEntity, Long>,
    private val productJpaRepository: JpaRepository<ProductEntity, Long>
) : CartItemRepository {

    override fun findByUserId(userId: Long): List<CartItemDetail> {
        val cartItems = cartItemJpaRepository.findByUserId(userId)

        // PRODUCT_OPTION, PRODUCT와 JOIN하여 정보 조회
        return cartItems.mapNotNull { cartItem ->
            val productOption = productOptionJpaRepository.findById(cartItem.productOptionId).orElse(null)
                ?: return@mapNotNull null
            val product = productJpaRepository.findById(productOption.productId).orElse(null)
                ?: return@mapNotNull null

            CartItemDetail(
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
    }

    override fun findByUserIdAndProductOptionId(
        userId: Long,
        productOptionId: Long
    ): CartItemDetail? {
        val cartItem = cartItemJpaRepository.findByUserIdAndProductOptionId(userId, productOptionId)
            ?: return null

        // PRODUCT_OPTION, PRODUCT와 JOIN하여 정보 조회
        val productOption = productOptionJpaRepository.findById(cartItem.productOptionId).orElse(null)
            ?: return null
        val product = productJpaRepository.findById(productOption.productId).orElse(null)
            ?: return null

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

        // PRODUCT_OPTION, PRODUCT와 JOIN하여 정보 조회
        val productOption = productOptionJpaRepository.findById(savedEntity.productOptionId).orElseThrow()
        val product = productJpaRepository.findById(productOption.productId).orElseThrow()

        return CartItemDetail(
            cartItemId = savedEntity.id,
            productOptionId = savedEntity.productOptionId,
            productName = product.name,
            optionCode = productOption.optionCode,
            origin = productOption.origin,
            grindType = productOption.grindType,
            weightGrams = productOption.weightGrams,
            price = productOption.price.toInt(),
            quantity = savedEntity.quantity,
            totalPrice = productOption.price.toInt() * savedEntity.quantity,
            createdAt = savedEntity.createdAt,
            updatedAt = savedEntity.updatedAt
        )
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

        // PRODUCT_OPTION, PRODUCT와 JOIN하여 정보 조회
        val productOption = productOptionJpaRepository.findById(savedEntity.productOptionId).orElseThrow()
        val product = productJpaRepository.findById(productOption.productId).orElseThrow()

        return CartItemDetail(
            cartItemId = savedEntity.id,
            productOptionId = savedEntity.productOptionId,
            productName = product.name,
            optionCode = productOption.optionCode,
            origin = productOption.origin,
            grindType = productOption.grindType,
            weightGrams = productOption.weightGrams,
            price = productOption.price.toInt(),
            quantity = savedEntity.quantity,
            totalPrice = productOption.price.toInt() * savedEntity.quantity,
            createdAt = savedEntity.createdAt,
            updatedAt = savedEntity.updatedAt
        )
    }

    @Transactional
    override fun deleteByUserId(userId: Long) {
        cartItemJpaRepository.deleteByUserId(userId)
    }
}
