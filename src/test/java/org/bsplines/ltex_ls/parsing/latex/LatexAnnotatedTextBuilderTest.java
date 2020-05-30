package org.bsplines.ltex_ls.parsing.latex;

import org.bsplines.ltex_ls.parsing.CodeAnnotatedTextBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.languagetool.markup.AnnotatedText;

public class LatexAnnotatedTextBuilderTest {
  private static void assertPlainText(String code, String expectedPlainText) {
    assertPlainText(code, expectedPlainText, "en-US");
  }

  private static void assertPlainText(String code, String expectedPlainText, String language) {
    assertPlainText(code, expectedPlainText, language, "latex");
  }

  private static void assertPlainText(String code, String expectedPlainText, String language,
      String codeLanguageId) {
    AnnotatedText annotatedText = buildAnnotatedText(code, language, codeLanguageId);
    Assertions.assertEquals(expectedPlainText, annotatedText.getPlainText());
  }

  private static AnnotatedText buildAnnotatedText(String code) {
    return buildAnnotatedText(code, "en-US", "latex");
  }

  private static AnnotatedText buildAnnotatedText(String code, String language,
        String codeLanguageId) {
    LatexAnnotatedTextBuilder builder =
        (LatexAnnotatedTextBuilder)CodeAnnotatedTextBuilder.create(codeLanguageId);
    builder.language = language;
    builder.isInStrictMode = true;
    return builder.addCode(code).build();
  }

  @Test
  public void testTextMode() {
    assertPlainText(
        "We can do\n" +
        "\\begin{itemize}[first-{test}]{[second]-test}\n" +
        "  \\item this or\n" +
        "  \\item that.\n" +
        "\\end{itemize}\n",
        "We can do this or that. ");
    assertPlainText("This is good\\dots No, it isn't.\n", "This is good... No, it isn't. ");
    assertPlainText("This is a test of\\\\line breaks.\n", "This is a test of line breaks. ");
    assertPlainText(
        "This is a sentence.%\n" +
        "\n" +
        "This is another sentence.\n",
        "This is a sentence.\n\nThis is another sentence. ");
    assertPlainText(
        "This is a sentence.%\r\n" +
        "\r\n" +
        "This is another sentence.\r\n",
        "This is a sentence.\n\nThis is another sentence. ");
    assertPlainText("This is a \\textcolor{mittelblau}{test}.\n", "This is a test. ");
    assertPlainText("This is a \\raisebox{-0.5\\height-0.5mm}{test}.\n", "This is a test. ");
    assertPlainText("This is a &test.\n", "This is a test. ");
    assertPlainText("You can see this in \\hyperref[alg:abc]{Sec.\\ \\ref*{alg:abc}}.\n",
        "You can see this in Sec. Dummy0. ");
    assertPlainText("This is a te\\-st.\n", "This is a test. ");
    assertPlainText("Ich hei\\ss{}e anders. Das Wasser ist hei\\ss.\n",
        "Ich hei\u00dfe anders. Das Wasser ist hei\u00df. ");
    assertPlainText("Das macht dann 10 \\euro. Oder z.\\,B. vielleicht doch 12~\\euro{}?\n",
        "Das macht dann 10 \u20ac. Oder z.\u202fB. vielleicht doch 12 \u20ac? ");
    assertPlainText(
        "\\\"E\\\"in T\\\"ext m\\\"{i}t v\\\"i\\\"{e}l\\\"en " +
        "\\\"{U}ml\\\"a\\\"{u}t\\\"en.\n",
        "\u00cb\u00efn T\u00ebxt m\u00eft v\u00ef\u00ebl\u00ebn " +
        "\u00dcml\u00e4\u00fct\u00ebn. ");
    assertPlainText(
        "\\AA\\O\\aa\\ss\\o" +
        "\\`A\\`E\\`I\\`O\\`U\\`a\\`e\\`i\\`\\i\\`o\\`u" +
        "\\'A\\'E\\'I\\'O\\'U\\'Y\\'a\\'e\\'i\\'\\i\\'o\\'u\\'y" +
        "\\^A\\^E\\^I\\^O\\^U\\^Y\\^a\\^e\\^i\\^\\i\\^o\\^u\\^y" +
        "\\~A\\~E\\~I\\~N\\~O\\~U\\~a\\~e\\~i\\~\\i\\~n\\~o\\~u" +
        "\\\"A\\\"E\\\"I\\\"O\\\"U\\\"Y\\\"a\\\"e\\\"i\\\"\\i\\\"o\\\"u\\\"y" +
        "\\=A\\=E\\=I\\=O\\=U\\=Y\\=a\\=e\\=i\\=\\i\\=o\\=u\\=y" +
        "\\.A\\.E\\.I\\.O\\.a\\.e\\.o",
        "\u00c5\u00d8\u00e5\u00df\u00f8" +
        "\u00c0\u00c8\u00cc\u00d2\u00d9\u00e0\u00e8\u00ec\u00ec\u00f2\u00f9" +
        "\u00c1\u00c9\u00cd\u00d3\u00da\u00dd\u00e1\u00e9\u00ed\u00ed\u00f3\u00fa\u00fd" +
        "\u00c2\u00ca\u00ce\u00d4\u00db\u0176\u00e2\u00ea\u00ee\u00ee\u00f4\u00fb\u0177" +
        "\u00c3\u1ebc\u0128\u00d1\u00d5\u0168\u00e3\u1ebd\u0129\u0129\u00f1\u00f5\u0169" +
        "\u00c4\u00cb\u00cf\u00d6\u00dc\u0178\u00e4\u00eb\u00ef\u00ef\u00f6\u00fc\u00ff" +
        "\u0100\u0112\u012a\u014c\u016a\u0232\u0101\u0113\u012b\u012b\u014d\u016b\u0233" +
        "\u0226\u0116\u0130\u022e\u0227\u0117\u022f");
    assertPlainText(
        "\\c{C}\\c c\\r{A}\\r U\\r a\\r u",
        "\u00c7\u00e7\u00c5\u016e\u00e5\u016f");
    assertPlainText(
        "This is a test: a, b, \\dots, c.\n",
        "This is a test: a, b, ..., c. ");
    assertPlainText(
        "This is a test -- this is another test --- this is the final test.\n",
        "This is a test \u2013 this is another test \u2014 this is the final test. ");

    {
      AnnotatedText annotatedText = buildAnnotatedText("\\cite{Kubota}*{Theorem 3.7}\n");
      int start = annotatedText.getOriginalTextPositionFor(5);
      int end = annotatedText.getOriginalTextPositionFor(8 - 1) + 1;
      Assertions.assertTrue(start < end, start + " not smaller than " + end);
    }
  }

