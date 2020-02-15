package com.example.argame.Model

import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator
import com.google.ar.sceneform.ux.TransformableNode

object AnimationAPI {

//// Ability testiä /////////
//    tposeNode.setOnTapListener {_, _ ->
//        val animationData = ProjectileAnimationData(
//            tposeAnchorNode.worldPosition,
//            duckAnchorNode.worldPosition,
//            tposeAnchor,
//            this,
//            fragment.arSceneView.scene,
//            fragment
//        )
//        tposeNPC.useAbility(
//            Ability("sphere", 20.0, "XD"),
//            tposeNPC,
//            animationData,
//            this
//        ) {
//            hpRenderableDuck?.view?.textView_healthbar?.text = duckNPC.getStatus().currentHealth.toString()
//        }
//    }
//    //////////////////

    // helper singleton for animations

    fun fireProjectile(model: TransformableNode, startPos: Vector3, endPos: Vector3, callback: () -> Unit) {
        val objectAnimation = ObjectAnimator()
        objectAnimation.setAutoCancel(true)
        objectAnimation.setTarget(model)
        objectAnimation.setObjectValues(startPos, endPos)
        objectAnimation.setPropertyName("worldPosition")
        objectAnimation.setEvaluator(Vector3Evaluator())
        objectAnimation.setInterpolator(LinearInterpolator())
        objectAnimation.setDuration(2000)
        objectAnimation.start()
        // currently executed immediately --
        // could implement some logic to see if it reached the target
        callback()
    }
}