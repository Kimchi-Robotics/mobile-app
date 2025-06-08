package com.kimchi.deliverybot.utils
import com.kimchi.grpc.Pose

data class Pose2D(val x: Float, val y: Float, val theta: Float) {
    companion object {
        fun fromProtoGrpcPose(pose: Pose): Pose2D = Pose2D(pose.x, pose.y, pose.theta)
    }
    fun toProtoGrpcPose(): Pose = Pose.newBuilder().setX(x)
                                      .setY(y)
                                      .setTheta(theta)
                                      .build()
}
