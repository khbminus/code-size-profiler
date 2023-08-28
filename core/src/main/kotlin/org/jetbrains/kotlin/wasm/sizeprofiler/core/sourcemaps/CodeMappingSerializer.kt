package org.jetbrains.kotlin.wasm.sizeprofiler.core.sourcemaps

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class CodeMappingSerializer : KSerializer<CodeMapping> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CodeMapping", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: CodeMapping) {
        TODO("Not yet implemented")
    }

    override fun deserialize(decoder: Decoder): CodeMapping {
        val string = decoder.decodeString()
        return CodeMapping.fromBase64VLQ(string)
    }
}