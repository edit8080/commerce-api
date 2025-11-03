package com.beanbliss.domain.user.repository

/**
 * [책임]: 사용자 영속성 계층의 '계약' 정의.
 * Service는 이 인터페이스에만 의존합니다. (DIP 준수)
 *
 * [주요 기능]:
 * - 사용자 존재 여부 확인
 */
interface UserRepository {
    /**
     * 사용자 ID로 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @return 존재 여부 (true: 존재, false: 미존재)
     */
    fun existsById(userId: Long): Boolean
}
