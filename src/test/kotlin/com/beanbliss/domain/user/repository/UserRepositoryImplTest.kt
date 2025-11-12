package com.beanbliss.domain.user.repository

import com.beanbliss.common.test.RepositoryTestBase
import com.beanbliss.domain.user.entity.UserEntity
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("User Repository 통합 테스트")
class UserRepositoryImplTest : RepositoryTestBase() {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testUser: UserEntity

    @BeforeEach
    fun setUpTestData() {
        // 테스트 사용자 생성
        testUser = UserEntity(
            email = "test@example.com",
            password = "password123",
            name = "테스트 사용자"
        )
        entityManager.persist(testUser)
        entityManager.flush()
        entityManager.clear()
    }

    @Test
    @DisplayName("사용자 ID로 존재 여부 확인 - 존재하는 경우")
    fun `existsById should return true when user exists`() {
        // When
        val exists = userRepository.existsById(testUser.id)

        // Then
        assertTrue(exists)
    }

    @Test
    @DisplayName("사용자 ID로 존재 여부 확인 - 존재하지 않는 경우")
    fun `existsById should return false when user does not exist`() {
        // When
        val exists = userRepository.existsById(999L)

        // Then
        assertFalse(exists)
    }
}
