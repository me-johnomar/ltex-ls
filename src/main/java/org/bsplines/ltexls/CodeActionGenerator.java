package org.bsplines.ltexls;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bsplines.ltexls.languagetool.LanguageToolRuleMatch;
import org.bsplines.ltexls.parsing.AnnotatedTextFragment;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ResourceOperation;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import org.eclipse.xtext.xbase.lib.Pair;

import org.languagetool.markup.AnnotatedText;

public class CodeActionGenerator {
  private SettingsManager settingsManager;

  private static final String acceptSuggestionCodeActionKind =
      CodeActionKind.QuickFix + ".ltex.acceptSuggestion";
  private static final String addToDictionaryCodeActionKind =
      CodeActionKind.QuickFix + ".ltex.addToDictionary";
  private static final String disableRuleCodeActionKind =
      CodeActionKind.QuickFix + ".ltex.disableRule";
  private static final String ignoreRuleInSentenceCodeActionKind =
      CodeActionKind.QuickFix + ".ltex.ignoreRuleInSentence";
  private static final String addToDictionaryCommandName = "ltex.addToDictionary";
  private static final String disableRuleCommandName = "ltex.disableRule";
  private static final String ignoreRuleInSentenceCommandName = "ltex.ignoreRuleInSentence";
  private static final List<String> codeActions = Arrays.asList(new String[]{
      acceptSuggestionCodeActionKind, addToDictionaryCodeActionKind,
      disableRuleCodeActionKind, ignoreRuleInSentenceCodeActionKind});
  private static final List<String> commandNames = Arrays.asList(new String[]{
      addToDictionaryCommandName, disableRuleCommandName, ignoreRuleInSentenceCommandName});
  private static final Set<String> commandNamesAsSet = new HashSet<>(commandNames);
  private static final String dummyPatternStr = "Dummy[0-9]+";
  private static final Pattern dummyPattern = Pattern.compile(dummyPatternStr);

  public CodeActionGenerator(SettingsManager settingsManager) {
    this.settingsManager = settingsManager;
  }

  /**
   * Create diagnostic from rule match.
   *
   * @param match LanguageTool rule match
   * @param document document in which the match occurred
   * @return diagnostic corresponding to @c match
   */
  public Diagnostic createDiagnostic(
        LanguageToolRuleMatch match, LtexTextDocumentItem document) {
    Diagnostic ret = new Diagnostic();
    ret.setRange(new Range(document.convertPosition(match.getFromPos()),
        document.convertPosition(match.getToPos())));
    ret.setSeverity(settingsManager.getSettings().getDiagnosticSeverity());
    ret.setSource("LTeX - " + match.getRuleId());
    ret.setMessage(match.getMessage().replaceAll("<suggestion>(.*?)</suggestion>", "'$1'"));
    return ret;
  }

