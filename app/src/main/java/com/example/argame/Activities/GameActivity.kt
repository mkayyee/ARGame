package com.example.argame.Activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.argame.Fragments.CustomArFragment
import com.example.argame.Fragments.MenuFragmentController
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.R
import com.google.ar.sceneform.ux.TransformableNode

class GameActivity : AppCompatActivity(), FragmentCallbackListener {

    private val menuFragController = MenuFragmentController()
    private lateinit var fragment: CustomArFragment
    private var curNode: TransformableNode? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //initMenuContainer()
    }

    private fun initMenuContainer() {
        supportFragmentManager.beginTransaction()
            .add(R.id.main_menu_container, menuFragController)
            .addToBackStack(null)
            .commit()
    }

    // This is a callback fired from every menu's buttons
    override fun onButtonPressed(btn: Button) {
        // The callback's are forwarded to MenuFragmentController,
        // that handles all the logic for these events
        menuFragController.onButtonPressed(btn)
    }
}
