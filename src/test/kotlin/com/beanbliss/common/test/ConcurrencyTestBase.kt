package com.beanbliss.common.test

import com.beanbliss.CommerceApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * [책임]: 동시성 테스트를 위한 공통 Base 클래스
 *
 * @SpringBootTest를 사용하여 전체 애플리케이션 컨텍스트 로드
 * - Service, Repository, UseCase 등 모든 빈이 주입됨
 * - TestContainers를 사용하여 실제 MySQL 환경에서 테스트
 * - 동시성 제어 로직 검증 (비관적 락, 트랜잭션 격리 등)
 *
 * 사용 방법:
 * ```
 * @DisplayName("재고 추가 동시성 테스트")
 * class InventoryAddStockConcurrencyTest : ConcurrencyTestBase() {
 *     @Autowired
 *     private lateinit var inventoryService: InventoryService
 *
 *     @Test
 *     fun `테스트 메서드`() { ... }
 * }
 * ```
 */
@SpringBootTest(classes = [CommerceApplication::class])
@Testcontainers
abstract class ConcurrencyTestBase {

    companion object {
        @Container
        @JvmStatic
        val mysqlContainer = MySQLContainer<Nothing>("mysql:8.0").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
            withReuse(true)
            waitingFor(Wait.forListeningPort())
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
