package com.example.argame.Activities

import com.example.argame.Model.BackgroundMusic
import android.animation.ObjectAnimator
import android.app.ActionBar
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.util.Log.wtf
import android.view.InflateException
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.marginEnd
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.example.argame.Fragments.CustomArFragment
import com.example.argame.Fragments.Menu.GameOverFragment
import com.example.argame.Fragments.Menu.MenuFragmentController
import com.example.argame.Fragments.Menu.NextLevelFragment
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.Model.*
import com.example.argame.Model.Ability.Ability
import com.example.argame.Model.Ability.PlayerUltimate
import com.example.argame.Model.Ability.ProjectileAnimationData
import com.example.argame.Model.Ability.UltimateHandler
import com.example.argame.Model.CombatControllable.CombatControllable
import com.example.argame.Model.NPC.*
import com.example.argame.Model.Persistence.AppDatabase
import com.example.argame.Model.Persistence.User
import com.example.argame.Model.Persistence.UserDao
import com.example.argame.Model.Player.Highscore
import com.example.argame.Model.Player.Player
import com.example.argame.Model.Player.PlayerTargetData
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
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_game_playground.*
import kotlinx.android.synthetic.main.activity_level_intermission.*
import kotlinx.android.synthetic.main.healthbar.*
import kotlinx.android.synthetic.main.healthbar.view.*
import kotlinx.android.synthetic.main.healthbar.view.textView_barrier
import kotlinx.android.synthetic.main.menu_container.*
import kotlinx.android.synthetic.main.ultimatebar.view.*
import org.jetbrains.anko.displayMetrics
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.*
import pl.droidsonroids.gif.GifImageView
import java.sql.Time
import kotlin.concurrent.thread
import kotlin.math.pow

