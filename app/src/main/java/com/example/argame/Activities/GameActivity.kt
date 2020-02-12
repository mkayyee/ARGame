package com.example.argame.Activities

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.example.argame.Fragments.CustomArFragment
import com.example.argame.Fragments.MenuFragmentController
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.R
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.TransformableNode

class GameActivity : AppCompatActivity(), FragmentCallbackListener {

    private val menuFragController = MenuFragmentController()
    private lateinit var fragment: CustomArFragment
    private lateinit var duckUri: Uri
    private lateinit var tposeUri: Uri
    private var renderedDuck: ModelRenderable? = null
    private var renderedTpose: ModelRenderable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        fragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as CustomArFragment

        val menuBtn = findViewById<Button>(R.id.menuBtn)
        menuBtn.setOnClickListener {
            initMenuContainer()
        }

        val spawnBtn = findViewById<Button>(R.id.spawnBtn)
        spawnBtn.setOnClickListener {
            spawnObjects()
        }

        // v Turn this mess into a to a proper function!!! v

        duckUri = Uri.parse("duck.sfb")
        tposeUri = Uri.parse("Character.sfb")
        val renderableFuture = ModelRenderable.builder()
            .setSource(this, duckUri)
            .build()
        renderableFuture.thenAccept{renderedDuck = it}

        val renderableFuture2 = ModelRenderable.builder()
            .setSource(this, tposeUri)
            .build()
        renderableFuture2.thenAccept{renderedTpose = it}

            }

    override fun onButtonPressed(btn: Button) {
        // The callback's are forwarded to MenuFragmentController,
        // that handles all the logic for these events
        menuFragController.onButtonPressed(btn)
    }

    private fun initMenuContainer() {
        supportFragmentManager.beginTransaction()
            .add(R.id.main_menu_container, menuFragController)
            .addToBackStack(null)
            .commit()
    }

    private fun spawnObjects() {
        // For prototyping only
        val frame = fragment.arSceneView.arFrame
        val pt = getScreenCenter()
        val hits: List<HitResult>
        if (frame != null && renderedDuck != null) {
            hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane) {
                    val duckAnchor = hit!!.createAnchor()
                    val duckAnchorNode = AnchorNode(duckAnchor)
                    val tposeAnchor = (frame.hitTest((pt.x.toFloat()-700.0f), (pt.y.toFloat()-200.0f)))[0].createAnchor()
                    val tposeAnchorNode = AnchorNode(tposeAnchor)
                    duckAnchorNode.setParent(fragment.arSceneView.scene)
                    tposeAnchorNode.setParent(fragment.arSceneView.scene)
                    val duckNode = TransformableNode(fragment.transformationSystem)
                    val tposeNode = TransformableNode(fragment.transformationSystem)
                    duckNode.scaleController.minScale = 0.1f
                    duckNode.scaleController.maxScale = 0.2f
                    tposeNode.scaleController.minScale = 0.04f
                    tposeNode.scaleController.maxScale = 0.1f
                    //duckNode.localScale = Vector3(0.2f,0.2f,0.2f)
                    duckNode.setParent(duckAnchorNode)
                    tposeNode.setParent(tposeAnchorNode)
                    duckNode.renderable = renderedDuck
                    tposeNode.renderable = renderedTpose
/*                    mNode.setOnTapListener { hitTestRes: HitTestResult?, motionEv: MotionEvent? ->
                    }*/
                    break
                }}}}

    private fun getScreenCenter(): android.graphics.Point {
        val vw = findViewById<View>(android.R.id.content)
        return android.graphics.Point(vw.width / 2, vw.height / 2)
    }
}
