package com.bkahlert.kustomize.util

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class XML(private val document: Document) {

    fun findNodes(xPathExpression: String): List<Node> {
        val nodeList = XPATH.evaluate(xPathExpression, document, XPathConstants.NODESET) as? NodeList ?: error("Error finding nodes")
        return nodeList.toList()
    }

    private fun NodeList.toList() = (0 until length).map { item(it) }

    companion object {
        fun from(xml: String): XML = XML(parse(xml))

        fun parse(xml: String): Document {
            val dbFactory = DocumentBuilderFactory.newInstance().apply {
                isValidating = false
                isNamespaceAware = true
                setFeature("http://xml.org/sax/features/namespaces", false)
                setFeature("http://xml.org/sax/features/validation", false)
                setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
                setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            }
            val dBuilder = dbFactory.newDocumentBuilder()
            val xmlInput = InputSource(StringReader(xml))
            return dBuilder.parse(xmlInput)
        }

        val XPATH: XPath by lazy {
            XPathFactory.newInstance().newXPath()
        }
    }
}

fun Node?.findSibling(predicate: Node.() -> Boolean): Node? {
    var node = this
    while (node != null) {
        if (node.predicate()) return node
        node = node.nextSibling
    }
    return null
}
