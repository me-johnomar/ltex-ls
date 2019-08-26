package latex;

import org.languagetool.markup.AnnotatedText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.regex.*;

public class AnnotatedTextBuilder {
  private enum Mode {
    TEXT,
    MATH,
  }

  private org.languagetool.markup.AnnotatedTextBuilder builder =
      new org.languagetool.markup.AnnotatedTextBuilder();

  private String text;
  private int pos;
  private int pseudoCounter;
  private String lastSpace;
  private String dummyLastSpace;
  private String dummyLastPunctuation;
  private boolean preserveDummyLast;
  private Stack<Mode> modeStack;

  private char curChar;
  private String curString;
  private Mode curMode;

  private static ArrayList<CommandSignature> defaultCommandSignatures = parseMagicIgnoreComments(
      "% VSCode-LT: ignoreCommand \\bibliography{}\n" +
      "% VSCode-LT: dummyCommand \\cite{}\n" +
      "% VSCode-LT: dummyCommand \\cite[]{}\n" +
      "% VSCode-LT: dummyCommand \\cref{}\n" +
      "% VSCode-LT: dummyCommand \\Cref{}\n" +
      "% VSCode-LT: ignoreCommand \\documentclass{}\n" +
      "% VSCode-LT: ignoreCommand \\documentclass[]{}\n" +
      "% VSCode-LT: dummyCommand \\eqref{}\n" +
      "% VSCode-LT: ignoreCommand \\hspace{}\n" +
      "% VSCode-LT: ignoreCommand \\hspace*{}\n" +
      "% VSCode-LT: ignoreCommand \\include{}\n" +
      "% VSCode-LT: dummyCommand \\includegraphics{}\n" +
      "% VSCode-LT: dummyCommand \\includegraphics[]{}\n" +
      "% VSCode-LT: ignoreCommand \\input{}\n" +
      "% VSCode-LT: ignoreCommand \\label{}\n" +
      "% VSCode-LT: dummyCommand \\ref{}\n" +
      "% VSCode-LT: ignoreCommand \\vspace{}\n" +
      "% VSCode-LT: ignoreCommand \\vspace*{}\n");

  private String matchFromPosition(Pattern pattern) {
    Matcher matcher = pattern.matcher(text.substring(pos));
    return (matcher.find() ? matcher.group() : "");
  }

  private String generateDummy() {
    String dummy;

    if (curMode == Mode.TEXT) {
      dummy = "Abc" + (pseudoCounter++);
    } else {
      dummy = "Abc" + (pseudoCounter++) + dummyLastPunctuation + dummyLastSpace;
    }

    dummyLastSpace = "";
    dummyLastPunctuation = "";
    return dummy;
  }

  private AnnotatedTextBuilder addText(String text) {
    if (text.isEmpty()) return this;
    builder.addText(text);
    pos += text.length();
    textAdded(text);
    return this;
  }

  private AnnotatedTextBuilder addMarkup(String markup) {
    if (markup.isEmpty()) return this;
    builder.addMarkup(markup);
    pos += markup.length();

    if (preserveDummyLast) {
      preserveDummyLast = false;
    } else {
      dummyLastSpace = "";
      dummyLastPunctuation = "";
    }

    return this;
  }

  private AnnotatedTextBuilder addMarkup(String markup, String interpretAs) {
    if (interpretAs.isEmpty()) {
      return addMarkup(markup);
    } else {
      builder.addMarkup(markup, interpretAs);
      pos += markup.length();
      preserveDummyLast = false;
      textAdded(interpretAs);
      return this;
    }
  }

  private void textAdded(String text) {
    if (text.isEmpty()) return;
    char lastChar = text.charAt(text.length() - 1);
    lastSpace = ((lastChar == ' ') ? " " : "");
  }

