package com.beanbliss.common.test

import com.beanbliss.domain.cart.repository.CartItemRepositoryImpl
import com.beanbliss.domain.coupon.repository.CouponRepositoryImpl
import com.beanbliss.domain.coupon.repository.CouponTicketRepositoryImpl
import com.beanbliss.domain.coupon.repository.UserCouponRepositoryImpl
import com.beanbliss.domain.inventory.repository.InventoryRepositoryImpl
import com.beanbliss.domain.inventory.repository.InventoryReservationRepositoryImpl
import com.beanbliss.domain.order.repository.OrderItemRepositoryImpl
import com.beanbliss.domain.order.repository.OrderRepositoryImpl
import com.beanbliss.domain.product.repository.ProductOptionRepositoryImpl
import com.beanbliss.domain.product.repository.ProductRepositoryImpl
import com.beanbliss.domain.user.repository.BalanceRepositoryImpl
import com.beanbliss.domain.user.repository.UserRepositoryImpl
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * [책임]: Repository 통합 테스트를 위한 공통 Base 클래스
 *
 * TestContainers를 사용하여 실제 MySQL 환경에서 테스트
 * - 프로덕션과 동일한 DB 환경 보장
 * - 격리된 테스트 환경 제공
 * - 각 테스트는 독립적으로 실행됨
 *
 * 사용 방법:
 * ```
 * @DisplayName("CartItem Repository 통합 테스트")
 * class CartItemRepositoryImplTest : RepositoryTestBase() {
 *     @Autowired
 *     private lateinit var cartItemRepository: CartItemRepository
 *
 *     @Test
 *     fun `테스트 메서드`() { ... }
 * }
 * ```
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    value = [
        CouponRepositoryImpl::class,
        CouponTicketRepositoryImpl::class,
        UserCouponRepositoryImpl::class,
        CartItemRepositoryImpl::class,
        InventoryRepositoryImpl::class,
        InventoryReservationRepositoryImpl::class,
        OrderRepositoryImpl::class,
        OrderItemRepositoryImpl::class,
        ProductRepositoryImpl::class,
        ProductOptionRepositoryImpl::class,
        UserRepositoryImpl::class,
        BalanceRepositoryImpl::class
    ]
)
@org.springframework.test.context.ActiveProfiles("test")
abstract class RepositoryTestBase {

    companion object {
        @Container
        @JvmStatic
        val mysqlContainer = MySQLContainer<Nothing>("mysql:8.0").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl)
            registry.add("spring.datasource.username", mysqlContainer::getUsername)
            registry.add("spring.datasource.password", mysqlContainer::getPassword)
        }
    }
}