  /**
   * Generate list of commands and code actions after checking a document.
   *
   * @param params parameters of @c CodeAction method
   * @param document document
   * @param checkingResult lists of rule matches and annotated text fragments
   * @return list of commands and code actions
   */
  public List<Either<Command, CodeAction>> generate(
        CodeActionParams params, LtexTextDocumentItem document,
        Pair<List<LanguageToolRuleMatch>, List<AnnotatedTextFragment>> checkingResult) {
    if (checkingResult.getValue() == null) return Collections.emptyList();

    VersionedTextDocumentIdentifier textDocument = new VersionedTextDocumentIdentifier(
        document.getUri(), document.getVersion());
    List<Either<Command, CodeAction>> result =
        new ArrayList<Either<Command, CodeAction>>();

    List<LanguageToolRuleMatch> addWordToDictionaryMatches = new ArrayList<>();
    List<LanguageToolRuleMatch> ignoreRuleInThisSentenceMatches = new ArrayList<>();
    List<LanguageToolRuleMatch> disableRuleMatches = new ArrayList<>();
    Map<String, List<LanguageToolRuleMatch>> useWordMatchesMap = new LinkedHashMap<>();

    for (LanguageToolRuleMatch match : checkingResult.getKey()) {
      if (match.isIntersectingWithRange(params.getRange(), document)) {
        String ruleId = match.getRuleId();

        if ((ruleId != null) && (ruleId.startsWith("MORFOLOGIK_")
              || ruleId.startsWith("HUNSPELL_") || ruleId.startsWith("GERMAN_SPELLER_"))) {
          addWordToDictionaryMatches.add(match);
        }

        if (match.getSentence() != null) {
          ignoreRuleInThisSentenceMatches.add(match);
        }

        disableRuleMatches.add(match);

        for (String newWord : match.getSuggestedReplacements()) {
          useWordMatchesMap.putIfAbsent(newWord, new ArrayList<>());
          useWordMatchesMap.get(newWord).add(match);
        }
      }
    }

    if (!addWordToDictionaryMatches.isEmpty()
          && settingsManager.getSettings().getLanguageToolHttpServerUri().isEmpty()) {
      CodeAction codeAction = getAddWordToDictionaryCodeAction(document,
          addWordToDictionaryMatches, checkingResult.getValue());
      result.add(Either.forRight(codeAction));
    }

    if (!ignoreRuleInThisSentenceMatches.isEmpty()) {
      List<Pair<String, String>> ruleIdSentencePairs = new ArrayList<>();
      JsonArray ruleIdsJson = new JsonArray();
      JsonArray sentencePatternStringsJson = new JsonArray();
      List<Diagnostic> diagnostics = new ArrayList<>();

      for (LanguageToolRuleMatch match : ignoreRuleInThisSentenceMatches) {
        String ruleId = match.getRuleId();
        String sentence = match.getSentence();
        if ((ruleId == null) || (sentence == null)) continue;
        sentence = sentence.trim();
        Pair<String, String> pair = new Pair<>(ruleId, sentence);

        if (!ruleIdSentencePairs.contains(pair)) {
          Matcher matcher = dummyPattern.matcher(sentence);
          StringBuilder sentencePatternStringBuilder = new StringBuilder();
          int lastEnd = 0;

          while (matcher.find()) {
            sentencePatternStringBuilder.append(Pattern.quote(
                sentence.substring(lastEnd, matcher.start())));
            sentencePatternStringBuilder.append(dummyPatternStr);
            lastEnd = matcher.end();
          }

          if (lastEnd < sentence.length()) {
            sentencePatternStringBuilder.append(Pattern.quote(sentence.substring(lastEnd)));
          }

          ruleIdSentencePairs.add(pair);
          ruleIdsJson.add(ruleId);
          String sentencePatternString = "^" + sentencePatternStringBuilder.toString() + "$";
          sentencePatternStringsJson.add(sentencePatternString);
        }

        diagnostics.add(createDiagnostic(match, document));
      }

      JsonObject arguments = new JsonObject();
      arguments.addProperty("type", "command");
      arguments.addProperty("command", ignoreRuleInSentenceCommandName);
      arguments.addProperty("uri", document.getUri());
      arguments.add("ruleId", ruleIdsJson);
      arguments.add("sentencePattern", sentencePatternStringsJson);
      Command command = new Command(((ruleIdSentencePairs.size() == 1)
          ? Tools.i18n("ignoreRuleInThisSentence")
          : Tools.i18n("ignoreAllRulesInTheSelectedSentences")),
          ignoreRuleInSentenceCommandName);
      command.setArguments(Arrays.asList(arguments));

      CodeAction codeAction = new CodeAction(command.getTitle());
      codeAction.setKind(ignoreRuleInSentenceCodeActionKind);
      codeAction.setDiagnostics(diagnostics);
      codeAction.setCommand(command);
      result.add(Either.forRight(codeAction));
    }

    if (!disableRuleMatches.isEmpty()) {
      List<String> ruleIds = new ArrayList<>();
      JsonArray ruleIdsJson = new JsonArray();
      List<Diagnostic> diagnostics = new ArrayList<>();

      for (LanguageToolRuleMatch match : disableRuleMatches) {
        String ruleId = match.getRuleId();

        if ((ruleId != null) && !ruleIds.contains(ruleId)) {
          ruleIds.add(ruleId);
          ruleIdsJson.add(ruleId);
        }

        diagnostics.add(createDiagnostic(match, document));
      }

      JsonObject arguments = new JsonObject();
      arguments.addProperty("type", "command");
      arguments.addProperty("command", disableRuleCommandName);
      arguments.addProperty("uri", document.getUri());
      arguments.add("ruleId", ruleIdsJson);
      Command command = new Command(((ruleIds.size() == 1)
          ? Tools.i18n("disableRule") : Tools.i18n("disableAllRulesWithMatchesInSelection")),
          disableRuleCommandName);
      command.setArguments(Arrays.asList(arguments));

      CodeAction codeAction = new CodeAction(command.getTitle());
      codeAction.setKind(disableRuleCodeActionKind);
      codeAction.setDiagnostics(diagnostics);
      codeAction.setCommand(command);
      result.add(Either.forRight(codeAction));
    }

    for (Map.Entry<String, List<LanguageToolRuleMatch>> entry : useWordMatchesMap.entrySet()) {
      String newWord = entry.getKey();
      List<LanguageToolRuleMatch> useWordMatches = entry.getValue();
      List<Diagnostic> diagnostics = new ArrayList<>();
      List<Either<TextDocumentEdit, ResourceOperation>> documentChanges = new ArrayList<>();

      for (LanguageToolRuleMatch match : useWordMatches) {
        Diagnostic diagnostic = createDiagnostic(match, document);
        Range range = diagnostic.getRange();

        diagnostics.add(diagnostic);
        documentChanges.add(Either.forLeft(new TextDocumentEdit(textDocument,
            Collections.singletonList(new TextEdit(range, newWord)))));
      }

      CodeAction codeAction = new CodeAction((useWordMatches.size() == 1)
          ? Tools.i18n("useWord", newWord) : Tools.i18n("useWordAllSelectedMatches", newWord));
      codeAction.setKind(acceptSuggestionCodeActionKind);
      codeAction.setDiagnostics(diagnostics);
      codeAction.setEdit(new WorkspaceEdit(documentChanges));
      result.add(Either.forRight(codeAction));
    }

    return result;
  }

