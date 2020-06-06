package org.bsplines.ltexls.parsing.latex;

import java.util.*;
import java.util.regex.*;

import org.bsplines.ltexls.Tools;
import org.bsplines.ltexls.parsing.DummyGenerator;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.eclipse.xtext.xbase.lib.Pair;

public class LatexCommandSignature {
  public enum ArgumentType {
    BRACE,
    BRACKET,
    PARENTHESIS,
  }

  public enum Action {
    IGNORE,
    DUMMY,
  }

  private static final Pattern commandPattern = Pattern.compile(
      "^\\\\([^A-Za-z@]|([A-Za-z@]+))\\*?");
  private static final Pattern argumentPattern = Pattern.compile("^((\\{\\})|(\\[\\])|(\\(\\)))");
  private static final Pattern commentPattern = Pattern.compile("^%.*?($|(\n[ \n\r\t]*))");

  public String name = "";
  public ArrayList<ArgumentType> argumentTypes = new ArrayList<ArgumentType>();
  public Action action = Action.IGNORE;
  public DummyGenerator dummyGenerator;

  private Pattern thisCommandPattern;

  public LatexCommandSignature(String commandPrototype) {
    this(commandPrototype, Action.IGNORE, DummyGenerator.getDefault());
  }

  public LatexCommandSignature(String commandPrototype, Action action) {
    this(commandPrototype, action, DummyGenerator.getDefault());
  }

  public LatexCommandSignature(String commandPrototype, Action action,
        DummyGenerator dummyGenerator) {
    this.dummyGenerator = dummyGenerator;
    Matcher commandMatcher = commandPattern.matcher(commandPrototype);

    if (!commandMatcher.find()) {
      Tools.logger.warning(Tools.i18n("invalidCommandPrototype", commandPrototype));
      this.thisCommandPattern = Pattern.compile(" ^$");
      return;
    }

    this.name = commandMatcher.group();
    int pos = commandMatcher.end();

    while (true) {
      Matcher argumentMatcher = argumentPattern.matcher(commandPrototype.substring(pos));
      if (!argumentMatcher.find()) break;

      LatexCommandSignature.ArgumentType argumentType;

      if (argumentMatcher.group(2) != null) {
        argumentType = LatexCommandSignature.ArgumentType.BRACE;
      } else if (argumentMatcher.group(3) != null) {
        argumentType = LatexCommandSignature.ArgumentType.BRACKET;
      } else if (argumentMatcher.group(4) != null) {
        argumentType = LatexCommandSignature.ArgumentType.PARENTHESIS;
      } else {
        argumentType = LatexCommandSignature.ArgumentType.BRACE;
      }

      argumentTypes.add(argumentType);
      pos += argumentMatcher.group().length();
      assert argumentMatcher.group().length() > 0;
    }

    this.action = action;
    this.thisCommandPattern = Pattern.compile("^" + Pattern.quote(name));
  }

  private static String matchPatternFromPosition(String code, int fromPos, Pattern pattern) {
    Matcher matcher = pattern.matcher(code.substring(fromPos));
    return (matcher.find() ? matcher.group() : "");
  }

  public static String matchArgumentFromPosition(
        String code, int fromPos, ArgumentType argumentType) {
    int pos = fromPos;
    Stack<ArgumentType> argumentTypeStack = new Stack<>();
    char openChar = '\0';

    switch (argumentType) {
      case BRACE: {
        openChar = '{';
        break;
      }
      case BRACKET: {
        openChar = '[';
        break;
      }
      case PARENTHESIS: {
        openChar = '(';
        break;
      }
    }

    if (code.charAt(pos) != openChar) return "";
    pos++;
    argumentTypeStack.push(argumentType);

    while (pos < code.length()) {
      switch (code.charAt(pos)) {
        case '\\': {
          if (pos + 1 < code.length()) pos++;
          break;
        }
        case '{': {
          argumentTypeStack.push(ArgumentType.BRACE);
          break;
        }
        case '[': {
          argumentTypeStack.push(ArgumentType.BRACKET);
          break;
        }
        case '}': {
          if (argumentTypeStack.peek() != ArgumentType.BRACE) {
            return "";
          } else if (argumentTypeStack.size() == 1) {
            return code.substring(fromPos, pos + 1);
          } else {
            argumentTypeStack.pop();
          }

          break;
        }
        case ']': {
          if (argumentTypeStack.peek() != ArgumentType.BRACKET) {
            return "";
          } else if (argumentTypeStack.size() == 1) {
            return code.substring(fromPos, pos + 1);
          } else {
            argumentTypeStack.pop();
          }

          break;
        }
        case ')': {
          if ((argumentTypeStack.peek() == ArgumentType.PARENTHESIS) &&
                (argumentTypeStack.size() == 1)) {
            return code.substring(fromPos, pos + 1);
          }

          break;
        }
      }

      pos++;
    }

    return "";
  }

  public @Nullable List<Pair<Integer, Integer>> matchArgumentsFromPosition(
        String code, int fromPos) {
    List<Pair<Integer, Integer>> arguments = new ArrayList<>();
    int toPos = matchFromPosition(code, fromPos, arguments);
    return ((toPos > -1) ? arguments : null);
  }

  public String matchFromPosition(String code, int fromPos) {
    int toPos = matchFromPosition(code, fromPos, null);
    return ((toPos > -1) ? code.substring(fromPos, toPos) : "");
  }

  private int matchFromPosition(String code, int fromPos,
        @Nullable List<Pair<Integer, Integer>> arguments) {
    int pos = fromPos;
    String match = matchPatternFromPosition(code, pos, thisCommandPattern);
    pos += match.length();

    for (ArgumentType argumentType : argumentTypes) {
      match = matchPatternFromPosition(code, pos, commentPattern);
      pos += match.length();

      match = matchArgumentFromPosition(code, pos, argumentType);
      if (match.isEmpty()) return -1;
      if (arguments != null) arguments.add(new Pair<>(pos, pos + match.length()));
      pos += match.length();
    }

    return pos;
  }
}
