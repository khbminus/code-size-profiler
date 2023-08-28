import org.jetbrains.kotlin.wasm.sizeprofiler.core.DifferenceTree
import org.jetbrains.kotlin.wasm.sizeprofiler.core.graph.VertexWithType
import kotlin.test.Test
import kotlin.test.assertEquals

class CompressionTreeTests {
    @Test
    fun `test tree`() {
        val leftNodes = buildMap {
            ('A'..'L').forEach {
                put(it.toString(), VertexWithType(it.toString().lowercase(), 90 - it.code, it.toString().lowercase()))
            }
        }
        val rightNodes = buildMap {
            ('A'..'M').forEach {
                if (it != 'H') {
                    val add = when (it) {
                        'D' -> -2
                        'E' -> -1
                        'F' -> +3
                        else -> 0
                    }
                    put(it.toString(), VertexWithType(it.toString().lowercase(), 90 - it.code + add, it.toString().lowercase()))
                }
            }
        }
        val commonParents = mapOf(
            "A" to "A",
            "B" to "A",
            "F" to "B",
            "C" to "B",
            "D" to "C",
            "E" to "D",
            "G" to "C",
            "I" to "G",
            "J" to "I",
            "K" to "I",
        )
        val leftParents = commonParents + mapOf("H" to "G", "L" to "I")
        val rightParents = commonParents + mapOf("M" to "F", "L" to "F")
        val compressed = DifferenceTree.build(
            DifferenceTree.RetainedTree(leftNodes, leftParents),
            DifferenceTree.RetainedTree(rightNodes, rightParents)
        )
        assertEquals("Mixed", compressed.nodes["A"]!!.type)
        assertEquals("Mixed", compressed.nodes["B"]!!.type)
        assertEquals("Removed", compressed.nodes["C"]!!.type)
        assertEquals("Removed", compressed.nodes["G"]!!.type)
        assertEquals("Removed", compressed.nodes["I"]!!.type)
        assertEquals("NotChanged", compressed.nodes["J"]!!.type)
        assertEquals("NotChanged", compressed.nodes["K"]!!.type)
        assertEquals("Added", compressed.nodes["F"]!!.type)
        assertEquals("Added", compressed.nodes["M"]!!.type)
        assertEquals("Added", compressed.nodes["L"]!!.type)
        assertEquals("Removed", compressed.nodes["H"]!!.type)
        assertEquals("Removed", compressed.nodes["L (removed)"]!!.type)
    }
}