package com.devssocial.localodge.ui.dashboard.utils

import com.devssocial.localodge.models.Post

object PostsUtil {

    /**
     * Should order specified posts based on priorities:
     * rating (descending) > likesCount (descending) > timestamp (descending)
     * @param posts to order
     */
    fun orderPosts(posts: List<Post>): List<Post> {
        val copy = arrayListOf<Post>()
        copy.addAll(posts)

        val ratingComparator = Comparator<Post> { post1, post2 ->
            post2.rating.compareTo(post1.rating)
        }
        val likesCountComparator = Comparator<Post> { post1, post2 ->
            post2.likes.size.compareTo(post1.likes.size)
        }
        val timestampComparator = Comparator<Post> { post1, post2 ->
            post2.createdDate?.compareTo(post1.createdDate) ?: 0
        }

        return copy.sortedWith(ComplexComparator(
            ratingComparator,
            likesCountComparator,
            timestampComparator
        ))
    }

}