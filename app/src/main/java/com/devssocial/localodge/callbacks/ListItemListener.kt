package com.devssocial.localodge.callbacks

import android.view.View

interface ListItemListener {

    fun onItemClick(view: View, position: Int) {}
    fun onItemLongPress(view: View, position: Int) {}

}