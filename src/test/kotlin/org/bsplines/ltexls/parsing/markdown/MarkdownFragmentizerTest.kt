/* Copyright (C) 2020 Julian Valentin, LTeX Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.parsing.markdown

import org.bsplines.ltexls.parsing.CodeFragment
import org.bsplines.ltexls.parsing.CodeFragmentizer
import org.bsplines.ltexls.settings.Settings
import org.junit.platform.suite.api.IncludeEngines
import kotlin.test.Test
import kotlin.test.assertEquals

@IncludeEngines("junit-jupiter")
class MarkdownFragmentizerTest {
  @Test
  fun testFragmentizer() {
    assertFragmentizer(
      "markdown",
      """
      Sentence 1
      
      [comment]: <> "ltex: language=de-DE"
      
      Sentence 2
      
      [comment]:	<>"ltex:	language=en-US"
      
      Sentence 3
      
      """.trimIndent()
    )
    assertFragmentizer(
      "markdown",
      """
      Sentence 1
      
        <!-- ltex: language=de-DE-->      
      
      Sentence 2
      
      <!--			ltex:				language=en-US		-->
      
      Sentence 3
      
      """.trimIndent()
    )
  }

  @Test
  fun testWrongSettings() {
    val fragmentizer: CodeFragmentizer = CodeFragmentizer.create("markdown")
    fragmentizer.fragmentize(
      "Sentence 1\n[comment]: <> \"ltex: languagede-DE\"\n\nSentence 2\n",
      Settings(),
    )
    fragmentizer.fragmentize(
      "Sentence 1\n[comment]: <> \"ltex: unknownKey=abc\"\n\nSentence 2\n",
      Settings(),
    )
  }

  companion object {
    private fun assertFragmentizer(codeLanguageId: String, code: String) {
      val fragmentizer: CodeFragmentizer = CodeFragmentizer.create(codeLanguageId)
      val codeFragments: List<CodeFragment> = fragmentizer.fragmentize(code, Settings())
      assertEquals(5, codeFragments.size)

      assertEquals(codeLanguageId, codeFragments[0].codeLanguageId)
      assertEquals(0, codeFragments[0].fromPos)
      assertEquals(12, codeFragments[0].code.length)
      assertEquals("en-US", codeFragments[0].settings.languageShortCode)

      assertEquals("nop", codeFragments[1].codeLanguageId)
      assertEquals(12, codeFragments[1].fromPos)
      assertEquals(36, codeFragments[1].code.length)
      assertEquals("de-DE", codeFragments[1].settings.languageShortCode)

      assertEquals(codeLanguageId, codeFragments[2].codeLanguageId)
      assertEquals(48, codeFragments[2].fromPos)
      assertEquals(14, codeFragments[2].code.length)
      assertEquals("de-DE", codeFragments[2].settings.languageShortCode)

      assertEquals("nop", codeFragments[3].codeLanguageId)
      assertEquals(62, codeFragments[3].fromPos)
      assertEquals(35, codeFragments[3].code.length)
      assertEquals("en-US", codeFragments[3].settings.languageShortCode)

      assertEquals(codeLanguageId, codeFragments[4].codeLanguageId)
      assertEquals(97, codeFragments[4].fromPos)
      assertEquals(13, codeFragments[4].code.length)
      assertEquals("en-US", codeFragments[4].settings.languageShortCode)
    }
  }
}
