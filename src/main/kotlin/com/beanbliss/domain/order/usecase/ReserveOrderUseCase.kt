package com.beanbliss.domain.order.usecase

import com.beanbliss.domain.cart.domain.CartItemDetail
import com.beanbliss.domain.cart.service.CartService
import com.beanbliss.domain.product.service.ProductOptionService
import com.beanbliss.domain.inventory.service.InventoryService
import com.beanbliss.domain.product.service.ProductService
import com.beanbliss.domain.user.service.UserService
import com.beanbliss.domain.order.exception.CartEmptyException
import org.springframework.stereotype.Component

/**
 * [책임]: 주문 예약 UseCase 구현
 * - 여러 도메인 Service 조율
 * - 복합 비즈니스 트랜잭션 오케스트레이션
 *
 * [DIP 준수]:
 * - UserService, CartService, ProductOptionService, ProductService, InventoryService에만 의존
 *
 * [트랜잭션]:
 * - @Transactional은 InventoryService.reserveInventory()에만 적용
 * - 재고 예약 실패 시 롤백
 */
@Component
class ReserveOrderUseCase(
    private val userService: UserService,
    private val cartService: CartService,
    private val productOptionService: ProductOptionService,
    private val productService: ProductService,
    private val inventoryService: InventoryService
) {

    fun reserveOrder(userId: Long): List<InventoryService.ReservationItem> {
        // 1. 사용자 존재 여부 검증
        userService.validateUserExists(userId)

        // 2. 장바구니 조회 (CART 도메인)
        val cartItems = cartService.getCartItems(userId)
        if (cartItems.isEmpty()) {
            throw CartEmptyException("장바구니가 비어 있습니다.")
        }

        // 2-1: 상품 옵션 정보 Batch 조회 (PRODUCT 도메인)
        val optionIds = cartItems.map { it.productOptionId }
        val productOptions = productOptionService.getOptionsBatch(optionIds)

        // 2-2: CART + PRODUCT 데이터 조합
        val cartItemDetails = cartItems.map { cartItem ->
            val productOption = productOptions[cartItem.productOptionId]
                ?: throw IllegalStateException("상품 옵션을 찾을 수 없습니다: ${cartItem.productOptionId}")

            CartItemDetail(
                cartItemId = cartItem.id,
                productOptionId = cartItem.productOptionId,
                productName = productOption.productName,
                optionCode = productOption.optionCode,
                origin = productOption.origin,
                grindType = productOption.grindType,
                weightGrams = productOption.weightGrams,
                price = productOption.price,
                quantity = cartItem.quantity,
                totalPrice = productOption.price * cartItem.quantity,
                createdAt = cartItem.createdAt,
                updatedAt = cartItem.updatedAt
            )
        }

        // 3. 상품 옵션 활성 여부 검증 (ProductService에 위임)
        productService.validateProductOptionsActive(optionIds)

        // 4. 재고 예약 생성 (InventoryService에 위임) - 도메인 데이터 반환
        return inventoryService.reserveInventory(userId, cartItemDetails)
    }
}
