package difference

import graph.Edge

data class DifferenceEdge(
    val from: DifferenceVertex,
    val to: DifferenceVertex,
    val status: DifferenceStatus
)