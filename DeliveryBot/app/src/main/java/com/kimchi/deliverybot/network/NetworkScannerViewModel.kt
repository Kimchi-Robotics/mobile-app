package com.kimchi.deliverybot.network

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kimchi.deliverybot.grpc.KimchiGrpc
import com.kimchi.deliverybot.storage.DataStoreRepository
import kotlinx.coroutines.launch

class NetworkScannerViewModel: ViewModel() {

    private val TAG = NetworkScannerViewModel::class.qualifiedName
    private var _kimchiService: KimchiGrpc? = null
    private var _dataStoreRepo: DataStoreRepository? = null

    fun setDataStoreRepository(repository: DataStoreRepository) {
        _dataStoreRepo = repository
    }

    suspend  fun tryUri(uri: Uri): Boolean {
        _kimchiService?.close()
        _kimchiService = KimchiGrpc(uri)

        if(_kimchiService?.isAlive() == true) {
            // Save uri, so it can be obtained again when reopening the App.
            viewModelScope.launch {
                _dataStoreRepo?.saveIPAddress(uri.toString())
                Log.i(TAG, "Uri: $uri saved")
            }

            return true
        }

        _kimchiService = null
        return false
    }

    override fun onCleared() {
        super.onCleared()
        _kimchiService?.close()
    }

}
