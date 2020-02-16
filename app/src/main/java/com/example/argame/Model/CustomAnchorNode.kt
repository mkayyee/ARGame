package com.example.argame.Model

import android.os.Parcel
import android.os.Parcelable
import com.google.ar.sceneform.AnchorNode

class CustomAnchorNode() : AnchorNode(), Parcelable {

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(anchor.toString())
    }

    companion object {
        fun CREATOR() = Parcelable.Creator<CustomAnchorNode>() {

            override fun createFromParcel (Parcel in)


        }
    }
}