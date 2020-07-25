/* Copyright (C) 2020 Julian Valentin, LTeX Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.parsing.latex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.xtext.xbase.lib.Pair;

public class LatexCommandSignatureMatcher {
  private List<LatexCommandSignature> commandSignatures;
  private Pattern commandPattern;
  private @Nullable String code;
  private @Nullable Matcher matcher;
  private @Nullable List<String> ignoreCommandPrototypes;

  public LatexCommandSignatureMatcher(LatexCommandSignature commandSignature) {
    this(Collections.singletonList(commandSignature));
  }

  public LatexCommandSignatureMatcher(
        Collection<? extends LatexCommandSignature> commandSignatures) {
    this.commandSignatures = new ArrayList<>(commandSignatures);

    Set<String> commandNames = new HashSet<>();
    this.commandSignatures.forEach((LatexCommandSignature x) -> commandNames.add(x.name));
    StringBuilder commandPatternStringBuilder = new StringBuilder("");
    boolean first = true;

    for (String commandName : commandNames) {
      if (first) {
        first = false;
      } else {
        commandPatternStringBuilder.append("|");
      }

      commandPatternStringBuilder.append(Pattern.quote(commandName));
    }

    this.commandPattern = Pattern.compile(commandPatternStringBuilder.toString());

    this.code = null;
    this.matcher = null;
    this.ignoreCommandPrototypes = null;
  }

  public void startMatching(String code, List<String> ignoreCommandPrototypes) {
    this.code = code;
    this.matcher = this.commandPattern.matcher(code);
    this.ignoreCommandPrototypes = new ArrayList<String>(ignoreCommandPrototypes);
  }

  public @Nullable LatexCommandSignatureMatch findNextMatch() {
    // fixes false-positive dereference.of.nullable warnings
    @Nullable String code = this.code;
    @Nullable Matcher matcher = this.matcher;
    @Nullable List<String> ignoreCommandPrototypes = this.ignoreCommandPrototypes;
    if ((code == null) || (matcher == null) || (ignoreCommandPrototypes == null)) return null;

    while (matcher.find()) {
      int fromPos = matcher.start();
      @Nullable LatexCommandSignatureMatch bestMatch = null;

      for (LatexCommandSignature commandSignature : this.commandSignatures) {
        if (ignoreCommandPrototypes.contains(commandSignature.getCommandPrototype())) {
          continue;
        }

        @Nullable List<Pair<Integer, Integer>> arguments =
            commandSignature.matchArgumentsFromPosition(code, fromPos);

        if (arguments != null) {
          LatexCommandSignatureMatch match = new LatexCommandSignatureMatch(
              commandSignature, code, fromPos, arguments);
          if ((bestMatch == null) || (match.getToPos() > bestMatch.getToPos())) bestMatch = match;
        }
      }

      if (bestMatch != null) return bestMatch;
    }

    return null;
  }

  public List<LatexCommandSignature> getCommandSignatures() {
    return Collections.unmodifiableList(this.commandSignatures);
  }
}
