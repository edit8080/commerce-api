package com.beanbliss.common.logging

import com.p6spy.engine.spy.appender.MessageFormattingStrategy
import java.text.SimpleDateFormat
import java.util.*

/**
 * P6Spy 커스텀 로그 포맷
 *
 * 출력 형식: yyyy-MM-dd HH:mm:ss | [실행시간ms] | 카테고리 | SQL
 */
class P6SpyFormatter : MessageFormattingStrategy {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    override fun formatMessage(
        connectionId: Int,
        now: String,
        elapsed: Long,
        category: String,
        prepared: String,
        sql: String,
        url: String
    ): String {
        // 현재 시간 포맷
        val timestamp = dateFormat.format(Date())

        // SQL이 비어있으면 로깅하지 않음
        if (sql.trim().isEmpty()) {
            return ""
        }

        // 포맷: 시간 | [실행시간ms] | 카테고리 | SQL
        return "$timestamp | [${elapsed}ms] | $category | ${sql.trim()}"
    }
}
