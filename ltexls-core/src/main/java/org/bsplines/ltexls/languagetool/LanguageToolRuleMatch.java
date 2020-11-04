/* Copyright (C) 2020 Julian Valentin, LTeX Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.languagetool;

import java.util.ArrayList;
import java.util.List;
import org.bsplines.ltexls.server.LtexTextDocumentItem;
import org.bsplines.ltexls.tools.Tools;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.lsp4j.Range;
import org.languagetool.rules.RuleMatch;

public class LanguageToolRuleMatch {
  private @MonotonicNonNull String ruleId;
  private @MonotonicNonNull String sentence;
  private int fromPos;
  private int toPos;
  private String message;
  private List<String> suggestedReplacements;

  public LanguageToolRuleMatch(RuleMatch match) {
    this(((match.getRule() != null) ? match.getRule().getId() : null),
        ((match.getSentence() != null) ? match.getSentence().getText() : null),
        match.getFromPos(), match.getToPos(), match.getMessage(), match.getSuggestedReplacements());
  }

  public LanguageToolRuleMatch(@Nullable String ruleId, @Nullable String sentence,
        int fromPos, int toPos, String message, List<String> suggestedReplacements) {
    if (ruleId != null) this.ruleId = ruleId;
    if (sentence != null) this.sentence = sentence;
    this.fromPos = fromPos;
    this.toPos = toPos;
    this.message = message;
    this.suggestedReplacements = new ArrayList<>(suggestedReplacements);
  }

  public @Nullable String getRuleId() {
    return this.ruleId;
  }

  public @Nullable String getSentence() {
    return this.sentence;
  }

  public int getFromPos() {
    return this.fromPos;
  }

  public int getToPos() {
    return this.toPos;
  }

  public String getMessage() {
    return this.message;
  }

  public List<String> getSuggestedReplacements() {
    return this.suggestedReplacements;
  }

  public void setFromPos(int fromPos) {
    this.fromPos = fromPos;
  }

  public void setToPos(int toPos) {
    this.toPos = toPos;
  }

  public boolean isIntersectingWithRange(Range range, LtexTextDocumentItem document) {
    return Tools.areRangesIntersecting(new Range(document.convertPosition(this.fromPos),
        document.convertPosition(this.toPos)), range);
  }

  public boolean isUnknownWordRule() {
    return ((this.ruleId != null) && (this.ruleId.startsWith("MORFOLOGIK_")
        || this.ruleId.startsWith("HUNSPELL_") || this.ruleId.startsWith("GERMAN_SPELLER_")));
  }
}
