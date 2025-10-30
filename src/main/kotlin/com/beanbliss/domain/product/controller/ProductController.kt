package com.beanbliss.domain.product.controller

import com.beanbliss.common.dto.ApiResponse
import com.beanbliss.domain.product.dto.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime

@RestController
@RequestMapping("/products")
class ProductController {

    // Mock 데이터
    private val mockProducts = listOf(
        MockProduct(
            id = 1L,
            name = "에티오피아 예가체프",
            description = "플로럴하고 산뜻한 산미가 특징인 프리미엄 원두",
            brand = "Bean Bliss Premium",
            thumbnailUrl = "https://example.com/images/ethiopia.jpg",
            salesCount = 245,
            options = listOf(
                MockProductOption(1L, 1L, "ETH-HD-200", "Ethiopia", "HAND_DRIP", "핸드드립", 200, BigDecimal("21000"), 8),
                MockProductOption(2L, 1L, "ETH-HD-500", "Ethiopia", "HAND_DRIP", "핸드드립", 500, BigDecimal("48000"), 15),
                MockProductOption(3L, 1L, "ETH-WB-200", "Ethiopia", "WHOLE_BEANS", "원두", 200, BigDecimal("20000"), 12)
            )
        ),
        MockProduct(
            id = 2L,
            name = "콜롬비아 수프리모",
            description = "부드럽고 균형잡힌 맛이 특징인 클래식 원두",
            brand = "Bean Bliss Classic",
            thumbnailUrl = "https://example.com/images/colombia.jpg",
            salesCount = 189,
            options = listOf(
                MockProductOption(4L, 2L, "COL-HD-200", "Colombia", "HAND_DRIP", "핸드드립", 200, BigDecimal("19000"), 20),
                MockProductOption(5L, 2L, "COL-HD-500", "Colombia", "HAND_DRIP", "핸드드립", 500, BigDecimal("44000"), 10),
                MockProductOption(6L, 2L, "COL-ES-200", "Colombia", "ESPRESSO", "에스프레소", 200, BigDecimal("19000"), 5)
            )
        ),
        MockProduct(
            id = 3L,
            name = "브라질 산토스",
            description = "고소하고 부드러운 바디감이 일품인 원두",
            brand = "Bean Bliss Classic",
            thumbnailUrl = "https://example.com/images/brazil.jpg",
            salesCount = 156,
            options = listOf(
                MockProductOption(7L, 3L, "BRA-HD-200", "Brazil", "HAND_DRIP", "핸드드립", 200, BigDecimal("17000"), 30),
                MockProductOption(8L, 3L, "BRA-WB-500", "Brazil", "WHOLE_BEANS", "원두", 500, BigDecimal("40000"), 18)
            )
        ),
        MockProduct(
            id = 4L,
            name = "케냐 AA",
            description = "강렬한 산미와 와인 같은 풍미가 매력적인 원두",
            brand = "Bean Bliss Premium",
            thumbnailUrl = "https://example.com/images/kenya.jpg",
            salesCount = 98,
            options = listOf(
                MockProductOption(9L, 4L, "KEN-HD-200", "Kenya", "HAND_DRIP", "핸드드립", 200, BigDecimal("23000"), 7),
                MockProductOption(10L, 4L, "KEN-HD-500", "Kenya", "HAND_DRIP", "핸드드립", 500, BigDecimal("52000"), 4)
            )
        ),
        MockProduct(
            id = 5L,
            name = "과테말라 안티구아",
            description = "초콜릿과 너트 향이 조화로운 중미 원두",
            brand = "Bean Bliss Classic",
            thumbnailUrl = "https://example.com/images/guatemala.jpg",
            salesCount = 134,
            options = listOf(
                MockProductOption(11L, 5L, "GUA-HD-200", "Guatemala", "HAND_DRIP", "핸드드립", 200, BigDecimal("20000"), 14),
                MockProductOption(12L, 5L, "GUA-ES-200", "Guatemala", "ESPRESSO", "에스프레소", 200, BigDecimal("20000"), 9)
            )
        )
    )

    private val now = LocalDateTime.now()

