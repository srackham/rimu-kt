package org.rimumarkup

/**
 * Double ended queue which can be pushed and popped from both ends.
 * @param E the type of elements contained in the list (defaults to empty list).
 */
class Deque<E>(list: MutableList<E> = ArrayList<E>().toMutableList()) : MutableList<E> by list {

    override fun toString(): String {
        return this.joinToString(prefix = "[", postfix = "]")
    }

    fun pushFirst(element: E) {
        this.add(0, element)
    }

    fun popFirst(): E? {
        if (this.isEmpty()) {
            return null
        }
        val value = this.first()
        removeFirst()
        return value
    }

    fun removeFirst() {
        if (this.isNotEmpty()) this.removeAt(0)
    }

    fun peekFirst(): E? {
        return if (this.isNotEmpty()) this.first() else null
    }

    fun pushLast(element: E) {
        this.add(element)
    }

    fun popLast(): E? {
        if (this.isEmpty()) {
            return null
        }
        val value = this.last()
        removeLast()
        return value
    }

    fun removeLast() {
        if (this.isNotEmpty()) this.removeAt(this.size - 1)
    }

    fun peekLast(): E? {
        return if (this.isNotEmpty()) this.last() else null
    }
}