package com.devssocial.localodge.extensions

import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.onScrolledToBottom(callback: () -> Unit) {
    this.addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if (!recyclerView.canScrollVertically(1)) {
                callback()
            }
        }
    })
}