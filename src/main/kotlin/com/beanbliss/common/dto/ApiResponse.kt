package com.beanbliss.common.dto

data class ApiResponse<T>(
    val data: T
)

data class ErrorResponse(
    val error: ErrorDetail
)

data class ErrorDetail(
    val code: String,
    val message: String
)
