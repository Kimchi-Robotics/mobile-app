package com.kimchi.deliverybot.utils

import android.graphics.Bitmap
import android.util.Log
import kotlin.math.cos
import kotlin.math.sin

data class MapInfo(val bitmap: Bitmap, val origin: Pose2D, val resolution: Float) {
    companion object {
        fun empty(): MapInfo {
            return MapInfo(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), Pose2D(0f,0f,0f), 0f)
        }
    }

    // Returns tranforms a pose in the frame of the world to the frame of an Android Bitmap.
    fun WorldToBitmap(worldPose: Pose2D): Pose2D {
        val xWorld = worldPose.x
        val yWorld = worldPose.y
        val worldHeight = bitmap.height * resolution

        val xMtrs = xWorld * cos(origin.theta) - yWorld * sin(origin.theta) + origin.x
        // This considers that the Y-axis of an Android Bitmap starts at the top and goes down.
        val yMtrs = -(xWorld * sin(origin.theta) + yWorld * cos(origin.theta) - (worldHeight - origin.y))
        val theta = - (worldPose.theta + origin.theta)

        return Pose2D(xMtrs/resolution, yMtrs/resolution, theta)
    }

    fun BitmapToWorld(bitmapPose: Pose2D): Pose2D {
        val xMtrs = bitmapPose.x * resolution
        val yMtrs = bitmapPose.y * resolution
        val worldHeight = bitmap.height * resolution

        val xWorld = cos(origin.theta) * (xMtrs - origin.x) + sin(origin.theta) * (worldHeight - yMtrs - origin.y)
        val yWorld = cos(origin.theta) * (worldHeight - yMtrs - origin.y) + sin(origin.theta) * (xMtrs - origin.x)
        val thetaWorld = - (bitmapPose.theta - origin.theta)

        return Pose2D(xWorld, yWorld, thetaWorld)
    }

    fun WorldToBitmap(worldPose: Path.Point2D): Path.Point2D {
        val xWorld = worldPose.x
        val yWorld = worldPose.y
        val worldHeight = bitmap.height * resolution

        val xMtrs = xWorld * cos(origin.theta) - yWorld * sin(origin.theta) + origin.x
        // This considers that the Y-axis of an Android Bitmap starts at the top and goes down.
        val yMtrs = -(xWorld * sin(origin.theta) + yWorld * cos(origin.theta) - (worldHeight - origin.y))

        return Path.Point2D(xMtrs/resolution, yMtrs/resolution)
    }

}
