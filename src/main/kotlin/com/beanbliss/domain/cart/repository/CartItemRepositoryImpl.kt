package com.beanbliss.domain.cart.repository

import com.beanbliss.domain.cart.domain.CartItem
import com.beanbliss.domain.cart.entity.CartItemEntity
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
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
 *
 * [동시성 제어]:
 * - findByUserIdAndProductOptionIdWithLock: 비관적 락 사용 (PESSIMISTIC_WRITE)
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
     * 사용자 ID와 상품 옵션 ID로 장바구니 아이템 조회 (비관적 락)
     *
     * [동시성 제어]:
     * - PESSIMISTIC_WRITE 락 사용
     * - SELECT ... FOR UPDATE
     * - Lost Update 방지
     *
     * @param userId 사용자 ID
     * @param productOptionId 상품 옵션 ID
     * @return CartItemEntity (없으면 null)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CartItemEntity c WHERE c.userId = :userId AND c.productOptionId = :productOptionId")
    fun findByUserIdAndProductOptionIdWithLock(
        @Param("userId") userId: Long,
        @Param("productOptionId") productOptionId: Long
    ): CartItemEntity?

    /**
     * 사용자의 모든 장바구니 아이템 삭제
     */
    @Modifying
    @Query("DELETE FROM CartItemEntity c WHERE c.userId = :userId")
    fun deleteByUserId(@Param("userId") userId: Long)

    /**
     * 장바구니 아이템 수량 직접 업데이트
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE CartItemEntity c SET c.quantity = :newQuantity WHERE c.id = :cartItemId")
    fun updateQuantity(@Param("cartItemId") cartItemId: Long, @Param("newQuantity") newQuantity: Int)

    /**
     * 장바구니 아이템 수량 원자적 증가 (최대 수량 제한 포함)
     *
     * [동시성 제어]:
     * - Native UPDATE 쿼리 사용
     * - UPDATE ... SET quantity = quantity + ? WHERE ... AND quantity + ? <= maxQuantity
     * - 최대 수량 검증을 UPDATE 쿼리에 포함하여 원자성 보장
     * - 비관적 락과 함께 사용하여 Lost Update 방지
     *
     * @return 업데이트된 행 수 (1: 성공, 0: 최대 수량 초과)
     */
    @Modifying(clearAutomatically = false)
    @Query(
        value = "UPDATE cart_item SET quantity = quantity + :incrementBy, updated_at = NOW() WHERE id = :cartItemId AND quantity + :incrementBy <= :maxQuantity",
        nativeQuery = true
    )
    fun incrementQuantityByIdWithLimit(
        @Param("cartItemId") cartItemId: Long,
        @Param("incrementBy") incrementBy: Int,
        @Param("maxQuantity") maxQuantity: Int
    ): Int

    /**
     * 장바구니 아이템 UPSERT (INSERT OR UPDATE)
     *
     * [동시성 제어]:
     * - INSERT ... ON DUPLICATE KEY UPDATE (MySQL)
     * - 원자적으로 INSERT 또는 UPDATE 수행
     * - UNIQUE 제약 (user_id, product_option_id) 활용
     *
     * [반환값]:
     * - 1: INSERT 성공
     * - 2: UPDATE 성공 (값 변경됨)
     *
     * [최대 수량 검증]:
     * - Service 계층에서 조회 후 검증
     * - 초과 시 트랜잭션 롤백
     */
    @Modifying(clearAutomatically = false)
    @Query(
        value = """
            INSERT INTO cart_item (user_id, product_option_id, quantity, created_at, updated_at)
            VALUES (:userId, :productOptionId, :quantity, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                quantity = quantity + VALUES(quantity),
                updated_at = NOW()
        """,
        nativeQuery = true
    )
    fun upsertCartItem(
        @Param("userId") userId: Long,
        @Param("productOptionId") productOptionId: Long,
        @Param("quantity") quantity: Int
    ): Int
}

/**
 * [책임]: CartItemRepository 인터페이스 구현체
 * - CartItemJpaRepository를 활용하여 실제 DB 접근
 * - CART_ITEM 테이블만 조회 (도메인 간 JOIN 제거)
 * - Entity ↔ Domain Model 변환 담당
 */
@Repository
class CartItemRepositoryImpl(
    private val cartItemJpaRepository: CartItemJpaRepository,
    private val entityManager: EntityManager
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

    override fun findByUserIdAndProductOptionIdWithLock(userId: Long, productOptionId: Long): CartItem? {
        // CART_ITEM만 조회 (비관적 락)
        val entity = cartItemJpaRepository.findByUserIdAndProductOptionIdWithLock(userId, productOptionId)
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

    override fun updateQuantity(cartItemId: Long, newQuantity: Int): CartItem {
        cartItemJpaRepository.updateQuantity(cartItemId, newQuantity)
        return findById(cartItemId)
            ?: throw IllegalStateException("Cart item not found after update: $cartItemId")
    }

    override fun deleteByUserId(userId: Long) {
        cartItemJpaRepository.deleteByUserId(userId)
    }

    override fun incrementQuantityWithLimit(cartItemId: Long, incrementBy: Int, maxQuantity: Int): Int {
        // Native UPDATE 쿼리 실행
        // 비관적 락이 유지된 상태에서 원자적으로 수량 증가 및 최대 수량 검증
        return cartItemJpaRepository.incrementQuantityByIdWithLimit(cartItemId, incrementBy, maxQuantity)
    }

    override fun upsertCartItem(userId: Long, productOptionId: Long, quantity: Int): Int {
        // INSERT ... ON DUPLICATE KEY UPDATE 실행
        // 원자적으로 INSERT 또는 UPDATE 수행, 동시성 안전
        return cartItemJpaRepository.upsertCartItem(userId, productOptionId, quantity)
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
