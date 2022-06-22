package org.rimumarkup

object Io {

    class Reader(text: String) {
        // Split lines on newline boundaries.
        // Lines are mutable so line macro values can be inserted into the reader.
        val lines: MutableList<String>
        var pos = 0       // Line index of current line.
        var cursor: String
            get() {
                assert(!this.eof())
                return this.lines[this.pos]
            }
            set(value) {
                assert(!this.eof())
                this.lines[pos] = value
            }

        init {
            var s = text
            s = s.replace("\u0000", " ") // Used internally by spans package.
            s = s.replace("\u0001", " ") // Used internally by spans package.
            s = s.replace("\u0002", " ") // Used internally by macros package.
            lines = s.split(Regex("""\r\n|\r|\n""")).toMutableList()
        }

        // Return true if the cursor has advanced over all input lines.
        fun eof(): Boolean {
            return this.pos >= this.lines.size
        }

        // Move cursor to next input line.
        fun next() {
            if (!this.eof()) this.pos++
        }

        // Read to the first line matching the re.
        // Return the array of lines preceding the match plus a line containing
        // the $1 match group (if it exists).
        // If an EOF is encountered return all lines.
        // Exit with the reader pointing to the line containing the matched line.
        fun readTo(re: Regex): List<String> {
            val result = mutableListOf<String>()
            var match: MatchResult?
            while (!this.eof()) {
                match = re.find(this.cursor)
                if (match != null) {
                    if (match.groupValues.size > 1) {
                        result.add(match.groupValues[1])   // $1
                    }
                    break
                }
                result.add(this.cursor)
                this.next()
            }
            return result
        }

        fun skipBlankLines() {
            while (!this.eof() && this.cursor.trim() == "") {
                this.next()
            }
        }

    }

    class Writer {
        val buffer = mutableListOf<String>()

        fun write(s: String) {
            this.buffer.add(s)
        }

        override fun toString(): String {
            return this.buffer.joinToString("")
        }

    }

}
