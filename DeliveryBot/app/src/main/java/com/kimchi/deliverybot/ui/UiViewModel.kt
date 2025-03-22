package com.kimchi.deliverybot.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import android.view.Display
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kimchi.deliverybot.grpc.KimchiGrpc
import com.kimchi.deliverybot.utils.MapInfo
import com.kimchi.deliverybot.utils.Pose2D
import com.kimchi.deliverybot.utils.RobotState
import com.kimchi.grpc.Velocity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Shared view model between all UI classes. It take care of the communication qirh the gRPC
 * services.
 */
class UiViewModel: ViewModel() {
    private var _robotState = MutableLiveData<RobotState>().apply {
        value = RobotState.WAITING
    }
    var robotState: LiveData<RobotState> = _robotState

    private var _pose = MutableLiveData<Pose2D>().apply {
        value = Pose2D(0f, 0f, 0f)
    }
    var pose: LiveData<Pose2D> = _pose

    private var _mapInfo = MutableLiveData<MapInfo>().apply {
        value = MapInfo.empty()
    }
    var mapInfo: LiveData<MapInfo> = _mapInfo


    // private val uri by lazy { Uri.parse("http://192.168.0.197:50051/") }
//    private val uri by lazy { Uri.parse("http://192.168.103.153:50051/") }
//    private val uri by lazy { Uri.parse("http://192.168.103.56:50051/") }
    private val _uri = Uri.EMPTY
    private var _kimchiService: KimchiGrpc? = null
//    private val kimchiService by lazy { KimchiGrpc(uri) }

    fun callPoseService() {
        Log.i("Arilow", "calling service")
        if(_kimchiService == null) {
            Log.d("Arilow", "gRPC server not yet initialized")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val poseClient = _kimchiService?.getPoseClient()
            withContext(Dispatchers.Main) {
                try {
                    poseClient?.collect { grpcPose ->
                        _pose.apply { value = Pose2D.fromProtoGrpcPose(grpcPose) }
                    }
                } catch (e: Exception) {
                    Log.e("Arilow", "The flow has thrown an exception: $e")
                }
            }
        }
    }

    fun callMapService() {
        Log.i("Arilow", "calling service")
        if(_kimchiService == null) {
            Log.d("Arilow", "gRPC server not yet initialized")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val map =  _kimchiService?.getMap()
                withContext(Dispatchers.Main) {
                    _mapInfo.apply { value = map }
                }
            } catch (e: Exception) {
                Log.e("Arilow", "The flow has thrown an exception: $e")
            }
        }
    }

    fun callMoveService(velocityFlow: Flow<Velocity>) {
        if(_kimchiService == null) {
            Log.d("Arilow", "gRPC server not yet initialized")
            return
        }
        // Launch in a coroutine scope
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Send the velocity flow to the server
                val response = _kimchiService?.move(velocityFlow)
                Log.d("Arilow", "Move RPC completed with response: $response")
            } catch (e: Exception) {
                Log.e("Arilow", "Error in Move flow: ${e.message}")
            }
        }
    }

    fun handleState(robotState: RobotState) {
        _robotState.apply { value = robotState }
    }

    suspend  fun tryUri(uri: Uri): Boolean {
        _kimchiService = KimchiGrpc(uri)

        if(_kimchiService?.isAlive() == true) {
            return true
        }
        _kimchiService = null
        return false
    }
}
