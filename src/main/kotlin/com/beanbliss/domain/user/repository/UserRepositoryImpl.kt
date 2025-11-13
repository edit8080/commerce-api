package com.beanbliss.domain.user.repository

import com.beanbliss.domain.user.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * [책임]: Spring Data JPA를 활용한 User 영속성 처리
 * Infrastructure Layer에 속하며, JPA 기술에 종속적
 */
interface UserJpaRepository : JpaRepository<UserEntity, Long>

/**
 * [책임]: UserRepository 인터페이스 구현체
 * - UserJpaRepository를 활용하여 실제 DB 접근
 */
@Repository
class UserRepositoryImpl(
    private val userJpaRepository: UserJpaRepository
) : UserRepository {

    override fun existsById(userId: Long): Boolean {
        return userJpaRepository.existsById(userId)
    }
}
