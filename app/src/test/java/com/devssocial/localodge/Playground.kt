package com.devssocial.localodge

import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.junit.Test

class Playground {

    @Test
    fun sample() {
        println(10 / 3)
    }

    @Test
    @Throws(InterruptedException::class)
    fun single_merge_inOrder() {
        val temp = Single.merge(
            listOf(
                Single.create<String> { e ->
                    e.onSuccess("hi im single 1")
                },
                Single.create<String> { e ->
                    e.onSuccess("hi im single 2")
                },
                Single.create<String> { e ->
                    e.onSuccess("hi im single 3")
                }
            )
        )

        temp.subscribeOn(Schedulers.trampoline()).observeOn(Schedulers.trampoline())
            .subscribeBy(
                onError = {
                    println(it)
                },
                onNext = {
                    println(it)
                },
                onComplete = {
                    println("hi im done.")
                }
            )
    }

}