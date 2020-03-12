package com.example.argame.Activities

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.argame.Fragments.TutorialState
import com.example.argame.Fragments.TutorialWindowFragment
import com.example.argame.R
import kotlinx.android.synthetic.main.activity_tutorial_second.*

/**
 *  The tutorial activity that handles the explanation
 *  of player health bar, ultimate bar and abilities.
 */

class TutorialActivitySecond : AppCompatActivity(), TutorialWindowFragment.TutorialButtonListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial_second)
        supportActionBar?.hide()
        handleFragmentChange(TutorialState.BARS)
    }

    private fun handleFragmentChange(state: TutorialState) {
        if (state == TutorialState.BARS) {
            val frag = TutorialWindowFragment(TutorialState.BARS)
            supportFragmentManager.beginTransaction()
                .add(R.id.container_tutorial_bars, frag)
                .addToBackStack(null)
                .commit()
        } else {
            val frag = TutorialWindowFragment(TutorialState.ABILITIES)
            supportFragmentManager.popBackStack()
            supportFragmentManager.beginTransaction()
                .add(R.id.container_tutorial_abilities, frag)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun enableContinue() {
        supportFragmentManager.popBackStack()
        container_tutorial_continue.isVisible = true
        container_tutorial_continue.setOnClickListener {
            val intent = Intent(this, TutorialUltActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onButtonPressed(state: TutorialState) {
        if (state == TutorialState.BARS) {
            handleFragmentChange(TutorialState.ABILITIES)
        } else {
            enableContinue()
        }
    }

}