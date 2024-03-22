package com.woleapp.netpos.network

import com.woleapp.netpos.model.FeedbackRequest
import com.woleapp.netpos.model.FeedbackResponse
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface SubmitComplaintsService {

    @POST("merchant-feedback")
    fun feedbackFromMerchants(
        @Body feedbackRequest: FeedbackRequest,
        @Query("partnerId") partnerId: String,
        @Query("deviceSerialId") deviceSerialId: String,
    ): Single<Response<FeedbackResponse>>
}