/* Copyright (C) 2020 Julian Valentin, LTeX Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SettingsTest {
  private static Settings compareSettings(Settings settings, Settings otherSettings) {
    Settings settings2 = new Settings(settings);
    Settings otherSettings2 = new Settings(otherSettings);
    Assertions.assertTrue(settings2.equals(settings));
    Assertions.assertTrue(settings.equals(settings2));
    Assertions.assertEquals(settings.hashCode(), settings2.hashCode());
    Assertions.assertFalse(otherSettings.equals(settings2));
    Assertions.assertFalse(settings.equals(otherSettings2));
    return settings2;
  }

  @Test
  public void testJsonSettings() {
    JsonElement jsonSettings = new JsonObject();
    Assertions.assertDoesNotThrow(() -> new Settings(jsonSettings));
  }

  @Test
  public void testProperties() {
    Settings settings = new Settings();
    Settings settings2 = new Settings();

    settings = settings.withEnabled(false);
    Assertions.assertEquals(false, settings.isEnabled());
    settings2 = compareSettings(settings, settings2);

    settings = settings.withLanguageShortCode("languageShortCode");
    Assertions.assertEquals("languageShortCode", settings.getLanguageShortCode());
    settings2 = compareSettings(settings, settings2);

    settings = settings.withDictionary(Collections.singletonList("dictionary"));
    Assertions.assertEquals(Collections.singletonList("dictionary"), settings.getDictionary());
    settings2 = compareSettings(settings, settings2);

    settings = settings.withDisabledRules(Collections.singletonList("disabledRules"));
    Assertions.assertEquals(Collections.singletonList("disabledRules"),
        settings.getDisabledRules());
    settings2 = compareSettings(settings, settings2);

    settings = settings.withEnabledRules(Collections.singletonList("enabledRules"));
    Assertions.assertEquals(Collections.singletonList("enabledRules"),
        settings.getEnabledRules());
    settings2 = compareSettings(settings, settings2);

    settings = settings.withLanguageToolHttpServerUri("languageToolHttpServerUri");
    Assertions.assertEquals("languageToolHttpServerUri", settings.getLanguageToolHttpServerUri());
    settings2 = compareSettings(settings, settings2);

    settings = settings.withDummyCommandPrototypes(
        Collections.singletonList("dummyCommandPrototypes"));
    Assertions.assertEquals(Collections.singletonList("dummyCommandPrototypes"),
        settings.getDummyCommandPrototypes());
    settings2 = compareSettings(settings, settings2);

    settings = settings.withIgnoreCommandPrototypes(
        Collections.singletonList("ignoreCommandPrototypes"));
    Assertions.assertEquals(Collections.singletonList("ignoreCommandPrototypes"),
        settings.getIgnoreCommandPrototypes());
    settings2 = compareSettings(settings, settings2);

    settings = settings.withIgnoreEnvironments(Collections.singletonList("ignoreEnvironments"));
    Assertions.assertEquals(Collections.singletonList("ignoreEnvironments"),
        settings.getIgnoreEnvironments());
    settings2 = compareSettings(settings, settings2);

    settings = settings.withDummyMarkdownNodeTypes(
        Collections.singletonList("dummyMarkdownNodeTypes"));
    Assertions.assertEquals(Collections.singletonList("dummyMarkdownNodeTypes"),
        settings.getDummyMarkdownNodeTypes());
    settings2 = compareSettings(settings, settings2);

    settings = settings.withIgnoreMarkdownNodeTypes(
        Collections.singletonList("ignoreMarkdownNodeTypes"));
    Assertions.assertEquals(Collections.singletonList("ignoreMarkdownNodeTypes"),
        settings.getIgnoreMarkdownNodeTypes());
    settings2 = compareSettings(settings, settings2);

    settings = settings.withIgnoreRuleSentencePairs(Collections.singletonList(
        new IgnoreRuleSentencePair("ruleId", "sentenceString")));
    Assertions.assertEquals(Collections.singletonList(
        new IgnoreRuleSentencePair("ruleId", "sentenceString")),
        settings.getIgnoreRuleSentencePairs());
    settings2 = compareSettings(settings, settings2);

    settings = settings.withMotherTongueShortCode("motherTongueShortCode");
    Assertions.assertEquals("motherTongueShortCode", settings.getMotherTongueShortCode());
    settings2 = compareSettings(settings, settings2);

    settings = settings.withLanguageModelRulesDirectory("languageModelRulesDirectory");
    Assertions.assertEquals("languageModelRulesDirectory",
        settings.getLanguageModelRulesDirectory());
    settings2 = compareSettings(settings, settings2);

    settings = settings.withNeuralNetworkModelRulesDirectory("neuralNetworkModelRulesDirectory");
    Assertions.assertEquals("neuralNetworkModelRulesDirectory",
        settings.getNeuralNetworkModelRulesDirectory());
    settings2 = compareSettings(settings, settings2);

    settings = settings.withWord2VecModelRulesDirectory("word2VecModelRulesDirectory");
    Assertions.assertEquals("word2VecModelRulesDirectory",
        settings.getWord2VecModelRulesDirectory());
    settings2 = compareSettings(settings, settings2);

    settings = settings.withSentenceCacheSize(1337);
    Assertions.assertEquals(1337, settings.getSentenceCacheSize());
    settings2 = compareSettings(settings, settings2);

    settings = settings.withDiagnosticSeverity(DiagnosticSeverity.Error);
    Assertions.assertEquals(DiagnosticSeverity.Error, settings.getDiagnosticSeverity());
    settings2 = compareSettings(settings, settings2);
  }
}