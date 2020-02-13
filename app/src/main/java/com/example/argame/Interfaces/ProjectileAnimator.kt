package com.example.argame.Interfaces

import com.google.ar.core.Pose

interface ProjectileAnimator {

    fun animateProjectile(startPose: Pose, endPose: Pose)
}