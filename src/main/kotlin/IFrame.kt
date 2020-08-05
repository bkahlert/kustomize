import org.w3c.dom.Element
import kotlin.js.Json
import kotlin.js.json

/**
 * Creates an `iframe` that *ignores* all headers of an included webpage.
 *
 * @param src if src url is passed in use that (this mode ignores body/head/html options),
 * @param name name of the iframe
 * @param body string contents for `<body>`
 * @param head string contents for `<head>`
 * @param html string contents for entire iframe
 * @param container (constructor only) dom element to append iframe to, default = document.body
 * @param scrollingDisabled (constructor only) boolean for the iframe scrolling attr
 * @param sandboxAttributes array of capability flag strings, default = ['allow-scripts']
 */
fun optionsIgnoringIFrame(
    name: String? = null,
    src: String? = null,
    body: String? = null,
    head: String? = null,
    html: String? = null,
    container: Element? = null,
    scrollingDisabled: Boolean? = null,
    vararg sandboxAttributes: SandboxAttribute
): IFrame {
    val json: Json = json()
    if (name != null) json["name"] = name
    if (src != null) json["src"] = src
    if (body != null) json["body"] = body
    if (head != null) json["head"] = head
    if (html != null) json["html"] = html
    if (container != null) json["container"] = container
    if (scrollingDisabled != null) json["scrollingDisabled"] = scrollingDisabled
    json["sandboxAttributes"] = sandboxAttributes.map { it.value() }.toTypedArray()
    return iframe(json)
}

enum class SandboxAttribute {
    /**
     * Allows form submission
     */
    AllowForms,

    /**
     * Allows to open modal windows
     */
    AllowModals,

    /**
     * Allows to lock the screen orientation
     */
    AllowOrientationLock,

    /**
     * Allows to use the Pointer Lock API
     */
    AllowPointerLock,

    /**
     * Allows popups
     */
    AllowPopups,

    /**
     * Allows popups to open new windows without inheriting the sandboxing
     */
    AllowPopupsToEscapeSandbox,

    /**
     * Allows to start a presentation session
     */
    AllowPresentation,

    /**
     * Allows the iframe content to be treated as being from the same origin
     */
    AllowSameOrigin,

    /**
     * Allows to run scripts
     */
    AllowScripts,

    /**
     * Allows the iframe content to navigate its top-level browsing context
     */
    AllowTopNavigation,

    /**
     * Allows the iframe content to navigate its top-level browsing context, but only if initiated by user
     */
    AllowTopNavigationByUserActivation;

    fun value(): String {
        return Regex("([a-z0-9]|(?=[A-Z]))([A-Z])")
            .replace(name.decapitalize(), "\$1${'-'}\$2")
            .let(String::toLowerCase);
    }
}

@JsModule("iframe")
@JsNonModule
private external fun iframe(json: Json): IFrame

@JsModule("iframe")
@JsNonModule
external interface IFrame {
    fun setHTML(a: Json): Unit
}
