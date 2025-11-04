package com.beanbliss.domain.order.usecase

import com.beanbliss.domain.order.dto.ReserveOrderResponse

/**
 * [책임]: 주문 예약 UseCase의 계약 정의
 * Controller는 이 인터페이스에만 의존합니다 (DIP 준수)
 *
 * [UseCase 패턴]:
 * - 여러 도메인 Repository를 조율
 * - 복잡한 비즈니스 트랜잭션 처리
 */
interface ReserveOrderUseCase {
    /**
     * 주문 예약 실행
     * - 장바구니 조회 및 검증
     * - 중복 예약 방지
     * - 가용 재고 계산
     * - 재고 예약 생성
     *
     * @param userId 사용자 ID
     * @return 예약 결과 (예약 목록)
     * @throws CartEmptyException 장바구니가 비어있는 경우
     * @throws ProductOptionInactiveException 비활성화된 상품 옵션이 포함된 경우
     * @throws DuplicateReservationException 이미 진행 중인 예약이 있는 경우
     * @throws InsufficientAvailableStockException 가용 재고가 부족한 경우
     */
    fun reserveOrder(userId: Long): ReserveOrderResponse
}
