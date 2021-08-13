/* Copyright (C) 2019-2021 Julian Valentin, LTeX Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.server

import org.apache.commons.text.StringEscapeUtils
import org.bsplines.ltexls.languagetool.LanguageToolInterface
import org.bsplines.ltexls.languagetool.LanguageToolRuleMatch
import org.bsplines.ltexls.parsing.AnnotatedTextFragment
import org.bsplines.ltexls.parsing.CodeAnnotatedTextBuilder
import org.bsplines.ltexls.parsing.CodeFragment
import org.bsplines.ltexls.parsing.CodeFragmentizer
import org.bsplines.ltexls.settings.HiddenFalsePositive
import org.bsplines.ltexls.settings.Settings
import org.bsplines.ltexls.settings.SettingsManager
import org.bsplines.ltexls.tools.I18n
import org.bsplines.ltexls.tools.Logging
import org.bsplines.ltexls.tools.Tools
import org.eclipse.lsp4j.Range
import org.languagetool.markup.AnnotatedText
import org.languagetool.markup.TextPart
import java.time.Duration
import java.time.Instant
import java.util.logging.Level

class DocumentChecker(
  val settingsManager: SettingsManager,
) {
  var lastCheckedDocument: LtexTextDocumentItem? = null
    private set

  private fun fragmentizeDocument(
    document: LtexTextDocumentItem,
    range: Range?,
  ): List<CodeFragment> {
    val codeFragmentizer: CodeFragmentizer = CodeFragmentizer.create(document.languageId)
    var code: String = document.text

    if (range != null) {
      code = code.substring(document.convertPosition(range.start),
          document.convertPosition(range.end))
    }

    return codeFragmentizer.fragmentize(code, this.settingsManager.settings)
  }

  private fun buildAnnotatedTextFragments(
    codeFragments: List<CodeFragment>,
    document: LtexTextDocumentItem,
  ): List<AnnotatedTextFragment> {
    val annotatedTextFragments = ArrayList<AnnotatedTextFragment>()

    for (codeFragment: CodeFragment in codeFragments) {
      val builder: CodeAnnotatedTextBuilder = CodeAnnotatedTextBuilder.create(
          codeFragment.codeLanguageId)
      builder.setSettings(codeFragment.settings)
      builder.addCode(codeFragment.code)
      val curAnnotatedText: AnnotatedText = builder.build()
      annotatedTextFragments.add(AnnotatedTextFragment(
          curAnnotatedText, codeFragment, document))
    }

    return annotatedTextFragments
  }

  private fun checkAnnotatedTextFragments(
    annotatedTextFragments: List<AnnotatedTextFragment>,
    rangeOffset: Int,
  ): List<LanguageToolRuleMatch> {
    val matches = ArrayList<LanguageToolRuleMatch>()

    for (annotatedTextFragment: AnnotatedTextFragment in annotatedTextFragments) {
      matches.addAll(checkAnnotatedTextFragment(annotatedTextFragment, rangeOffset))
    }

    return matches
  }

  @Suppress("TooGenericExceptionCaught")
  private fun checkAnnotatedTextFragment(
    annotatedTextFragment: AnnotatedTextFragment,
    rangeOffset: Int,
  ): List<LanguageToolRuleMatch> {
    val codeFragment: CodeFragment = annotatedTextFragment.codeFragment
    val settings: Settings = codeFragment.settings
    this.settingsManager.settings = settings
    val languageToolInterface: LanguageToolInterface =
        this.settingsManager.languageToolInterface ?: run {
          Logging.logger.warning(I18n.format(
              "skippingTextCheckAsLanguageToolHasNotBeenInitialized"))
          return emptyList()
        }

    val codeLanguageId: String = codeFragment.codeLanguageId

    if (!settings.enabled.contains(codeLanguageId)
          && (codeLanguageId != "nop") && (codeLanguageId != "plaintext")) {
      Logging.logger.fine(I18n.format("skippingTextCheckAsLtexHasBeenDisabled", codeLanguageId))
      return emptyList()
    } else if (settings.dictionary.contains("BsPlInEs")) {
      languageToolInterface.enableEasterEgg()
    }

    logTextToBeChecked(annotatedTextFragment.annotatedText, settings)

    val beforeCheckingInstant: Instant = Instant.now()
    val matches: ArrayList<LanguageToolRuleMatch> = try {
      ArrayList(languageToolInterface.check(annotatedTextFragment))
    } catch (e: RuntimeException) {
      Tools.rethrowCancellationException(e)
      Logging.logger.severe(I18n.format("languageToolFailed", e))
      return emptyList()
    }

    if (Logging.logger.isLoggable(Level.FINER)) {
      Logging.logger.finer(I18n.format("checkingDone",
          Duration.between(beforeCheckingInstant, Instant.now()).toMillis()))
    }

    Logging.logger.fine(if (matches.size == 1) I18n.format("obtainedRuleMatch") else
        I18n.format("obtainedRuleMatches", matches.size))
    removeIgnoredMatches(matches)

    val result = ArrayList<LanguageToolRuleMatch>()

    for (match: LanguageToolRuleMatch in matches) {
      result.add(match.copy(
        fromPos = match.fromPos + annotatedTextFragment.codeFragment.fromPos + rangeOffset,
        toPos = match.toPos + annotatedTextFragment.codeFragment.fromPos + rangeOffset,
      ))
    }

    return result
  }

  private fun logTextToBeChecked(annotatedText: AnnotatedText, settings: Settings) {
    if (Logging.logger.isLoggable(Level.FINER)) {
      Logging.logger.finer(I18n.format("checkingText", settings.languageShortCode,
          StringEscapeUtils.escapeJava(annotatedText.plainText),
          ""))

      if (Logging.logger.isLoggable(Level.FINEST)) {
        val builder = StringBuilder()

        for (textPart: TextPart in annotatedText.parts) {
          builder.append(if (builder.isEmpty()) "annotatedTextParts = [" else ", ")
          builder.append(textPart.type.toString())
          builder.append("(\"")
          builder.append(StringEscapeUtils.escapeJava(textPart.part))
          builder.append("\")")
        }

        builder.append("]")
        Logging.logger.finest(builder.toString())
      }
    } else if (Logging.logger.isLoggable(Level.FINE)) {
      var logText: String = annotatedText.plainText
      var postfix = ""

      if (logText.length > MAX_LOG_TEXT_LENGTH) {
        logText = logText.substring(0, MAX_LOG_TEXT_LENGTH)
        postfix = I18n.format("truncatedPostfix", MAX_LOG_TEXT_LENGTH)
      }

      Logging.logger.fine(I18n.format("checkingText",
          settings.languageShortCode, StringEscapeUtils.escapeJava(logText), postfix))
    }
  }

  private fun searchMatchInHiddenFalsePositives(
    ruleId: String,
    sentence: String,
    hiddenFalsePositives: Set<HiddenFalsePositive>,
  ): Boolean {
    for (pair: HiddenFalsePositive in hiddenFalsePositives) {
      if ((pair.ruleId == ruleId) && (pair.sentenceRegex.find(sentence) != null)) return true
    }

    return false
  }

  private fun removeIgnoredMatches(matches: MutableList<LanguageToolRuleMatch>) {
    val settings: Settings = this.settingsManager.settings
    val hiddenFalsePositives: Set<HiddenFalsePositive> = settings.hiddenFalsePositives
    if (matches.isEmpty() || hiddenFalsePositives.isEmpty()) return

    val ignoreMatches = ArrayList<LanguageToolRuleMatch>()

    for (match: LanguageToolRuleMatch in matches) {
      val ruleId: String? = match.ruleId
      val sentence: String? = match.sentence?.trim()
      if ((ruleId == null) || (sentence == null)) continue

      if (searchMatchInHiddenFalsePositives(ruleId, sentence, hiddenFalsePositives)) {
        Logging.logger.fine(I18n.format("hidingFalsePositive", ruleId, sentence))
        ignoreMatches.add(match)
      }
    }

    if (ignoreMatches.isNotEmpty()) {
      Logging.logger.fine(if (ignoreMatches.size == 1)
          I18n.format("hidFalsePositive") else
          I18n.format("hidFalsePositives", ignoreMatches.size))
      for (match: LanguageToolRuleMatch in ignoreMatches) matches.remove(match)
    }
  }

  fun check(
    document: LtexTextDocumentItem,
    range: Range? = null,
  ): Pair<List<LanguageToolRuleMatch>, List<AnnotatedTextFragment>> {
    this.lastCheckedDocument = document
    val originalSettings: Settings = this.settingsManager.settings
    val rangeOffset: Int = (if (range == null) 0 else document.convertPosition(range.start))

    try {
      val codeFragments: List<CodeFragment> = fragmentizeDocument(document, range)
      val annotatedTextFragments: List<AnnotatedTextFragment> =
          buildAnnotatedTextFragments(codeFragments, document)
      val matches: List<LanguageToolRuleMatch> =
          checkAnnotatedTextFragments(annotatedTextFragments, rangeOffset)
      return Pair(matches, annotatedTextFragments)
    } finally {
      this.settingsManager.settings = originalSettings
    }
  }

  companion object {
    private const val MAX_LOG_TEXT_LENGTH = 100
  }
}
