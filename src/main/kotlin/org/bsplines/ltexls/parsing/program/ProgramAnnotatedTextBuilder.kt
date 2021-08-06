/* Copyright (C) 2020 Julian Valentin, LTeX Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.parsing.program

import org.bsplines.ltexls.parsing.CodeAnnotatedTextBuilder
import org.bsplines.ltexls.parsing.markdown.MarkdownAnnotatedTextBuilder
import org.bsplines.ltexls.tools.I18n
import org.bsplines.ltexls.tools.Logging
import org.languagetool.markup.AnnotatedText

class ProgramAnnotatedTextBuilder(
  codeLanguageId: String
) : CodeAnnotatedTextBuilder(codeLanguageId) {
  private val markdownAnnotatedTextBuilder = MarkdownAnnotatedTextBuilder("markdown")
  private val commentRegexs = ProgramCommentRegexs.fromCodeLanguageId(codeLanguageId)
  private val commentBlockRegex: Regex = commentRegexs.getCommentBlockRegex()
  private val lineCommentPatternString: String? = commentRegexs.lineCommentRegexString

  override fun addCode(code: String): CodeAnnotatedTextBuilder {
    var curPos = 0

    for (matchResult: MatchResult in commentBlockRegex.findAll(code)) {
      val lastPos: Int = curPos
      val isLineComment: Boolean = (matchResult.groups["lineComment"] != null)
      val commentGroupName: String = (if (isLineComment) "lineComment" else "blockComment")
      val commentGroup: MatchGroup? = matchResult.groups[commentGroupName]

      if (commentGroup == null) {
        Logging.logger.warning(
          I18n.format("couldNotFindExpectedGroupInRegularExpressionMatch", commentGroupName)
        )
        continue
      }

      curPos = commentGroup.range.first
      markdownAnnotatedTextBuilder.addMarkup(code.substring(lastPos, curPos), "\n\n")

      val comment: String = commentGroup.value
      addComment(comment, isLineComment)
      curPos = commentGroup.range.last + 1
    }

    if (curPos < code.length) markdownAnnotatedTextBuilder.addMarkup(code.substring(curPos))
    return this
  }

  private fun addComment(comment: String, isLineComment: Boolean): CodeAnnotatedTextBuilder {
    val commonFirstCharacter: String = getCommonFirstCharacterInComment(comment)
    val lineContentsRegex = Regex(
      "[ \t]*"
      + (if (isLineComment && (lineCommentPatternString != null)) lineCommentPatternString else "")
      + "(?:" + Regex.escape(commonFirstCharacter) + ")?[ \t]*(.*?)(?:\r?\n|$)"
    )
    var curPos = 0

    for (matchResult: MatchResult in lineContentsRegex.findAll(comment)) {
      val matchGroup: MatchGroup = matchResult.groups[1] ?: continue

      var lastPos = curPos
      curPos = matchGroup.range.first
      markdownAnnotatedTextBuilder.addMarkup(comment.substring(lastPos, curPos), "\n")

      lastPos = curPos
      curPos = matchGroup.range.last + 1
      markdownAnnotatedTextBuilder.addCode(comment.substring(lastPos, curPos))
    }

    if (curPos < comment.length) markdownAnnotatedTextBuilder.addMarkup(comment.substring(curPos))
    return this
  }

  private fun getCommonFirstCharacterInComment(comment: String): String {
    var commonFirstCharacter = ""

    for (line: String in comment.split(LINE_SEPARATOR_REGEX)) {
      val firstCharacterMatchResult: MatchResult = FIRST_CHARACTER_REGEX.find(line) ?: continue

      if (firstCharacterMatchResult.groups[1] == null) {
        return ""
      }

      val firstCharacter: String = firstCharacterMatchResult.groupValues[1]

      if (commonFirstCharacter.isEmpty()) {
        commonFirstCharacter = firstCharacter
      } else if (firstCharacter != commonFirstCharacter) {
        return ""
      }
    }

    return commonFirstCharacter
  }

  override fun build(): AnnotatedText {
    return markdownAnnotatedTextBuilder.build()
  }

  companion object {
    private val LINE_SEPARATOR_REGEX = Regex("\r?\n")
    private val FIRST_CHARACTER_REGEX = Regex("^[ \t]*(?:([#$%*+\\-/])|(.))")
  }
}
