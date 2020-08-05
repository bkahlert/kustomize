import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.html.*
import kotlinx.html.dom.append
import org.hildan.krossbow.stomp.*
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import kotlin.browser.document

suspend fun main() {
    val root: HTMLElement = requireRoot()
    root.append.div {
        h1 {
            +"Welcome to Kotlin/JS!"
        }
        p {
            +"Fancy joining this year's "
            a("https://kotlinconf.com/") {
                +"KotlinConf"
            }
            +"?"
        }
        iframe(IframeSandbox.allowScripts, null) {
            src = "https://google.de"
        }
//        var frame = iframe(json("container" to document.querySelector("#root"), "src" to "https://google.com"))
//        frame.setHTML(json("head" to "<title>test</title>", "body" to "<h1>H1</h1>"))
        var frame =
            optionsIgnoringIFrame(src = "https://google.com", sandboxAttributes = *arrayOf(SandboxAttribute.AllowScripts, SandboxAttribute.AllowSameOrigin))
        // TODO connect to backend
        // TODO replace content
    }

    xy("ws://192.168.16.45:1880")
}

suspend fun xy(url: String) {
    val client = StompClient() // custom WebSocketClient and other config can be passed in here
    val session: StompSession = client.connect(url) // optional login/passcode can be provided here
    session.use { // this: StompSession
        sendText("/some/destination", "Basic text message")
        val subscription: Flow<String> = subscribeText("/status/updates")
        // terminal operators that finish early (like first) also trigger UNSUBSCRIBE automatically
        val firstMessage: String = subscription.first()
        requireRoot().append.div {
            h1 {
                +firstMessage
            }
        }
    }
}


private fun requireRoot(): HTMLElement {
    val rootElement: Element? = document.getElementById("root")
    return if (rootElement is HTMLElement) rootElement else throw NoSuchElementException("Element with ID root is missing")
}
