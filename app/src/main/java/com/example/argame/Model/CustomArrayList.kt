package com.example.argame.Model

import android.os.Parcel
import android.os.Parcelable
import com.google.ar.sceneform.AnchorNode
import java.util.ArrayList

class CustomArrayList(): ArrayList<AnchorNode>(), Parcelable {

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest.writeTypedList(CustomArrayList())
    }


}