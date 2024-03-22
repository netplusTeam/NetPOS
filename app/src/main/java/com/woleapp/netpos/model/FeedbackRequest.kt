package com.woleapp.netpos.model

data class FeedbackRequest(
    val feedback: String,
    val subject: String,
    val username: String
)

data class FeedbackResponse(
    val success: Boolean,
    val message: String
)