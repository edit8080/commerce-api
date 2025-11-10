package com.beanbliss.domain.cart.service

import com.beanbliss.domain.cart.repository.CartItemDetail
import com.beanbliss.domain.cart.repository.CartItemRepository
import com.beanbliss.domain.order.exception.CartEmptyException
import com.beanbliss.domain.order.exception.ProductOptionInactiveException
import com.beanbliss.domain.product.repository.ProductOptionDetail
import com.beanbliss.domain.product.repository.ProductOptionRepository
import com.beanbliss.common.exception.InvalidParameterException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 장바구니 Upsert 결과
 *
 * @property cartItem 추가/수정된 장바구니 아이템 정보
 * @property isNewItem 신규 아이템 여부 (true: 신규 추가, false: 기존 수량 증가)
 */
data class UpsertCartItemResult(
    val cartItem: CartItemDetail,
    val isNewItem: Boolean
)

/**
 * [책임]: 장바구니 비즈니스 로직 구현
 * - 중복 아이템 처리 (수량 증가)
 * - 최대 수량 제한 검증
 * - Cart 도메인 내 데이터 관리
 * - 장바구니 아이템 검증 (상품 옵션 활성화 여부)
 * - 장바구니 비우기
 *
 * [트랜잭션]: @Transactional을 통해 원자성 보장
 * - 단일 트랜잭션 내에서 조회 → 저장/수정 수행
 * - 예외 발생 시 자동 롤백
 *
 * [DIP 준수]:
 * - CartItemRepository Interface에만 의존
 * - ProductOptionRepository Interface에만 의존
 */
@Service
@Transactional
class CartService(
    private val cartItemRepository: CartItemRepository,
    private val productOptionRepository: ProductOptionRepository
) {

    companion object {
        private const val MAX_QUANTITY = 999
    }

    /**
     * 장바구니 아이템 추가 또는 수정 (Upsert)
     *
     * [비즈니스 규칙]:
     * - 같은 사용자가 같은 상품 옵션을 추가하면 기존 수량 증가
     * - 최대 수량: 999개
     *
     * @param userId 사용자 ID
     * @param productOption 상품 옵션 정보
     * @param quantity 추가할 수량
     * @return 추가/수정된 장바구니 아이템 정보 및 신규 여부
     * @throws InvalidParameterException 최대 수량 초과 시
     */
    fun upsertCartItem(
        userId: Long,
        productOption: ProductOptionDetail,
        quantity: Int
    ): UpsertCartItemResult {
        // 1. 기존 장바구니 아이템 조회
        val existingCartItem = cartItemRepository.findByUserIdAndProductOptionId(
            userId = userId,
            productOptionId = productOption.optionId
        )

        return if (existingCartItem != null) {
            // 2-1. 기존 아이템 존재: 수량 증가
            val newQuantity = existingCartItem.quantity + quantity

            // 최대 수량 검증
            if (newQuantity > MAX_QUANTITY) {
                throw InvalidParameterException("장바구니 내 동일 옵션의 최대 수량은 ${MAX_QUANTITY}개입니다.")
            }

            // 수량 업데이트 후 UpsertCartItemResult로 래핑 (기존 아이템 수정)
            val updatedCartItem = cartItemRepository.updateQuantity(existingCartItem.cartItemId, newQuantity)
            UpsertCartItemResult(
                cartItem = updatedCartItem,
                isNewItem = false
            )
        } else {
            // 2-2. 신규 아이템: 저장
            // 최대 수량 검증
            if (quantity > MAX_QUANTITY) {
                throw InvalidParameterException("장바구니 내 동일 옵션의 최대 수량은 ${MAX_QUANTITY}개입니다.")
            }

            // 신규 장바구니 아이템 생성
            val newCartItem = CartItemDetail(
                cartItemId = 0L, // 신규 저장 시 0
                productOptionId = productOption.optionId,
                productName = productOption.productName,
                optionCode = productOption.optionCode,
                origin = productOption.origin,
                grindType = productOption.grindType,
                weightGrams = productOption.weightGrams,
                price = productOption.price,
                quantity = quantity,
                totalPrice = productOption.price * quantity,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            // 저장 후 UpsertCartItemResult로 래핑 (신규 아이템 생성)
            val savedCartItem = cartItemRepository.save(newCartItem, userId)
            UpsertCartItemResult(
                cartItem = savedCartItem,
                isNewItem = true
            )
        }
    }

    /**
     * 사용자의 장바구니 아이템 목록 조회 (상품 정보 포함)
     *
     * [비즈니스 규칙]:
     * - 장바구니가 비어 있으면 예외 발생
     *
     * @param userId 사용자 ID
     * @return 장바구니 아이템 목록 (상품명, 옵션 코드, 가격 등 포함)
     * @throws CartEmptyException 장바구니가 비어 있는 경우
     */
    fun getCartItemsWithProducts(userId: Long): List<CartItemDetail> {
        // 1. 장바구니 조회
        val cartItems = cartItemRepository.findByUserId(userId)

        // 2. 빈 장바구니 검증
        if (cartItems.isEmpty()) {
            throw CartEmptyException("장바구니가 비어 있습니다.")
        }

        return cartItems
    }

    /**
     * 장바구니 아이템 검증
     *
     * [비즈니스 규칙]:
     * - 모든 상품 옵션이 활성화되어 있는지 확인
     *
     * @param cartItems 검증할 장바구니 아이템 목록
     * @throws ProductOptionInactiveException 비활성화된 상품 옵션이 포함된 경우
     */
    fun validateCartItems(cartItems: List<CartItemDetail>) {
        cartItems.forEach { cartItem ->
            val productOption = productOptionRepository.findActiveOptionWithProduct(cartItem.productOptionId)
            if (productOption == null || !productOption.isActive) {
                throw ProductOptionInactiveException("비활성화된 상품 옵션이 포함되어 있습니다. 상품 옵션 ID: ${cartItem.productOptionId}")
            }
        }
    }

    /**
     * 사용자의 장바구니 비우기
     *
     * [비즈니스 규칙]:
     * - 사용자의 모든 장바구니 아이템 삭제
     *
     * @param userId 사용자 ID
     */
    fun clearCart(userId: Long) {
        cartItemRepository.deleteByUserId(userId)
    }
}
