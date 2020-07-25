/* Copyright (C) 2020 Julian Valentin, LTeX Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.parsing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.bsplines.ltexls.Settings;
import org.bsplines.ltexls.Tools;
import org.bsplines.ltexls.parsing.latex.LatexFragmentizer;
import org.bsplines.ltexls.parsing.markdown.MarkdownFragmentizer;
import org.bsplines.ltexls.parsing.plaintext.PlaintextFragmentizer;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class CodeFragmentizer {
  private static Map<String, Function<String, Function<Settings, CodeFragmentizer>>>
      constructorMap = new HashMap<>();

  protected String codeLanguageId;
  protected Settings originalSettings;

  static {
    constructorMap.put("latex", (String codeLanguageId) -> (Settings originalSettings) ->
        new LatexFragmentizer(codeLanguageId, originalSettings));
    constructorMap.put("markdown", (String codeLanguageId) -> (Settings originalSettings) ->
        new MarkdownFragmentizer(codeLanguageId, originalSettings));
    constructorMap.put("plaintext", (String codeLanguageId) -> (Settings originalSettings) ->
        new PlaintextFragmentizer(codeLanguageId, originalSettings));
    constructorMap.put("rsweave", (String codeLanguageId) -> (Settings originalSettings) ->
        new LatexFragmentizer(codeLanguageId, originalSettings));
  }

  public CodeFragmentizer(String codeLanguageId, Settings originalSettings) {
    this.codeLanguageId = codeLanguageId;
    this.originalSettings = originalSettings;
  }

  /**
   * Create a @c CodeFragmentizer according to the given code langugage.
   *
   * @param codeLanguageId ID of the code language
   * @param originalSettings settings at the beginning of the document
   * @return corresponding @c CodeFragmentizer
   */
  public static CodeFragmentizer create(String codeLanguageId, Settings originalSettings) {
    @Nullable Function<String, Function<Settings, CodeFragmentizer>> constructor =
        constructorMap.get(codeLanguageId);

    if (constructor != null) {
      return constructor.apply(codeLanguageId).apply(originalSettings);
    } else {
      Tools.logger.warning(Tools.i18n("invalidCodeLanguageId", codeLanguageId));
      return new PlaintextFragmentizer("plaintext", originalSettings);
    }
  }

  public abstract List<CodeFragment> fragmentize(String code);

  public List<CodeFragment> fragmentize(List<CodeFragment> fragments) {
    Settings originalOriginalSettings = this.originalSettings;
    List<CodeFragment> newFragments = new ArrayList<>();

    try {
      for (CodeFragment oldFragment : fragments) {
        this.originalSettings = oldFragment.getSettings();
        List<CodeFragment> curNewFragments = fragmentize(oldFragment.getCode());

        for (CodeFragment newFragment : curNewFragments) {
          newFragments.add(newFragment.withFromPos(
              newFragment.getFromPos() + oldFragment.getFromPos()));
        }
      }
    } finally {
      this.originalSettings = originalOriginalSettings;
    }

    return newFragments;
  }
}
