package com.beanbliss.domain.user.service

/**
 * [책임]: 사용자 관리 기능의 '계약' 정의.
 * UseCase는 이 인터페이스에만 의존합니다. (DIP 준수)
 *
 * [주요 기능]:
 * - 사용자 존재 여부 확인 및 검증
 */
interface UserService {
    /**
     * 사용자 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @throws ResourceNotFoundException 사용자가 존재하지 않는 경우
     */
    fun validateUserExists(userId: Long)
}
