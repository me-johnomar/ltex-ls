/* Copyright (C) 2020 Julian Valentin, LTeX Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.parsing.plaintext;

import org.bsplines.ltexls.parsing.CodeAnnotatedTextBuilder;

public class PlaintextAnnotatedTextBuilder extends CodeAnnotatedTextBuilder {
  @Override
  public CodeAnnotatedTextBuilder addCode(String code) {
    addText(code);
    return this;
  }
}
