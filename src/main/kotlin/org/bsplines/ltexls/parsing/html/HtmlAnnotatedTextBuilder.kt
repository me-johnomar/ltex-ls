/* Copyright (C) 2019-2021 Julian Valentin, LTeX Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.parsing.html

import com.ctc.wstx.api.WstxInputProperties
import org.bsplines.ltexls.parsing.CodeAnnotatedTextBuilder
import org.bsplines.ltexls.tools.Logging
import java.io.StringReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader

class HtmlAnnotatedTextBuilder(
  codeLanguageId: String,
) : CodeAnnotatedTextBuilder(codeLanguageId) {
  private val xmlInputFactory = XMLInputFactory.newInstance()

  private var code = ""
  private var pos = 0
  private val elementNameStack = ArrayDeque<String>()
  private var nextText = ""

  init {
    this.xmlInputFactory.setProperty(WstxInputProperties.P_MIN_TEXT_SEGMENT, 1)
    this.xmlInputFactory.setProperty(WstxInputProperties.P_TREAT_CHAR_REFS_AS_ENTS, true)
    this.xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true)
    this.xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
    this.xmlInputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false)
    this.xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false)
  }

  @Suppress("SwallowedException")
  override fun addCode(code: String): CodeAnnotatedTextBuilder {
    this.code = code
    this.pos = 0
    this.elementNameStack.clear()
    this.elementNameStack.addLast("html")
    this.nextText = ""

    try {
      val xmlStreamReader: XMLStreamReader =
          this.xmlInputFactory.createXMLStreamReader(StringReader(code))

      while (xmlStreamReader.hasNext()) {
        processXmlStreamReaderEvent(xmlStreamReader)
      }
    } catch (e: XMLStreamException) {
      // ignore parser errors
    }

    if (this.pos < code.length) addTextWithWhitespace(code.substring(this.pos))
    return this
  }

  private fun processXmlStreamReaderEvent(xmlStreamReader: XMLStreamReader) {
    val eventType: Int = xmlStreamReader.next()
    val oldPos: Int = this.pos
    this.pos = xmlStreamReader.location.characterOffset
    var skippedCode: String = this.code.substring(oldPos, this.pos)
    var interpretAs = ""

    Logging.logger.finest("Position " + this.pos + " ("
        + xmlStreamReader.location.lineNumber
        + "," + xmlStreamReader.location.columnNumber + "): Event type = "
        + eventType + ", skippedCode = '" + skippedCode + "'")

    if (this.nextText.isNotEmpty()) {
      if (this.nextText == skippedCode) {
        addTextWithWhitespace(this.nextText)
      } else {
        addMarkup(skippedCode, this.nextText)
      }

      skippedCode = ""
      this.nextText = ""
    }

    when (eventType) {
      XMLStreamReader.START_ELEMENT -> {
        val elementName: String = xmlStreamReader.localName
        this.elementNameStack.addLast(elementName)
        Logging.logger.finest(
            "START_ELEMENT: elementName = '" + xmlStreamReader.localName + "'")

        when (elementName) {
          "body", "div", "h1", "h2", "h3", "h4", "h5", "h6", "p", "table", "tr" -> {
            interpretAs += "\n\n"
          }
          "br", "li" -> {
            interpretAs += "\n"
          }
        }
      }
      XMLStreamReader.END_ELEMENT -> {
        Logging.logger.finest("END_ELEMENT")
        this.elementNameStack.removeLastOrNull()
      }
      XMLStreamReader.CHARACTERS -> {
        val elementName: String = (
            if (this.elementNameStack.isEmpty()) "" else this.elementNameStack.last())
        val text: String = xmlStreamReader.text
        Logging.logger.finest("CHARACTERS: text = '$text'")
        if ((elementName != "script") && (elementName != "style")) this.nextText = text
      }
      XMLStreamReader.ENTITY_REFERENCE -> {
        this.nextText = xmlStreamReader.text
        Logging.logger.finest("ENTITY_REFERENCE: text = '" + this.nextText + "'")
      }
      else -> {
        // ignore other event types
      }
    }

    addMarkup(skippedCode, interpretAs)
  }

  private fun addTextWithWhitespace(text: String): CodeAnnotatedTextBuilder {
    var pos = 0

    for (matchResult: MatchResult in WHITESPACE_REGEX.findAll(text)) {
      if (matchResult.range.first > pos) addText(text.substring(pos, matchResult.range.first))
      addMarkup(matchResult.value)
      pos = matchResult.range.last + 1
    }

    if (pos < text.length) addText(text.substring(pos))
    return this
  }

  companion object {
    private val WHITESPACE_REGEX = Regex(" *\r?\n *")
  }
}
