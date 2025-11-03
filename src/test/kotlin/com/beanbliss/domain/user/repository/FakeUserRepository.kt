package com.beanbliss.domain.user.repository

/**
 * 테스트용 In-Memory UserRepository 구현체
 *
 * [특징]:
 * - 실제 DB 없이 메모리 기반으로 동작
 * - Repository 인터페이스의 계약을 준수
 * - 테스트 격리를 위해 각 테스트마다 새 인스턴스 생성
 */
class FakeUserRepository : UserRepository {

    private val userIds = mutableSetOf<Long>()

    override fun existsById(userId: Long): Boolean {
        return userIds.contains(userId)
    }

    // === Test Helper Methods ===

    /**
     * 테스트용: 사용자 ID 추가
     */
    fun addUser(userId: Long) {
        userIds.add(userId)
    }

    /**
     * 테스트용: 모든 데이터 삭제
     */
    fun clear() {
        userIds.clear()
    }
}
