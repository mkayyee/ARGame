package com.example.argame.Model

import android.content.Context
import com.google.ar.core.Anchor
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.ArFragment

data class ProjectileAnimationData(
    val startPos: Vector3,
    val endPos: Vector3,
    val anchor: Anchor,
    val context: Context,
    // scene could be retrieved from fragment
    val scene: Scene,
    val fragment: ArFragment)