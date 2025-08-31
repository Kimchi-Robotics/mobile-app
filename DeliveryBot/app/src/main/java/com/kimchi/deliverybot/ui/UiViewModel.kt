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
import com.kimchi.deliverybot.utils.Path
import com.kimchi.deliverybot.utils.Pose2D
import com.kimchi.deliverybot.utils.RobotState
import com.kimchi.grpc.Velocity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Shared view model between all UI classes. It takes care of the communication with the gRPC
 * services.
 */
class UiViewModel: ViewModel() {
    enum class Subscriptions {
        MAP,
        POSE,
        PATH,
        ROBOT_STATE
    }

    // Constant that holds the required grpc subscriptions for each RobotState possible.
    val kStateSubscriptions: Map<RobotState, List<Subscriptions>> = mapOf(
        RobotState.MAPPING_WITH_EXPLORATION to listOf(Subscriptions.POSE, Subscriptions.ROBOT_STATE, Subscriptions.MAP),
        RobotState.MAPPING_WITH_TELEOP to listOf(Subscriptions.POSE, Subscriptions.ROBOT_STATE, Subscriptions.MAP),
        RobotState.NAVIGATION to listOf(Subscriptions.POSE, Subscriptions.ROBOT_STATE, Subscriptions.PATH),
        RobotState.LOCATING to listOf(Subscriptions.POSE, Subscriptions.ROBOT_STATE),
        RobotState.TELEOP to listOf(Subscriptions.POSE, Subscriptions.ROBOT_STATE),
        RobotState.IDLE to listOf(Subscriptions.POSE, Subscriptions.ROBOT_STATE, Subscriptions.PATH),
        RobotState.LOST to listOf(Subscriptions.POSE, Subscriptions.ROBOT_STATE),
        RobotState.RECOVERING to listOf(Subscriptions.POSE, Subscriptions.ROBOT_STATE, Subscriptions.PATH),
        RobotState.GOAL_REACHED to listOf(Subscriptions.POSE, Subscriptions.ROBOT_STATE, Subscriptions.PATH),
        RobotState.NO_MAP to listOf(Subscriptions.POSE, Subscriptions.ROBOT_STATE),
    )

    private val TAG = UiViewModel::class.qualifiedName

