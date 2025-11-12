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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * [책임]: 성능 테스트를 위한 공통 Base 클래스 (P6Spy 활성화)
 *
 * TestContainers + P6Spy를 함께 사용하여 실제 쿼리 로깅
 * - 프로덕션과 동일한 DB 환경 보장
 * - P6Spy를 통한 상세한 쿼리 로깅
 * - 쿼리 실행 시간 측정
 *
 * 사용 방법:
 * ```
 * @DisplayName("성능 테스트")
 * class MyPerformanceTest : PerformanceTestBase() {
 *     @Test
 *     fun `성능 테스트`() { ... }
 * }
 * ```
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
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
abstract class PerformanceTestBase {

    companion object {
        @Container
        @JvmStatic
        val mysqlContainer = MySQLContainer<Nothing>("mysql:8.0").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }

        /**
         * TestContainers URL을 P6Spy로 래핑하여 DataSource 설정
         */
        @DynamicPropertySource
        @JvmStatic
        fun configureDataSourceWithP6Spy(registry: DynamicPropertyRegistry) {
            // TestContainers의 원본 JDBC URL
            val originalJdbcUrl = mysqlContainer.jdbcUrl

            // jdbc:mysql://... -> jdbc:p6spy:mysql://...
            val p6spyJdbcUrl = originalJdbcUrl.replace("jdbc:mysql://", "jdbc:p6spy:mysql://")

            // DataSource 설정
            registry.add("spring.datasource.url") { p6spyJdbcUrl }
            registry.add("spring.datasource.username") { mysqlContainer.username }
            registry.add("spring.datasource.password") { mysqlContainer.password }
            registry.add("spring.datasource.driver-class-name") { "com.p6spy.engine.spy.P6SpyDriver" }
        }
    }
}
