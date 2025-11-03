package com.beanbliss.domain.cart.service

import com.beanbliss.domain.cart.dto.AddToCartRequest
import com.beanbliss.domain.cart.dto.CartItemResponse
import com.beanbliss.domain.cart.repository.CartItemRepository
import com.beanbliss.domain.product.repository.ProductOptionRepository
import com.beanbliss.domain.user.repository.UserRepository
import com.beanbliss.common.exception.ResourceNotFoundException
import com.beanbliss.common.exception.InvalidParameterException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * [책임]: 장바구니 비즈니스 로직 구현
 * - 사용자 및 상품 옵션 검증
 * - 중복 아이템 처리 (수량 증가)
 * - 최대 수량 제한 검증
 *
 * [트랜잭션]: @Transactional을 통해 원자성 보장
 * - 단일 트랜잭션 내에서 검증 → 저장/수정 수행
 * - 예외 발생 시 자동 롤백
 */
@Service
@Transactional
class CartServiceImpl(
    private val cartItemRepository: CartItemRepository,
    private val productOptionRepository: ProductOptionRepository,
    private val userRepository: UserRepository
) : CartService {

    companion object {
        private const val MAX_QUANTITY = 999
    }

    override fun addToCart(request: AddToCartRequest): AddToCartResult {
        // 1. 사용자 검증
        if (!userRepository.existsById(request.userId)) {
            throw ResourceNotFoundException("사용자 ID: ${request.userId}를 찾을 수 없습니다.")
        }

        // 2. 상품 옵션 검증 (활성 옵션만 조회)
        val productOption = productOptionRepository.findActiveOptionWithProduct(request.productOptionId)
            ?: throw ResourceNotFoundException("상품 옵션 ID: ${request.productOptionId}를 찾을 수 없습니다.")

        // 3. 기존 장바구니 아이템 조회
        val existingCartItem = cartItemRepository.findByUserIdAndProductOptionId(
            userId = request.userId,
            productOptionId = request.productOptionId
        )

        return if (existingCartItem != null) {
            // 4-1. 기존 아이템 존재: 수량 증가
            val newQuantity = existingCartItem.quantity + request.quantity

            // 최대 수량 검증
            if (newQuantity > MAX_QUANTITY) {
                throw InvalidParameterException("장바구니 내 동일 옵션의 최대 수량은 ${MAX_QUANTITY}개입니다.")
            }

            // 수량 업데이트 후 AddToCartResult로 래핑 (기존 아이템 수정)
            val updatedCartItem = cartItemRepository.updateQuantity(existingCartItem.cartItemId, newQuantity)
            AddToCartResult(
                cartItem = updatedCartItem,
                isNewItem = false
            )
        } else {
            // 4-2. 신규 아이템: 저장
            // 최대 수량 검증
            if (request.quantity > MAX_QUANTITY) {
                throw InvalidParameterException("장바구니 내 동일 옵션의 최대 수량은 ${MAX_QUANTITY}개입니다.")
            }

            // 신규 장바구니 아이템 생성
            val newCartItem = CartItemResponse(
                cartItemId = 0L, // 신규 저장 시 0
                productOptionId = productOption.optionId,
                productName = productOption.productName,
                optionCode = productOption.optionCode,
                origin = productOption.origin,
                grindType = productOption.grindType,
                weightGrams = productOption.weightGrams,
                price = productOption.price,
                quantity = request.quantity,
                totalPrice = productOption.price * request.quantity,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            // 저장 후 AddToCartResult로 래핑 (신규 아이템 생성)
            val savedCartItem = cartItemRepository.save(newCartItem, request.userId)
            AddToCartResult(
                cartItem = savedCartItem,
                isNewItem = true
            )
        }
    }
}
