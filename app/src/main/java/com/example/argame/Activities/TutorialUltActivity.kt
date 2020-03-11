package com.example.argame.Activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import com.example.argame.Fragments.TutorialState
import com.example.argame.Fragments.TutorialWindowFragment
import com.example.argame.R
import kotlinx.android.synthetic.main.activity_tutorial_ults.*

/**
 *  The tutorial activity for explaining ultimates.
 *
 *  Has logic for calling the dialog fragment with different states,
 *  explaining where the ultimates are, and how to use both of them.
 */

class TutorialUltActivity : AppCompatActivity(), TutorialWindowFragment.TutorialButtonListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial_ults)
        supportActionBar?.hide()
        initUltFragment()
    }

    override fun onButtonPressed(state: TutorialState) {
        if (state != TutorialState.SERENITY) {
            updateLayout(state)
        } else {
            container_tutorial_ults.visibility = View.GONE
            btn_tutorial_finish.visibility = View.VISIBLE
            btn_tutorial_finish.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun initUltFragment() {
        val frag = TutorialWindowFragment(TutorialState.ULTS)
        supportFragmentManager.beginTransaction()
            .add(R.id.container_tutorial_ults, frag)
            .commit()
    }

    private fun updateLayout(state: TutorialState) {
        val frag = when (state) {
            TutorialState.ABILITIES -> TutorialWindowFragment(TutorialState.ULTS)
            TutorialState.ULTS -> TutorialWindowFragment(TutorialState.KILLALL)
            TutorialState.KILLALL -> TutorialWindowFragment(TutorialState.SERENITY)
            else -> TutorialWindowFragment(TutorialState.ULTS)
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.container_tutorial_ults, frag)
            .addToBackStack(null)
            .commit()
        //supportFragmentManager.popBackStack()
    }
}
