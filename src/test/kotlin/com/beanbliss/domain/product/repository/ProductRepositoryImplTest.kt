package com.beanbliss.domain.product.repository

import com.beanbliss.common.test.RepositoryTestBase
import com.beanbliss.domain.product.entity.ProductEntity
import com.beanbliss.domain.product.entity.ProductOptionEntity
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@DisplayName("Product Repository 통합 테스트")
@Transactional
class ProductRepositoryImplTest : RepositoryTestBase() {

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testProduct1: ProductEntity
    private lateinit var testProduct2: ProductEntity
    private lateinit var testProduct3: ProductEntity

    @BeforeEach
    fun setUpTestData() {
        // 기존 데이터 정리 (다른 테스트의 영향 제거)
        entityManager.createQuery("DELETE FROM ProductOptionEntity").executeUpdate()
        entityManager.createQuery("DELETE FROM ProductEntity").executeUpdate()
        entityManager.flush()
        entityManager.clear()

        // 테스트 상품 1 생성 (활성 옵션 2개)
        testProduct1 = ProductEntity(
            name = "에티오피아 예가체프",
            description = "고급 원두",
            brand = "Bean Bliss"
        )
        entityManager.persist(testProduct1)
        entityManager.flush()

        val option1_1 = ProductOptionEntity(
            optionCode = "ETH-001",
            productId = testProduct1.id,
            origin = "Ethiopia",
            grindType = "Whole Bean",
            weightGrams = 200,
            price = BigDecimal("15000"),
            isActive = true
        )
        val option1_2 = ProductOptionEntity(
            optionCode = "ETH-002",
            productId = testProduct1.id,
            origin = "Ethiopia",
            grindType = "Ground",
            weightGrams = 200,
            price = BigDecimal("15500"),
            isActive = true
        )
        entityManager.persist(option1_1)
        entityManager.persist(option1_2)

        // 테스트 상품 2 생성 (활성 옵션 1개)
        testProduct2 = ProductEntity(
            name = "콜롬비아 수프리모",
            description = "중간 로스트",
            brand = "Bean Bliss"
        )
        entityManager.persist(testProduct2)
        entityManager.flush()

        val option2_1 = ProductOptionEntity(
            optionCode = "COL-001",
            productId = testProduct2.id,
            origin = "Colombia",
            grindType = "Whole Bean",
            weightGrams = 250,
            price = BigDecimal("18000"),
            isActive = true
        )
        entityManager.persist(option2_1)

        // 테스트 상품 3 생성 (비활성 옵션만 있음 - 조회되지 않아야 함)
        testProduct3 = ProductEntity(
            name = "브라질 산토스",
            description = "라이트 로스트",
            brand = "Bean Bliss"
        )
        entityManager.persist(testProduct3)
        entityManager.flush()

        val option3_1 = ProductOptionEntity(
            optionCode = "BRA-001",
            productId = testProduct3.id,
            origin = "Brazil",
            grindType = "Whole Bean",
            weightGrams = 300,
            price = BigDecimal("12000"),
            isActive = false  // 비활성
        )
        entityManager.persist(option3_1)

        entityManager.flush()
        entityManager.clear()
    }

    @Test
    @DisplayName("활성 상품 목록 조회 - 페이징 및 정렬")
    fun `findActiveProducts should return products with active options only`() {
        // When
        val products = productRepository.findActiveProducts(
            page = 1,
            size = 10,
            sortBy = "name",
            sortDirection = "ASC"
        )

        // Then: 활성 옵션이 있는 상품 2개만 조회되어야 함
        assertEquals(2, products.size)

        // 첫 번째 상품 검증 (에티오피아 - name ASC 정렬)
        val product1 = products[0]
        assertEquals(testProduct1.name, product1.name)
        assertEquals(2, product1.options.size)
        assertTrue(product1.options.all { it.optionCode.startsWith("ETH") })

        // 두 번째 상품 검증 (콜롬비아)
        val product2 = products[1]
        assertEquals(testProduct2.name, product2.name)
        assertEquals(1, product2.options.size)
    }

    @Test
    @DisplayName("활성 상품 개수 조회")
    fun `countActiveProducts should return count of products with active options`() {
        // When
        val count = productRepository.countActiveProducts()

        // Then: 활성 옵션이 있는 상품 2개
        assertEquals(2, count)
    }

    @Test
    @DisplayName("상품 ID로 상세 조회 (옵션 포함) - 성공")
    fun `findByIdWithOptions should return product with active options`() {
        // When
        val product = productRepository.findByIdWithOptions(testProduct1.id)

        // Then
        assertNotNull(product)
        assertEquals(testProduct1.name, product!!.name)
        assertEquals(2, product.options.size)

        // 옵션 정렬 검증 (weightGrams ASC, grindType ASC)
        // grindType은 알파벳 순: "Ground" < "Whole Bean"
        val options = product.options
        assertEquals("Ground", options[0].grindType)
        assertEquals("Whole Bean", options[1].grindType)
    }

    @Test
    @DisplayName("상품 ID로 상세 조회 - 존재하지 않는 상품")
    fun `findByIdWithOptions should return null when product not found`() {
        // When
        val product = productRepository.findByIdWithOptions(999L)

        // Then
        assertNull(product)
    }

    @Test
    @DisplayName("상품 목록 조회 - 정렬 옵션 테스트 (createdAt DESC)")
    fun `findActiveProducts should sort by createdAt descending`() {
        // When
        val products = productRepository.findActiveProducts(
            page = 1,
            size = 10,
            sortBy = "createdAt",
            sortDirection = "DESC"
        )

        // Then: 최근 생성된 상품이 먼저 (testProduct2가 testProduct1보다 나중)
        assertEquals(2, products.size)
        assertEquals(testProduct2.name, products[0].name)
        assertEquals(testProduct1.name, products[1].name)
    }
}
