package com.kimchi.deliverybot.grpc

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.kimchi.grpc.KimchiAppGrpcKt
import com.kimchi.grpc.Pose
import com.kimchi.grpc.Empty
import com.kimchi.grpc.Map
import com.kimchi.grpc.StartMappingResponse
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.Flow
import java.io.Closeable
import android.util.Base64
import com.kimchi.deliverybot.utils.MapInfo
import com.kimchi.deliverybot.utils.Pose2D
import com.kimchi.deliverybot.utils.RobotState
import com.kimchi.grpc.IsAliveResponse
import com.kimchi.grpc.Path
import com.kimchi.grpc.Velocity
import com.kimchi.grpc.RobotStateMsg
import com.kimchi.grpc.StartNavigationResponse
import java.util.concurrent.TimeUnit

class KimchiGrpc(uri: Uri) : Closeable {
    private val TAG = KimchiGrpc::class.qualifiedName
    private val responseState = mutableStateOf("")

    private val channel = let {
        Log.i(TAG, "Connecting to $uri")
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

    fun getMapClient(): Flow<Map>? {
        try {
            val request = Empty.newBuilder().build()
            val response = stub.subscribeToMap(request)
            return response
        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            e.printStackTrace()
        }
        return null
    }

    fun getPathClient(): Flow<Path>? {
        try {
            val request = Empty.newBuilder().build()
            val response = stub.subscribeToPath(request)
            return response
        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            e.printStackTrace()
        }
        return null
    }

    fun getRobotStateClient(): Flow<RobotStateMsg>? {
        try {
            val request = Empty.newBuilder().build()
            val response = stub.subscribeToRobotState(request)
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

    suspend fun startMapping() {
        var response = StartMappingResponse.getDefaultInstance()
        try {
            val request = Empty.newBuilder().build()
            response = stub.startMapping(request)
        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            e.printStackTrace()
        }
        if(!response.success) {
            Log.e(TAG, "Mapping not started: ${response.info}")
        }
    }

    suspend fun startNavigation() {
        var response = StartNavigationResponse.getDefaultInstance()
        try {
            val request = Empty.newBuilder().build()
            response = stub.startNavigation(request)
        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            e.printStackTrace()
        }
        if(!response.success) {
            Log.e(TAG, "Navigation not started: ${response.info}")
        }
    }

    suspend fun navigationCancelGoalService() {
        try {
            val request = Empty.newBuilder().build()
            stub.navigationCancelGoalService(request)
        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            e.printStackTrace()
        }
    }

    suspend fun navigationContinuePathService() {
        try {
            val request = Empty.newBuilder().build()
            stub.navigationContinuePathService(request)
        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            e.printStackTrace()
        }
    }

    suspend fun navigationCancelMissionService() {
        try {
            val request = Empty.newBuilder().build()
            stub.navigationCancelMissionService(request)
        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            e.printStackTrace()
        }
    }


    suspend fun isAlive(): Boolean {
        var response: IsAliveResponse = IsAliveResponse.getDefaultInstance()

        try {
            val request = Empty.newBuilder().build()
            response = stub.isAlive(request)
            Log.i(TAG, "Response: $response, ${response.alive}")

        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            Log.i(TAG, "Error calling isAlive: ${e.message}")
            return false
        }
        Log.i(TAG, "Response: $response, ${response.alive}")

        return response.alive
    }

    // Send a stream of velocity updates to the server using Kotlin Flow
    suspend fun move(velocityFlow: Flow<Velocity>): Empty {
        Log.d(TAG, "Starting new Move stream with Flow")
        return try {
            // The stub.move() method now accepts a Flow<Velocity> and returns Empty
            val response = stub.move(velocityFlow)
            Log.d(TAG, "Move stream completed successfully")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Error in Move stream: ${e.message}")
            throw e
        }
    }

    suspend fun getRobotState(): RobotState {
        var response: RobotStateMsg = RobotStateMsg.getDefaultInstance()
        try {
            val request = Empty.newBuilder().build()
            response = stub.getRobotState(request)
        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            e.printStackTrace()
        }

        return RobotState.fromKimchiRobotStateEnum(response.state)
    }

    suspend fun sendSelectedPose(pose: Pose2D) {
        try {
            Log.e(TAG, "Calling sendsellected pose")
            val request = pose.toProtoGrpcPose()
            Log.e(TAG, "Middle")
            stub.sendSelectedPose(request)
            Log.e(TAG, "Send selected pose called")
        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            e.printStackTrace()
        }

    }

    override fun close() {
        Log.d(TAG, "Closing")
        channel.shutdown()
        if(!channel.awaitTermination(5, TimeUnit.SECONDS)) {
            channel.shutdownNow()
        }
    }
}

