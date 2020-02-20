package com.example.argame.Model

import com.google.ar.core.HitResult
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.Node

data class ObjectBuilder (
    val hitResult : HitResult,
    val wPos: Vector3,
    //val model: ModelRenderable, TODO: <-- pura osiin
    val scale: Vector3
    //var hpNode: Node? = null TODO: <-- pura osiin
)