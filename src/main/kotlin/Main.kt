import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import java.time.LocalDate

class F1 : CliktCommand() {
    override fun run() = Unit
}

class DriverCommands : CliktCommand(name = "drivers") {

    val season: Int by option(help = "Season year").int().default(LocalDate.now().year)

    override fun run() {
        println("Fetching drivers of $season season")
    }

}

class CircuitCommands : CliktCommand(name = "circuits") {
    val season: Int by option(help = "Season year").int().default(LocalDate.now().year)

    override fun run() {
        println("Fetching circuits of $season season")
    }

}

fun main(args: Array<String>) {
    F1().subcommands(DriverCommands(), CircuitCommands()).main(args)
}
