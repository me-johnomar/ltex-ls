import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.*;

public class Settings {
  private String languageShortCode = null;
  private List<String> dictionary = null;
  private List<String> dummyCommandPrototypes = null;
  private List<String> ignoreCommandPrototypes = null;
  private String languageModelRulesDirectory = null;
  private String neuralNetworkModelRulesDirectory = null;
  private String word2VecModelRulesDirectory = null;

  private static JsonElement getSettingFromJSON(JsonElement jsonSettings, String name) {
    for (String component : name.split("\\.")) {
      jsonSettings = jsonSettings.getAsJsonObject().get(component);
    }

    return jsonSettings;
  }

  private static List<String> convertJsonArrayToList(JsonArray array) {
    List<String> result = new ArrayList<>();
    for (JsonElement element : array) result.add(element.getAsString());
    return result;
  }

  public void setSettings(JsonElement jsonSettings) {
    languageShortCode = getSettingFromJSON(jsonSettings, "ltex.language").getAsString();

    String languagePrefix = languageShortCode;
    int dashPos = languagePrefix.indexOf("-");
    if (dashPos != -1) languagePrefix = languagePrefix.substring(0, dashPos);
    dictionary = convertJsonArrayToList(
        getSettingFromJSON(jsonSettings, "ltex." + languagePrefix + ".dictionary").
        getAsJsonArray());

    dummyCommandPrototypes = convertJsonArrayToList(
        getSettingFromJSON(jsonSettings, "ltex.commands.dummy").getAsJsonArray());
    ignoreCommandPrototypes = convertJsonArrayToList(
        getSettingFromJSON(jsonSettings, "ltex.commands.ignore").getAsJsonArray());

    languageModelRulesDirectory = getSettingFromJSON(
        jsonSettings, "ltex.additionalRules.languageModel").getAsString();
    neuralNetworkModelRulesDirectory = getSettingFromJSON(
        jsonSettings, "ltex.additionalRules.neuralNetworkModel").getAsString();
    word2VecModelRulesDirectory = getSettingFromJSON(
        jsonSettings, "ltex.additionalRules.word2VecModel").getAsString();
  }

  @Override
  public Object clone() {
    Settings obj = new Settings();
    obj.languageShortCode = languageShortCode;
    obj.dictionary = ((dictionary == null) ? null : new ArrayList<>(dictionary));
    obj.dummyCommandPrototypes = ((dummyCommandPrototypes == null) ? null :
        new ArrayList<>(dummyCommandPrototypes));
    obj.ignoreCommandPrototypes = ((ignoreCommandPrototypes == null) ? null :
        new ArrayList<>(ignoreCommandPrototypes));
    obj.languageModelRulesDirectory = languageModelRulesDirectory;
    obj.neuralNetworkModelRulesDirectory = neuralNetworkModelRulesDirectory;
    obj.word2VecModelRulesDirectory = word2VecModelRulesDirectory;
    return obj;
  }

  @Override
  public boolean equals(Object obj) {
    if ((obj == null) || !Settings.class.isAssignableFrom(obj.getClass())) return false;
    Settings other = (Settings) obj;

    if ((languageShortCode == null) ? (other.languageShortCode != null) :
        !languageShortCode.equals(other.languageShortCode)) {
      return false;
    }

    if ((dictionary == null) ? (other.dictionary != null) :
        !dictionary.equals(other.dictionary)) {
      return false;
    }

    if ((dummyCommandPrototypes == null) ? (other.dummyCommandPrototypes != null) :
        !dummyCommandPrototypes.equals(other.dummyCommandPrototypes)) {
      return false;
    }

    if ((ignoreCommandPrototypes == null) ? (other.ignoreCommandPrototypes != null) :
        !ignoreCommandPrototypes.equals(other.ignoreCommandPrototypes)) {
      return false;
    }

    if ((languageModelRulesDirectory == null) ? (other.languageModelRulesDirectory != null) :
        !languageModelRulesDirectory.equals(other.languageModelRulesDirectory)) {
      return false;
    }

    if ((neuralNetworkModelRulesDirectory == null) ?
        (other.neuralNetworkModelRulesDirectory != null) :
        !neuralNetworkModelRulesDirectory.equals(other.neuralNetworkModelRulesDirectory)) {
      return false;
    }

    if ((word2VecModelRulesDirectory == null) ? (other.word2VecModelRulesDirectory != null) :
        !word2VecModelRulesDirectory.equals(other.word2VecModelRulesDirectory)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hash = 3;
    hash = 53 * hash + ((languageShortCode != null) ? languageShortCode.hashCode() : 0);
    hash = 53 * hash + ((dictionary != null) ? dictionary.hashCode() : 0);
    hash = 53 * hash + ((dummyCommandPrototypes != null) ? dummyCommandPrototypes.hashCode() : 0);
    hash = 53 * hash + ((ignoreCommandPrototypes != null) ? ignoreCommandPrototypes.hashCode() : 0);
    hash = 53 * hash + ((languageModelRulesDirectory != null) ?
        languageModelRulesDirectory.hashCode() : 0);
    hash = 53 * hash + ((neuralNetworkModelRulesDirectory != null) ?
        neuralNetworkModelRulesDirectory.hashCode() : 0);
    hash = 53 * hash + ((word2VecModelRulesDirectory != null) ?
        word2VecModelRulesDirectory.hashCode() : 0);
    return hash;
  }

  private static <T> T getDefault(T obj, T default_) {
    return ((obj != null) ? obj : default_);
  }

  public String getLanguageShortCode() {
    return getDefault(languageShortCode, "en-US");
  }

  public List<String> getDictionary() {
    return getDefault(dictionary, Collections.emptyList());
  }

  public List<String> getDummyCommandPrototypes() {
    return getDefault(dummyCommandPrototypes, Collections.emptyList());
  }

  public List<String> getIgnoreCommandPrototypes() {
    return getDefault(ignoreCommandPrototypes, Collections.emptyList());
  }

  public String getLanguageModelRulesDirectory() {
    return getDefault(languageModelRulesDirectory, "");
  }

  public String getNeuralNetworkModelRulesDirectory() {
    return getDefault(neuralNetworkModelRulesDirectory, "");
  }

  public String getWord2VecModelRulesDirectory() {
    return getDefault(word2VecModelRulesDirectory, "");
  }
}
