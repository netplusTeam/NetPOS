package com.woleapp.netpos.network

import com.woleapp.netpos.model.FeedbackRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubmitComplaintsRepository @Inject constructor(
    private val submitComplaintsService: SubmitComplaintsService
) {

    fun feedbackFromMerchants(feedbackRequest: FeedbackRequest,
                              terminalId: String,
                              username: String) = submitComplaintsService.feedbackFromMerchants(
        feedbackRequest, terminalId, username)

}