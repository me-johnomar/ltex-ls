package org.bsplines.languagetool_languageserver.markdown;

import com.vladsch.flexmark.ast.Document;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.languagetool.markup.AnnotatedText;

public class MarkdownAnnotatedTextBuilder {
  private org.languagetool.markup.AnnotatedTextBuilder builder =
      new org.languagetool.markup.AnnotatedTextBuilder();

  private int idx = 0;

  private boolean inParagraph = false;

  public void visit(Document node) {
    visitChildren(node);
  }

  private void visitChildren(final Node parent) {
    parent.getChildren().forEach(this::visit);
  }

  private void visit(Node node) {
    if (node.getClass() == Paragraph.class) {
      BasedSequence originalText = node.getChars().getBaseSequence();
      BasedSequence passedOver = originalText.subSequence(idx, node.getStartOffset());

      if (inParagraph) {
        throw new UnsupportedOperationException("Nested paragraphs are not supported");
      }

      processProceedingCharacters(passedOver);

      idx = node.getStartOffset();

      inParagraph = true;
      visitChildren(node);
      inParagraph = false;
    } else if (node.getClass() == Text.class) {
      BasedSequence originalText = node.getChars().getBaseSequence();
      BasedSequence passedOver = originalText.subSequence(idx, node.getStartOffset());

      processProceedingCharacters(passedOver);

      idx = node.getEndOffset();

      builder.addText(node.getChars().toString());
    } else {
      visitChildren(node);
    }
  }

  private void processProceedingCharacters(BasedSequence passedOver) {
    for (char ch : passedOver.toString().toCharArray()) {
      if ((ch == '\r') || (ch == '\n')) {
        if (inParagraph) {
          builder.addText(" ");
        } else {
          builder.addText(String.valueOf(ch));
        }
      } else {
        builder.addMarkup(String.valueOf(ch));
      }
    }
  }

  public AnnotatedText getAnnotatedText() {
    return builder.build();
  }
}
