package com.kimchi.deliverybot.grpc

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.kimchi.grpc.KimchiAppGrpcKt
import com.kimchi.grpc.Pose
import com.kimchi.grpc.Empty
import com.kimchi.grpc.Map
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.Flow
import java.io.Closeable
import android.util.Base64
import com.kimchi.deliverybot.utils.MapInfo
import com.kimchi.deliverybot.utils.Pose2D
import com.kimchi.grpc.IsAliveResponse
import com.kimchi.grpc.Velocity
import io.grpc.stub.StreamObserver

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

    fun getPoseClient(): Flow<Pose>? {
        try {
            val request = Empty.newBuilder().build()
            val response = stub.getPose(request)
            return response
        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            e.printStackTrace()
        }
        return null
    }

    suspend fun getMap(): MapInfo {
        var response: Map = Map.getDefaultInstance()
        try {
            val request = Empty.newBuilder().build()
            response = stub.getMap(request)
        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            e.printStackTrace()
        }
        val imageBytes = Base64.decode(response.image.toByteArray(), Base64.DEFAULT)
        val bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        return MapInfo(bmp, Pose2D(response.origin.x, response.origin.y, response.origin.theta), response.resolution)
    }

    suspend fun isAlive(): Boolean {
        var response: IsAliveResponse = IsAliveResponse.getDefaultInstance()
        Log.i("Arilow", "Calling is alive")

        try {
            val request = Empty.newBuilder().build()
            response = stub.isAlive(request)
            Log.i("Arilow", "Response: $response, ${response.alive}")

        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            e.printStackTrace()
            return false
        }
        Log.i("Arilow", "Response: $response, ${response.alive}")

        return response.alive
    }

    // Send a stream of velocity updates to the server using Kotlin Flow
    suspend fun move(velocityFlow: Flow<Velocity>): Empty {
        Log.d("Arilow", "Starting new Move stream with Flow")
        return try {
            // The stub.move() method now accepts a Flow<Velocity> and returns Empty
            val response = stub.move(velocityFlow)
            Log.d("Arilow", "Move stream completed successfully")
            response
        } catch (e: Exception) {
            Log.e("Arilow", "Error in Move stream: ${e.message}")
            throw e
        }
    }

    override fun close() {
        channel.shutdownNow()
    }
}