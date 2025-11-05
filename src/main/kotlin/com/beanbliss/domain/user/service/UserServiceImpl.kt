package com.beanbliss.domain.user.service

import com.beanbliss.domain.user.repository.UserRepository
import com.beanbliss.common.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [책임]: 사용자 관리 기능 구현
 *
 * [주요 기능]:
 * - 사용자 존재 여부 확인 및 검증
 *
 * [DIP 준수]:
 * - UserRepository Interface에만 의존
 *
 * [트랜잭션]:
 * - 읽기 전용 트랜잭션 사용
 */
@Service
@Transactional(readOnly = true)
class UserServiceImpl(
    private val userRepository: UserRepository
) : UserService {

    /**
     * 사용자 존재 여부 확인 및 검증
     *
     * [비즈니스 규칙]:
     * - 사용자가 존재하지 않으면 ResourceNotFoundException 발생
     *
     * @param userId 사용자 ID
     * @throws ResourceNotFoundException 사용자가 존재하지 않는 경우
     */
    override fun validateUserExists(userId: Long) {
        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException("사용자 ID: ${userId}를 찾을 수 없습니다.")
        }
    }
}
