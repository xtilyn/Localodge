package com.devssocial.localodge

import com.devssocial.localodge.extensions.waitWithCondition
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ObjectExtTest {

    @Test
    fun shouldWaitUntilConditionIsMet() {
        val lock = ReentrantLock()
        val condition = lock.newCondition()
        val atomicBool = AtomicBoolean() // if set to true, lock should stop waiting

        val start = System.currentTimeMillis()
        Completable.timer(2000, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.trampoline())
            .observeOn(Schedulers.trampoline())
            .subscribe{
                atomicBool.set(true)
                lock.withLock {
                    condition.signal()
                }
                val end = System.currentTimeMillis()
                println("total time elapsed = ${end - start} milliseconds")
            }

        this.waitWithCondition(lock, condition, atomicBool)
    }

}