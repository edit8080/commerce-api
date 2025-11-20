## 동시성 제어 구현 대상

### 1. Inventory 도메인 - 재고 관리

**동시성 제어 메커니즘**:
- PESSIMISTIC_WRITE

1. **재고 추가**: `InventoryService.addStock()`
2. **재고 차감**: `InventoryService.reduceStockForOrder()`
3. **재고 예약**: `InventoryService.reserveInventory()`
4. **예약 확인**: `InventoryReservationService.confirmReservations()`

### 2. Cart 도메인 - 장바구니 아이템 추가

**동시성 제어 메커니즘**:
- PESSIMISTIC_WRITE

1. **장바구니 아이템 추가**: `CartService.addCartItem()`

---

### 3. Coupon 도메인 - 선착순 쿠폰 발급

**동시성 제어 메커니즘**:
- **FOR UPDATE SKIP LOCKED** 사용
- PESSIMISTIC_WRITE + SKIP LOCKED

- `CouponIssueUseCase.issueCoupon()`
  1. 티켓 선점 (FOR UPDATE SKIP LOCKED)
  2. 사용자 쿠폰 생성
  3. 티켓 상태 변경 (AVAILABLE → ISSUED)

### 4. Order 도메인 - 주문 생성

**동시성 제어 메커니즘**:
- PESSIMISTIC_WRITE

1. **동일 사용자 동시 주문**: 같은 사용자가 동시에 2개 주문 생성
   - 장바구니 비우기 타이밍
   - 재고 예약 중복 방지
   - 잔액 이중 차감 방지

2. **재고 예약 만료 타이밍**: 예약 만료 시점과 주문 생성 시점의 Race Condition
   - 30분 예약 만료 직전에 주문 생성 시도

3. **쿠폰 동시 사용**: 여러 주문에서 동일 쿠폰 사용 시도

---

## 구현된 동시성 제어 패턴

### 1. 비관적 락 (PESSIMISTIC_WRITE)

**사용 영역**: Balance, Inventory

**패턴**:
```kotlin
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT ... FROM ... WHERE ... FOR UPDATE")
fun findByIdWithLock(id: Long): Entity?
```
---

### 2. FOR UPDATE SKIP LOCKED

**사용 영역**: Coupon 티켓 발급

**패턴**:
```kotlin
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(QueryHint(name = "javax.persistence.lock.timeout", value = "-2"))
@Query("SELECT ... FROM ... WHERE ... LIMIT 1")
fun findAvailableTicketWithLock(): Entity?
```
