/* Copyright (C) 2020 Julian Valentin, LTeX Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.parsing.restructuredtext;

import java.util.List;
import org.bsplines.ltexls.parsing.CodeFragment;
import org.bsplines.ltexls.parsing.CodeFragmentizer;
import org.bsplines.ltexls.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RestructuredtextFragmentizerTest {
  public static void assertFragmentizer(String codeLanguageId, String code) {
    CodeFragmentizer fragmentizer = CodeFragmentizer.create(codeLanguageId);
    List<CodeFragment> codeFragments = fragmentizer.fragmentize(code, new Settings());
    Assertions.assertEquals(5, codeFragments.size());

    Assertions.assertEquals(codeLanguageId, codeFragments.get(0).getCodeLanguageId());
    Assertions.assertEquals(0, codeFragments.get(0).getFromPos());
    Assertions.assertEquals(12, codeFragments.get(0).getCode().length());
    Assertions.assertEquals("en-US", codeFragments.get(0).getSettings().getLanguageShortCode());

    Assertions.assertEquals("nop", codeFragments.get(1).getCodeLanguageId());
    Assertions.assertEquals(12, codeFragments.get(1).getFromPos());
    Assertions.assertEquals(23, codeFragments.get(1).getCode().length());
    Assertions.assertEquals("de-DE", codeFragments.get(1).getSettings().getLanguageShortCode());

    Assertions.assertEquals(codeLanguageId, codeFragments.get(2).getCodeLanguageId());
    Assertions.assertEquals(35, codeFragments.get(2).getFromPos());
    Assertions.assertEquals(14, codeFragments.get(2).getCode().length());
    Assertions.assertEquals("de-DE", codeFragments.get(2).getSettings().getLanguageShortCode());

    Assertions.assertEquals("nop", codeFragments.get(3).getCodeLanguageId());
    Assertions.assertEquals(49, codeFragments.get(3).getFromPos());
    Assertions.assertEquals(23, codeFragments.get(3).getCode().length());
    Assertions.assertEquals("en-US", codeFragments.get(3).getSettings().getLanguageShortCode());

    Assertions.assertEquals(codeLanguageId, codeFragments.get(4).getCodeLanguageId());
    Assertions.assertEquals(72, codeFragments.get(4).getFromPos());
    Assertions.assertEquals(13, codeFragments.get(4).getCode().length());
    Assertions.assertEquals("en-US", codeFragments.get(4).getSettings().getLanguageShortCode());
  }

  @Test
  public void testFragmentizer() {
    assertFragmentizer("restructuredtext",
        "Sentence 1\n"
        + "\n.. ltex: language=de-DE\n\nSentence 2\n"
        + "\n..\tltex:\tlanguage=en-US\n\nSentence 3\n");
  }

  @Test
  public void testWrongSettings() {
    CodeFragmentizer fragmentizer = CodeFragmentizer.create("restructuredtext");
    Assertions.assertDoesNotThrow(() -> fragmentizer.fragmentize(
        "Sentence 1\n\n.. ltex: languagede-DE\n\nSentence 2\n", new Settings()));
    Assertions.assertDoesNotThrow(() -> fragmentizer.fragmentize(
        "Sentence 1\n\n.. ltex: unknownKey=abc\n\nSentence 2\n", new Settings()));
  }
}
