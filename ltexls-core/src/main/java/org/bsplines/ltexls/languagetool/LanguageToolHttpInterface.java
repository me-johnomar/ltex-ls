/* Copyright (C) 2020 Julian Valentin, LTeX Development Community
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.bsplines.ltexls.languagetool;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bsplines.ltexls.Tools;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.languagetool.markup.AnnotatedText;
import org.languagetool.markup.TextPart;

public class LanguageToolHttpInterface extends LanguageToolInterface {
  private String languageShortCode;
  private String motherTongueShortCode;

  private @MonotonicNonNull URL url;
  private List<String> enabledRuleIds;
  private List<String> disabledRuleIds;

  /**
   * Constructor.
   *
   * @param uri URI of the LanguageTool HTTP server
   * @param languageShortCode short code of the checking language
   * @param motherTongueShortCode short code of the mother tongue language
   */
  public LanguageToolHttpInterface(String uri, String languageShortCode,
        String motherTongueShortCode) {
    this.languageShortCode = languageShortCode;
    this.motherTongueShortCode = motherTongueShortCode;
    this.enabledRuleIds = new ArrayList<>();
    this.disabledRuleIds = new ArrayList<>();

    try {
      this.url = new URL(new URL(uri), "v2/check");
    } catch (MalformedURLException e) {
      Tools.logger.severe(Tools.i18n("couldNotParseHttpServerUri", uri, e.getMessage()));
      e.printStackTrace();
    }
  }

  @EnsuresNonNullIf(expression = "this.url", result = true)
  @Override
  public boolean isReady() {
    return (this.url != null);
  }

  @Override
  public List<LanguageToolRuleMatch> check(AnnotatedText annotatedText) {
    if (!isReady()) return Collections.emptyList();

    JsonArray jsonDataAnnotation = new JsonArray();
    List<TextPart> parts = annotatedText.getParts();

    for (int i = 0; i < parts.size(); i++) {
      JsonObject jsonPart = new JsonObject();

      if (parts.get(i).getType() == TextPart.Type.TEXT) {
        jsonPart.addProperty("text", parts.get(i).getPart());
      } else if (parts.get(i).getType() == TextPart.Type.MARKUP) {
        jsonPart.addProperty("markup", parts.get(i).getPart());

        if ((i < parts.size() - 1) && (parts.get(i + 1).getType() == TextPart.Type.FAKE_CONTENT)) {
          i++;
          jsonPart.addProperty("interpretAs", parts.get(i).getPart());
        }
      } else {
        // should not happen
        continue;
      }

      jsonDataAnnotation.add(jsonPart);
    }

    JsonObject jsonData = new JsonObject();
    jsonData.add("annotation", jsonDataAnnotation);

    Map<String, String> requestEntries = new HashMap<>();
    requestEntries.put("language", this.languageShortCode);
    requestEntries.put("data", jsonData.toString());

    if (!this.motherTongueShortCode.isEmpty()) {
      requestEntries.put("motherTongue", this.motherTongueShortCode);
    }

    if (!this.enabledRuleIds.isEmpty()) {
      requestEntries.put("enabledRules", String.join(",", this.enabledRuleIds));
    }

    if (!this.disabledRuleIds.isEmpty()) {
      requestEntries.put("disabledRules", String.join(",", this.disabledRuleIds));
    }

    StringBuilder builder = new StringBuilder();

    for (Map.Entry<String, String> requestEntry : requestEntries.entrySet()) {
      if (requestEntry.getValue() == null) {
        continue;
      }

      if (builder.length() > 0) {
        builder.append("&");
      }

      try {
        builder.append(URLEncoder.encode(requestEntry.getKey(), "utf-8"))
            .append("=").append(URLEncoder.encode(requestEntry.getValue(), "utf-8"));
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
    }

    String requestBody = builder.toString();
    String responseBody;
    CloseableHttpClient httpClient = HttpClients.createDefault();

    try {
      HttpPost httpPost;

      try {
        httpPost = new HttpPost(this.url.toURI());
      } catch (URISyntaxException e) {
        Tools.logger.severe(Tools.i18n("couldNotParseHttpServerUri", this.url.toString(),
            e.getMessage()));
        e.printStackTrace();
        return Collections.emptyList();
      }

      httpPost.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
      @Nullable CloseableHttpResponse httpResponse = null;

      try {
        httpResponse = httpClient.execute(httpPost);
      } catch (IOException e) {
        Tools.logger.severe(Tools.i18n("couldNotSendHttpRequestToLanguageTool", e.getMessage()));
        e.printStackTrace();
        return Collections.emptyList();
      }

      try {
        int statusCode = httpResponse.getStatusLine().getStatusCode();

        if (statusCode != 200) {
          Tools.logger.severe(Tools.i18n("languageToolFailed",
              Tools.i18n("receivedStatusCodeFromLanguageTool", statusCode)));
          return Collections.emptyList();
        }

        try {
          InputStream inputStream = httpResponse.getEntity().getContent();
          ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
          byte[] buffer = new byte[1024];
          int length;

          while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
          }

          responseBody = outputStream.toString("UTF-8");
          EntityUtils.consume(httpResponse.getEntity());
        } catch (IOException e) {
          Tools.logger.severe(Tools.i18n("couldNotReadHttpResponseFromLanguageTool",
              e.getMessage()));
          e.printStackTrace();
          return Collections.emptyList();
        }
      } finally {
        try {
          if (httpResponse != null) httpResponse.close();
        } catch (IOException e) {
          Tools.logger.warning(Tools.i18n("couldNotCloseHttpResponseForLanguageTool",
              e.getMessage()));
        }
      }
    } finally {
      try {
        httpClient.close();
      } catch (IOException e) {
        Tools.logger.warning(Tools.i18n("couldNotCloseHttpClientForLanguageTool", e.getMessage()));
      }
    }

    JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
    JsonArray jsonMatches = jsonResponse.get("matches").getAsJsonArray();
    List<LanguageToolRuleMatch> result = new ArrayList<>();

    for (JsonElement jsonElement : jsonMatches) {
      JsonObject jsonMatch = jsonElement.getAsJsonObject();
      String ruleId = jsonMatch.get("rule").getAsJsonObject().get("id").getAsString();
      String sentence = jsonMatch.get("sentence").getAsString();
      int fromPos = jsonMatch.get("offset").getAsInt();
      int toPos = fromPos + jsonMatch.get("length").getAsInt();
      String message = jsonMatch.get("message").getAsString();
      List<String> suggestedReplacements = new ArrayList<>();

      for (JsonElement replacement : jsonMatch.get("replacements").getAsJsonArray()) {
        suggestedReplacements.add(replacement.getAsJsonObject().get("value").getAsString());
      }

      result.add(new LanguageToolRuleMatch(ruleId, sentence, fromPos, toPos, message,
          suggestedReplacements));
    }

    return result;
  }

  @Override
  public void activateDefaultFalseFriendRules() {
    // handled by LanguageTool HTTP server
  }

  @Override
  public void activateLanguageModelRules(String languageModelRulesDirectory) {
    // handled by LanguageTool HTTP server
  }

  @Override
  public void activateNeuralNetworkRules(String neuralNetworkRulesDirectory) {
    // handled by LanguageTool HTTP server
  }

  @Override
  public void activateWord2VecModelRules(String word2vecRulesDirectory) {
    // handled by LanguageTool HTTP server
  }

  @Override
  public void enableRules(List<String> ruleIds) {
    this.enabledRuleIds.addAll(ruleIds);
    this.disabledRuleIds.removeAll(ruleIds);
  }

  @Override public void disableRules(List<String> ruleIds) {
    this.enabledRuleIds.removeAll(ruleIds);
    this.disabledRuleIds.addAll(ruleIds);
  }

  @Override public void enableEasterEgg() {
    // not possible with LanguageTool HTTP server
  }
}
