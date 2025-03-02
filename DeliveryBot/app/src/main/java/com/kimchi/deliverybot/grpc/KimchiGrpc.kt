package com.kimchi.deliverybot.grpc

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.kimchi.grpc.KimchiAppGrpcKt
import com.kimchi.grpc.empty
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.io.Closeable
import com.kimchi.grpc.Pose
import kotlinx.coroutines.flow.Flow

class KimchiGrpc(uri: Uri) : Closeable {
    val responseState = mutableStateOf("")
    private val channel = let {
        Log.i("Arilow", "Connecting to ${uri.host}:${uri.port}")

        val builder = ManagedChannelBuilder.forAddress(uri.host, uri.port)
        if (uri.scheme == "https") {
            builder.useTransportSecurity()
        } else {
            builder.usePlaintext()
        }

        builder.executor(Dispatchers.IO.asExecutor()).build()
    }
    private val stub = KimchiAppGrpcKt.KimchiAppCoroutineStub(channel)

    suspend fun getPoseClient(): Flow<Pose>? {
        try {
            val request = empty {}
            val response = stub.getPose(request)
            return response
//            try {
//                response.collect { value ->
//                    Log.i("Arilow", "Received response $value");
//                }
//            } catch (e: Exception) {
//                Log.e("Arilow", "The flow has thrown an exception: $e")
//            }
//
//            responseState.value = "lalalal"
        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            e.printStackTrace()
        }
        return null
    }
    override fun close() {
        channel.shutdownNow()
    }
}