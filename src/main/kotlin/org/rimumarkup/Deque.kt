package org.rimumarkup

/**
 * Double ended queue which can be pushed and popped from both ends.
 * @param E the type of elements contained in the list (defaults to empty list).
 */
class Deque<E>(list: MutableList<E> = ArrayList<E>().toMutableList()) : MutableList<E> by list {

    override fun toString(): String {
        return this.joinToString(prefix = "[", postfix = "]")
    }

    /**
     * Prepends [element] to start of queue.
     */
    fun pushFirst(element: E) {
        this.add(0, element)
    }

    /**
     * Removes and returns first element.
     * @return first element.
     * @throws [NoSuchElementException] if queue is empty.
     */
    fun popFirst(): E {
        val value = this.first()
        removeFirst()
        return value
    }

    /**
     * Removes first element.
     * @throws [IndexOutOfBoundsException] if queue is empty.
     */
    fun removeFirst() {
        this.removeAt(0)
    }

    /**
     * Returns first element.
     * @return first element.
     * @throws [NoSuchElementException] if queue is empty.
     */
    fun peekFirst(): E {
        return this.first()
    }

    /**
     * Appends [element] to end of queue.
     */
    fun pushLast(element: E) {
        this.add(element)
    }

    /**
     * Removes and returns last element.
     * @throws [NoSuchElementException] if queue is empty.
     */
    fun popLast(): E {
        val value = this.last()
        removeLast()
        return value
    }

    /**
     * Removes last element.
     * @throws [IndexOutOfBoundsException] if queue is empty.
     */
    fun removeLast() {
        this.removeAt(this.size - 1)
    }

    /**
     * Returns last element.
     * @return last element.
     * @throws [NoSuchElementException] if queue is empty.
     */
    fun peekLast(): E {
        return this.last()
    }
}