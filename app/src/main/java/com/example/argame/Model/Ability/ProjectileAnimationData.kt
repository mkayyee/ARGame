package com.example.argame.Model.Ability

import android.content.Context
import android.net.Uri
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode

data class ProjectileAnimationData(
    val context: Context,
    val fragment: ArFragment,
    val modelUri: Uri? = null,
    val abilityRenderable: ModelRenderable? = null,
    val gifRenderable: ViewRenderable? = null,
    val targetNode: Node,
    val casterNode: Node
)