package com.beanbliss.domain.order.usecase

import com.beanbliss.domain.cart.service.CartService
import com.beanbliss.domain.inventory.service.InventoryService
import com.beanbliss.domain.product.service.ProductService
import com.beanbliss.domain.user.service.UserService
import org.springframework.stereotype.Component

/**
 * [책임]: 주문 예약 UseCase 구현
 * - 여러 도메인 Service 조율
 * - 복합 비즈니스 트랜잭션 오케스트레이션
 *
 * [DIP 준수]:
 * - UserService, CartService, ProductService, InventoryService Interface에만 의존
 *
 * [트랜잭션]:
 * - @Transactional은 InventoryService.reserveInventory()에만 적용
 * - 재고 예약 실패 시 롤백
 */
@Component
class ReserveOrderUseCaseImpl(
    private val userService: UserService,
    private val cartService: CartService,
    private val productService: ProductService,
    private val inventoryService: InventoryService
) : ReserveOrderUseCase {

    override fun reserveOrder(userId: Long): List<InventoryService.ReservationItem> {
        // 1. 사용자 존재 여부 검증
        userService.validateUserExists(userId)

        // 2. 장바구니 조회 및 검증 (CartService에 위임)
        val cartItems = cartService.getCartItemsWithProducts(userId)

        // 3. 상품 옵션 활성 여부 검증 (ProductService에 위임)
        val optionIds = cartItems.map { it.productOptionId }
        productService.validateProductOptionsActive(optionIds)

        // 4. 재고 예약 생성 (InventoryService에 위임) - 도메인 데이터 반환
        return inventoryService.reserveInventory(userId, cartItems)
    }
}
