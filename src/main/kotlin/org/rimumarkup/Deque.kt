package org.rimumarkup

/**
 * Implementation of the Deque functionality which provides a double ended queue.
 *
 * This implementation of Deque is backed by a MutableList
 *
 * Created by Andy Bowes on 05/05/2016.
 * https://github.com/gazolla/Kotlin-Algorithm/tree/master/Deque
 *
 * SJR: 2017-07-12: Added contructor argument and toString() method.
 *
 */
class Deque<T>(list: MutableList<T>) {

    var backingList = list

    override fun toString(): String {
        return backingList.toString()
    }

    fun pushFirst(element: T) {
        backingList.add(0, element)
    }

    fun popFirst(): T? {
        if (backingList.isEmpty()) {
            return null
        }
        val value = backingList.first()
        removeFirst()
        return value
    }

    fun removeFirst() {
        if (backingList.isNotEmpty()) backingList.removeAt(0)
    }

    fun peekFirst(): T? {
        return if (backingList.isNotEmpty()) backingList.first() else null
    }

    fun pushLast(element: T) {
        backingList.add(element)
    }

    fun popLast(): T? {
        if (backingList.isEmpty()) {
            return null
        }
        val value = backingList.last()
        removeLast()
        return value
    }

    fun removeLast() {
        if (backingList.isNotEmpty()) backingList.removeAt(backingList.size - 1)
    }

    fun peekLast(): T? {
        return if (backingList.isNotEmpty()) backingList.last() else null
    }
}