import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import tasks.Diff
import tasks.Dominators
import tasks.SourceMaps
import tasks.StructuredDiff


class Profiler : CliktCommand() {
    override fun run() = Unit
}


fun main(args: Array<String>) = Profiler()
        .subcommands(Dominators(), Diff(), StructuredDiff(), SourceMaps())
        .main(args)