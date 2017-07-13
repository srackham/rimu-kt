package org.rimumarkup

fun <E> MutableList<E>.pushFirst(element: E) {
    this.add(0, element)
}

fun <E> MutableList<E>.popFirst(): E? {
    if (this.isEmpty()) {
        return null
    }
    val value = this.first()
    this.removeFirst()
    return value
}

fun <E> MutableList<E>.removeFirst() {
    if (this.isNotEmpty()) this.removeAt(0)
}

fun <E> MutableList<E>.peekFirst(): E? {
    return if (this.isNotEmpty()) this.first() else null
}

fun <E> MutableList<E>.pushLast(element: E) {
    this.add(element)
}

fun <E> MutableList<E>.popLast(): E? {
    if (this.isEmpty()) {
        return null
    }
    val value = this.last()
    this.removeLast()
    return value
}

fun <E> MutableList<E>.removeLast() {
    if (this.isNotEmpty()) this.removeAt(this.size - 1)
}

fun <E> MutableList<E>.peekLast(): E? {
    return if (this.isNotEmpty()) this.last() else null
}


/**
 * Implementation of the Deque functionality which provides a double ended queue.
 *
 * This implementation of Deque is backed by a MutableList
 *
 * Created by Andy Bowes on 05/05/2016.
 * https://github.com/gazolla/Kotlin-Algorithm/tree/master/Deque
 *
 * SJR: 2017-07-12:
 * Added contructor argument and toString() method.
 * Added isEmpty() method.
 *
 */
//class Deque<T>(list: MutableList<T>) {
//
//    var backingList = list
//
//    override fun toString(): String {
//        return backingList.toString()
//    }
//
//    fun isEmpty() = backingList.isEmpty()
//
//    fun pushFirst(element: T) {
//        backingList.add(0, element)
//    }
//
//    fun popFirst(): T? {
//        if (backingList.isEmpty()) {
//            return null
//        }
//        val value = backingList.first()
//        removeFirst()
//        return value
//    }
//
//    fun removeFirst() {
//        if (backingList.isNotEmpty()) backingList.removeAt(0)
//    }
//
//    fun peekFirst(): T? {
//        return if (backingList.isNotEmpty()) backingList.first() else null
//    }
//
//    fun pushLast(element: T) {
//        backingList.add(element)
//    }
//
//    fun popLast(): T? {
//        if (backingList.isEmpty()) {
//            return null
//        }
//        val value = backingList.last()
//        removeLast()
//        return value
//    }
//
//    fun removeLast() {
//        if (backingList.isNotEmpty()) backingList.removeAt(backingList.size - 1)
//    }
//
//    fun peekLast(): T? {
//        return if (backingList.isNotEmpty()) backingList.last() else null
//    }
//}