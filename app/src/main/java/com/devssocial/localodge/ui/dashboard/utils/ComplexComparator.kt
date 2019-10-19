package com.devssocial.localodge.ui.dashboard.utils

import com.devssocial.localodge.models.Post

/**
 * Order collection by descending priority of comparators:
 * @param comparator1 highest priority
 * @param comparator2
 * @param comparator3 lowest priority
 */
class ComplexComparator(
    private val comparator1: Comparator<Post>,
    private val comparator2: Comparator<Post>,
    private val comparator3: Comparator<Post>
) : Comparator<Post> {

    override fun compare(p0: Post?, p1: Post?): Int {
        val result1 = comparator1.compare(p0, p1)

        return if (result1 == 0) {
            val result2 = comparator2.compare(p0, p1)
            if (result2 == 0) {
                return comparator3.compare(p0, p1)
            } else result2
        } else return result1
    }

}