  @Test
  public void testTikzMode() {
    assertPlainText("This is a \\tikzset{bla}test.\n", "This is a test. ");
    assertPlainText(
        "This is a test.\n" +
        "\\begin{tikzpicture}\n" +
        "  \\node[color=mittelblau] at (42mm,0mm) {qwerty};\n" +
        "\\end{tikzpicture}\n" +
        "This is another sentence.\n",
        "This is a test. This is another sentence. ");
    assertPlainText(
        "This is a test:\n" +
        "\\begin{tikzpicture}\n" +
        "  \\node {$\\dots$};\n" +
        "  \\node {$a$};\n" +
        "\\end{tikzpicture}\n",
        "This is a test: ");
  }

  @Test
  public void testMathMode() {
    assertPlainText(
        "Recall that\n" +
        "\\begin{equation*}\n" +
        "  \\begin{cases}\n" +
        "    a&\\text{if $b$,}\\\\\n" +
        "    c&\\text{otherwise.}\n" +
        "  \\end{cases}\n" +
        "\\end{equation*}\n" +
        "Now we argue.\n",
        "Recall that Dummy0 if Dummy1, Dummy2 otherwise. Now we argue. ");
    assertPlainText("This equals $a^{b}$.\n", "This equals Dummy0. ");
    assertPlainText(
        "This is the proof:\n" +
        "\\begin{equation}\n" +
        "    a^2 + b^2 = c^2\\hspace*{10mm}.\\quad\\qed\n" +
        "\\end{equation}\n",
        "This is the proof: Dummy0. ");
    assertPlainText(
        "This is another proof:\n" +
        "\\begin{equation}\n" +
        "    a^2 + b^2 = c^2.\\\\[-6.4em]\\qquad\\notag\n" +
        "\\end{equation}\n",
        "This is another proof: Dummy0. ");
    assertPlainText(
        "This equals\n" +
        "\\begin{equation}\n" +
        "  \\begin{split}\n" +
        "    abcdef.\n" +
        "  \\end{split}\n" +
        "\\end{equation}\n" +
        "This is the next sentence.\n",
        "This equals Dummy0. This is the next sentence. ");
    assertPlainText(
        "This is an equation:\n" +
        "\\begin{equation}\n" +
        "    a^2 + b^2 = c^2,\\qquad\\text{which proves the theorem.}" +
        "\\end{equation}%\n" +
        "This is a sentence.\n",
        "This is an equation: Dummy0, which proves the theorem. " +
        "This is a sentence. ");
    assertPlainText(
        "This is a test:\n" +
        "\\begin{equation*}\n" +
        "  a \\text{,~and} b.\n" +
        "\\end{equation*}\n",
        "This is a test: Dummy0, and Dummy1. ");
    assertPlainText(
        "This is a test:\n" +
        "\\[\n" +
        "  E = mc^2.\n" +
        "\\]\n" +
        "And this is another one: \\(c^2\\).\n",
        "This is a test: Dummy0. And this is another one: Dummy1. ");
    assertPlainText(
        "This is a test: $a = b \\footnote{This is another test: $c$.}$.\n" +
        "This is the next sentence: $E = mc^2$.\n",
        "This is a test: Dummy0. This is the next sentence: Dummy1. ");
    assertPlainText(
        "This is a test: $a, b, \\dots, c$.\n" +
        "Second sentence: a, b, $\\dots$, c.\n",
        "This is a test: Dummy0. Second sentence: a, b, Dummy1, c. ");
    assertPlainText(
        "C'est un test: $E = mc^2$.\n",
        "C'est un test: Jimmy-0. ",
        "fr");

    {
      AnnotatedText annotatedText = buildAnnotatedText(
          "This is a test:\n" +
          "\\begin{equation}\n" +
          "  \\scalebox{0.92}{$a$}.\n" +
          "\\end{equation}\n" +
          "This is a sentence.\n");
      int start = annotatedText.getOriginalTextPositionFor(29);
      int end = annotatedText.getOriginalTextPositionFor(31 - 1) + 1;
      Assertions.assertTrue(start < end, start + " not smaller than " + end);
    }

    {
      AnnotatedText annotatedText = buildAnnotatedText(
          "This is a test:\n" +
          "\\begin{equation*}\n" +
          "  a \\text{,~and} b.\n" +
          "\\end{equation*}\n");
      int start = annotatedText.getOriginalTextPositionFor(22);
      int end = annotatedText.getOriginalTextPositionFor(24 - 1) + 1;
      Assertions.assertTrue(start < end, start + " not smaller than " + end);
    }

    {
      AnnotatedText annotatedText = buildAnnotatedText(
          "abc. Let $$\\footnote{$a$.}$$\n" +
          "\n" +
          "abc\n");
      int start = annotatedText.getOriginalTextPositionFor(16);
      int end = annotatedText.getOriginalTextPositionFor(18 - 1) + 1;
      Assertions.assertTrue(start < end, start + " not smaller than " + end);
    }
  }

  @Test
  public void testRsweaveMode() {
    assertPlainText(
        "\\SweaveOpts{prefix.string=figures}\n" +
        "This is a first sentence.\n" +
        "\n" +
        "<<import-packages, echo=false>>=\n" +
        "library(tidyverse, quietly = T)\n" +
        "@\n" +
        "\n" +
        "This is a second sentence.\n" +
        "<<mca-graph, fig=true, echo=false>>=\n" +
        "plot(1:1000, rnorm(1000))\n" +
        "@\n",
        " This is a first sentence.\n\n\n\nThis is a second sentence. ",
        "en-US", "rsweave");
    assertPlainText("<<import-packages>>=\n" +
        "library(tidyverse)\n" +
        "@\n",
        " ",
        "en-US", "rsweave");
    assertPlainText("<<import-packages>>=\n" +
        "library(tidyverse)\n" +
        "@\n",
        "<<import-packages>>= library(tidyverse) @ ");
  }
}
