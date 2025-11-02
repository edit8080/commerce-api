package com.beanbliss.domain.product.repository

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * ProductRepository의 데이터 접근 로직을 검증하는 테스트
 *
 *
 * 검증 목표:
 * 1. 활성 옵션만 포함되는가? (PRODUCT_OPTION.is_active = true)
 * 2. 활성 옵션이 있는 상품만 반환되는가? (활성 옵션이 없으면 상품 자체를 제외)
 * 3. 지정된 정렬 기준으로 정렬되는가? (sortBy, sortDirection)
 * 4. 페이징이 올바르게 적용되는가?
 * 5. countActiveProducts가 정확한 값을 반환하는가?
 */
@DisplayName("상품 Repository 테스트")
class ProductRepositoryTest {

    private lateinit var productRepository: FakeProductRepository

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
    }

    @Test
    @DisplayName("활성 옵션만 포함되어야 한다 (비활성 옵션 제외)")
    fun `활성 옵션만 포함되어야 한다`() {
        // Given: 상품 A (활성 옵션 2개, 비활성 옵션 1개)
        productRepository.addProduct(
            FakeProductRepository.Product(
                productId = 1L,
                name = "상품 A",
                description = "Test Description",
                brand = "Test Brand",
                createdAt = java.time.LocalDateTime.now(),
                options = listOf(
                    FakeProductRepository.ProductOption(
                        optionId = 1L,
                        optionCode = "OPT-001",
                        origin = "Test Origin",
                        grindType = "WHOLE_BEANS",
                        weightGrams = 200,
                        price = 20000,
                        isActive = true  // 활성
                    ),
                    FakeProductRepository.ProductOption(
                        optionId = 2L,
                        optionCode = "OPT-002",
                        origin = "Test Origin",
                        grindType = "HAND_DRIP",
                        weightGrams = 200,
                        price = 21000,
                        isActive = true  // 활성
                    ),
                    FakeProductRepository.ProductOption(
                        optionId = 3L,
                        optionCode = "OPT-003",
                        origin = "Test Origin",
                        grindType = "ESPRESSO",
                        weightGrams = 200,
                        price = 22000,
                        isActive = false  // 비활성
                    )
                )
            )
        )

        // When
        val result = productRepository.findActiveProducts(
            page = 1,
            size = 10,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        val productA = result.first { it.name == "상품 A" }
        assertEquals(2, productA.options.size, "활성 옵션만 2개 포함되어야 함")
        assertTrue(
            productA.options.none { it.optionCode == "OPT-003" },
            "비활성 옵션 OPT-003은 포함되지 않아야 함"
        )
    }

    @Test
    @DisplayName("created_at DESC 순으로 정렬되어야 한다")
    fun `created_at DESC 순으로 정렬되어야 한다`() {
        // Given: 3개의 활성 상품
        productRepository.addProduct(
            FakeProductRepository.Product(
                productId = 1L,
                name = "상품 A",
                description = "Test",
                brand = "Test Brand",
                createdAt = java.time.LocalDateTime.of(2025, 1, 1, 10, 0),
                options = listOf(
                    FakeProductRepository.ProductOption(
                        1L, "OPT-001", "Origin", "WHOLE_BEANS", 200, 20000, true
                    )
                )
            )
        )
        productRepository.addProduct(
            FakeProductRepository.Product(
                productId = 2L,
                name = "상품 B",
                description = "Test",
                brand = "Test Brand",
                createdAt = java.time.LocalDateTime.of(2025, 1, 15, 10, 0), // 최신
                options = listOf(
                    FakeProductRepository.ProductOption(
                        2L, "OPT-002", "Origin", "WHOLE_BEANS", 200, 20000, true
                    )
                )
            )
        )
        productRepository.addProduct(
            FakeProductRepository.Product(
                productId = 3L,
                name = "상품 C",
                description = "Test",
                brand = "Test Brand",
                createdAt = java.time.LocalDateTime.of(2025, 1, 10, 10, 0),
                options = listOf(
                    FakeProductRepository.ProductOption(
                        3L, "OPT-003", "Origin", "WHOLE_BEANS", 200, 20000, true
                    )
                )
            )
        )

        // When
        val result = productRepository.findActiveProducts(
            page = 1,
            size = 10,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        assertEquals(3, result.size)
        assertEquals("상품 B", result[0].name, "최신 상품이 첫 번째여야 함")
        assertEquals("상품 C", result[1].name, "두 번째로 최신 상품")
        assertEquals("상품 A", result[2].name, "가장 오래된 상품이 마지막")

        // 날짜 순서 확인
        assertTrue(
            result[0].createdAt.isAfter(result[1].createdAt),
            "첫 번째 상품이 두 번째보다 최신이어야 함"
        )
        assertTrue(
            result[1].createdAt.isAfter(result[2].createdAt),
            "두 번째 상품이 세 번째보다 최신이어야 함"
        )
    }

    @Test
    @DisplayName("created_at ASC 순으로 정렬되어야 한다")
    fun `created_at ASC 순으로 정렬되어야 한다`() {
        // Given: 3개의 활성 상품
        productRepository.addProduct(
            FakeProductRepository.Product(
                productId = 1L,
                name = "상품 A",
                description = "Test",
                brand = "Test Brand",
                createdAt = java.time.LocalDateTime.of(2025, 1, 1, 10, 0),
                options = listOf(
                    FakeProductRepository.ProductOption(
                        1L, "OPT-001", "Origin", "WHOLE_BEANS", 200, 20000, true
                    )
                )
            )
        )
        productRepository.addProduct(
            FakeProductRepository.Product(
                productId = 2L,
                name = "상품 B",
                description = "Test",
                brand = "Test Brand",
                createdAt = java.time.LocalDateTime.of(2025, 1, 15, 10, 0), // 최신
                options = listOf(
                    FakeProductRepository.ProductOption(
                        2L, "OPT-002", "Origin", "WHOLE_BEANS", 200, 20000, true
                    )
                )
            )
        )
        productRepository.addProduct(
            FakeProductRepository.Product(
                productId = 3L,
                name = "상품 C",
                description = "Test",
                brand = "Test Brand",
                createdAt = java.time.LocalDateTime.of(2025, 1, 10, 10, 0),
                options = listOf(
                    FakeProductRepository.ProductOption(
                        3L, "OPT-003", "Origin", "WHOLE_BEANS", 200, 20000, true
                    )
                )
            )
        )

        // When
        val result = productRepository.findActiveProducts(
            page = 1,
            size = 10,
            sortBy = "created_at",
            sortDirection = "ASC"
        )

        // Then
        assertEquals(3, result.size)
        assertEquals("상품 A", result[0].name, "가장 오래된 상품이 첫 번째여야 함")
        assertEquals("상품 C", result[1].name, "두 번째로 오래된 상품")
        assertEquals("상품 B", result[2].name, "최신 상품이 마지막")

        // 날짜 순서 확인
        assertTrue(
            result[0].createdAt.isBefore(result[1].createdAt),
            "첫 번째 상품이 두 번째보다 오래되어야 함"
        )
        assertTrue(
            result[1].createdAt.isBefore(result[2].createdAt),
            "두 번째 상품이 세 번째보다 오래되어야 함"
        )
    }

    @Test
    @DisplayName("페이징이 올바르게 적용되어야 한다 - 첫 번째 페이지")
    fun `페이징이 올바르게 적용되어야 한다 - 첫 번째 페이지`() {
        // Given: 총 12개의 활성 상품
        for (i in 1..12) {
            productRepository.addProduct(
                FakeProductRepository.Product(
                    productId = i.toLong(),
                    name = "상품 $i",
                    description = "Test",
                    brand = "Test Brand",
                    createdAt = java.time.LocalDateTime.now().plusDays(i.toLong()),
                    options = listOf(
                        FakeProductRepository.ProductOption(
                            i.toLong(), "OPT-$i", "Origin", "WHOLE_BEANS", 200, 20000, true
                        )
                    )
                )
            )
        }

        // When: 페이지 크기 5로 첫 번째 페이지 조회
        val result = productRepository.findActiveProducts(
            page = 1,
            size = 5,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        assertEquals(5, result.size, "첫 번째 페이지는 5개의 상품을 포함해야 함")
    }

    @Test
    @DisplayName("페이징이 올바르게 적용되어야 한다 - 두 번째 페이지")
    fun `페이징이 올바르게 적용되어야 한다 - 두 번째 페이지`() {
        // Given: 총 12개의 활성 상품
        for (i in 1..12) {
            productRepository.addProduct(
                FakeProductRepository.Product(
                    productId = i.toLong(),
                    name = "상품 $i",
                    description = "Test",
                    brand = "Test Brand",
                    createdAt = java.time.LocalDateTime.now().plusDays(i.toLong()),
                    options = listOf(
                        FakeProductRepository.ProductOption(
                            i.toLong(), "OPT-$i", "Origin", "WHOLE_BEANS", 200, 20000, true
                        )
                    )
                )
            )
        }

        // When: 페이지 크기 5로 두 번째 페이지 조회
        val result = productRepository.findActiveProducts(
            page = 2,
            size = 5,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        assertEquals(5, result.size, "두 번째 페이지도 5개의 상품을 포함해야 함")
    }

    @Test
    @DisplayName("페이징이 올바르게 적용되어야 한다 - 마지막 페이지")
    fun `페이징이 올바르게 적용되어야 한다 - 마지막 페이지`() {
        // Given: 총 12개의 활성 상품
        for (i in 1..12) {
            productRepository.addProduct(
                FakeProductRepository.Product(
                    productId = i.toLong(),
                    name = "상품 $i",
                    description = "Test",
                    brand = "Test Brand",
                    createdAt = java.time.LocalDateTime.now().plusDays(i.toLong()),
                    options = listOf(
                        FakeProductRepository.ProductOption(
                            i.toLong(), "OPT-$i", "Origin", "WHOLE_BEANS", 200, 20000, true
                        )
                    )
                )
            )
        }

        // When: 페이지 크기 5로 세 번째 페이지 조회
        val result = productRepository.findActiveProducts(
            page = 3,
            size = 5,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        assertEquals(2, result.size, "마지막 페이지는 나머지 2개의 상품만 포함해야 함")
    }

    @Test
    @DisplayName("활성 옵션이 있는 상품만 반환되어야 한다 (활성 옵션이 없으면 상품 제외)")
    fun `활성 옵션이 있는 상품만 반환되어야 한다`() {
        // Given: 상품 3개
        // 상품 A: 활성 옵션 2개
        productRepository.addProduct(
            FakeProductRepository.Product(
                productId = 1L,
                name = "상품 A",
                description = "Test",
                brand = "Test Brand",
                createdAt = java.time.LocalDateTime.of(2025, 1, 3, 10, 0),
                options = listOf(
                    FakeProductRepository.ProductOption(
                        1L, "OPT-001", "Origin", "WHOLE_BEANS", 200, 20000, true
                    ),
                    FakeProductRepository.ProductOption(
                        2L, "OPT-002", "Origin", "HAND_DRIP", 200, 21000, true
                    )
                )
            )
        )
        // 상품 B: 모든 옵션이 비활성
        productRepository.addProduct(
            FakeProductRepository.Product(
                productId = 2L,
                name = "상품 B",
                description = "Test",
                brand = "Test Brand",
                createdAt = java.time.LocalDateTime.of(2025, 1, 2, 10, 0),
                options = listOf(
                    FakeProductRepository.ProductOption(
                        3L, "OPT-003", "Origin", "WHOLE_BEANS", 200, 20000, false // 비활성
                    )
                )
            )
        )
        // 상품 C: 활성 옵션 1개
        productRepository.addProduct(
            FakeProductRepository.Product(
                productId = 3L,
                name = "상품 C",
                description = "Test",
                brand = "Test Brand",
                createdAt = java.time.LocalDateTime.of(2025, 1, 1, 10, 0),
                options = listOf(
                    FakeProductRepository.ProductOption(
                        4L, "OPT-004", "Origin", "WHOLE_BEANS", 200, 20000, true
                    )
                )
            )
        )

        // When
        val result = productRepository.findActiveProducts(
            page = 1,
            size = 10,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        // [Repository 책임]: 활성 옵션이 있는 상품만 반환 (상품 B 제외)
        assertEquals(2, result.size, "활성 옵션이 있는 상품만 반환되어야 함")
        assertEquals("상품 A", result[0].name)
        assertEquals("상품 C", result[1].name)

        // 상품 B가 제외되었는지 확인
        assertFalse(
            result.any { it.name == "상품 B" },
            "활성 옵션이 없는 '상품 B'는 반환되지 않아야 함"
        )
    }

    @Test
    @DisplayName("모든 상품이 활성 옵션이 없으면 빈 리스트를 반환해야 한다")
    fun `모든 상품이 활성 옵션이 없으면 빈 리스트를 반환해야 한다`() {
        // Given: 모든 상품이 활성 옵션 없음
        productRepository.addProduct(
            FakeProductRepository.Product(
                productId = 1L,
                name = "상품 A",
                description = "Test",
                brand = "Test Brand",
                createdAt = java.time.LocalDateTime.now(),
                options = listOf(
                    FakeProductRepository.ProductOption(
                        1L, "OPT-001", "Origin", "WHOLE_BEANS", 200, 20000, false // 비활성
                    )
                )
            )
        )
        productRepository.addProduct(
            FakeProductRepository.Product(
                productId = 2L,
                name = "상품 B",
                description = "Test",
                brand = "Test Brand",
                createdAt = java.time.LocalDateTime.now(),
                options = listOf(
                    FakeProductRepository.ProductOption(
                        2L, "OPT-002", "Origin", "WHOLE_BEANS", 200, 20000, false // 비활성
                    )
                )
            )
        )

        // When
        val result = productRepository.findActiveProducts(
            page = 1,
            size = 10,
            sortBy = "created_at",
            sortDirection = "DESC"
        )

        // Then
        assertTrue(result.isEmpty(), "활성 옵션이 있는 상품이 없으면 빈 리스트를 반환해야 함")
    }

    @Test
    @DisplayName("countActiveProducts는 활성 옵션이 있는 상품만 카운트해야 한다")
    fun `countActiveProducts는 활성 옵션이 있는 상품만 카운트해야 한다`() {
        // Given: 상품 5개 중 활성 옵션이 있는 상품 3개
        // 활성 옵션 있는 상품 3개
        for (i in 1..3) {
            productRepository.addProduct(
                FakeProductRepository.Product(
                    productId = i.toLong(),
                    name = "활성 상품 $i",
                    description = "Test",
                    brand = "Test Brand",
                    createdAt = java.time.LocalDateTime.now(),
                    options = listOf(
                        FakeProductRepository.ProductOption(
                            i.toLong(), "OPT-$i", "Origin", "WHOLE_BEANS", 200, 20000, true
                        )
                    )
                )
            )
        }
        // 활성 옵션 없는 상품 2개
        for (i in 4..5) {
            productRepository.addProduct(
                FakeProductRepository.Product(
                    productId = i.toLong(),
                    name = "비활성 상품 $i",
                    description = "Test",
                    brand = "Test Brand",
                    createdAt = java.time.LocalDateTime.now(),
                    options = listOf(
                        FakeProductRepository.ProductOption(
                            i.toLong(), "OPT-$i", "Origin", "WHOLE_BEANS", 200, 20000, false // 비활성
                        )
                    )
                )
            )
        }

        // When
        val count = productRepository.countActiveProducts()

        // Then
        assertEquals(3L, count, "활성 옵션이 있는 상품만 카운트되어야 함")
    }
}
