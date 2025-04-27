package com.kimchi.deliverybot.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kimchi.deliverybot.grpc.KimchiGrpc
import com.kimchi.deliverybot.storage.DataStoreRepository
import com.kimchi.deliverybot.utils.MapInfo
import com.kimchi.deliverybot.utils.Pose2D
import com.kimchi.deliverybot.utils.RobotState
import com.kimchi.grpc.Velocity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Shared view model between all UI classes. It takes care of the communication with the gRPC
 * services.
 */
class UiViewModel: ViewModel() {
    private val TAG = UiViewModel::class.qualifiedName

    private var _robotState = MutableLiveData<RobotState>().apply {
        value = RobotState.IDLE
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

    private var _kimchiService: KimchiGrpc? = null
    private var _dataStoreRepo: DataStoreRepository? = null

    fun setDataStoreRepository(repository: DataStoreRepository) {
        _dataStoreRepo = repository
    }

    fun initRobotState() {
        if (_dataStoreRepo == null) {
            Log.e(TAG, "Data Store not initialized. Setting Robot state to NOT_CONNECTED")
            _robotState.apply { value = RobotState.NOT_CONNECTED }
            return
        }

        viewModelScope.launch {
            val savedUri = _dataStoreRepo?.getCurrentIPAddress()
            // Check if there is an IP saved in the Data Store
            if (savedUri == null) {
                _robotState.apply { value = RobotState.NOT_CONNECTED }
                return@launch
            }

            Log.d(TAG, "Trying to connect to: $savedUri")
            // Try to connect to the saved IP.
            if(!tryUri(Uri.parse(savedUri))) {
                _robotState.apply { value = RobotState.NOT_CONNECTED }
                return@launch
            }
            Log.d(TAG, "Connected to: $savedUri")

            Log.d(TAG, "Getting robot State")
            _robotState.apply { value = _kimchiService!!.getRobotState() }
            subscribeToRobotStateService()
            handleCurrentState()
        }
    }

    fun callPoseService() {
        Log.i(TAG, "calling service")
        if(_kimchiService == null) {
            Log.d(TAG, "gRPC server not yet initialized")
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
                    Log.e(TAG, "The flow has thrown an exception: $e")
                }
            }
        }
    }

    private fun callMapService() {
        Log.i(TAG, "calling service")
        if(_kimchiService == null) {
            Log.d(TAG, "gRPC server not yet initialized")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val map =  _kimchiService?.getMap()
                withContext(Dispatchers.Main) {
                    _mapInfo.apply { value = map }
                }
            } catch (e: Exception) {
                Log.e(TAG, "The flow has thrown an exception: $e")
            }
        }
    }

    fun callMoveService(velocityFlow: Flow<Velocity>) {
        if(_kimchiService == null) {
            Log.d(TAG, "gRPC server not yet initialized")
            return
        }
        // Launch in a coroutine scope
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Send the velocity flow to the server
                val response = _kimchiService?.move(velocityFlow)
                Log.d(TAG, "Move RPC completed with response: $response")
            } catch (e: Exception) {
                Log.e(TAG, "Error in Move flow: ${e.message}")
            }
        }
    }

    private fun subscribeToMapService() {
        if(_kimchiService == null) {
            Log.d(TAG, "gRPC server not yet initialized")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val mapClient = _kimchiService?.getMapClient()
            withContext(Dispatchers.Main) {
                try {
                    mapClient?.collect {
                        grpcMap -> _mapInfo.apply {
                            val imageBytes = Base64.decode(grpcMap.image.toByteArray(), Base64.DEFAULT)
                            val bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            value = MapInfo(bmp, Pose2D(grpcMap.origin.x, grpcMap.origin.y, grpcMap.origin.theta), grpcMap.resolution)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "The flow has thrown an exception: $e")
                }
            }
        }
    }

    private fun subscribeToRobotStateService() {
        if(_kimchiService == null) {
            Log.d(TAG, "gRPC server not yet initialized")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val robotStateClient = _kimchiService?.getRobotStateClient()
            withContext(Dispatchers.Main) {
                try {
                    robotStateClient?.collect {
                        grpcRobotState -> handleState(RobotState.fromKimchiRobotStateEnum(grpcRobotState.state))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "The flow has thrown an exception: $e")
                }
            }
        }

    }

    fun callStartMappingService() {
        if(_kimchiService == null) {
            Log.d(TAG, "gRPC server not yet initialized")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _kimchiService!!.startMapping()
            } catch (e: Exception) {
                Log.e(TAG, "The flow has thrown an exception: $e")
            }
        }

    }

    fun callStartNavigationService() {
        if(_kimchiService == null) {
            Log.d(TAG, "gRPC server not yet initialized")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _kimchiService!!.startNavigation()
            } catch (e: Exception) {
                Log.e(TAG, "The flow has thrown an exception: $e")
            }
        }
    }

    fun handleState(robotState: RobotState) {
        if (robotState == _robotState.value) {
            return
        }
        _robotState.apply { value = robotState }
        handleCurrentState()
    }

    private fun handleCurrentState(){
        when(_robotState.value) {
            RobotState.IDLE -> {
                Log.i(TAG, "RobotState.IDLE set!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                // This shouldn't be here because we only want to call those services when navigation starts
                callMapService()
                // init navigation
                callPoseService()
            }
            RobotState.NO_MAP -> {

            } // Dialog saying that there is no map and we required to create one by mapping
            RobotState.MAPPING_WITH_EXPLORATION -> TODO()
            RobotState.MAPPING_WITH_TELEOP -> {
                subscribeToMapService()
//                subscribeToPoseService()

            }
            RobotState.NAVIGATION -> {
                Log.i(TAG, "RobotState.NAVIGATION set!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                // TODO: clean up services and
            }
            RobotState.TELEOP -> TODO()
            RobotState.NOT_CONNECTED -> TODO()
            null -> TODO()
        }
    }
    private suspend fun tryUri(uri: Uri): Boolean {
        _kimchiService = KimchiGrpc(uri)
        if(!_kimchiService!!.isAlive()) {
            _kimchiService = null
            return false
        }

        // Save uri, so it can be obtained again when reopening the App.
        viewModelScope.launch {
            _dataStoreRepo?.saveIPAddress(uri.toString())
            Log.i(TAG, "Uri: $uri saved")
        }

        return true
    }

    override fun onCleared() {
        super.onCleared()
        _kimchiService?.close()
    }
}
