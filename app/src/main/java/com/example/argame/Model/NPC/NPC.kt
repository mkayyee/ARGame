package com.example.argame.Model.NPC

import android.content.Context
import android.util.Log.wtf
import com.example.argame.Model.CombatControllable.CombatControllable
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable


class NPC(ap: Double,
          name: String,
          startHealth: Double,
          model: ModelRenderable? = null,
          private var type: NPCType,
          private var id: Int,
          context: Context,
          private var hpBar: ViewRenderable? = null)
    : CombatControllable(startHealth, name, ap, model, context) {

    private var node: Node? = null

    fun getID() : Int{
        return id
    }

    fun setNode(node: Node) {
        this.node = node
    }

    fun getNode() : Node {
        if (node == null) {
            throw error("NPC NODE IS NULL!!!!!!!!!!!!!!!!")
        } else {
            return node!!
        }
    }

    fun getType() : NPCType {
        return type
    }

    fun setHPRenderable(renderable: ViewRenderable) {
        hpBar = renderable;
    }

    fun getHPBar() : ViewRenderable? {
        return hpBar
    }
}