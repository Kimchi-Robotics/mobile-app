package com.kimchi.deliverybot.utils

import com.kimchi.grpc.RobotStateEnum

enum class RobotState {
    NO_MAP,
    MAPPING_WITH_EXPLORATION,
    MAPPING_WITH_TELEOP,
    NAVIGATION,
    TELEOP,
    IDLE,
    NOT_CONNECTED;

    companion object {
        fun fromKimchiRobotStateEnum(kimchiEnum: RobotStateEnum): RobotState {
            val output = when(kimchiEnum) {
                RobotStateEnum.NO_MAP -> NO_MAP
                RobotStateEnum.MAPPING_WITH_EXPLORATION -> MAPPING_WITH_EXPLORATION
                RobotStateEnum.MAPPING_WITH_TELEOP -> MAPPING_WITH_TELEOP
                RobotStateEnum.NAVIGATING -> NAVIGATION
                RobotStateEnum.TELEOP -> TELEOP
                RobotStateEnum.IDLE -> IDLE
                RobotStateEnum.UNRECOGNIZED -> NOT_CONNECTED
            }

            return output
        }
    }
}
