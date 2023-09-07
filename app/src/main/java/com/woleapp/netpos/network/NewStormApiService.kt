package com.woleapp.netpos.network

import com.woleapp.netpos.model.GetPartnerInterSwitchThresholdResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path

interface NewStormApiService {

    @GET("/partners/{partnerId}/isw_threshold")
    fun getPartnerInterSwitchThreshold(
        @Path("partnerId") partnerId: String,
    ): Single<GetPartnerInterSwitchThresholdResponse?>
}
