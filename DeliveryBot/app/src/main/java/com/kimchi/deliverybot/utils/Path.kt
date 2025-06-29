package com.kimchi.deliverybot.utils

import android.util.Log
import com.kimchi.grpc.Path as GrpcPath

data class Path(var points: MutableList<Point2D>) {
    data class Point2D(val x: Float, val y: Float)
    companion object {
        fun fromProtoGrpcPath(grpcPath: GrpcPath): Path {
            var output = mutableListOf<Point2D>()
            Log.e("Arilow", "copying grpc path")

            for (point in grpcPath.pointsList) {
                Log.e("Arilow", "copying point, x: ${point.x}, y: ${point.y}")
                output += Point2D(point.x, point.y)
            }

            return Path(output)
        }

        fun empty(): Path {
            return Path(mutableListOf<Point2D>())
        }
    }

    fun isEmpty(): Boolean {
        return points.size == 0
    }
}
