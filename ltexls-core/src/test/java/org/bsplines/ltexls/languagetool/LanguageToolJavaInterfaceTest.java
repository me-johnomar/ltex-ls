package org.bsplines.ltexls.languagetool;

import java.util.List;
import org.bsplines.ltexls.DocumentChecker;
import org.bsplines.ltexls.DocumentCheckerTest;
import org.bsplines.ltexls.LtexTextDocumentItem;
import org.bsplines.ltexls.SettingsManager;
import org.bsplines.ltexls.parsing.AnnotatedTextFragment;
import org.checkerframework.checker.nullness.NullnessUtil;
import org.eclipse.xtext.xbase.lib.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class LanguageToolJavaInterfaceTest {
  private SettingsManager settingsManager = new SettingsManager();
  private DocumentChecker documentChecker = new DocumentChecker(this.settingsManager);

  @Test
  public void testCheck() {
    LtexTextDocumentItem document = DocumentCheckerTest.createDocument("latex",
        "This is an \\textbf{test.}\n% LTeX: language=de-DE\nDies ist eine \\textbf{Test}.\n");
    Pair<List<LanguageToolRuleMatch>, List<AnnotatedTextFragment>> checkingResult =
        this.documentChecker.check(document);
    DocumentCheckerTest.assertMatches(checkingResult.getKey(), 8, 10, 58, 75);
  }

  @Test
  public void testOtherMethods() {
    LanguageToolInterface ltInterface = this.settingsManager.getLanguageToolInterface();
    Assertions.assertNotNull(NullnessUtil.castNonNull(ltInterface));
    Assertions.assertDoesNotThrow(() -> ltInterface.activateDefaultFalseFriendRules());
    Assertions.assertDoesNotThrow(() -> ltInterface.activateLanguageModelRules("foobar"));
    Assertions.assertDoesNotThrow(() -> ltInterface.activateNeuralNetworkRules("foobar"));
    Assertions.assertDoesNotThrow(() -> ltInterface.activateWord2VecModelRules("foobar"));
    Assertions.assertDoesNotThrow(() -> ltInterface.enableEasterEgg());
  }
}
