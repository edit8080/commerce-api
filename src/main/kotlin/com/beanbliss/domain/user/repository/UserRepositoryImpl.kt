package com.beanbliss.domain.user.repository

import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * [책임]: 사용자 In-memory 저장소 구현
 * - ConcurrentHashMap.KeySetView를 사용하여 Thread-safe 보장
 * - 사용자 ID 존재 여부만 관리 (최소한의 구현)
 *
 * [주의사항]:
 * - 애플리케이션 재시작 시 데이터 소실 (In-memory 특성)
 * - 실제 DB 사용 시 JPA 기반 구현체로 교체 필요
 */
@Repository
class UserRepositoryImpl : UserRepository {

    // Thread-safe한 In-memory 저장소 (ID만 관리)
    private val userIds = ConcurrentHashMap.newKeySet<Long>()

    override fun existsById(userId: Long): Boolean {
        return userIds.contains(userId)
    }

    /**
     * 테스트용 헬퍼 메서드: 사용자 ID 추가
     */
    fun add(userId: Long) {
        userIds.add(userId)
    }

    /**
     * 테스트용 헬퍼 메서드: 모든 데이터 삭제
     */
    fun clear() {
        userIds.clear()
    }
}
