package com.kimchi.deliverybot.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import io.grpc.ManagedChannelBuilder
import io.grpc.examples.helloworld.GreeterGrpcKt
import io.grpc.examples.helloworld.helloRequest
import kotlinx.coroutines.asExecutor
import java.io.Closeable

class UiViewModel: ViewModel() {
    private val uri by lazy { Uri.parse("http://192.168.0.197:50051/") }

    private val greeterService by lazy { GreeterRCP(uri) }

    fun callService() {
        Log.i("Arilow", "calling service")
        viewModelScope.launch(Dispatchers.IO) {
            greeterService.sayHello("ari")
        }

        if (greeterService.responseState.value.isNotEmpty()) {
            Log.i("Arilow", "Service response: " + greeterService.responseState.value);
        } else {
            Log.i("Arilow", "Service not responded");
        }
    }
}

class GreeterRCP(uri: Uri) : Closeable {
    val responseState = mutableStateOf("")

    private val channel = let {
        println("Connecting to ${uri.host}:${uri.port}")

        val builder = ManagedChannelBuilder.forAddress(uri.host, uri.port)
        if (uri.scheme == "https") {
            builder.useTransportSecurity()
        } else {
            builder.usePlaintext()
        }

        builder.executor(Dispatchers.IO.asExecutor()).build()
    }

    private val greeter = GreeterGrpcKt.GreeterCoroutineStub(channel)

    suspend fun sayHello(name: String) {
        try {
            val request = helloRequest { this.name = name }
            val response = greeter.sayHello(request)
            responseState.value = response.message
        } catch (e: Exception) {
            responseState.value = e.message ?: "Unknown Error"
            e.printStackTrace()
        }
    }

    override fun close() {
        channel.shutdownNow()
    }
}