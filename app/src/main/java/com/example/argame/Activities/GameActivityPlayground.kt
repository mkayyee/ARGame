package com.example.argame.Activities

import android.animation.ObjectAnimator
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Log.wtf
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.marginEnd
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.example.argame.Fragments.CustomArFragment
import com.example.argame.Fragments.GameOverFragment
import com.example.argame.Fragments.MenuFragmentController
import com.example.argame.Fragments.NextLevelFragment
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.Model.*
import com.example.argame.R
import com.google.ar.core.Anchor
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
import kotlinx.android.synthetic.main.activity_game_playground.*
import kotlinx.android.synthetic.main.activity_level_intermission.*
import kotlinx.android.synthetic.main.healthbar.view.*
import org.jetbrains.anko.matchParent
import java.sql.Time
import kotlin.math.pow

class GameActivityPlayground : AppCompatActivity(), FragmentCallbackListener,
    NPCSpawnHandler.NPCSpawnCallback, Ability.AbilityCallbackListener,
    CombatControllable.CombatControllableListener {

    private val menuFragController = MenuFragmentController()
    private lateinit var fragment: CustomArFragment
    private lateinit var playerUri: Uri
    private var renderedPlayer: ModelRenderable? = null
    private var anchorList = ArrayList<AnchorNode>()
    private lateinit var playerAnchorNode: AnchorNode
    private lateinit var playerNode: TransformableNode

    // MARK: Testing-abilities-related stuff
    private var playerTarget: PlayerTargetData? = null
    private var hpRenderableNPC: ViewRenderable? = null
    private var hpRenderablePlayer: ViewRenderable? = null
    private lateinit var player: Player
    var ducksInScene = false
    var playerInScene = false

    // SHAREDPREFERENCE

    private lateinit var saver: SharedPreferences
    private var curLevel: Int? = null

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
        fragment =
            supportFragmentManager.findFragmentById(R.id.playground_sceneform_fragment) as CustomArFragment
        fragment.arSceneView.scene.addOnUpdateListener {
            sceneUpdateListener()
        }
        saver = PreferenceManager.getDefaultSharedPreferences(this)
        curLevel = saver.getInt("levelNum", 1)

        initButtons()
        prepareModels()
        // MARK: Testing-abilities-related stuff
        initHPRenderables()
        playground_targetTxt.text = "Ducks alive ${npcsAlive.size}"
        spawnHandler = NPCSpawnHandler(this, curLevel ?: 1, Handler())
    }

    override fun onPause() {
        super.onPause()
        saver.edit().putInt("levelNum", curLevel!!).apply()
        Log.d("SAVE", "Saving level " + curLevel.toString())
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

    }

    override fun onButtonPressed(btn: Button) {
        if (btn.id == button_close_ability_menu.id) {
            callFragment("NextLevel")
        } else {
            // The callback's are forwarded to MenuFragmentController,
            // that handles all the logic for these events
            menuFragController.onButtonPressed(btn)
        }
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

        playground_spawnBtn.setOnClickListener {
            if (curLevel != null && !playerInScene) {
                spawnObjects()
                mainSpawner(player)
            }
        }

        playground_exitBtn.setOnClickListener {
            callFragment("GameOver")
        }
        // MARK: Testing-abilities-related stuff
        playground_attackDuckBtn.setOnClickListener {
            attackTarget()
        }
        playground_beamDuckBtn.setOnClickListener {
            beamTarget()
        }
        playground_destroyBtn.setOnClickListener {
            clearModels()
        }

        val levelButton = findViewById<Button>(R.id.playground_toggleLevel)
        levelButton.setOnClickListener {
            when (curLevel) {
                1 -> {
                    curLevel = 2
                }
                2 -> {
                    curLevel = 10
                }
                else -> {
                    curLevel = 1

                }
            }
            spawnHandler = NPCSpawnHandler(this, curLevel ?: 1, Handler())
            levelButton.text = "Level " + curLevel
        }
        // Update current level view
        findViewById<Button>(R.id.playground_toggleLevel).text = "Level " + curLevel.toString()
    }

    private fun callFragment(fragmentName: String) {
        val fragmentToGet = when (fragmentName) {
            "NextLevel" -> NextLevelFragment(supportFragmentManager)
            else -> GameOverFragment()
        } as Fragment
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.playground_main_menu_container, fragmentToGet)
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
            hpBar?.background =
                ContextCompat.getDrawable(this, R.drawable.gradient_player_hpbar)
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
                playground_attackDuckBtn.isEnabled = true
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
            val attackAnimationData =
                player.model?.getAnimationData(Ability.BEAM.getCastAnimationString())
            val data = ProjectileAnimationData(
                playerNode.worldPosition,
                playerTarget!!.node.worldPosition,
                this,
                fragment,
                beam.uri()
            )
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
                val difAdded = (zDifPow + xDifPow)
                val result = Math.sqrt(difAdded.toDouble())
                Log.d("BEAM", "RESULT " + npcAnchors.indexOf(it) + "  " + result)

                if (result < 0.8) {
                    Log.d("BEAM", "HIT NPC  " + npcAnchors.indexOf(it))
                    //it.anchorNode.localScale = Vector3(0.4f, 0.4f, 0.4f)
                }
            }
        }
    }

    private fun clearPlayerTarget() {
        playerTarget = null
    }

    // MARK: Testing-abilities-related stuff
    private fun createHPBar(
        node: TransformableNode,
        renderable: ViewRenderable?
    ) {
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

    private fun mainSpawner(spawnable: Any) {
        Log.d("mainSpawner", "CALLED")

        // Init vars
        var ableToSpawn = false
        lateinit var render: ModelRenderable
        lateinit var hpRenderable: ViewRenderable
        lateinit var anchor: Anchor
        val valuePool = (200..1200)
        val randX = valuePool.shuffled().first().toFloat()
        val randY = valuePool.shuffled().first().toFloat()

        // Spawn initializers
        if (spawnable is Player && !playerInScene && renderedPlayer != null
            && hpRenderablePlayer != null
        ) {
            playerInScene = true
            render = renderedPlayer!!
            hpRenderable = hpRenderablePlayer!!
            ableToSpawn = true
        }
        if (spawnable is NPC) {
            val ids = spawnedNPCs.filter { it.getID() == spawnable.getID() }
            val checkAnchors = npcAnchors.filter { it.npcID == spawnable.getID() }
            if (ids.isEmpty() && checkAnchors.isEmpty()) {
                render = spawnable.model!!
                spawnedNPCs.add(spawnable)
                ableToSpawn = true
            }
        }

        // When all necessary components are ready, proceed with spawn
        if (ableToSpawn) {
            val frame = fragment.arSceneView.arFrame
            val pt = getScreenCenter()
            val hits: List<HitResult>
            if (frame != null) {
                hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat() + 400)
                var trackableWasPlane = false
                for (hit in hits) {
                    if (!trackableWasPlane) {
                        val trackable = hit.trackable
                        if (trackable is Plane) {
                            fragment.arSceneView.planeRenderer.isVisible = false
                            trackableWasPlane = true
                            //playerAnchorPos = hit!!
                            when (spawnable) {
                                is Player -> anchor = hit.createAnchor()
                                else -> {
                                    anchor = (frame.hitTest(
                                        (pt.x.toFloat() - randX),
                                        (pt.y.toFloat() + randY)
                                    ))[0].createAnchor()
                                }
                            }
                            val anchorNode = AnchorNode(anchor)
                            anchorList.add(anchorNode)
                            anchorNode.setParent(fragment.arSceneView.scene)
                            val node = TransformableNode(fragment.transformationSystem)
                            anchorNode.setLookDirection(Vector3.forward())
                            node.scaleController.isEnabled = false
                            node.rotationController.isEnabled = false
                            node.setParent(anchorNode)
                            node.renderable = render
                            if (spawnable is Player) {
                                //PLAYER POST SPAWN CODE
                                playerNode = node
                                playerAnchorNode = anchorNode
                                createHPBar(node, hpRenderable)
                                node.setOnTouchListener { _, _ ->
                                    val oldPosition = playerNode.worldPosition
                                    Handler().postDelayed({
                                        if (playerNode.worldPosition != oldPosition)
                                            updatePlayerRotation()
                                    }, 500)
                                }
                            }
                            if (spawnable is NPC) {
                                // NPC POST SPAWN CODE
                                val renderableFutureNPC = ViewRenderable.builder()
                                    .setView(this, R.layout.healthbar)
                                    .build()
                                renderableFutureNPC.thenAccept {
                                    hpRenderableNPC = it
                                    hpRenderable = it
                                    spawnable.setHPRenderable(hpRenderable)
                                    createHPBar(node, hpRenderable)
                                    Log.d("mainSpawner", "BUILT")
                                }
                                npcAnchors.add(NPCAnchorData(anchorNode, spawnable.getID()))
                                npcsAlive.add(spawnable)
                                playground_targetTxt.text = "Ducks alive ${npcsAlive.size}"
                                node.localScale = Vector3(0.1f, 0.1f, 0.1f)
                                if (spawnable.getID() == 100) {
                                    node.localScale = Vector3(0.3f, 0.3f, 0.3f)
                                }

                                val newTargetNode = randomMove(node)
                                moveToTarget(node, newTargetNode)
                                attackInitializer(spawnable.getType(), spawnable, node)
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
                                                spawnable,
                                                hpRenderable.view.textView_healthbar
                                            )
                                            playerTarget = newTarget
                                            updateNewTargetHPBar(newTarget)
                                            updatePlayerRotation()
                                        }
                                    }, 250)
                                }
                            }
                            // Create and add HP bars after everything else
                        }
                    }
                }
            }
        } else {
            Log.e(
                "mainSpawner",
                "Impossible spawn type or trying to spawn player twice. SPAWNTYPE: "
                        + spawnable.toString() + " PLAYERINSCENE: " + playerInScene.toString()
            )
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
        val parent = tv?.parent as View
        val ratio = model.getStatus().currentHealth / model.getStatus().maxHealth
        val layOutParams = LinearLayout.LayoutParams((parent.width * ratio).toInt(), matchParent)
        val currentMargin = tv.marginEnd
        tv.layoutParams = layOutParams
        layOutParams.setMargins(currentMargin, currentMargin, currentMargin, currentMargin)
        Log.d(
            "width",
            " Parent width (${parent.width}) * ratio ($ratio) ${(parent.width * ratio).toInt()}"
        )
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

    private fun randomMove(node: TransformableNode): Node {
        Log.d("RMOVE", "1")
        return when ((1..100).shuffled().first()) {
            in 1..10 -> NodeCreator(node, Vector3(0.9f, 0.0f, 0.0f))
            in 11..30 -> NodeCreator(node, Vector3(0.8f, 0.0f, -1.0f))
            in 31..60 -> NodeCreator(node, Vector3(0.6f, 0.0f, -1.0f))
            in 61..100 -> NodeCreator(node, Vector3(0.4f, 0.0f, 0.0f))
            else -> {
                wtf("WTF", "Impossible randomInt in randomMove")
                NodeCreator(node, Vector3(0.9f, 0.0f, 0.0f))
            }
        }
    }

    private fun NodeCreator(
        initialNode: TransformableNode,
        newLocation: Vector3
    ): Node {
        Log.d("RMOVE", "2")
        val newNode = Node()
        val summedVector = Vector3.add(initialNode.worldPosition, newLocation)
        newNode.worldPosition = summedVector
        return newNode
    }

    private fun moveToTarget(model: TransformableNode, targetNode: Node) {
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
    }

    fun attackInitializer(type: NPCType, npc: NPC, model: TransformableNode) {
        // Move to player and start attacking. Movement and attack pattern are received from NPCTYPE
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
            val stopX = playerAnchorNode.worldPosition.x
            val stopZ = playerAnchorNode.worldPosition.z
            val startX = model.worldPosition.x
            val startZ = model.worldPosition.z
            when (type) {
                NPCType.MELEE -> {
                    val valuePool =
                        floatArrayOf(-0.12f, -0.10f, -0.08f, -0.06f, 0.06f, 0.08f, 0.10f, 0.12f)
                    val randomifier = valuePool[(0..7).shuffled().first()]
                    val newX = (stopX + randomifier)
                    val stop = Vector3(
                        newX,
                        playerAnchorNode.worldPosition.y,
                        playerAnchorNode.worldPosition.z
                    )
                    objectAnimation.setObjectValues(model.worldPosition, stop)
                    objectAnimation.duration = 15000
                    Handler().postDelayed({
                        // Attack until attacker or target is dead
                        attackLooperMelee(npc, model)
                    }, 15000)
                }
                else -> {
                    val newX = ((stopX + startX) / 2)
                    val newZ = ((stopZ + startZ) / 2)
                    val stop = Vector3(newX, model.worldPosition.y, newZ)
                    objectAnimation.setObjectValues(model.worldPosition, stop)
                    objectAnimation.duration = 7000
                    // Attack until attacker or target is dead
                    attackLooper(npc, model)
                }
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

    fun attackLooperMelee(npc: NPC, model: TransformableNode) {

        // TODO: Redo properly with ability and animation

        var cooldown = false
        val thread = Thread {
            while (npc.getStatus().isAlive && player.getStatus().isAlive) {
                if (!cooldown) {
                    cooldown = true
                    model.setLookDirection(Vector3.forward())
                    Handler(Looper.getMainLooper()).postDelayed({
                        model.setLookDirection(Vector3.forward())
                        Handler(Looper.getMainLooper()).postDelayed({
                            model.setLookDirection(
                                Vector3.subtract(
                                    model.worldPosition,
                                    playerAnchorNode.worldPosition
                                )
                            )
                            npc.dealDamage(100.0, player)
                        }, 1500)
                        cooldown = false
                    }, 6000)
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
                mainSpawner(npcObject)
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

    private fun updateOldTargetHPBar(hpBar: TextView) {
        val parent = hpBar.parent as View
        parent.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.healthbar_nontarget_background
            )
    }

    private fun updateNewTargetHPBar(targetData: PlayerTargetData) {
        val parent = targetData.healthBar?.parent as View
        parent.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.healthbar_background
            )
    }

    private fun updateNPCRemainingText(text: String) {
        playground_remaining.text = text
    }

    override fun onAbilityCast(
        caster: CombatControllable,
        target: CombatControllable,
        ability: Ability
    ) {
        // create cast animation here
        // TODO: caster.model!!.getAnimationData("")
    }

    override fun onAbilityHit(
        caster: CombatControllable,
        target: CombatControllable,
        ability: Ability
    ) {
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
        Log.d(
            "NPCDED",
            "curLevel: $curLevel LevelOne.size: ${NPCDataForLevels.getNPCForLevelCount(curLevel!!)}"
        )
        val npcsRemaining = totalNpcCount - spawnedNPCs.size
        if (cc == player) {
            Toast.makeText(this, "YOU DIED", Toast.LENGTH_LONG)
                .show()
            playerNode.localRotation = Quaternion(0f, 0f, 1f, 0f)
            callFragment("GameOver")
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
                            Log.d(
                                "NPCDED",
                                "npcAnchors.size: ${npcAnchors.size} npcsRemaining: $npcsRemaining, spawnedNpcs.count: ${spawnedNPCs.size}"
                            )
                            if (npcAnchors.size == 0 && npcsRemaining == 0) {
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
                                callFragment("NextLevel")
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