  private static int getPlainTextPositionFor(int originalTextPosition,
      AnnotatedText inverseAnnotatedText) {
    return inverseAnnotatedText.getOriginalTextPositionFor(originalTextPosition);
  }

  private CodeAction getAddWordToDictionaryCodeAction(
        LtexTextDocumentItem document,
        List<LanguageToolRuleMatch> addWordToDictionaryMatches,
        List<AnnotatedTextFragment> annotatedTextFragments) {
    List<@Nullable AnnotatedText> invertedAnnotatedTexts = new ArrayList<>();
    List<@Nullable String> plainTexts = new ArrayList<>();

    for (int i = 0; i < annotatedTextFragments.size(); i++) {
      invertedAnnotatedTexts.add(null);
      plainTexts.add(null);
    }

    List<String> unknownWords = new ArrayList<>();
    JsonArray unknownWordsJson = new JsonArray();
    List<Diagnostic> diagnostics = new ArrayList<>();

    for (LanguageToolRuleMatch match : addWordToDictionaryMatches) {
      int fragmentIndex = -1;

      for (int i = 0; i < annotatedTextFragments.size(); i++) {
        if (annotatedTextFragments.get(i).getCodeFragment().contains(match)) {
          fragmentIndex = i;
          break;
        }
      }

      if (fragmentIndex == -1) {
        Tools.logger.warning(Tools.i18n("couldNotFindFragmentForUnknownWord"));
        continue;
      }

      AnnotatedTextFragment annotatedTextFragment = annotatedTextFragments.get(fragmentIndex);

      if (invertedAnnotatedTexts.get(fragmentIndex) == null) {
        invertedAnnotatedTexts.set(fragmentIndex, annotatedTextFragment.invert());
        plainTexts.set(fragmentIndex, annotatedTextFragment.getAnnotatedText().getPlainText());
      }

      AnnotatedText inverseAnnotatedText = invertedAnnotatedTexts.get(fragmentIndex);
      String plainText = plainTexts.get(fragmentIndex);
      int offset = annotatedTextFragment.getCodeFragment().getFromPos();

      @SuppressWarnings({"dereference.of.nullable", "argument.type.incompatible"})
      String word = plainText.substring(
          getPlainTextPositionFor(match.getFromPos() - offset, inverseAnnotatedText),
          getPlainTextPositionFor(match.getToPos() - offset, inverseAnnotatedText));

      if (!unknownWords.contains(word)) {
        unknownWords.add(word);
        unknownWordsJson.add(word);
      }

      diagnostics.add(createDiagnostic(match, document));
    }

    JsonObject arguments = new JsonObject();
    arguments.addProperty("type", "command");
    arguments.addProperty("command", addToDictionaryCommandName);
    arguments.addProperty("uri", document.getUri());
    arguments.add("word", unknownWordsJson);
    Command command = new Command(((unknownWords.size() == 1)
        ? Tools.i18n("addWordToDictionary", unknownWords.get(0))
        : Tools.i18n("addAllUnknownWordsInSelectionToDictionary")),
        addToDictionaryCommandName);
    command.setArguments(Arrays.asList(arguments));

    CodeAction codeAction = new CodeAction(command.getTitle());
    codeAction.setKind(addToDictionaryCodeActionKind);
    codeAction.setDiagnostics(diagnostics);
    codeAction.setCommand(command);

    return codeAction;
  }

  public static List<String> getCodeActions() {
    return codeActions;
  }

  public static List<String> getCommandNames() {
    return commandNames;
  }

  public static Set<String> getCommandNamesAsSet() {
    return commandNamesAsSet;
  }
}