import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.jetbrains.kotlin.wasm.sizeprofiler.tasks.*


class Profiler : CliktCommand(invokeWithoutSubcommand = false) {
    override fun run() = Unit
}


fun main(args: Array<String>) = Profiler()
        .subcommands(Dominators(), Diff(), StructuredDiff(), sourceMaps, FilterVariable())
        .main(args)