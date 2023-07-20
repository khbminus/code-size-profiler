package sourcemaps

import kotlinx.serialization.Serializable

@Serializable
data class SourceMapFile(
    val version: Int,
    val file: String? = null,
    val sourceRoot: String? = null,
    val sources: List<String>, // TODO: add custom Serializer for path
    val sourcesContent: List<String?>? = null,
    val names: List<String>,
    val mappings: CodeMapping
) {
    init {
        require(version == 3) { "Only version 3 sourcemaps are supported!" }
        require(sourcesContent == null || sourcesContent.size == sources.size) { "SourceContent neither absent or equal sized with sources" }
    }
}