    // 1. 인기 상품 Top K 조회
    @GetMapping("/top-selling")
    fun getTopSellingProducts(
        @RequestParam(required = false) days: Int?,
        @RequestParam(required = false) limit: Int?
    ): ResponseEntity<ApiResponse<TopSellingProductsResponse>> {
        val actualLimit = limit ?: 5

        // mock 데이터 기반 임시 처리
        val topProducts = mockProducts
            .sortedByDescending { it.salesCount }
            .take(actualLimit)
            .map { product ->
                TopSellingProductDto(
                    id = product.id,
                    name = product.name,
                    description = product.description,
                    origin = product.options.firstOrNull()?.origin ?: "Unknown",
                    salesCount = product.salesCount,
                    basePrice = product.options.minOfOrNull { it.price } ?: BigDecimal.ZERO,
                    thumbnailUrl = product.thumbnailUrl
                )
            }

        return ResponseEntity.ok(
            ApiResponse(
                data = TopSellingProductsResponse(products = topProducts)
            )
        )
    }

    // 2. 상품 목록 조회
    @GetMapping
    fun getProducts(
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) origin: String?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) order: String?
    ): ResponseEntity<ApiResponse<ProductListResponse>> {
        val productDtos = mockProducts.map { product ->
            ProductDto(
                id = product.id,
                name = product.name,
                description = product.description,
                origin = product.options.firstOrNull()?.origin ?: "Unknown",
                basePrice = product.options.minOfOrNull { it.price } ?: BigDecimal.ZERO,
                thumbnailUrl = product.thumbnailUrl
            )
        }

        return ResponseEntity.ok(
            ApiResponse(
                data = ProductListResponse(
                    products = productDtos,
                    pagination = PaginationDto(
                        currentPage = page ?: 1,
                        pageSize = size ?: 20,
                        totalElements = mockProducts.size.toLong(),
                        totalPages = 1
                    )
                )
            )
        )
    }

    // 3. 상품 상세 정보 조회
    @GetMapping("/{productId}")
    fun getProductDetail(@PathVariable productId: Long): ResponseEntity<ApiResponse<ProductDetailResponse>> {
        val product = mockProducts.first { it.id == productId }

        val response = ProductDetailResponse(
            id = product.id,
            name = product.name,
            description = product.description,
            origin = product.options.firstOrNull()?.origin ?: "Unknown",
            thumbnailUrl = product.thumbnailUrl,
            options = product.options.map { option ->
                ProductOptionSummaryDto(
                    id = option.id,
                    optionCode = option.optionCode,
                    grindType = option.grindType,
                    grindTypeName = option.grindTypeName,
                    weightGrams = option.weightGrams,
                    price = option.price,
                    stockQuantity = option.stockQuantity,
                    isAvailable = option.stockQuantity > 0
                )
            },
            createdAt = now.minusDays(30),
            updatedAt = now
        )

        return ResponseEntity.ok(ApiResponse(data = response))
    }

    // 4. 상품 옵션 상세 조회
    @GetMapping("/options/{optionCode}")
    fun getProductOption(@PathVariable optionCode: String): ResponseEntity<ApiResponse<ProductOptionDetailResponse>> {
        val (product, productOption) = mockProducts
            .flatMap { product -> product.options.map { option -> product to option } }
            .first { (_, option) -> option.optionCode == optionCode }

        val response = ProductOptionDetailResponse(
            id = productOption.id,
            productId = product.id,
            productName = product.name,
            optionCode = productOption.optionCode,
            grindType = productOption.grindType,
            grindTypeName = productOption.grindTypeName,
            weightGrams = productOption.weightGrams,
            price = productOption.price,
            inventory = InventoryDto(
                stockQuantity = productOption.stockQuantity,
                isAvailable = productOption.stockQuantity > 0,
                updatedAt = now.minusHours(2)
            ),
            isActive = true,
            createdAt = now.minusDays(30),
            updatedAt = now
        )

        return ResponseEntity.ok(ApiResponse(data = response))
    }

    // Mock 데이터 클래스
    private data class MockProduct(
        val id: Long,
        val name: String,
        val description: String,
        val brand: String,
        val thumbnailUrl: String,
        val salesCount: Int,
        val options: List<MockProductOption>
    )

    private data class MockProductOption(
        val id: Long,
        val productId: Long,
        val optionCode: String,
        val origin: String,
        val grindType: String,
        val grindTypeName: String,
        val weightGrams: Int,
        val price: BigDecimal,
        val stockQuantity: Int
    )
}
