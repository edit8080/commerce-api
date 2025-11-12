package com.beanbliss.domain.cart.repository

import com.beanbliss.common.test.RepositoryTestBase
import com.beanbliss.domain.cart.entity.CartItemEntity
import com.beanbliss.domain.product.entity.ProductEntity
import com.beanbliss.domain.product.entity.ProductOptionEntity
import com.beanbliss.domain.user.entity.UserEntity
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import java.math.BigDecimal

@DisplayName("CartItem Repository 통합 테스트")
class CartItemRepositoryImplTest : RepositoryTestBase() {

    @Autowired
    private lateinit var cartItemRepository: CartItemRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testUser: UserEntity
    private lateinit var testProduct: ProductEntity
    private lateinit var testProductOption: ProductOptionEntity

    @BeforeEach
    fun setUpTestData() {
        // 테스트 사용자 생성
        testUser = UserEntity(
            email = "test@example.com",
            password = "password123",
            name = "테스트 사용자"
        )
        entityManager.persist(testUser)

        // 테스트 상품 생성
        testProduct = ProductEntity(
            name = "에티오피아 예가체프",
            description = "고급 원두",
            brand = "Bean Bliss"
        )
        entityManager.persist(testProduct)

        // 테스트 상품 옵션 생성
        testProductOption = ProductOptionEntity(
            optionCode = "ETH-001",
            productId = testProduct.id,
            origin = "Ethiopia",
            grindType = "Whole Bean",
            weightGrams = 200,
            price = BigDecimal("15000"),
            isActive = true
        )
        entityManager.persist(testProductOption)

        entityManager.flush()
        entityManager.clear()
    }

    @Test
    @DisplayName("사용자 ID로 장바구니 아이템 조회 - 성공 (CART 도메인만)")
    fun `findByUserId should return cart items without product info`() {
        // Given: 장바구니 아이템 생성
        val cartItem = CartItemEntity(
            userId = testUser.id,
            productOptionId = testProductOption.id,
            quantity = 2
        )
        entityManager.persist(cartItem)
        entityManager.flush()
        entityManager.clear()

        // When: 사용자 ID로 조회 (CART_ITEM 테이블만)
        val results = cartItemRepository.findByUserId(testUser.id)

        // Then: CART 도메인 정보만 반환 (PRODUCT 정보 없음)
        assertEquals(1, results.size)
        val result = results[0]
        assertEquals(cartItem.id, result.id) // Fixed: Added !!
        assertEquals(testProductOption.id, result.productOptionId)
        assertEquals(testUser.id, result.userId)
        assertEquals(2, result.quantity)
        // PRODUCT 정보는 UseCase 계층에서 조합하므로 여기서는 검증하지 않음
    }

    @Test
    @DisplayName("사용자 ID로 장바구니 아이템 조회 - 빈 결과")
    fun `findByUserId should return empty list when no cart items`() {
        // When: 장바구니 아이템이 없는 사용자 조회
        val results = cartItemRepository.findByUserId(999L)

        // Then: 빈 리스트 반환
        assertTrue(results.isEmpty())
    }

    @Test
    @DisplayName("사용자 ID와 상품 옵션 ID로 장바구니 아이템 조회 - 성공")
    fun `findByUserIdAndProductOptionId should return cart item`() {
        // Given
        val cartItem = CartItemEntity(
            userId = testUser.id,
            productOptionId = testProductOption.id,
            quantity = 3
        )
        entityManager.persist(cartItem)
        entityManager.flush()
        entityManager.clear()

        // When
        val result = cartItemRepository.findByUserIdAndProductOptionId(
            testUser.id,
            testProductOption.id
        )

        // Then
        assertNotNull(result)
        assertEquals(testProductOption.id, result!!.productOptionId)
        assertEquals(3, result.quantity)
    }

    @Test
    @DisplayName("사용자 ID와 상품 옵션 ID로 장바구니 아이템 조회 - null 반환")
    fun `findByUserIdAndProductOptionId should return null when not found`() {
        // When
        val result = cartItemRepository.findByUserIdAndProductOptionId(999L, 999L)

        // Then
        assertNull(result)
    }

    @Test
    @DisplayName("장바구니 아이템 저장 - 신규 생성 (CART 도메인만)")
    fun `save should create new cart item`() {
        // Given: CartItem 도메인 모델 생성
        val now = java.time.LocalDateTime.now()
        val newCartItem = com.beanbliss.domain.cart.domain.CartItem(
            id = 0L,  // 신규 생성
            userId = testUser.id,
            productOptionId = testProductOption.id,
            quantity = 5,
            createdAt = now,
            updatedAt = now
        )

        // When: 저장 (CART_ITEM 테이블만)
        val savedItem = cartItemRepository.save(newCartItem)

        // Then: CART 도메인 정보만 반환 (PRODUCT 정보 없음)
        assertTrue(savedItem.id > 0)
        assertEquals(testUser.id, savedItem.userId)
        assertEquals(testProductOption.id, savedItem.productOptionId)
        assertEquals(5, savedItem.quantity)
        // PRODUCT 정보는 UseCase 계층에서 조합하므로 여기서는 검증하지 않음
    }

    @Test
    @DisplayName("장바구니 아이템 저장 - 기존 아이템 업데이트")
    fun `save should update existing cart item`() {
        // Given: 기존 장바구니 아이템 생성
        val cartItem = CartItemEntity(
            userId = testUser.id,
            productOptionId = testProductOption.id,
            quantity = 2
        )
        entityManager.persist(cartItem)
        entityManager.flush()
        entityManager.clear()

        // When: 수량 업데이트 (save 메서드 사용)
        val now = java.time.LocalDateTime.now()
        val updatedCartItem = com.beanbliss.domain.cart.domain.CartItem(
            id = cartItem.id,
            userId = testUser.id,
            productOptionId = testProductOption.id,
            quantity = 10,
            createdAt = now,
            updatedAt = now
        )
        val savedItem = cartItemRepository.save(updatedCartItem)

        // Then: 수량이 업데이트되어야 함
        assertEquals(cartItem.id, savedItem.id)
        assertEquals(10, savedItem.quantity)
    }

    @Test
    @DisplayName("사용자의 모든 장바구니 아이템 삭제 - 성공")
    fun `deleteByUserId should delete all cart items for user`() {
        // Given: 여러 장바구니 아이템 생성
        val cartItem1 = CartItemEntity(
            userId = testUser.id,
            productOptionId = testProductOption.id,
            quantity = 2
        )
        val cartItem2 = CartItemEntity(
            userId = testUser.id,
            productOptionId = testProductOption.id,
            quantity = 3
        )
        entityManager.persist(cartItem1)
        entityManager.persist(cartItem2)
        entityManager.flush()
        entityManager.clear()

        // When: 삭제
        cartItemRepository.deleteByUserId(testUser.id)
        entityManager.flush()
        entityManager.clear()

        // Then: 모든 아이템이 삭제되어야 함
        val results = cartItemRepository.findByUserId(testUser.id)
        assertTrue(results.isEmpty())
    }
}
