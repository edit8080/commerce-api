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
import java.math.BigDecimal

@DisplayName("ProductOption Repository 통합 테스트")
class ProductOptionRepositoryImplTest : RepositoryTestBase() {

    @Autowired
    private lateinit var productOptionRepository: ProductOptionRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testProduct: ProductEntity
    private lateinit var testActiveOption: ProductOptionEntity
    private lateinit var testInactiveOption: ProductOptionEntity

    @BeforeEach
    fun setUpTestData() {
        // 테스트 상품 생성
        testProduct = ProductEntity(
            name = "에티오피아 예가체프",
            description = "고급 원두",
            brand = "Bean Bliss"
        )
        entityManager.persist(testProduct)

        // 활성 옵션 생성
        testActiveOption = ProductOptionEntity(
            optionCode = "ETH-001",
            productId = testProduct.id,
            origin = "Ethiopia",
            grindType = "Whole Bean",
            weightGrams = 200,
            price = BigDecimal("15000"),
            isActive = true
        )
        entityManager.persist(testActiveOption)

        // 비활성 옵션 생성
        testInactiveOption = ProductOptionEntity(
            optionCode = "ETH-002",
            productId = testProduct.id,
            origin = "Ethiopia",
            grindType = "Ground",
            weightGrams = 200,
            price = BigDecimal("15500"),
            isActive = false
        )
        entityManager.persist(testInactiveOption)

        entityManager.flush()
        entityManager.clear()
    }

    @Test
    @DisplayName("활성 옵션 ID로 상세 조회 (상품 정보 포함) - 성공")
    fun `findActiveOptionWithProduct should return active option with product info`() {
        // When
        val optionDetail = productOptionRepository.findActiveOptionWithProduct(testActiveOption.id)

        // Then
        assertNotNull(optionDetail)
        assertEquals(testActiveOption.id, optionDetail!!.optionId)
        assertEquals(testProduct.id, optionDetail.productId)
        assertEquals(testProduct.name, optionDetail.productName)
        assertEquals("ETH-001", optionDetail.optionCode)
        assertEquals("Ethiopia", optionDetail.origin)
        assertEquals("Whole Bean", optionDetail.grindType)
        assertEquals(200, optionDetail.weightGrams)
        assertEquals(15000, optionDetail.price)
        assertTrue(optionDetail.isActive)
    }

    @Test
    @DisplayName("비활성 옵션 ID로 조회 - null 반환")
    fun `findActiveOptionWithProduct should return null for inactive option`() {
        // When
        val optionDetail = productOptionRepository.findActiveOptionWithProduct(testInactiveOption.id)

        // Then
        assertNull(optionDetail)
    }

    @Test
    @DisplayName("존재하지 않는 옵션 ID로 조회 - null 반환")
    fun `findActiveOptionWithProduct should return null when option not found`() {
        // When
        val optionDetail = productOptionRepository.findActiveOptionWithProduct(999L)

        // Then
        assertNull(optionDetail)
    }
}