    private var _robotState = MutableLiveData<RobotState>().apply {
        value = RobotState.UNKNOWN
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

    private var _robotPath = MutableLiveData<Path>().apply {
        value = Path.empty()
    }
    var path: LiveData<Path> = _robotPath

    private var _subscriptionJobs = mutableMapOf<Subscriptions, Job>()
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
            callMapService()
            handleState(_kimchiService!!.getRobotState())
        }
    }

    fun startTeleoperation() {
        handleState(RobotState.TELEOP)
    }

    fun callPoseService() {
        Log.i(TAG, "calling service")
        if(_kimchiService == null) {
            Log.e(TAG, "gRPC server not yet initialized")
            return
        }
        _subscriptionJobs[Subscriptions.POSE] = viewModelScope.launch(Dispatchers.IO) {
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
            Log.e(TAG, "gRPC server not yet initialized")
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
            Log.e(TAG, "gRPC server not yet initialized")
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
            Log.e(TAG, "gRPC server not yet initialized")
            return
        }

        _subscriptionJobs[Subscriptions.MAP] = viewModelScope.launch(Dispatchers.IO) {
            val mapClient = _kimchiService?.getMapClient()
            withContext(Dispatchers.Main) {
                try {
                    mapClient?.collect {
                        grpcMap -> _mapInfo.apply {
                            if (grpcMap.image.size() == 0) {
                                Log.d(
                                    TAG,
                                    "grpcMap.image size is 0, returning until getting valid map information"
                                )
                                return@apply
                            }
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

    private fun subscribeToPathService() {
        if(_kimchiService == null) {
            Log.e(TAG, "gRPC server not yet initialized")
            return
        }

        _subscriptionJobs[Subscriptions.PATH] = viewModelScope.launch(Dispatchers.IO) {
            val pathClient = _kimchiService?.getPathClient()
            withContext(Dispatchers.Main) {
                try {
                    pathClient?.collect {
                            grpcPath -> _robotPath.apply {
                            value = Path.fromProtoGrpcPath(grpcPath)
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
            Log.e(TAG, "gRPC server not yet initialized")
            return
        }

        _subscriptionJobs[Subscriptions.ROBOT_STATE] = viewModelScope.launch(Dispatchers.IO) {
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
            Log.e(TAG, "gRPC server not yet initialized")
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
            Log.e(TAG, "gRPC server not yet initialized")
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

    fun callNavigationCancelGoalService() {
        if(_kimchiService == null) {
            Log.e(TAG, "gRPC server not yet initialized")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _kimchiService!!.navigationCancelGoalService()
            } catch (e: Exception) {
                Log.e(TAG, "The flow has thrown an exception: $e")
            }
        }
    }

    fun callNavigationContinuePathService() {
        if(_kimchiService == null) {
            Log.e(TAG, "gRPC server not yet initialized")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _kimchiService!!.navigationContinuePathService()
            } catch (e: Exception) {
                Log.e(TAG, "The flow has thrown an exception: $e")
            }
        }
    }

    fun callNavigationCancelMissionService() {
        if(_kimchiService == null) {
            Log.e(TAG, "gRPC server not yet initialized")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _kimchiService!!.navigationCancelMissionService()
            } catch (e: Exception) {
                Log.e(TAG, "The flow has thrown an exception: $e")
            }
        }
    }

    fun handleState(robotState: RobotState) {
        Log.i(TAG, "################################################################################################3")
        Log.i(TAG, "handling new state: ${robotState} when old state is ${_robotState.value}")
        if (robotState == _robotState.value) {
            Log.i(TAG, "Returnin from handle state")
            return
        } else if (_robotState.value == RobotState.MAPPING_WITH_EXPLORATION || _robotState.value == RobotState.MAPPING_WITH_TELEOP){
            if (robotState == RobotState.IDLE ) {
                // Cancel subscription to map
            }
        }
        Log.i(TAG, "Handling new state")

        _robotState.apply { value = robotState }

        Log.i(TAG, "New robotstate is ${_robotState.value}")
        Log.i(TAG, "Required subscriptions are: ${kStateSubscriptions[_robotState.value]}")

        // Unsubscribe from non required services.
        for (job in _subscriptionJobs) {
            if(!kStateSubscriptions[_robotState.value]!!.contains(job.key)){
                Log.i(TAG, "Unsubscribing from: ${job.key}")

                job.value.cancel()
                _subscriptionJobs.remove(job.key)
            }
        }

        // Subscribe to required services if they are not subscribed yet.
        for (requiredJob in kStateSubscriptions[_robotState.value]!!) {
            when (requiredJob) {
                Subscriptions.MAP -> {
                    if (!_subscriptionJobs.contains(Subscriptions.MAP)) {
                        Log.i(TAG, "Subscribing to: ${Subscriptions.MAP}")
                        subscribeToMapService()
                    } else {
                        Log.i(TAG, "Already subscribed to: ${Subscriptions.MAP}")
                    }
                }
                Subscriptions.POSE -> {
                    if (!_subscriptionJobs.contains(Subscriptions.POSE)) {
                        Log.i(TAG, "Subscribing to: ${Subscriptions.POSE}")
                        callPoseService()
                    } else {
                        Log.i(TAG, "Already subscribed to: ${Subscriptions.POSE}")
                    }
                }
                Subscriptions.PATH -> {
                    if (!_subscriptionJobs.contains(Subscriptions.PATH)) {
                        Log.i(TAG, "Subscribing to: ${Subscriptions.PATH}")
                        subscribeToPathService()
                    } else {
                        Log.i(TAG, "Already subscribed to: ${Subscriptions.PATH}")
                    }
                }
                Subscriptions.ROBOT_STATE -> {
                    if (!_subscriptionJobs.contains(Subscriptions.ROBOT_STATE)) {
                        Log.i(TAG, "Subscribing to: ${Subscriptions.ROBOT_STATE}")
                        subscribeToRobotStateService()
                    } else {
                        Log.i(TAG, "Already subscribed to: ${Subscriptions.ROBOT_STATE}")
                    }
                }
            }
        }

//        handleCurrentState()
    }
/*
    private fun handleCurrentState(){
        when(_robotState.value) {
            RobotState.IDLE -> {
                Log.i(TAG, "RobotState.IDLE")
                // This shouldn't be here because we only want to call those services when navigation starts
                callMapService()
                // init navigation
                callPoseService()
            }
            RobotState.NO_MAP -> {
                Log.i(TAG, "RobotState.NO_MAP")

            } // Dialog saying that there is no map and we required to create one by mapping
            RobotState.MAPPING_WITH_EXPLORATION -> {
                Log.i(TAG, "RobotState.MAPPING_WITH_EXPLORATION")
            }
            RobotState.MAPPING_WITH_TELEOP -> {
                subscribeToMapService()
                callPoseService()
            }
            RobotState.NAVIGATION -> {
                Log.i(TAG, "RobotState.NAVIGATION")
                subscribeToPathService()
                // TODO: clean up services and
            }
            RobotState.TELEOP -> {
                Log.i(TAG, "RobotState.TELEOP")
            }
            RobotState.NOT_CONNECTED -> {
                Log.i(TAG, "RobotState.NOT_CONNECTED")
            }
            null -> TODO()
            RobotState.LOCATING -> {
                callPoseService()
                Log.i(TAG, "RobotState.LOCATING")
            }
            RobotState.LOST -> {
                Log.i(TAG, "RobotState.LOST")
                callMapService()
            }
            RobotState.RECOVERING -> {
                Log.i(TAG, "RobotState.RECOVERING")
            }
            RobotState.GOAL_REACHED -> {
                Log.i(TAG, "RobotState.GOAL_REACHED")
            }
            RobotState.CHARGING -> {
                Log.i(TAG, "RobotState.CHARGING")
            }
        }
    }
*/
    fun onSingleTouch(xBitmap: Float, yBitmap: Float) {
        if(_kimchiService == null) {
            Log.e(TAG, "gRPC server not yet initialized")
            return
        }
        Log.i(TAG, "onSingleTouch")

        val poseWorld = _mapInfo.value!!.BitmapToWorld(Pose2D(xBitmap, yBitmap, 0F))

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "onSingleTouch, sending selectedpose")
                _kimchiService!!.sendSelectedPose(poseWorld)
            } catch (e: Exception) {
                Log.e(TAG, "The flow has thrown an exception: $e")
            }
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
