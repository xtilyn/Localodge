package com.devssocial.localodge.extensions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View

fun View.instaGone() {
    visibility = View.GONE
}

fun View.instaInvisible() {
    visibility = View.INVISIBLE
}

fun View.instaVisible() {
    visibility = View.VISIBLE
}

fun View.visible(animate: Boolean = true, onAnimationEndCallback: () -> Unit = {}) {
    if (animate) {
        animate().alpha(1f).setDuration(300).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                super.onAnimationEnd(animation, isReverse)
                onAnimationEndCallback()
            }
        })
    } else {
        visibility = View.VISIBLE
    }
}

/** Set the View visibility to INVISIBLE and eventually animate view alpha till 0% */
fun View.invisible(animate: Boolean = true) {
    if (animate) {
        animate().alpha(0f).setDuration(300).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                visibility = View.INVISIBLE
            }
        })
    } else {
        visibility = View.INVISIBLE
    }
}

/** Set the View visibility to GONE and eventually animate view alpha till 0% */
fun View.gone(animate: Boolean = true) {
    if (animate) {
        animate().alpha(0f).setDuration(300).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                visibility = View.GONE
            }
        })
    } else {
        visibility = View.GONE
    }
}