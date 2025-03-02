package com.kimchi.deliverybot.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.kimchi.deliverybot.grpc.KimchiGrpc
import com.kimchi.deliverybot.utils.Pose2D
import kotlinx.coroutines.withContext

class UiViewModel: ViewModel() {
    private var _pose = MutableLiveData<Pose2D>().apply {
        value = Pose2D(0f, 0f, 0f)
    }
    var pose: LiveData<Pose2D> = _pose

    // private val uri by lazy { Uri.parse("http://192.168.0.197:50051/") }
    private val uri by lazy { Uri.parse("http://172.30.124.125:50051/") }
    private val kimchiService by lazy { KimchiGrpc(uri) }

    fun callService() {
        Log.i("Arilow", "calling service")
        viewModelScope.launch(Dispatchers.IO) {
            val poseClient = kimchiService.getPoseClient()
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
}
