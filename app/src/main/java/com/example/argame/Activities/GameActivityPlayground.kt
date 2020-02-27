package com.example.argame.Activities

import android.animation.ObjectAnimator
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.util.toRange
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import com.example.argame.Fragments.CustomArFragment
import com.example.argame.Fragments.GameOverFragment
import com.example.argame.Fragments.MenuFragmentController
import com.example.argame.Fragments.NextLevelFragment
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.Interfaces.PreferenceHelper.defaultPreference
import com.example.argame.Interfaces.PreferenceHelper.setGson
import com.example.argame.Interfaces.PreferenceHelper.clearValues
import com.example.argame.Model.*
import com.example.argame.R
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.animation.ModelAnimator
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.TransformableNode
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_game.*
import kotlinx.android.synthetic.main.activity_game_playground.*
import kotlinx.android.synthetic.main.healthbar.*
import kotlinx.android.synthetic.main.healthbar.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.doAsyncResult
import org.jetbrains.anko.matchParent
import java.sql.Time
import kotlin.concurrent.thread
import kotlin.math.absoluteValue
import kotlin.math.atan
import kotlin.math.pow

class GameActivityPlayground : AppCompatActivity(), FragmentCallbackListener,
    NPCSpawnHandler.NPCSpawnCallback, Ability.AbilityCallbackListener, CombatControllable.CombatControllableListener {

    private val menuFragController = MenuFragmentController()
    private lateinit var fragment: CustomArFragment
    private lateinit var playerUri: Uri
    private var renderedPlayer: ModelRenderable? = null
    private var anchorList = ArrayList<AnchorNode>()
    private lateinit var playerAnchorPos: HitResult
    private lateinit var playerAnchorNode: AnchorNode
    private lateinit var playerNode: TransformableNode

    // MARK: Testing-abilities-related stuff
    private var playerTarget: PlayerTargetData? = null
    private var hpRenderableDuck: ViewRenderable? = null
    private var hpRenderablePlayer: ViewRenderable? = null
    private lateinit var player: Player
    var ducksInScene = false
    var playerInScene = false


    // GSON and SHAREDPREFERENCE

    private lateinit var saver: SharedPreferences
    private val gson = Gson()
    private var curLevel: Int? = null
    private var newLevel: Int? = null

    // Spawning NPC's
    private lateinit var spawnHandler: NPCSpawnHandler
    private lateinit var npcSpawnThread: Thread
    private var spawnedNPCs = arrayListOf<NPC>()
    private var npcsAlive = arrayListOf<NPC>()
    private var npcAnchors = arrayListOf<NPCAnchorData>()
    private var allNPChaveSpawned = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_playground)
        this.supportFragmentManager.popBackStack()
        fragment =
            supportFragmentManager.findFragmentById(R.id.playground_sceneform_fragment) as CustomArFragment
        //saver = defaultPreference(this)
        saver = PreferenceManager.getDefaultSharedPreferences(this)
        curLevel = saver.getInt("levelNum", 1)
        initButtons()
        prepareModels()
        // MARK: Testing-abilities-related stuff
        initHPRenderables()
        playground_targetTxt.text = "Ducks alive ${npcsAlive.size}"
        //saver.clearValues

        newLevel = Level(this).createLevel()
        Log.d("LEVEL", curLevel.toString())
        findViewById<Button>(R.id.playground_toggleLevel).text = "Level " + curLevel.toString()

        spawnHandler = NPCSpawnHandler(this, curLevel ?: 1, Handler())

        fragment.arSceneView.scene.addOnUpdateListener {
            sceneUpdateListener()
        }
    }

    override fun onPause() {
        super.onPause()
        // TODO: Save level
        saver.edit().putInt("levelNum", curLevel!!).apply()
        Log.d("SAVE", "Saving level " + curLevel.toString())
/* // MARK: Example of saving items to class -> to gson -> to SharedPreferences
        val classified = ObjectLister(builderList)
        Log.d("Builderlist", builderList.size.toString())
        val saveGame = GameVault(classified)
        val saveToGson = gson.toJson(saveGame)
        saver.setGson = saveToGson
*/
        spawnHandler.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        saver.edit().putInt("levelNum", curLevel!!).apply()
        Log.d("SAVE", "Saving level " + curLevel.toString())
    }

    override fun onResume() {
        super.onResume()
        curLevel = saver.getInt("levelNum", 1)
        spawnHandler.resume()


        // TODO: Restore level and abilities
/*
            val parsedVault = gson.fromJson(saver.setGson, GameVault::class.java)
            val parsedLister = parsedVault.classifiedBuilder
            val parsedBuilder = parsedLister.builderList
            val firstItem = parsedBuilder[0]
            //spawnObjectsMarkThree(builderList)
        }*/
    }

    override fun onButtonPressed(btn: Button) {
        // The callback's are forwarded to MenuFragmentController,
        // that handles all the logic for these events
        menuFragController.onButtonPressed(btn)
    }

    private fun sceneUpdateListener() {
        val frame = fragment.arSceneView.arFrame
        val pt = getScreenCenter()
        val hits: List<HitResult>
        if (frame != null) {
            hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat() + 400)
            for (hit in hits) {
                val trackable = hit.trackable
                playground_spawnBtn.isEnabled = trackable is Plane && !playerInScene
                // TODO - figure out an efficient implementation for code below.. maybe?
//                    if (playerInScene) {
//                        if (trackable !is Plane) {
//                            spawnHandler.pause()
//                        } else {
//                            spawnHandler.resume()
//                        }
//                    }
            }
        }
    }

    private fun prepareModels() {
        playerUri = Uri.parse("playermodeltwithend.sfb")
        val renderableFuturePlayer = ModelRenderable.builder()
            .setSource(this, playerUri)
            .build()
        renderableFuturePlayer.thenAccept {
            renderedPlayer = it
            // lateinit Player, so it has a reference to the renderable, therefor the cast animation data
            player = Player(5.0, "player", 5000.0, it, this)
        }
    }

    private fun initButtons() {

        val spawnBtn = findViewById<Button>(R.id.playground_spawnBtn)
        spawnBtn.setOnClickListener {
            if (newLevel != null && !playerInScene) {
                spawnObjects()
                spawnPlayer()
            }
        }

        val exitBtn = findViewById<Button>(R.id.playground_exitBtn)
        exitBtn.setOnClickListener {
            callGameOverFragment()
        }
        // MARK: Testing-abilities-related stuff
        playground_attackDuckBtn.setOnClickListener {
            attackTarget()
        }

        val beamBtn = findViewById<Button>(R.id.playground_beamDuckBtn)
        beamBtn.setOnClickListener {
            beamTarget()
        }
        val destroyBtn = findViewById<Button>(R.id.playground_destroyBtn)
        destroyBtn.setOnClickListener {
            clearModels()
        }

        val levelButton = findViewById<Button>(R.id.playground_toggleLevel)
        levelButton.setOnClickListener {
            when (curLevel) {
                1 -> {
                    levelButton.text = "Level 2"
                    curLevel = 2
                    spawnHandler = NPCSpawnHandler(this, curLevel ?: 1, Handler())
                }
                2 -> {
                    levelButton.text = "Level 10"
                    curLevel = 10
                    spawnHandler = NPCSpawnHandler(this, curLevel ?: 1, Handler())
                }
                else -> {
                    levelButton.text = "Level 1"
                    curLevel = 1
                    spawnHandler = NPCSpawnHandler(this, curLevel ?: 1, Handler())

                }
            }
        }
    }

    private fun callMenuFragment() {
        // TODO: Move menu to *betweenlevelsActivity*
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.playground_main_menu_container, MenuFragmentController())
            .addToBackStack(null)
            .commit()
    }

        private fun callGameOverFragment() {
        // TODO: Move menu to *betweenlevelsActivity*
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.playground_main_menu_container, GameOverFragment())
            .addToBackStack("game_over")
            .commit()
    }

    private fun callNextLevelFragment() {
        // TODO: Move menu to *betweenlevelsActivity*
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.playground_main_menu_container, NextLevelFragment())
            .addToBackStack(null)
            .commit()
    }



    private fun initHPRenderables() {
        val renderableFuturePlayer = ViewRenderable.builder()
            .setView(this, R.layout.healthbar)
            .build()
        renderableFuturePlayer.thenAccept {
            hpRenderablePlayer = it
            player.setHPRenderable(it)
            val hpBar = hpRenderablePlayer?.view?.textView_healthbar
            hpBar?.background = ContextCompat.getDrawable(this, R.drawable.gradient_player_hpbar)
        }
    }

    // MARK: Testing-abilities-related stuff
    private fun attackTarget() {
        // disable attack button for the animation duration
        if (playerTarget != null) {
            playground_attackDuckBtn.isEnabled = false
            val ability = Ability.TEST
            val animData = ProjectileAnimationData(
                // TODO make start position relative to screen position
                playerAnchorNode.worldPosition,
                playerTarget!!.node.worldPosition,
                this,
                fragment,
                ability.uri()
            )
            // cancel the current animation if any
            cancelAnimator(player)
            // the cast animation data (related to the caster's 3d model, not the projectile)
            val animationData = player.model?.getAnimationData(ability.getCastAnimationString())
            player.setModelAnimator(ModelAnimator(animationData, player.model))
            player.useAbility(ability, playerTarget!!.model, animData) {
//                if (playerTarget!!.healthBar != null) {
//                    updateHPBar(playerTarget!!.healthBar, playerTarget!!.model)
//                }
                //beamTarget(playerTarget!!)

                // enable attack button after in callback
                playground_attackDuckBtn.isEnabled = true
//                if (!playerTarget!!.model.getStatus().isAlive) {
//                    playerTarget!!.node.localRotation = Quaternion(0f, 0f, 1f, 0f)
//                }
            }
        } else {
            Toast.makeText(this, "You don't have a target", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelAnimator(cc: CombatControllable) {
        val animator = cc.getModelAnimator()
        if (animator != null) {
            if (animator.isRunning) {
                animator.cancel()
            }
        }
    }

    private fun attackPlayer(npc: NPC, node: Node) {
        // disable attack button for the animation duration
            npc.dealDamage(5.0, player)
            val ability = Ability.TEST
            val animData = ProjectileAnimationData(
                // TODO make start position relative to screen position
                node.worldPosition,
                playerAnchorNode.worldPosition,
                this,
                fragment,
                ability.uri()
            )
            npc.useAbility(ability, player, animData) {
//                if (hpRenderablePlayer?.view?.textView_healthbar != null) {
//                    updateHPBar(hpRenderablePlayer?.view?.textView_healthbar, player)
//                }
            }
        }

    private fun beamTarget() {
        if (playerTarget != null) {
            cancelAnimator(player)
            playground_beamDuckBtn.isEnabled = false
            val beam = Ability.BEAM
            val attackAnimationData = player.model?.getAnimationData(Ability.BEAM.getCastAnimationString())
            val data = ProjectileAnimationData(
                playerNode.worldPosition, playerTarget!!.node.worldPosition, this, fragment, beam.uri())
            player.setModelAnimator(ModelAnimator(attackAnimationData, player.model))
            player.useAbility(beam, playerTarget!!.model, data) {
                // should do code below in onCCDamaged()
//                if (playerTarget!!.healthBar != null) {
//                    updateHPBar(playerTarget!!.healthBar, playerTarget!!.model)
//                }
                playground_beamDuckBtn.isEnabled = true
            }
            npcAnchors.forEach {
                val npcAnchorPos = it.anchorNode.worldPosition
                val targetPos = playerTarget!!.node.worldPosition
                val zDifPow = (npcAnchorPos.z - targetPos.z).pow(2)
                val xDifPow = (npcAnchorPos.x - targetPos.x).pow(2)
                val difAdded = (zDifPow+xDifPow)
                val result = Math.sqrt(difAdded.toDouble())
                Log.d("BEAM", "RESULT " + npcAnchors.indexOf(it)+ "  " + result)

                if (result < 0.8) {
                    Log.d("BEAM", "HIT NPC  " + npcAnchors.indexOf(it) )
                    //it.anchorNode.localScale = Vector3(0.4f, 0.4f, 0.4f)
                }

 /*               if (npcAnchorNode.worldPosition.x - playerTarget!!.node.worldPosition.x > 0.05f || npcAnchorNode.worldPosition.z - playerTarget!!.node.worldPosition.z < 0.05) {
                    Log.d("BEAM", "Additional target found " + npcAnchorNode.toString())
                    npcAnchorNode.localScale = Vector3(1.3f, 1.3f, 1.3f)

                    Log.d(
                        "BEAM", "Target position " + playerTarget!!.node.worldPosition.toString()
                    )
                }*/
            }
        }
    }

    private fun clearPlayerTarget() {
        playerTarget = null
    }

    // MARK: Testing-abilities-related stuff
    private fun createHPBar(node: TransformableNode, renderable: ViewRenderable?, model: CombatControllable) {
        val hpNode = Node()
        hpNode.setParent(node)
        hpNode.renderable = renderable

        if (node == playerNode) {
            hpNode.localScale = Vector3(0.35f, 0.35f, 0.35f)
            hpNode.localPosition = Vector3(0f, 0.5f, 0f)
        } else {
            hpNode.localScale = Vector3(5f, 5f, 5f)
            hpNode.localPosition = Vector3(0f, 5f, 0f)
        }
    }

    private fun clearModels() {
        for (anchor in anchorList) {
            removeAnchorNode(anchor)
        }
        for (anchor in npcAnchors) {
            removeAnchorNode(anchor.anchorNode)
        }
        spawnedNPCs.clear()
        anchorList.clear()
        npcAnchors.clear()
        npcsAlive.clear()
        ducksInScene = false
        playerInScene = false
    }

    private fun updatePlayerRotation() {
        if (playerTarget != null) {
            val playerPos = playerNode.worldPosition
            val targetPos: Vector3
            if (playerTarget!!.node.children.isNotEmpty()) {
                targetPos = playerTarget!!.node.children[0].worldPosition
            } else {
                targetPos = playerTarget!!.node.worldPosition
            }
            val rotation = AnimationAPI.calculateNewRotation(playerPos, targetPos)
            playerNode.localRotation = Quaternion(0f, rotation.x, 0f, rotation.z)

        }
    }

    private fun spawnPlayer() {
        if (!playerInScene) {
            val frame = fragment.arSceneView.arFrame
            val pt = getScreenCenter()
            val hits: List<HitResult>
            if (frame != null && renderedPlayer != null) {
                hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat() + 400)
                var trackableWasPlane = false
                for (hit in hits) {
                    if (!trackableWasPlane) {
                        val trackable = hit.trackable
                        if (trackable is Plane) {
                            fragment.arSceneView.planeRenderer.isVisible = false
                            trackableWasPlane = true
                            playerInScene = true
                            playerAnchorPos = hit!!
                            val playerAnchor = hit.createAnchor()
                            playerAnchorNode = AnchorNode(playerAnchor)
//                        playerAnchorNode.localRotation =
//                            Quaternion.axisAngle(Vector3(1f, 0f, 0f), 180f)
                            anchorList.add(playerAnchorNode)
                            playerAnchorNode.setParent(fragment.arSceneView.scene)
                            playerNode = TransformableNode(fragment.transformationSystem)
                            val forward = fragment.arSceneView.scene.camera.forward
                            playerAnchorNode.setLookDirection(
                                Vector3(
                                    -(forward.x),
                                    -(forward.y),
                                    -(forward.z)
                                )
                            )
                            playerNode.scaleController.isEnabled = false
                            playerNode.rotationController.isEnabled = false
                            playerNode.setParent(playerAnchorNode)
                            playerNode.renderable = renderedPlayer
                            createHPBar(playerNode, hpRenderablePlayer, player)

                            // update player look direction toward target if
                            // player position changes during 0.5 seconds
                            playerNode.setOnTouchListener { _, _ ->
                                val oldPosition = playerNode.worldPosition
                                Handler().postDelayed({
                                    if (playerNode.worldPosition != oldPosition)
                                        updatePlayerRotation()
                                }, 500)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun spawnObjects() {
        if (!ducksInScene && !spawnHandler.isRunning()) {
            // "Spawn NPC's"
            npcSpawnThread = Thread {
                spawnHandler.run()
            }
            npcSpawnThread.start()
            ducksInScene = true
            updateNPCRemainingText("NPCs spawning: ${NPCDataForLevels.LevelOne.npcs.size}")
        }
    }

    private fun updateHPBar(tv: TextView?, model: CombatControllable) {
        //tv?.text = model.getStatus().currentHealth.toString()
        val parent = tv?.parent as View
        val ratio = model.getStatus().currentHealth / model.getStatus().maxHealth
        val layOutParams = LinearLayout.LayoutParams((parent.width * ratio).toInt(), matchParent)
        val currentMargin = tv.marginEnd
        tv.layoutParams = layOutParams
        layOutParams.setMargins(currentMargin, currentMargin, currentMargin, currentMargin)
        Log.d("width", " Parent width (${parent.width}) * ratio ($ratio) ${(parent.width * ratio).toInt()}")
        Log.d("width", tv.width.toString())
    }

    private fun getScreenCenter(): android.graphics.Point {
        val vw = findViewById<View>(android.R.id.content)
        return android.graphics.Point(vw.width / 2, vw.height / 2)
    }

    private fun removeAnchorNode(nodeToremove: AnchorNode?) { //Remove an anchor node
        if (nodeToremove != null) {
            fragment.arSceneView.scene.removeChild(nodeToremove)
            nodeToremove.anchor!!.detach()
            nodeToremove.setParent(null)
        }
    }

    private fun randomMove(node: TransformableNode, npc: NPC, type: NPCType) {
        Log.d("RMOVE", "1")
        val randomInt = (1..100).shuffled().first()
        when (randomInt) {
            in 1..10 -> NodeCreator(node, Vector3(0.9f, 0.0f, 0.0f), npc, type)
            in 11..30 -> NodeCreator(node, Vector3(0.8f, 0.0f, -1.0f), npc, type)
            in 31..60 -> NodeCreator(node, Vector3(0.6f, 0.0f, -1.0f), npc, type)
            in 61..100 -> NodeCreator(node, Vector3(0.4f, 0.0f, 0.0f), npc, type)
        }
    }

    private fun NodeCreator(initialNode: TransformableNode, newLocation: Vector3, npc: NPC, type: NPCType) {
        Log.d("RMOVE", "2")
        val newNode = Node()
        val summedVector = Vector3.add(initialNode.worldPosition, newLocation)
        newNode.worldPosition = summedVector
        moveToTarget(initialNode, newNode, npc, type)
    }

    private fun moveToTarget(model: TransformableNode, targetNode: Node, npc: NPC, type: NPCType) {
        Log.d("RMOVE", "3")
        // Move to previously randomized location
        val objectAnimation = ObjectAnimator()
        objectAnimation.setAutoCancel(true)
        objectAnimation.target = model
        model.setLookDirection(Vector3.subtract(model.worldPosition, targetNode.worldPosition))
        objectAnimation.setObjectValues(model.worldPosition, targetNode.worldPosition)
        objectAnimation.setPropertyName("worldPosition")
        objectAnimation.setEvaluator(Vector3Evaluator())
        objectAnimation.interpolator = LinearInterpolator()
        objectAnimation.duration = 3000
        objectAnimation.start()

        // Move towards player after X seconds
        Handler().postDelayed({
            val objectAnimation = ObjectAnimator()
            objectAnimation.setAutoCancel(true)
            objectAnimation.target = model
            model.setLookDirection(
                Vector3.subtract(
                    model.worldPosition,
                    playerAnchorNode.worldPosition
                )
            )
            if (type == NPCType.MELEE) {
                val stopX = playerAnchorNode.worldPosition.x
                // If NPC is melee -> stop next to the player
                val valuePool = floatArrayOf(-0.12f,-0.10f,-0.08f,-0.06f,0.06f,0.08f,0.10f,0.12f)
                val randomifier = valuePool[(0..7).shuffled().first()]
                val newX = (stopX+randomifier)
                val stop = Vector3(newX,playerAnchorNode.worldPosition.y, playerAnchorNode.worldPosition.z)
                objectAnimation.setObjectValues(model.worldPosition, stop)
                objectAnimation.duration = 15000
                //attackLooper(npc, model) TODO: Looping melee attack
                Handler().postDelayed({
                    // Correct look direction towards player (again)
                    model.setLookDirection(                        Vector3.subtract(

                            model.worldPosition,
                            playerAnchorNode.worldPosition
                        )
                    )
                }, 15000)
            }
            else {
                // If NPC is ranged, stop halfway
                val rangedStartX = model.worldPosition.x
                val rangedStartZ = model.worldPosition.z
                val rangedStopX = playerAnchorNode.worldPosition.x
                val rangedStopZ = playerAnchorNode.worldPosition.z
                val newX = ((rangedStopX+rangedStartX)/2)
                val newZ = ((rangedStopZ+rangedStartZ)/2)
                val stop = Vector3(newX,model.worldPosition.y,newZ)
                objectAnimation.setObjectValues(model.worldPosition, stop)
                objectAnimation.duration = 7000
                // Attack until attacker or target is dead
                attackLooper(npc, model)
            }

            objectAnimation.setPropertyName("worldPosition")
            objectAnimation.setEvaluator(Vector3Evaluator())
            objectAnimation.interpolator = LinearInterpolator()

            objectAnimation.start()

        }, 3500)
    }

    fun attackLooper(npc: NPC, model: TransformableNode) {
        var cooldown = false
        val thread = Thread {
            while (npc.getStatus().isAlive && player.getStatus().isAlive) {
                if (!cooldown) {
                    cooldown = true
                    Handler(Looper.getMainLooper()).postDelayed({
                        attackPlayer(npc, model)
                        cooldown = false
                    }, 7100)
                }
            }
        }
        thread.start()
    }

    override fun notifyNPCSpawned(type: NPCType, remaining: Int, npcID: Int) {
        val ids = spawnedNPCs.filter { it.getID() == npcID }
        val anchors = npcAnchors.filter { it.npcID == npcID }
        if (ids.isEmpty() && anchors.isEmpty()) {
            lateinit var renderable: ModelRenderable
            // get NPC model
            val renderableFuture = ModelRenderable.builder()
                .setSource(this, type.modelUri())
                .build()
            renderableFuture.thenAccept {
                renderable = it
                // create the NPC object when we have a ModelRenderable ready
                val npcObject = type.getNPCObject(curLevel!!, renderable, npcID, this)
                // spawn NPC
                spawnNPC(npcObject, type)
                // random debugs
                val time = Time(System.currentTimeMillis())
                Log.d(
                    "NPCSPAWN",
                    "NPC of type: ${type.name} spawned at: $time. NPC's remaining: $remaining"
                )
                for (index in spawnedNPCs.indices) {
                    Log.d("NPCSPAWN", "spawnedNPCs[$index]: ${spawnedNPCs[index]}")
                }
                if (remaining > 0) {
                    updateNPCRemainingText("NPCs spawning: $remaining")
                } else {
                    updateNPCRemainingText("")
                }
            }
        }
    }

    override fun notifyAllNPCSpawned() {
        spawnHandler.stop()
        Handler().removeCallbacks(spawnHandler)
        allNPChaveSpawned = true
    }


    private fun spawnNPC(npc: NPC, type: NPCType) {
        // To make sure no duplicates are possible. Not that it should be, but apparently it is
        val ids = spawnedNPCs.filter { it.getID() == npc.getID() }
        val checkAnchors = npcAnchors.filter { it.npcID == npc.getID() }
        if (ids.isEmpty() && checkAnchors.isEmpty()) {
            spawnedNPCs.add(npc)
            lateinit var hpRenderable: ViewRenderable
            val renderableFuture = ViewRenderable.builder()
                .setView(this, R.layout.healthbar)
                .build()
            renderableFuture.thenAccept {
                hpRenderable = it
                npc.setHPRenderable(it)
                hpRenderableDuck?.view?.textView_healthbar?.text =
                    npc.getStatus().currentHealth.toString()
                val frame = fragment.arSceneView.arFrame
                val pt = getScreenCenter()
                val hits: List<HitResult>
                if (frame != null) {
                    hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())
                    var trackableWasPlane = false
                    for (hit in hits) {
                        if (!trackableWasPlane) {
                            val trackable = hit.trackable
                            if (trackable is Plane) {
                                trackableWasPlane = true
                                val valuePool = (200..1200)
                                val randX = valuePool.shuffled().first().toFloat()
                                val randY = valuePool.shuffled().first().toFloat()
                                Log.d("XYVALUES", randX.toString() + "  " + randY.toString())
                                val anchor = (frame.hitTest(
                                    (pt.x.toFloat() - randX),
                                    (pt.y.toFloat() + randY)
                                ))[0].createAnchor()
                                val anchorNode = AnchorNode(anchor)
                                npcAnchors.add(NPCAnchorData(anchorNode, npc.getID()))
                                npcsAlive.add(npc)
                                playground_targetTxt.text = "Ducks alive ${npcsAlive.size}"
                                anchorNode.setParent(fragment.arSceneView.scene)
                                val node = TransformableNode(fragment.transformationSystem)
                                node.scaleController.isEnabled = false
                                node.rotationController.isEnabled = false
                                node.setParent(anchorNode)
                                node.renderable = npc.model
                                node.localScale = Vector3(0.1f, 0.1f, 0.1f)
                                if (npc.getID() == 100) {
                                    node.localScale = Vector3(0.4f, 0.4f, 0.4f)
                                }
                                createHPBar(node, hpRenderable, npc)
                                randomMove(node, npc, type)
                                node.setOnTouchListener { _, _ ->
                                    val oldPosition = node.worldPosition
                                    Handler().postDelayed({
                                        if (node.worldPosition != oldPosition) {
                                            if (playerTarget != null) {
                                                val playerTargetNode = playerTarget!!.node
                                                if (playerTargetNode != node) {
                                                    val playerTargetBar = playerTarget!!.healthBar!!
                                                    updateOldTargetHPBar(playerTargetBar)
                                                }
                                            }
                                            // set new target
                                            val newTarget = PlayerTargetData(
                                                node,
                                                npc,
                                                hpRenderable.view.textView_healthbar
                                            )
                                            playerTarget = newTarget
                                            updateNewTargetHPBar(newTarget)
                                            updatePlayerRotation()
                                        }
                                    }, 250)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateOldTargetHPBar(hpBar: TextView) {
        val parent = hpBar.parent as View
        parent.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.healthbar_nontarget_background
            )
        // clear old target hp text
        //hpBar.text = ""
    }

    private fun updateNewTargetHPBar(targetData: PlayerTargetData) {
        val parent = targetData.healthBar?.parent as View
        parent.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.healthbar_background
            )
        //playerTarget!!.healthBar?.text = targetData.model.getStatus().currentHealth.toString()
    }

    private fun updateNPCRemainingText(text: String) {
        playground_remaining.text = text
    }

    override fun onAbilityCast(caster: CombatControllable, target: CombatControllable, ability: Ability) {
        // create cast animation here
        // TODO: caster.model!!.getAnimationData("")
    }

    override fun onAbilityHit(caster: CombatControllable, target: CombatControllable, ability: Ability) {
        // create being hit animation here
        // TODO: target.model!!.getAnimationData("")
    }

    override fun onCCDamaged(cc: CombatControllable) {
        Log.d("CCDMG", "CombatControllable: ${cc.getStatus().name} damaged.")
        if (cc == player) {
            updateHPBar(hpRenderablePlayer?.view?.textView_healthbar, player)
        } else {
            if (cc is NPC) {
                Log.d("CCDMG", "NPC damaged. ID: ${cc.getID()}")
                if (cc.getHPBar() != null) {
                    updateHPBar(cc.getHPBar()!!.view.textView_healthbar, cc)
                }
            }
        }
    }

    override fun onCCDeath(cc: CombatControllable) {
        // TODO: Stop any pending animation here
        val totalNpcCount = NPCDataForLevels.getNPCForLevelCount(curLevel!!)
        Log.d("NPCDED", "curLevel: $curLevel LevelOne.size: ${NPCDataForLevels.getNPCForLevelCount(curLevel!!)}")
        val npcsRemaining = totalNpcCount - spawnedNPCs.size
        if (cc == player) {
            Toast.makeText(this, "YOU DIED", Toast.LENGTH_LONG)
                .show()
            playerNode.localRotation = Quaternion(0f, 0f, 1f, 0f)
            callGameOverFragment()
        } else {
            if (cc is NPC) {
                npcsAlive.forEach {
                    if (cc == it) {
                        // the indices should be the same..?
                        // might need to change this to a safer approach
                        val anchor = npcAnchors[npcsAlive.indexOf(it)]
                        // check that the correct anchor was indeed picked
                        if (anchor.npcID == it.getID()) {
                            val hpBar = cc.getHPBar()
                            hpBar?.view?.visibility = View.GONE
                            updateHPBar(hpBar!!.view.textView_healthbar, cc)
                            val node = anchor.anchorNode.children[0]
                            node.localRotation = Quaternion(0f, 0f, 1f, 0f)

                            if (node is TransformableNode) {
                                node.translationController.isEnabled = false
                            }
                        }
                        Handler().postDelayed({
                            // Remove the NPC from scene after a delay
                            // When using many abilities at the same time,
                            // the first one might already kill the target.
                            // That is why we need to check if the target
                            // still exists when receiving another callback.
                            if (npcsAlive.indexOf(it) >= 0) {
                                if (npcAnchors.size >= npcsAlive.indexOf(it)) {
                                    npcAnchors.removeAt(npcsAlive.indexOf(it))
                                    npcsAlive.removeAt(npcsAlive.indexOf(it))
                                }
                            }
                            // Level completed!
                            Log.d("NPCDED", "npcAnchors.size: ${npcAnchors.size} npcsRemaining: $npcsRemaining, spawnedNpcs.count: ${spawnedNPCs.size}")
                            if (npcAnchors.size == 0 && npcsRemaining == 0 ) {
                                Toast.makeText(this, "ALL DUCKS DEAD!", Toast.LENGTH_LONG)
                                    .show()
                                Log.d("CURLEVEL", curLevel.toString())
                                when (curLevel) {
                                    1 -> curLevel = 2
                                    2 -> curLevel = 10
                                    else -> curLevel = 1
                                }
                                Log.d("CURLEVEL", curLevel.toString())

                                saver.edit().putInt("levelNum", curLevel!!).apply()
                                callNextLevelFragment()
                            }
                            playground_targetTxt.text = "Ducks alive ${npcsAlive.size}"
                            removeAnchorNode(anchor.anchorNode)
                        }, 2000)
                    }
                }
            }
        }
    }
}
