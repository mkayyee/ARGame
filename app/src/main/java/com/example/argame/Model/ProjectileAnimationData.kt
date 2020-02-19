package com.example.argame.Model

import android.content.Context
import android.net.Uri
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.ArFragment

data class ProjectileAnimationData(
    val startPos: Vector3,
    val endPos: Vector3,
    val context: Context,
    val fragment: ArFragment,
    val modelUri: Uri? = null)