package com.example.argame.Activities

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.argame.Fragments.TutorialState
import com.example.argame.Fragments.TutorialWindowFragment
import com.example.argame.R
import kotlinx.android.synthetic.main.activity_tutorial_first.*

class TutorialActivityFirst : AppCompatActivity(), TutorialWindowFragment.TutorialButtonListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial_first)
        supportActionBar?.hide()
        handleFragmentChange(TutorialState.START_LEVEL)
    }

    private fun enableNext() {
        supportFragmentManager.popBackStack()
        container_tutorial_next.isVisible = true
        container_tutorial_next.animation = AnimationUtils.loadAnimation(this, R.anim.pulse)
        container_tutorial_next.setOnClickListener {
            val intent = Intent(this, TutorialActivitySecond::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun handleFragmentChange(state: TutorialState) {
        if (state == TutorialState.START_LEVEL) {
            val frag = TutorialWindowFragment(TutorialState.START_LEVEL)
            supportFragmentManager.beginTransaction()
                .add(R.id.container_tutorial_start, frag)
                .addToBackStack(null)
                .commit()
        } else {
            val frag = TutorialWindowFragment(TutorialState.MENU)
            supportFragmentManager.popBackStack()
            supportFragmentManager.beginTransaction()
                .add(R.id.container_tutorial_x, frag)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onButtonPressed(state: TutorialState) {
        Log.d("TUTORIAL", "Button pressed. State: $state")
        if (state == TutorialState.START_LEVEL) {
            handleFragmentChange(TutorialState.MENU)
        } else {
            enableNext()
        }
    }
}