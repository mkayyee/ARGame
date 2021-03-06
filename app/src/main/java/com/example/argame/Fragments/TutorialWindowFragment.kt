package com.example.argame.Fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.example.argame.R
import kotlinx.android.synthetic.main.activity_tutorial_first.*
import kotlinx.android.synthetic.main.activity_tutorial_second.*
import kotlinx.android.synthetic.main.tutorial_window.*

enum class TutorialState {
    START_LEVEL,
    MENU,
    BARS,
    ABILITIES,
    ULTS,
    KILLALL,
    SERENITY;
}

/**
 *  The fragment for the tutorial dialog, and a callback interface for it's initializer.
 *  The callback is fired by the dialog's check button, and is handled by a tutorial Activity,
 *  and those activities call this fragment again, based on the state the callback was sent in.
 *
 *  TutorialState defines what should be inside the dialog's TextView.
 */

class TutorialWindowFragment(private val state: TutorialState) : Fragment() {

    private lateinit var buttonCallback: TutorialButtonListener

    interface TutorialButtonListener {
        fun onButtonPressed(state: TutorialState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        buttonCallback = context as TutorialButtonListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.tutorial_window, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textView_tutorialText.text = when (state) {
            TutorialState.START_LEVEL -> getString(R.string.tutorial_start_level)
            TutorialState.MENU -> getString(R.string.tutorial_menu)
            TutorialState.BARS -> getString(R.string.tutorial_bars)
            TutorialState.ABILITIES -> getString(R.string.tutorial_abilities)
            TutorialState.ULTS -> getString(R.string.tutorial_ults)
            TutorialState.KILLALL -> getString(R.string.tutorial_killall)
            TutorialState.SERENITY -> getString(R.string.tutorial_serenity)
        }
        setButtonListeners()
    }

    private fun setButtonListeners() {
        btn_closeTutorialWindow.setOnClickListener {
            fragmentManager!!.beginTransaction()
                .remove(this)
            buttonCallback.onButtonPressed(state)
        }
    }
}