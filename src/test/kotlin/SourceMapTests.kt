import sourcemaps.Base64VLQDecoder
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceMapTests {
    @Test
    fun `basic decode`() {
        val line = "AAAA;AAAA,EAAA,OAAO,CAAC,GAAR,CAAY,aAAZ,CAAA,CAAA;AAAA";
        val decoder = Base64VLQDecoder()
        val decoded = line.split(";").map { it.split(",").map(decoder::decode) }
        assertEquals(3, decoded.size)
        val exact = listOf(
            listOf(listOf(0, 0, 0, 0)),
            listOf(
                listOf(0, 0, 0, 0),
                listOf(2, 0, 0, 0),
                listOf(7, 0, 0, 7),
                listOf(1, 0, 0, 1),
                listOf(3, 0, 0, -8),
                listOf(1, 0, 0, 12),
                listOf(13, 0, 0, -12),
                listOf(1, 0, 0, 0),
                listOf(1, 0, 0, 0)
            ),
            listOf(listOf(0, 0, 0, 0)),
        )
        assertEquals(exact, decoded)
    }
}