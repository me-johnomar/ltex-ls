package org.bsplines.ltex_ls.parsing.plaintext;

import org.bsplines.ltex_ls.parsing.CodeAnnotatedTextBuilder;

public class PlaintextAnnotatedTextBuilder extends CodeAnnotatedTextBuilder {
  @Override
  public CodeAnnotatedTextBuilder addCode(String text) {
    addText(text);
    return this;
  }
}