class GameActivityPlayground : AppCompatActivity(), FragmentCallbackListener,
    NPCSpawnHandler.NPCSpawnCallback, Ability.AbilityCallbackListener,
    CombatControllable.CombatControllableListener, UltimateHandler.UltimateHandlerListener {

    private val menuFragController =
        MenuFragmentController()
    private lateinit var fragment: CustomArFragment
    private var rootView : View? = null
    private lateinit var playerUri: Uri
    private var renderedPlayer: ModelRenderable? = null
    private var anchorList = ArrayList<AnchorNode>()
    private lateinit var playerAnchorNode: AnchorNode
    private lateinit var playerNode: TransformableNode

    // MARK: Testing-abilities-related stuff
    private var playerTarget: PlayerTargetData? = null
    private var hpRenderableNPC: ViewRenderable? = null
    private var hpRenderablePlayer: ViewRenderable? = null
    private var ultRenderablePlayer: ViewRenderable? = null
    private lateinit var player: Player
    private lateinit var ultimateHandler: UltimateHandler
    var ducksInScene = false
    var playerInScene = false
    val cdHandler = Handler(Looper.getMainLooper())


    // SHAREDPREFERENCE

    private lateinit var saver: SharedPreferences
    private var userId: Int? = null
    private var curLevel: Int? = null

    // MUSIC & SOUNDS
    private lateinit var intentm : Intent
    private lateinit var effectPlayer : SoundEffectPlayer

    // Spawning NPC's
    private lateinit var spawnHandler: NPCSpawnHandler
    private lateinit var npcSpawnThread: Thread
    private var spawnedNPCs = arrayListOf<NPC>()
    private var npcsAlive = arrayListOf<NPC>()
    private var npcAnchors = arrayListOf<NPCAnchorData>()
    private var allNPChaveSpawned = false
    private var forceStop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_playground)
        intentm = Intent(this, BackgroundMusic::class.java)

        effectPlayer = SoundEffectPlayer(this)
        effectPlayer.initSoundEffectPlayer()

        fragment =
            supportFragmentManager.findFragmentById(R.id.playground_sceneform_fragment) as CustomArFragment
        fragment.arSceneView.scene.addOnUpdateListener {
            sceneUpdateListener()
        }
        saver = PreferenceManager.getDefaultSharedPreferences(this)
        curLevel = saver.getInt("levelNum", 1)
        val uid = saver.getInt("USER", -1)
        if (uid != -1) {
            userId = uid
        }

        initButtons()
        prepareModels()
        // MARK: Testing-abilities-related stuff
        initHPRenderables()
        playground_targetTxt.text = "Ducks alive ${npcsAlive.size}"
        spawnHandler = NPCSpawnHandler(
            this,
            curLevel ?: 1,
            Handler()
        )
        initUltimateHandler()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val layoutParamsTop = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        val layoutParamsBot = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        var marginTopTop = 0
        var marginTopBot = 0
        var marginRight = 0
        var marginLeft = 0

        when (newConfig.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                marginTopTop = 300*displayMetrics.density.toInt()
                marginTopBot = 500*displayMetrics.density.toInt()
                marginRight = 10*displayMetrics.density.toInt()
                marginLeft = 10*displayMetrics.density.toInt()
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                marginTopTop = 150*displayMetrics.density.toInt()
                marginTopBot = 320*displayMetrics.density.toInt()
                marginRight = 20*displayMetrics.density.toInt()
                marginLeft = 20*displayMetrics.density.toInt()
            }
        }
        layoutParamsTop.setMargins(marginLeft,marginTopTop,marginRight,0)
        layoutParamsBot.setMargins(marginLeft,marginTopBot,marginRight,0)
        teleportLayout.layoutParams = layoutParamsTop
        shieldLayout.layoutParams = layoutParamsTop
        beamLayout.layoutParams = layoutParamsBot
        attackLayout.layoutParams = layoutParamsBot
    }

    override fun onPause() {
        super.onPause()
        saver.edit().putInt("levelNum", curLevel!!).apply()
        Log.d("SAVE", "Saving level " + curLevel.toString())
        spawnHandler.pause()
        stopService(intentm)
    }

    override fun onDestroy() {
        super.onDestroy()
        saver.edit().putInt("levelNum", curLevel!!).apply()
        Log.d("SAVE", "Saving level " + curLevel.toString())
        stopService(intentm)
    }

    override fun onResume() {
        super.onResume()
        startService(intentm)
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

    private fun initUltimateHandler() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        ultimateHandler = UltimateHandler(accel, light, sensorManager, this)
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
        playerUri = Uri.parse("Alien.sfb")
        val renderableFuturePlayer = ModelRenderable.builder()
            .setSource(this, playerUri)
            .build()
        renderableFuturePlayer.thenAccept {
            val prevScore = saver.getInt("SCORE", 0)
            renderedPlayer = it
            player = Player(
                5.0,
                "player",
                500.0,
                it,
                this,
                score = prevScore
            )
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
        playground_shieldDuckBtn.setOnClickListener {
            useBarrier(hpRenderablePlayer, player)
        }
        playground_destroyBtn.setOnClickListener {
            clearModels()
        }
        playground_teleportDuckBtn.setOnClickListener {
            Log.d("Teleport", "button pressed")
            doAsync { teleportPlayer() }
        }
        playground_killallBtn.setOnClickListener {
            ultimateHandler.beginMeasuring(PlayerUltimate.KILLALL)
            // TODO: Hide ultimate bar
            Toast.makeText(this, "Move phone fast enough in any direction to trigger KILLALL", Toast.LENGTH_LONG).show()
        }
        playground_serenityBtn.setOnClickListener {
            ultimateHandler.beginMeasuring(PlayerUltimate.SERENITY)
            Toast.makeText(this, "Cover light sensor to trigger Serenity", Toast.LENGTH_LONG).show()
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
            spawnHandler = NPCSpawnHandler(
                this,
                curLevel ?: 1,
                Handler()
            )
            levelButton.text = "Level " + curLevel
        }
        // Update current level view
        findViewById<Button>(R.id.playground_toggleLevel).text = "Level " + curLevel.toString()
    }

    private fun callFragment(fragmentName: String) {
        val fragmentToGet = when (fragmentName) {
            "NextLevel" -> NextLevelFragment(
                supportFragmentManager
            )
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
                ContextCompat.getDrawable(this, R.drawable.playerhpbar)
        }
        val renderableFuture2 = ViewRenderable.builder()
            .setView(this, R.layout.ultimatebar)
            .build()
        renderableFuture2.thenAccept {
            ultRenderablePlayer = it
            player.setUltRenderable(it)
        }
    }

    private fun attemptUltimate(ability: PlayerUltimate) {
        ultimateHandler.beginMeasuring(ability)
    }

    // MARK: Testing-abilities-related stuff
    private fun attackTarget() {
        // disable attack button for the animation duration
        if (playerTarget != null) {
            playground_attackDuckBtn.isEnabled = false
            playground_attackDuckBtn_cd.isEnabled = true
            val ability = Ability.TEST
            val animData =
                ProjectileAnimationData(
                    // TODO make start position relative to screen position
                    playerNode.worldPosition,
                    playerTarget!!.node.worldPosition,
                    this,
                    fragment,
                    ability.uri()
                )
            doAsync { doCooldown(playground_attackDuckBtn_cd, Ability.TEST.getCooldown(), playground_attackDuckBtn) }
            effectPlayer.playSound(R.raw.fireball)

            // cancel the current animation if any
            cancelAnimator(player)
            // the cast animation data (related to the caster's 3d model, not the projectile)
            val animationData = player.model?.getAnimationData(ability.getCastAnimationString())
            player.setModelAnimator(ModelAnimator(animationData, player.model))
            // TODO: put 3 lines below in onCCDamaged and modify it to return the ability as well
            player.incrementAbilitiesUsed()
            player.increaseUltProgress(ability.getDamage(player.getStatus()).toInt())
            updateUltBar(player.getUltBar()?.view?.textView_ultbar, player)
            player.useAbility(ability, playerTarget!!.model, animData) {

            }

            //playground_beamDuckBtn_cd.visibility = View.
        } else {
            Toast.makeText(this, "You don't have a target", Toast.LENGTH_SHORT).show()
        }
    }

    private fun doCooldown(cdView: TextView, cdTime: Long, skillBtn : ImageButton) {
        Log.d("cooldown", "yes")
        var timePassed = 0.toLong()
        var cycle = 0.toLong()
        var update = true
        var cooldown = true
        while (cooldown) {
            if (update) {
            if (cdTime - timePassed >= cycle) {
                    Log.d("cooldown", "ROUND")
                    update = false
                    cdHandler.postDelayed({
                      runOnUiThread {
                            cdView.visibility = View.VISIBLE
                            cdView.text =
                                (((cdTime - timePassed).toInt())/1000).toString()
                        }
                        if (cycle == 0.toLong()) {
                            cycle = 1000.toLong()
                        }
                        timePassed += cycle
                        update = true
                    }, cycle)
                } else {
                Log.d("cooldown", "WHILE OVER")

                cooldown = false
            }
            }
        }
        cdHandler.postDelayed({
        runOnUiThread {
            cdView.visibility = View.INVISIBLE
            cdView.text = ""
            skillBtn.isEnabled = true
        }
        }, cycle)
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
        val ability = Ability.TEST
        val animData = ProjectileAnimationData(
            // TODO make start position relative to screen position
            node.worldPosition,
            playerNode.worldPosition,
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

    private fun useBarrier(renderable: ViewRenderable?, cc: CombatControllable) {
        animateCast(Ability.TELEPORT.getCastAnimationString()!!)
        player.incrementAbilitiesUsed()
        playground_beamDuckBtn.isEnabled = false
        doAsync { doCooldown(playground_shieldDuckBtn_cd, Ability.SHIELD.getCooldown(), playground_shieldDuckBtn) }
        renderable?.view?.textView_barrier?.visibility = View.VISIBLE
        cc.useShield()
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
            doAsync { doCooldown(playground_beamDuckBtn_cd, Ability.BEAM.getCooldown(), playground_beamDuckBtn) }
            player.setModelAnimator(ModelAnimator(attackAnimationData, player.model))
            player.incrementAbilitiesUsed()
            player.increaseUltProgress(beam.getDamage(player.getStatus()).toInt())
            updateUltBar(player.getUltBar()?.view?.textView_ultbar, player)
            player.useAbility(beam, playerTarget!!.model, data) {
                // should do code below in onCCDamaged()
//                if (playerTarget!!.healthBar != null) {
//                    updateHPBar(playerTarget!!.healthBar, playerTarget!!.model)
//                }
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

    private fun animateCast(abilityStr: String) {
        val animData
                = renderedPlayer!!.getAnimationData(abilityStr)
        val animator = ModelAnimator(animData, renderedPlayer)
        val currentAnimator = player.getModelAnimator()
        if (currentAnimator != null) {
            if (currentAnimator.isRunning) currentAnimator.end()
        }
        player.setModelAnimator(animator)
        animator.start()
    }

    private fun teleportPlayer() {
        Log.d("Teleport", "function")
        playground_teleportDuckBtn.isEnabled = false
        player.incrementAbilitiesUsed()
        fragment.setOnTapArPlaneListener{hitResult, plane, motionEvent ->
            Log.d("Teleport", "tap")
            doAsync { effectPlayer.playSound(R.raw.swoosh) }
            val newAnchor = hitResult.createAnchor()
            val node = AnchorNode(newAnchor)
            val objectAnimation = ObjectAnimator()
            Log.d("Teleport", node.worldPosition.toString())
            objectAnimation.setAutoCancel(true)
            objectAnimation.target = playerNode
            objectAnimation.setObjectValues(playerNode.worldPosition, node.worldPosition)
            objectAnimation.setPropertyName("worldPosition")
            objectAnimation.setEvaluator(Vector3Evaluator())
            objectAnimation.interpolator =LinearInterpolator()
            objectAnimation.duration = 0
            objectAnimation.start()
            animateCast(Ability.TELEPORT.getCastAnimationString()!!)
            doAsync { doCooldown(playground_teleportDuckBtn_cd, Ability.TELEPORT.getCooldown(), playground_teleportDuckBtn) }
            forceStop = true
            fragment.setOnTapArPlaneListener(null)
            npcAnchors.forEach {
                if (it.npc.getType() == NPCType.MELEE) {
                    attackInitializer(it.npc.getType(),it.npc,
                        it.anchorNode.children[0] as TransformableNode
                    )
                }
            }
/*            val savedRenderable = playerNode.renderable
            val savedHp = playerNode.children[0].renderable
            playerNode.renderable = null
            playerNode.children[0].renderable = null
            hpRenderablePlayer = null
            Handler().postDelayed({
                playerNode.renderable = savedRenderable
                playerNode.children[0].renderable = savedHp
            },150)*/
        }
    }

    private fun clearPlayerTarget() {
        playerTarget = null
    }

    // The player is currently the only one who has an ultimate
    private fun createUltBarPlayer(node: TransformableNode, renderable: ViewRenderable?) {
        val ultNode = Node()
        ultNode.setParent(node)
        ultNode.renderable = renderable
        ultNode.localScale = Vector3(4f, 2.85f, 2.85f)
        ultNode.localPosition = Vector3(0f, 3.4f, 0f)
    }

    // MARK: Testing-abilities-related stuff
    private fun createHPBar(node: TransformableNode, renderable: ViewRenderable?) {
        val hpNode = Node()
        hpNode.setParent(node)
        hpNode.renderable = renderable

        if (node == playerNode) {
            hpNode.localScale = Vector3(4f, 4f, 4f)
            hpNode.localPosition = Vector3(0f, 3.5f, 0f)
        } else {
            hpNode.localScale = Vector3(8f, 8f, 8f)
            hpNode.localPosition = Vector3(0f, 10f, 0f)
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
        allNPChaveSpawned = false
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
            val checkAnchors = npcAnchors.filter { it.npc.getID() == spawnable.getID() }
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
                            node.localScale = Vector3(0.1f, 0.1f, 0.1f)
                            anchorNode.setLookDirection(Vector3.forward())
                            node.scaleController.isEnabled = false
                            node.rotationController.isEnabled = false
                            node.setParent(anchorNode)
                            node.renderable = render
                            if (spawnable is Player) {
                                //PLAYER POST SPAWN CODE
                                val animData= render.getAnimationData("AlienArmature|Alien_Idle")
                                val animator = ModelAnimator(animData, render)
                                player.setModelAnimator(animator)
                                animator.start()
                                playerNode = node
                                playerAnchorNode = anchorNode
                                createHPBar(node, hpRenderable)
                                createUltBarPlayer(node, ultRenderablePlayer)
                                playground_shieldDuckBtn.isEnabled = false
                                playground_teleportDuckBtn.isEnabled = false
                                playground_beamDuckBtn.isEnabled = false
                                doAsync { doCooldown(playground_teleportDuckBtn_cd, Ability.TELEPORT.getCooldown(), playground_teleportDuckBtn) }
                                doAsync { doCooldown(playground_beamDuckBtn_cd, Ability.BEAM.getCooldown(), playground_beamDuckBtn) }
                                doAsync { doCooldown(playground_shieldDuckBtn_cd, Ability.SHIELD.getCooldown(), playground_shieldDuckBtn) }
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
                                npcAnchors.add(
                                    NPCAnchorData(
                                        anchorNode,
                                        spawnable
                                    )
                                )
                                npcsAlive.add(spawnable)
                                playground_targetTxt.text = "Ducks alive ${npcsAlive.size}"
                                node.localScale = Vector3(0.05f, 0.05f, 0.05f)
                                if (spawnable.getID() == 100) {
                                    node.localScale = Vector3(0.6f, 0.6f, 0.6f)
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
                                            val newTarget =
                                                PlayerTargetData(
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
        //val layOutParams = LinearLayout.LayoutParams((parent.width * ratio).toInt(), matchParent)
        val layOutParams = FrameLayout.LayoutParams((parent.width * ratio).toInt(), matchParent)
        val currentMargin = tv.marginEnd
        tv.layoutParams = layOutParams
        layOutParams.setMargins(currentMargin, currentMargin, currentMargin, currentMargin)
        Log.d("SHIELD", "shield amount (updateHpBar): ${model.getStatus().shieldAmount}, hp: ${model.getStatus().currentHealth}")
    }

    private fun updateUltBar(tv: TextView?, player: Player) {
        val parent = tv?.parent as View
        val ratio = (player.getUltProgress().toFloat() / player.getMaxUlt().toFloat())
        //val layOutParams = LinearLayout.LayoutParams((parent.width * ratio).toInt(), matchParent)
        val layOutParams = FrameLayout.LayoutParams((parent.width * ratio).toInt(), matchParent)
        val currentMargin = tv.marginEnd
        tv.layoutParams = layOutParams
        layOutParams.setMargins(currentMargin, currentMargin, currentMargin, currentMargin)
        if (ratio == 1f) {
            playground_ultimateBar.visibility = View.VISIBLE
        }
    }

    private fun updateShieldBar(tv: GifImageView?, model: CombatControllable) {
        val parent = tv?.parent as View
        val ratio = model.getStatus().shieldAmount / model.getStatus().maxShieldAmount
        val layoutParams: FrameLayout.LayoutParams
        if (model.getStatus().shieldAmount == 0.0) {
            layoutParams = FrameLayout.LayoutParams((parent.width ), matchParent)
            tv.visibility = View.INVISIBLE
        } else {
            layoutParams = FrameLayout.LayoutParams((parent.width * ratio).toInt(), matchParent)
        }
        val currentMargin = tv.marginEnd
        tv.layoutParams = layoutParams
        layoutParams.setMargins(currentMargin, currentMargin, currentMargin, currentMargin)
        Log.d("SHIELD", "shield amount: ${model.getStatus().shieldAmount}")
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
                    playerNode.worldPosition
                )
            )
            val stopX = playerNode.worldPosition.x
            val stopZ = playerNode.worldPosition.z
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
                        playerNode.worldPosition.y,
                        playerNode.worldPosition.z
                    )
                    objectAnimation.setObjectValues(model.worldPosition, stop)
                    objectAnimation.duration = 15000
                    Handler().postDelayed({
                        // Attack until attacker or target is dead
                        attackLooperMelee(npc, model)
                    }, 8000)
                }
                else -> {
                    val newX = ((stopX + startX) / 2)
                    val newZ = ((stopZ + startZ) / 2)
                    val stop = Vector3(newX, model.worldPosition.y, newZ)
                    objectAnimation.setObjectValues(model.worldPosition, stop)
                    objectAnimation.duration = 6000
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
        forceStop = false
        var cooldown = false
        val thread = Thread {
            while (npc.getStatus().isAlive && player.getStatus().isAlive && !forceStop) {
                if (!cooldown) {
                    cooldown = true
                    model.setLookDirection(Vector3.forward())
                    Handler(Looper.getMainLooper()).postDelayed({
                        model.setLookDirection(Vector3.forward())
                        Handler(Looper.getMainLooper()).postDelayed({
                            model.setLookDirection(
                                Vector3.subtract(
                                    model.worldPosition,
                                    playerNode.worldPosition
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
        val anchors = npcAnchors.filter { it.npc.getID() == npcID }
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
                R.drawable.healthbarbg
            )
    }

    private fun updateNewTargetHPBar(targetData: PlayerTargetData) {
        val parent = targetData.healthBar?.parent as View
        parent.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.hpbarbgselected
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
        Log.d("CCDMG", "isShielded: ${cc.getStatus().isShielded} amount: ${cc.getStatus().shieldAmount}")
        if (cc == player) {
            if (cc.getStatus().isShielded) {
                updateShieldBar(hpRenderablePlayer?.view?.textView_barrier, player)
            } else {
                // for clearing the view
                // TODO: come up with a smarter implementation
                if (player.getHPBar()?.view?.textView_barrier?.visibility == View.VISIBLE) {
                    updateShieldBar(hpRenderablePlayer?.view?.textView_barrier, player)
                }
                updateHPBar(hpRenderablePlayer?.view?.textView_healthbar, player)
                doAsync { effectPlayer.playSound(R.raw.gothit) }
            }
        } else {
            if (cc is NPC) {
                if (cc.getStatus().isShielded) {
                    updateShieldBar(cc.getHPBar()?.view?.textView_barrier, cc)
                } else {
                    Log.d("CCDMG", "NPC damaged. ID: ${cc.getID()}")
                    if (cc.getHPBar() != null) {
                        updateHPBar(cc.getHPBar()!!.view.textView_healthbar, cc)
                    }
                }
            }
        }
    }

    // Adds 1 to numberOfGames and updates user high score if needed
    private fun newHighScore(score: Int) {
        Log.d("POINTS", "user id: $userId")
        if (userId != null) {
            val db = AppDatabase.get(this).userDao()
            getUser(db, userId!!) {
                val savedScore = saver.getInt("SCORE", 0)
                val totalScore = score + savedScore
                doAsync {
                    // only increment number of games if player is dead
                    if (!player.getStatus().isAlive) db.incrementNumOfGames(userId!!)
                    Log.d(
                        "POINTS", "totalScore ($totalScore) > it.highScore " +
                                "(${it.highScore} : ${totalScore > it.highScore}"
                    )
                    if (totalScore > it.highScore) {
                        //val highscore = Highscore(userId!!, user.username, totalScore)
                        db.updateHighScore(totalScore, it.id)
                        Log.d("POINTS", "New highscore: $totalScore")
                        // TODO: NetworkAPI.postNewHighScore(highScore)
                        uiThread {
                            Toast.makeText(
                                this@GameActivityPlayground,
                                "NEW HIGHSCORE!\nScore: $totalScore",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun getUser(db: UserDao, id: Int, cb: (User) -> Unit) {
        doAsyncResult {
            val user = db.getUser(id)
            onComplete {
                cb(user)
            }
        }
    }

    override fun onCCDeath(cc: CombatControllable) {
        // TODO: Stop any pending animation here
        val totalNpcCount = NPCDataForLevels.getNPCForLevelCount(curLevel!!)
        doAsync { effectPlayer.playSound(R.raw.gothit) }
        Log.d(
            "NPCDED",
            "curLevel: $curLevel LevelOne.size: ${NPCDataForLevels.getNPCForLevelCount(curLevel!!)}"
        )
        val npcsRemaining = totalNpcCount - spawnedNPCs.size
        if (cc == player) {
            doAsyncResult {
                newHighScore(player.calculateScore())
                uiThread {
                    animateCast("AlienArmature|Alien_Death")
                    Toast.makeText(this@GameActivityPlayground, "YOU DIED", Toast.LENGTH_LONG)
                        .show()
                }
                onComplete {
                    saver.edit().putInt("SCORE", 0).apply()
                    player.clearStatus()
                    callFragment("GameOver")
                }
            }
        } else {
            if (cc is NPC) {
                player.addPoints(cc.getStatus().maxHealth.toInt())
                npcsAlive.forEach {
                    if (cc == it) {
                        // the indices should be the same..?
                        // might need to change this to a safer approach
                        val anchor = npcAnchors[npcsAlive.indexOf(it)]
                        // check that the correct anchor was indeed picked
                        if (anchor.npc.getID() == it.getID()) {
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
                                doAsyncResult {
                                    newHighScore(player.calculateScore())
                                    saver.edit().putInt("SCORE", player.calculateScore()).apply()
                                    uiThread {
                                        Toast.makeText(
                                            this@GameActivityPlayground,
                                            "Score: ${player.calculateScore()}",
                                            Toast.LENGTH_LONG
                                        )
                                            .show()
                                        Log.d("CURLEVEL", curLevel.toString())
                                    }
                                    when (curLevel) {
                                        1 -> curLevel = 2
                                        2 -> curLevel = 10
                                        else -> curLevel = 1
                                    }
                                    Log.d("CURLEVEL", curLevel.toString())
                                    saver.edit().putInt("levelNum", curLevel!!).apply()
                                    onComplete {
                                        callFragment("NextLevel") } }
                            }
                            playground_targetTxt.text = "Ducks alive ${npcsAlive.size}"
                            removeAnchorNode(anchor.anchorNode)
                        }, 2000)
                    }
                }
            }
        }
    }

    // UltimateHandler callback to tell whether the player succeeded in casting the ultimate
    override fun onMeasured(succeeded: Boolean, ability: PlayerUltimate) {
        if (succeeded) {
            player.ultUsed()
            playground_ultimateBar.visibility = View.GONE
            updateUltBar(player.getUltBar()?.view?.textView_ultbar, player)
            if (ability == PlayerUltimate.KILLALL) {
                npcsAlive.forEach {
                    val health = it.getStatus().maxHealth
                    player.dealDamage(health * 2, it)
                }
            } else {
                // Serenity heals player to full health and gives shield?
                player.restoreFullHealth()
                useBarrier(hpRenderablePlayer, player)
                updateShieldBar(hpRenderablePlayer?.view?.textView_barrier, player)
                updateHPBar(hpRenderablePlayer?.view?.textView_healthbar, player)
            }
            // TODO: MAKE ULTIMATE BUTTONS INVISIBLE
        }
    }
}