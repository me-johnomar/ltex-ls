package org.bsplines.ltexls.parsing.markdown;

import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.bsplines.ltexls.Settings;
import org.bsplines.ltexls.parsing.CodeAnnotatedTextBuilder;
import org.bsplines.ltexls.parsing.DummyGenerator;

public class MarkdownAnnotatedTextBuilder extends CodeAnnotatedTextBuilder {
  private String code;
  private int pos;
  private int dummyCounter;
  private Stack<String> nodeTypeStack = new Stack<>();

  public String language = "en-US";
  public List<String> ignoreNodeTypes = new ArrayList<>();
  public List<String> dummyNodeTypes = new ArrayList<>();

  public MarkdownAnnotatedTextBuilder() {
    this.code = "";
  }

  private void visitChildren(final Node node) {
    node.getChildren().forEach(this::visit);
  }

  private boolean isInNodeType(String nodeType) {
    return this.nodeTypeStack.contains(nodeType);
  }

  private boolean isInNodeType(List<String> nodeTypes) {
    for (String nodeType : this.nodeTypeStack) {
      if (nodeTypes.contains(nodeType)) return true;
    }

    return false;
  }

  private void addMarkup(int newPos) {
    boolean inParagraph = isInNodeType("Paragraph");

    while (true) {
      if ((this.pos >= this.code.length()) || (this.pos >= newPos)) break;
      int curPos = this.code.indexOf('\r', this.pos);

      if ((curPos == -1) || (curPos >= newPos)) {
        curPos = this.code.indexOf('\n', this.pos);
        if ((curPos == -1) || (curPos >= newPos)) break;
      }

      if (curPos > pos) super.addMarkup(this.code.substring(this.pos, curPos));
      super.addMarkup(this.code.substring(curPos, curPos + 1), (inParagraph ? " " : "\n"));

      this.pos = curPos + 1;
    }

    if (newPos > pos) {
      super.addMarkup(this.code.substring(this.pos, newPos));
      this.pos = newPos;
    }
  }

  private void addText(int newPos) {
    if (newPos > pos) {
      super.addText(this.code.substring(this.pos, newPos));
      this.pos = newPos;
    }
  }

  private String generateDummy() {
    return DummyGenerator.getDefault().generate(this.language, this.dummyCounter++);
  }

  /**
   * Add Markdown code to the builder, i.e., parse it and call @c addText and @c addMarkup.
   *
   * @param code Markdown code
   * @return @c this
   */
  public MarkdownAnnotatedTextBuilder addCode(String code) {
    Parser parser = Parser.builder().build();
    Document document = parser.parse(code);
    visit(document);
    return this;
  }

  private void visit(Document document) {
    this.code = document.getChars().toString();
    this.pos = 0;
    this.dummyCounter = 0;
    this.nodeTypeStack.clear();
    visitChildren(document);
    if (this.pos < this.code.length()) addMarkup(this.code.length());
  }

  private void visit(Node node) {
    String nodeType = node.getClass().getSimpleName();

    if (nodeType.equals("Text")) {
      if (isInNodeType(this.ignoreNodeTypes)) {
        addMarkup(node.getEndOffset());
      } else {
        addMarkup(node.getStartOffset());
        addText(node.getEndOffset());
      }
    } else if (this.dummyNodeTypes.contains(nodeType)) {
      addMarkup(node.getStartOffset());
      int newPos = node.getEndOffset();

      if (newPos > pos) {
        super.addMarkup(this.code.substring(this.pos, newPos), generateDummy());
        this.pos = newPos;
      }
    } else {
      if (nodeType.equals("Paragraph")) {
        addMarkup(node.getStartOffset());
      }

      this.nodeTypeStack.push(nodeType);
      visitChildren(node);
      this.nodeTypeStack.pop();
    }
  }

  @Override
  public void setSettings(Settings settings) {
    this.language = settings.getLanguageShortCode();
    this.dummyNodeTypes.addAll(settings.getDummyMarkdownNodeTypes());
    this.ignoreNodeTypes.addAll(settings.getIgnoreMarkdownNodeTypes());
  }
}
