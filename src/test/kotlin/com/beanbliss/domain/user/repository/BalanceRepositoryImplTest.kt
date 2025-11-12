package com.beanbliss.domain.user.repository

import com.beanbliss.common.test.RepositoryTestBase
import com.beanbliss.domain.user.entity.BalanceEntity
import com.beanbliss.domain.user.entity.UserEntity
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

@DisplayName("Balance Repository 통합 테스트")
class BalanceRepositoryImplTest : RepositoryTestBase() {

    @Autowired
    private lateinit var balanceRepository: BalanceRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testUser: UserEntity
    private lateinit var testBalance: BalanceEntity

    @BeforeEach
    fun setUpTestData() {
        // 테스트 사용자 생성
        testUser = UserEntity(
            email = "test@example.com",
            password = "password123",
            name = "테스트 사용자"
        )
        entityManager.persist(testUser)

        // 테스트 잔액 생성
        testBalance = BalanceEntity(
            userId = testUser.id,
            amount = BigDecimal("100000")
        )
        entityManager.persist(testBalance)

        entityManager.flush()
        entityManager.clear()
    }

    @Test
    @DisplayName("사용자 ID로 잔액 조회 - 성공")
    fun `findByUserId should return balance when exists`() {
        // When
        val balance = balanceRepository.findByUserId(testUser.id)

        // Then
        assertNotNull(balance)
        assertEquals(testUser.id, balance!!.userId)
        assertEquals(0, BigDecimal("100000.00").compareTo(balance.amount))
    }

    @Test
    @DisplayName("사용자 ID로 잔액 조회 - 존재하지 않는 경우")
    fun `findByUserId should return null when not exists`() {
        // When
        val balance = balanceRepository.findByUserId(999L)

        // Then
        assertNull(balance)
    }

    @Test
    @DisplayName("사용자 ID로 잔액 조회 (비관적 락) - 성공")
    fun `findByUserIdWithLock should return balance with lock`() {
        // When
        val balance = balanceRepository.findByUserIdWithLock(testUser.id)

        // Then
        assertNotNull(balance)
        assertEquals(testUser.id, balance!!.userId)
        assertEquals(0, BigDecimal("100000.00").compareTo(balance.amount))
    }

    @Test
    @DisplayName("사용자 ID로 잔액 조회 (비관적 락) - 존재하지 않는 경우")
    fun `findByUserIdWithLock should return null when not exists`() {
        // When
        val balance = balanceRepository.findByUserIdWithLock(999L)

        // Then
        assertNull(balance)
    }

    @Test
    @DisplayName("잔액 저장 - 신규 생성")
    fun `save should create new balance`() {
        // Given: 새로운 사용자 생성
        val newUser = UserEntity(
            email = "new@example.com",
            password = "password123",
            name = "신규 사용자"
        )
        entityManager.persist(newUser)
        entityManager.flush()
        entityManager.clear()

        val newBalance = BalanceEntity(
            userId = newUser.id,
            amount = BigDecimal("50000")
        )

        // When
        val savedBalance = balanceRepository.save(newBalance)

        // Then
        assertTrue(savedBalance.id > 0)
        assertEquals(newUser.id, savedBalance.userId)
        assertEquals(0, BigDecimal("50000.00").compareTo(savedBalance.amount))
    }

    @Test
    @DisplayName("잔액 저장 - 기존 잔액 업데이트")
    fun `save should update existing balance`() {
        // Given: 기존 잔액 조회 및 수정
        val existingBalance = balanceRepository.findByUserId(testUser.id)!!
        val updatedBalance = BalanceEntity(
            id = existingBalance.id,
            userId = existingBalance.userId,
            amount = BigDecimal("200000"),
            createdAt = existingBalance.createdAt
        )

        // When
        val savedBalance = balanceRepository.save(updatedBalance)

        // Then
        assertEquals(existingBalance.id, savedBalance.id)
        assertEquals(0, BigDecimal("200000.00").compareTo(savedBalance.amount))
    }
}
