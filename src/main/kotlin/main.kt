import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.ImgCstmzrTelemetry
import com.imgcstmzr.cli.CustomizeCommand
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.Semantics.formattedAs

fun main(vararg args: String) {
    echo("Tracing UI: ${ImgCstmzrTelemetry.tracerUI}".ansi.formattedAs.meta)
    CustomizeCommand().main(args)
}
