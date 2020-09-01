package com.imgcstmzr.process

/**
 * Classifier for different types of [Output].
 */
enum class OutputType(val symbol: String) {
    /**
     * Status messages from the runner itself
     */
    META("𝕄"),

    /**
     * Redirected standard output of the called process
     */
    OUT("𝕆"),

    /**
     * Redirected error output of the called process
     */
    ERR("𝔼")
}
