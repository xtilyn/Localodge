package com.devssocial.localodge.interfaces

import com.devssocial.localodge.models.PostViewItem

interface PostOptionsListener {
    fun onReportUser(userIdToReport: String, reason: String, desc: String)
    fun onReportPost(postId: String, reason: String, desc: String)
    fun onBlockUser(userId: String)
    fun onBlockPost(postViewItem: PostViewItem, position: Int? = null)
}