  private static ArrayList<CommandSignature> parseMagicIgnoreComments(String text) {
    Pattern startPattern = Pattern.compile(
        "% *VSCode-LT *: *(ignoreCommand|dummyCommand) *(\\\\([^A-Za-z]|([A-Za-z]+))\\*?)");
    Pattern argumentPattern = Pattern.compile("^((\\{\\})|(\\[\\])|(\\(\\)))");
    Matcher startMatcher = startPattern.matcher(text);
    ArrayList<CommandSignature> result = new ArrayList<CommandSignature>();

    while (startMatcher.find()) {
      CommandSignature commandSignature = new CommandSignature();

      if (startMatcher.group(1).equals("ignoreCommand")) {
        commandSignature.action = CommandSignature.Action.IGNORE;
      } else if (startMatcher.group(1).equals("dummyCommand")) {
        commandSignature.action = CommandSignature.Action.DUMMY;
      }

      commandSignature.name = startMatcher.group(2);
      int pos = startMatcher.end();

      while (true) {
        Matcher argumentMatcher = argumentPattern.matcher(text.substring(pos));
        if (!argumentMatcher.find()) break;

        CommandSignature.ArgumentType argumentType = null;

        if (argumentMatcher.group(2) != null) {
          argumentType = CommandSignature.ArgumentType.BRACE;
        } else if (argumentMatcher.group(3) != null) {
          argumentType = CommandSignature.ArgumentType.BRACKET;
        } else if (argumentMatcher.group(4) != null) {
          argumentType = CommandSignature.ArgumentType.PARENTHESIS;
        }

        commandSignature.argumentTypes.add(argumentType);
        pos += argumentMatcher.group().length();
      }

      result.add(commandSignature);
    }

    return result;
  }

