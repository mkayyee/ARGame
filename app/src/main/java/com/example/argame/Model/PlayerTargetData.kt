package com.example.argame.Model

import android.widget.TextView
import com.google.ar.sceneform.Node

/***
 *  The play is able to select a Target if multiple NPC in scene.
 */
data class PlayerTargetData(val node: Node, val model: NPC, val healthBar: TextView?)