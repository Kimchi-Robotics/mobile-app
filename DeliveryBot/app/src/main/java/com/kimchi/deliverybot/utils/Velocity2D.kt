package com.kimchi.deliverybot.utils

import com.kimchi.grpc.Pose
import com.kimchi.grpc.Velocity

class Velocity2D(var linear: Float, var angular: Float) {
    fun toProtoGrpcVelocity(): Velocity = Velocity.newBuilder()
                                                    .setLinear(linear)
                                                    .setAngular(angular)
                                                    .build()
}

