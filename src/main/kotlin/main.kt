import com.bkahlert.kustomize.cli.CustomizeCommand
import com.github.ajalt.clikt.output.TermUi.echo
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.Semantics.formattedAs

fun main(vararg args: String) {
    echo("Tracing UI: ${com.bkahlert.kustomize.KustomizeTelemetry.tracerUI}".ansi.formattedAs.meta)
    CustomizeCommand().main(args)
}
