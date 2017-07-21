import org.junit.Assert.assertEquals
import org.junit.Test
import org.rimumarkup.Reader
import org.rimumarkup.Writer

class IoTest {
    @Test
    fun readerTest() {
        var reader: Reader

        reader = Reader("")
        assertEquals(false, reader.eof())
        assertEquals(1, reader.lines.size)
        assertEquals("", reader.cursor)
        reader.next()
        assertEquals(true, reader.eof())

        reader = Reader("Hello\nWorld!")
        assertEquals(2, reader.lines.size)
        assertEquals("Hello", reader.cursor)
        reader.next()
        assertEquals("World!", reader.cursor)
        assertEquals(false, reader.eof())
        reader.next()
        assertEquals(true, reader.eof())

        reader = Reader("\n\nHello")
        assertEquals(3, reader.lines.size)
        reader.skipBlankLines()
        assertEquals("Hello", reader.cursor)
        assertEquals(false, reader.eof())
        reader.next()
        assertEquals(true, reader.eof())

        reader = Reader("Hello\n*\nWorld!\nHello\n< Goodbye >")
        assertEquals(5, reader.lines.size)
        var lines = reader.readTo("""\*""") ?: mutableListOf()
        assertEquals(1, lines.size)
        assertEquals("Hello", lines[0])
        assertEquals(false, reader.eof())
        lines = reader.readTo("""^<(.*)>$""") ?: mutableListOf()
        assertEquals(3, lines.size)
        assertEquals(" Goodbye ", lines[2])
        assertEquals(true, reader.eof())

        reader = Reader("\n\nHello\nWorld!")
        assertEquals(4, reader.lines.size)
        reader.skipBlankLines()
        lines = reader.readTo("""^$""") ?: mutableListOf()
        assertEquals(2, lines.size)
        assertEquals("World!", lines[1])
        assertEquals(true, reader.eof())
    }

    @Test
    fun writerTest() {
        val writer = Writer()
        writer.write("Hello")
        assertEquals("Hello", writer.buffer[0])
        writer.write("World!")
        assertEquals("World!", writer.buffer[1])
        assertEquals("HelloWorld!", writer.toString())
    }
}
