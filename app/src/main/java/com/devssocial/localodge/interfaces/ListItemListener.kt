package com.devssocial.localodge.interfaces

import android.view.View

interface ListItemListener {

    fun onItemClick(view: View, position: Int) {}
    fun onItemLongPress(view: View, position: Int): Boolean = true

}