package com.example.argame.Interfaces

import android.content.Context
import android.graphics.Color
import com.example.argame.Model.AnimationAPI
import com.example.argame.Model.ProjectileAnimationData
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.TransformableNode

interface ProjectileAnimator {

    fun animateProjectile(projectileAnimationData: ProjectileAnimationData, node: TransformableNode, cb: () -> Unit) {
        val startPos = projectileAnimationData.startPos
        val endPos = projectileAnimationData.endPos
        AnimationAPI.fireProjectile(node, startPos, endPos) {
            // in the makings of a callback hell
            cb()
        }

    }

    fun instantiateProjectile(projAnimData: ProjectileAnimationData, cb: () -> Unit) {
        // some random test sphere to be fired at a target
        MaterialFactory
            .makeOpaqueWithColor(projAnimData.context, com.google.ar.sceneform.rendering.Color(Color.BLUE))
            .thenAccept {
                val renderable: ModelRenderable = ShapeFactory.makeSphere(0.1f, Vector3(0f, 0.1f, 0f), it)
                val node = TransformableNode(projAnimData.fragment.transformationSystem)
                node.renderable = renderable
                projAnimData.scene.addChild(node)
                animateProjectile(projAnimData, node) {
                    // projectile on its way to the target
                    // cb #...3?
                    cb()
                }
            }
    }
    // TODO
    // needs a reference to fragment.arSceneView.scene
    fun deleteProjectile(anchor: AnchorNode?, scene: Scene) {
        if (anchor != null) {
            scene.removeChild(anchor)
            anchor.anchor!!.detach()
            anchor.setParent(null)
        }
    }
}