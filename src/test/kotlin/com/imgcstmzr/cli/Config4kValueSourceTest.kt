package com.imgcstmzr.cli

import com.github.ajalt.clikt.sources.ExperimentalValueSourceApi
import com.github.ajalt.clikt.sources.ValueSource.Invocation
import com.imgcstmzr.cli.Config4kValueSource.Companion.update
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(ExecutionMode.CONCURRENT)
internal class Config4kValueSourceTest {

    val config4kValueSource = Config4kValueSource.proxy().also {
        it.update(javaClass.getResource("/sample.conf"))
    }

    @ExperimentalValueSourceApi
    @Test
    internal fun `should load simple property`() {
        val values = config4kValueSource.getValues(TestCli.ctx, TestCli.cmd.os)
        expectThat(values).isEqualTo(listOf(Invocation(listOf("RASPBERRY_PI_OS_LITE"))))
    }

    @ExperimentalValueSourceApi
    @Test
    internal fun `should load complex property`() {
        val values = config4kValueSource.getValues(TestCli.ctx, TestCli.cmd.commands_)
        expectThat(values).isEqualTo(listOf(Invocation(listOf("the-basics=\n" + """
            sudo -i
            :
            echo "Configuring SSH port"
            sed -i 's/^\#Port 22$/Port 1234/g' /etc/ssh/sshd_config
            :
            exit
        
        """.trimMargin())),
            Invocation(listOf("very-----------------long=echo ''")),
            Invocation(listOf("middle=echo ''")),
            Invocation(listOf("shrt=echo ''")), Invocation(listOf("leisure=\n" + """
            sudo -i
            : I'm titling things
            # I rather explain things

            echo "I'm writing a poem __〆(￣ー￣ )"
            cat <<EOF >poem-for-you
            Song of the Witches: “Double, double toil and trouble”
            BY WILLIAM SHAKESPEARE
            (from Macbeth)
            Double, double toil and trouble;
            Fire burn and caldron bubble.
            Fillet of a fenny snake,
            In the caldron boil and bake;
            Eye of newt and toe of frog,
            Wool of bat and tongue of dog,
            Adder's fork and blind-worm's sting,
            Lizard's leg and howlet's wing,
            For a charm of powerful trouble,
            Like a hell-broth boil and bubble.
            Double, double toil and trouble;
            Fire burn and caldron bubble.
            Cool it with a baboon's blood,
            Then the charm is firm and good.
            EOF
            :
            exit
        
""".trimMargin()))))
    }
}
