package com.kimchi.deliverybot.draw

import android.graphics.Canvas

class MarkersBitmapArray {
    var markersArray = mutableListOf<MarkerBitmap>()

    fun drawOnCanvas(canvas: Canvas) {
        for (marker in markersArray) {
            marker.drawOnCanvas(canvas)
        }
    }

    fun addMarker(newMarker: MarkerBitmap) {
        newMarker.setNumber(markersArray.size + 1)
        markersArray += newMarker
    }

    fun clear() {
        markersArray.clear()
    }
}