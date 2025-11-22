package com.beanbliss.common.test

import com.beanbliss.CommerceApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * [책임]: 동시성 테스트를 위한 공통 Base 클래스
 *
 * @SpringBootTest를 사용하여 전체 애플리케이션 컨텍스트 로드
 * - Service, Repository, UseCase 등 모든 빈이 주입됨
 * - Docker 기반 MySQL 환경에서 테스트
 * - 동시성 제어 로직 검증 (비관적 락, 트랜잭션 격리 등)
 *
 * 사전 준비:
 * ```bash
 * # Docker Compose로 MySQL 컨테이너 시작
 * docker-compose -f docker-compose.test.yml up -d
 * ```
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
@ActiveProfiles("test")
abstract class ConcurrencyTestBase
