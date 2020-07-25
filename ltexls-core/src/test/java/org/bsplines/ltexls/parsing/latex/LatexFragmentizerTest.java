/* Copyright (C) 2020 Julian Valentin, LTeX Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.parsing.latex;

import java.util.Arrays;
import java.util.List;
import org.bsplines.ltexls.Settings;
import org.bsplines.ltexls.parsing.CodeFragment;
import org.bsplines.ltexls.parsing.CodeFragmentizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LatexFragmentizerTest {
  private static void testCodeLanguage(String codeLanguageId) {
    CodeFragmentizer fragmentizer = CodeFragmentizer.create(codeLanguageId);
    List<CodeFragment> codeFragments = fragmentizer.fragmentize(
        "Sentence\\footnote[abc]{Footnote} 1\n"
        + "\t\t  %\t ltex: language=de-DE\nSentence 2\\todo{Todo note}\n"
        + "%ltex:\tlanguage=en-US\n\nSentence 3\n", new Settings());
    Assertions.assertEquals(5, codeFragments.size());

    for (CodeFragment codeFragment : codeFragments) {
      Assertions.assertEquals(codeLanguageId, codeFragment.getCodeLanguageId());
    }

    Assertions.assertEquals("Footnote", codeFragments.get(0).getCode());
    Assertions.assertEquals(23, codeFragments.get(0).getFromPos());
    Assertions.assertEquals("en-US", codeFragments.get(0).getSettings().getLanguageShortCode());

    Assertions.assertEquals("Sentence\\footnote[abc]{Footnote} 1\n",
        codeFragments.get(1).getCode());
    Assertions.assertEquals(0, codeFragments.get(1).getFromPos());
    Assertions.assertEquals("en-US", codeFragments.get(1).getSettings().getLanguageShortCode());

    Assertions.assertEquals("Todo note", codeFragments.get(2).getCode());
    Assertions.assertEquals(79, codeFragments.get(2).getFromPos());
    Assertions.assertEquals("de-DE", codeFragments.get(2).getSettings().getLanguageShortCode());

    Assertions.assertEquals("\t\t  %\t ltex: language=de-DE\nSentence 2\\todo{Todo note}\n",
        codeFragments.get(3).getCode());
    Assertions.assertEquals(35, codeFragments.get(3).getFromPos());
    Assertions.assertEquals("de-DE", codeFragments.get(3).getSettings().getLanguageShortCode());

    Assertions.assertEquals("%ltex:\tlanguage=en-US\n\nSentence 3\n",
        codeFragments.get(4).getCode());
    Assertions.assertEquals(90, codeFragments.get(4).getFromPos());
    Assertions.assertEquals("en-US", codeFragments.get(4).getSettings().getLanguageShortCode());

    codeFragments = fragmentizer.fragmentize(
        "This is a \\foreignlanguage{de-DE}{Beispiel}.\n"
        + "\\selectlanguage{fr}\n"
        + "C'est un autre \\textenUS{example}.\n"
        + "\\selectlanguage{german}\n"
        + "Dies ist weiterer \\begin{otherlanguage*}{en-GB}test\\end{otherlanguage*}.\n"
        + "Und schlie\u00dflich ein abschlie\u00dfender "
        + "\\begin{en-US}[abc]\n"
        + "  sentence\n"
        + "  \\begin{french}[abc]\n"
        + "    phrase\n"
        + "  \\end{french}\n"
        + "\\end{en-US}.\n", new Settings());
    Assertions.assertEquals(8, codeFragments.size());

    for (CodeFragment codeFragment : codeFragments) {
      Assertions.assertEquals(codeLanguageId, codeFragment.getCodeLanguageId());
    }

    Assertions.assertEquals("Beispiel", codeFragments.get(0).getCode());
    Assertions.assertEquals(34, codeFragments.get(0).getFromPos());
    Assertions.assertEquals("de-DE", codeFragments.get(0).getSettings().getLanguageShortCode());

    Assertions.assertEquals("This is a \\foreignlanguage{de-DE}{Beispiel}.\n",
        codeFragments.get(1).getCode());
    Assertions.assertEquals(0, codeFragments.get(1).getFromPos());
    Assertions.assertEquals("en-US", codeFragments.get(1).getSettings().getLanguageShortCode());

    Assertions.assertEquals("example", codeFragments.get(2).getCode());
    Assertions.assertEquals(90, codeFragments.get(2).getFromPos());
    Assertions.assertEquals("en-US", codeFragments.get(2).getSettings().getLanguageShortCode());

    Assertions.assertEquals("\\selectlanguage{fr}\nC'est un autre \\textenUS{example}.\n",
        codeFragments.get(3).getCode());
    Assertions.assertEquals(45, codeFragments.get(3).getFromPos());
    Assertions.assertEquals("fr", codeFragments.get(3).getSettings().getLanguageShortCode());

    Assertions.assertEquals("test", codeFragments.get(4).getCode());
    Assertions.assertEquals(171, codeFragments.get(4).getFromPos());
    Assertions.assertEquals("en-GB", codeFragments.get(4).getSettings().getLanguageShortCode());

    Assertions.assertEquals("\n"
        + "    phrase\n"
        + "  ", codeFragments.get(5).getCode());
    Assertions.assertEquals(283, codeFragments.get(5).getFromPos());
    Assertions.assertEquals("fr", codeFragments.get(5).getSettings().getLanguageShortCode());

    Assertions.assertEquals("\n"
        + "  sentence\n"
        + "  \\begin{french}[abc]\n"
        + "    phrase\n"
        + "  \\end{french}\n", codeFragments.get(6).getCode());
    Assertions.assertEquals(250, codeFragments.get(6).getFromPos());
    Assertions.assertEquals("en-US", codeFragments.get(6).getSettings().getLanguageShortCode());

    Assertions.assertEquals("\\selectlanguage{german}\n"
        + "Dies ist weiterer \\begin{otherlanguage*}{en-GB}test\\end{otherlanguage*}.\n"
        + "Und schlie\u00dflich ein abschlie\u00dfender "
        + "\\begin{en-US}[abc]\n"
        + "  sentence\n"
        + "  \\begin{french}[abc]\n"
        + "    phrase\n"
        + "  \\end{french}\n"
        + "\\end{en-US}.\n",
        codeFragments.get(7).getCode());
    Assertions.assertEquals(100, codeFragments.get(7).getFromPos());
    Assertions.assertEquals("de-DE", codeFragments.get(7).getSettings().getLanguageShortCode());
  }

  @Test
  public void testLatex() {
    testCodeLanguage("latex");

    {
      Settings settings = new Settings();
      settings.setIgnoreCommandPrototypes(Arrays.asList(new String[]{"\\todo{}"}));
      CodeFragmentizer fragmentizer = CodeFragmentizer.create("latex");
      List<CodeFragment> codeFragments = fragmentizer.fragmentize(
          "Sentence\\footnote[abc]{Footnote} 1\n"
          + "\t\t  %\t ltex: language=de-DE\nSentence 2\\todo{Todo note}\n"
          + "%ltex:\tlanguage=en-US\n\nSentence 3\n", settings);
      Assertions.assertEquals(4, codeFragments.size());
    }
  }

  @Test
  public void testRsweave() {
    testCodeLanguage("rsweave");
  }

  @Test
  public void testBabelEnvironment() {
    CodeFragmentizer fragmentizer = CodeFragmentizer.create("latex");
    List<CodeFragment> codeFragments = fragmentizer.fragmentize(
        "This is a \\begin{otherlanguage*}{de-DE}Beispiel\\end{otherlanguage*}.\n", new Settings());
    Assertions.assertEquals(2, codeFragments.size());

    codeFragments = fragmentizer.fragmentize(
        "This is a \\begin{de-DE}Beispiel\\end{de-DE}.\n", new Settings());
    Assertions.assertEquals(2, codeFragments.size());
  }
}