  public AnnotatedTextBuilder addCode(String text) {
    ArrayList<CommandSignature> commandSignatures =
        new ArrayList<CommandSignature>(defaultCommandSignatures);
    commandSignatures.addAll(parseMagicIgnoreComments(text));

    Pattern commandPattern = Pattern.compile("^\\\\(([^A-Za-z]|([A-Za-z]+))\\*?)");
    Pattern argumentPattern = Pattern.compile("^\\{[^\\}]*?\\}");
    Pattern commentPattern = Pattern.compile("^%.*?($|(\n[ \n\r\t]*))");
    Pattern whiteSpacePattern = Pattern.compile("^[ \n\r\t]+(%.*?\n[ \n\r\t]*)?");
    Pattern lengthPattern = Pattern.compile("-?[0-9]*(\\.[0-9]+)?(pt|mm|cm|ex|em|bp|dd|pc|in)");
    Pattern lengthInBracePattern = Pattern.compile("^\\{" + lengthPattern.pattern() + "\\}");
    Pattern lengthInBracketPattern = Pattern.compile("^\\[" + lengthPattern.pattern() + "\\]");

    String[] mathEnvironments = {"equation", "equation*", "align", "align*",
        "gather", "gather*", "alignat", "alignat*", "multline", "multline*",
        "flalign", "flalign*"};

    this.text = text;
    pos = 0;
    pseudoCounter = 0;
    lastSpace = "";
    dummyLastSpace = "";
    dummyLastPunctuation = "";
    preserveDummyLast = false;

    modeStack = new Stack<Mode>();
    modeStack.push(Mode.TEXT);

    boolean canInsertSpaceBeforeDummy = false;
    boolean preserveCanInsertSpaceBeforeDummy = false;

    while (pos < text.length()) {
      curChar = text.charAt(pos);
      curString = String.valueOf(curChar);
      curMode = modeStack.peek();
      preserveCanInsertSpaceBeforeDummy = false;

      switch (curChar) {
        case '\\': {
          String command = matchFromPosition(commandPattern);

          if (command.equals("\\begin") || command.equals("\\end")) {
            preserveDummyLast = true;
            addMarkup(command);

            String argument = matchFromPosition(argumentPattern);
            String environment = argument.substring(1, argument.length() - 1);
            String interpretAs = "";

            if (Arrays.asList(mathEnvironments).contains(environment)) {
              if (command.equals("\\begin")) {
                modeStack.push(Mode.MATH);
                canInsertSpaceBeforeDummy = true;
                preserveCanInsertSpaceBeforeDummy = true;
              } else {
                modeStack.pop();
                if (modeStack.isEmpty()) modeStack.push(Mode.TEXT);
                interpretAs = generateDummy();
              }
            } else {
              if (command.equals("\\begin")) {
                modeStack.push(curMode);
              } else {
                modeStack.pop();
                if (modeStack.isEmpty()) modeStack.push(Mode.TEXT);
              }
            }

            preserveDummyLast = true;
            addMarkup(argument, interpretAs);
          } else if (command.equals("\\$") || command.equals("\\%") || command.equals("\\&")) {
            addMarkup(command, command.substring(1));
          } else if (command.equals("\\,") || command.equals("\\;") || command.equals("\\quad")) {
            if ((curMode == Mode.MATH) && (canInsertSpaceBeforeDummy)) {
              addMarkup(command, " ");
            } else {
              preserveDummyLast = true;
              addMarkup(command);
              dummyLastSpace = " ";
            }
          } else if (command.equals("\\footnote")) {
            if (lastSpace.isEmpty()) {
              addMarkup(command, " ");
              lastSpace = " ";
            } else {
              addMarkup(command);
            }
          } else if (command.equals("\\text") || command.equals("\\intertext")) {
            modeStack.push(Mode.TEXT);
            String interpretAs = ((curMode == Mode.MATH) ? generateDummy() : "");
            addMarkup(command + "{", interpretAs);
          } else {
            String match = "";
            CommandSignature matchingCommand = null;

            for (CommandSignature commandSignature : commandSignatures) {
              if (commandSignature.name.equals(command)) {
                match = commandSignature.matchFromPosition(text, pos);
                if (!match.isEmpty()) {
                  matchingCommand = commandSignature;
                  break;
                }
              }
            }

            if (matchingCommand == null) {
              addMarkup(command);
            } else {
              switch (matchingCommand.action) {
                case IGNORE: {
                  addMarkup(match);
                  break;
                }
                case DUMMY: {
                  addMarkup(match, generateDummy());
                  break;
                }
              }
            }
          }

          break;
        }
        case '{': {
          String length = matchFromPosition(lengthInBracePattern);

          if (!length.isEmpty()) {
            addMarkup(length);
          } else {
            modeStack.push(curMode);
            addMarkup(curString);
          }

          break;
        }
        case '}': {
          modeStack.pop();
          addMarkup(curString);
          canInsertSpaceBeforeDummy = true;
          preserveCanInsertSpaceBeforeDummy = true;
          break;
        }
        case '$': {
          if (curMode == Mode.TEXT) {
            modeStack.push(Mode.MATH);
            addMarkup(curString);
            canInsertSpaceBeforeDummy = true;
            preserveCanInsertSpaceBeforeDummy = true;
          } else {
            modeStack.pop();
            addMarkup(curString, generateDummy());
          }

          break;
        }
        case '%': {
          String comment = matchFromPosition(commentPattern);
          preserveDummyLast = true;
          preserveCanInsertSpaceBeforeDummy = true;
          addMarkup(comment);
          break;
        }
        case ' ':
        case '\n':
        case '\r':
        case '\t': {
          String whiteSpace = matchFromPosition(whiteSpacePattern);
          preserveDummyLast = true;
          preserveCanInsertSpaceBeforeDummy = true;

          if (curMode == Mode.TEXT) {
            if (lastSpace.isEmpty()) {
              addMarkup(whiteSpace, " ");
              lastSpace = " ";
            } else {
              addMarkup(whiteSpace);
            }
          } else {
            addMarkup(whiteSpace);
          }

          break;
        }
        case '`':
        case '\'':
        case '"': {
          if (curMode == Mode.TEXT) {
            String quote = "";
            String smartQuote = "";

            if (pos + 1 < text.length()) {
              quote = text.substring(pos, pos + 2);

              if (quote.equals("``") || quote.equals("\"'")) {
                smartQuote = "\u201c";
              } else if (quote.equals("''")) {
                smartQuote = "\u201d";
              } else if (quote.equals("\"`")) {
                smartQuote = "\u201e";
              } else {
                quote = "";
              }
            }

            if (quote.isEmpty()) addText(curString);
            else addMarkup(quote, smartQuote);
          } else {
            addMarkup(curString);
          }

          break;
        }
        case '[':
        {
          String length = matchFromPosition(lengthInBracketPattern);

          if (!length.isEmpty()) {
            addMarkup(length);
            break;
          }
        }
        default: {
          if (curMode == Mode.TEXT) {
            addText(curString);
          } else {
            addMarkup(curString);

            if ((curChar == '.') || (curChar == ',') || (curChar == ':') ||
                (curChar == ';')) {
              dummyLastPunctuation = curString;
            }
          }

          break;
        }
      }

      if (!preserveCanInsertSpaceBeforeDummy) canInsertSpaceBeforeDummy = false;
    }

    return this;
  }

  public AnnotatedText getAnnotatedText() {
    return builder.build();
  }
}
