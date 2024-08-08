package com.boringdroid.systemui.utils

import android.util.Log
import okhttp3.*
import java.io.IOException

class NetworkUtils {
    private val client = OkHttpClient()

    fun postRequest( url :String ,requestBody :FormBody) {
        // URL to which the POST request will be sent
        // Build the request
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        // Execute the request asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle the error
                Log.e(TAG, "Request failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle the response
                    val responseData = response?.body()?.string()
                    Log.i(TAG, "Response: $responseData")
                } else {
                    // Handle the error
                    Log.e(TAG, "Request failed: ${response?.message()}")
                }
            }
        })
    }

    companion object {
        private const val TAG = "NetworkUtils"
    }
}