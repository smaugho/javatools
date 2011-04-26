package javatools.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javatools.administrative.D;
import javatools.datatypes.FinalMap;
import javatools.filehandlers.FileLines;
import javatools.parsers.Char;


/**
 * This class is part of the Java Tools (see
 * http://mpii.de/yago-naga/javatools). It is licensed under the Creative
 * Commons Attribution License (see http://creativecommons.org/licenses/by/3.0)
 * by the YAGO-NAGA team (see http://mpii.de/yago-naga).
 * 
 * 
 * The class Name represents a name. There are three sub-types (subclasses) of
 * names: Abbreviations, person names and company names. These subclasses
 * provide methods to access the components of the name (like the family name).
 * Use the factory method Name.of to create a Name-object of the appropriate
 * subclass.<BR>
 * Example:
 * 
 * <PRE>
 * Name.isName("Mouse");
 *   --> true
 *   Name.isAbbreviation("PMM");
 *   --> true  
 *   Name.isPerson("Mickey Mouse");
 *   --> false
 *   Name.couldBePerson("Mickey Mouse");
 *   --> true
 *   Name.isPerson("Prof. Mickey Mouse");
 *   --> true
 *   Name.of("Prof. Dr. Fabian the Great III of Saarbruecken").describe()
 *   // equivalent to new PersonName(...) in this case
 *   -->
 *   PersonName
 *     Original: Prof. Dr. Fabian the Great III of Saarbruecken
 *     Titles: Prof. Dr.
 *     Given Name: Fabian
 *     Given Names: Fabian
 *     Family Name Prefix: null
 *     Attribute Prefix: the
 *     Family Name: null
 *     Attribute: Great
 *     Family Name Suffix: null
 *     Roman: III
 *     City: Saarbruecken
 *     Normalized: Fabian_Great
 * </PRE>
 */

public class NameML {

  static final File CONFIG_DIR = new File("data/parser");

  // FIXME: this should perhaps be obtained from the configuration file

  /** Holds the general default name */
  public static final String ANYNAME = "NAME";

  // -----------------------------------------------------------------------------------
  // Punctation
  // -----------------------------------------------------------------------------------

  /** Contains romam digits */
  public static String roman = "\\b(?:[XIV]++)\\b";

  /** Contains the English "of" */
  public static String of = "\\bof\\b";

  /** Contains upper case Characters */
  public static final String U = "\\p{Lu}";

  /** Contains lower case Characters */
  public static final String L = "\\p{Ll}";

  /** Contains characters */
  public static final String A = "\\p{L}";

  /** Contains blank */
  public static final String B = "(?:[\\s_]++)";

  /** Contains a word boundary */
  public static final String BD = "\\b";

  /** Contains blank with optional comma */
  public static final String BC = "[,\\s_]++";

  /** Contains digits */
  public static final String DG = "\\d";

  /** Contains hypens */
  public static final String H = "-";

  /** Contains "|" */
  public static final String or = "|";

  /** Repeats the token with blanks one or more times */
  public static String mul(String s) {
    return ("(?:" + s + B + ")*" + s);
  }

  /** Repeats the token with hyphens one or more times */
  public static String mulHyp(String s) {
    return ("(?:" + s + H + ")*" + s);
  }

  /** optional component */
  public static String opt(String s) {
    return ("(?:" + s + ")?");
  }

  /** optional multiple component */
  public static String optMul(String s) {
    return ("(?:" + s + ")*");
  }

  /** alternavive */
  public static String or(String s1, String s2) {
    return ("(?:" + s1 + "|" + s2 + ")");
  }

  /** Capturing group */
  public static String c(String s) {
    return ("(" + s + ")");
  }

  // -----------------------------------------------------------------------------------
  // Family Name Prefixes
  // -----------------------------------------------------------------------------------

  /** Contains common family name prefixes (like "von") */
  public static final String familyNamePrefix = "(?:"
      + "[aA]l|[dD][ea]|[dD]el|[dD]e las|[bB]in|[dD]e la|[dD]e los|[dD]i|[zZ]u[mr]|[aA]m|[vV][oa]n de[rnm]|[vV][oa][nm]|[dD]o|[dD]')";

  public static final Pattern familyNamePrefixPattern = Pattern.compile(familyNamePrefix);

  /** Says whether this String is a family name prefix */
  public static boolean isFamilyNamePrefix(String s) {
    return (familyNamePrefixPattern.matcher(s).matches());
  }

  // -----------------------------------------------------------------------------------
  // Attribute Prefixes
  // -----------------------------------------------------------------------------------

  /** Contains attribute Prefixes (like "the" in "Alexander the Great") */
  public static String attributePrefix = "(?:" + "the|der|die|il|la|le)";

  public static Pattern attributePrefixPattern = Pattern.compile(attributePrefix);

  /**
   * Says whether this String is an attribute Prefix (like "the" in
   * "Alexander the Great")
   */
  public static boolean isAttributePrefix(String s) {
    return (s.matches(attributePrefix));
  }

  // -----------------------------------------------------------------------------------
  // Attribute Suffixes
  // -----------------------------------------------------------------------------------

  /** Contains common name suffixes (like "Junior") */
  public static final String familyNameSuffix = "(?:" + "CBE|" + // Commander
      "DBE|" + // Knight or Dame Commander
      "GBE|" + // Knight or Dame Grand Cross
      "[jJ]r\\.?|" + "[jJ]unior|" + "hijo|" + "hija|" + "P[hH]\\.?[dD]\\.?|" + "KBE|" + // Knight
      // or
      // Dame
      // Commander
      "MBE|" + // Member
      "M\\.?D\\.|" + "OBE|" + // Officer
      "[sS]enior|" + "[sS]r\\.?)";

  public static final Pattern familyNameSuffixPattern = Pattern.compile(familyNameSuffix);

  /** Says whether this String is a person name suffix */
  public static boolean isPersonNameSuffix(String s) {
    return (familyNameSuffixPattern.matcher(s).matches());
  }

  // -----------------------------------------------------------------------------------
  // Titles
  // -----------------------------------------------------------------------------------

  /** Matches common titles (like "Mr.") */
  public static final Pattern titlePatternEn = createTitlePattern(Language.ENGLISH);

  public static final Pattern titlePatternDe = createTitlePattern(Language.GERMAN);

  public static final Pattern titlePatternFr = createTitlePattern(Language.FRENCH);

  public static final Pattern titlePatternEs = createTitlePattern(Language.SPANISH);

  public static final Pattern titlePatternIt = createTitlePattern(Language.ITALIAN);

  // public static final String titles="(?:"+title+B+")*"+B+title;

  private static Pattern createTitlePattern(Language lang) {
    StringBuilder titleRegExp = new StringBuilder();
    titleRegExp.append("\\b(?:");
    List<String> titles;
    boolean first=true;
    try {
      titles = NameML.readTextFileLines(new File(CONFIG_DIR, "titles." + lang.getId()), "UTF-8");
      
      for (String title : titles) {
        title = title.trim();
        if (!title.startsWith("##") && title.length() > 0) {
          if(first)
            first=false;
          else
            titleRegExp.append('|');
          titleRegExp.append(title);         
        }
      }
      titleRegExp.append(")");
      return Pattern.compile(titleRegExp.toString());
    } catch (IOException e) {
      return null;
    }
  }

  /** Says whether this String is a title */
  public static boolean isTitle(String s, Language lang) {
    if (lang.equals(Language.ENGLISH)) return (titlePatternEn.matcher(s).matches());
    else if (lang.equals(Language.FRENCH)) return (titlePatternFr.matcher(s).matches());
    else if (lang.equals(Language.GERMAN)) return (titlePatternDe.matcher(s).matches());
    else if (lang.equals(Language.SPANISH)) return (titlePatternEs.matcher(s).matches());
    else if (lang.equals(Language.ITALIAN)) return (titlePatternIt.matcher(s).matches());
    else throw new IllegalArgumentException("Unsupported Language");
  }

  /**
   * Contains those titles that go with the given name (e.g. "Queen" in
   * "Queen Elisabeth"), lowercase
   */
  protected static Set<String> titlesForGivenNamesEn = NameML.readTextFileLinesSet(new File(CONFIG_DIR, "titles." + Language.ENGLISH.getId()));

  protected static Set<String> titlesForGivenNamesDe = NameML.readTextFileLinesSet(new File(CONFIG_DIR, "titles." + Language.GERMAN.getId()));

  protected static Set<String> titlesForGivenNamesEs = NameML.readTextFileLinesSet(new File(CONFIG_DIR, "titles." + Language.SPANISH.getId()));

  protected static Set<String> titlesForGivenNamesFr = NameML.readTextFileLinesSet(new File(CONFIG_DIR, "titles." + Language.FRENCH.getId()));

  protected static Set<String> titlesForGivenNamesIt = NameML.readTextFileLinesSet(new File(CONFIG_DIR, "titles." + Language.ITALIAN.getId()));
  
  protected static Set<String> stopWordDE = NameML.readTextFileLinesSet(new File(CONFIG_DIR, "stopwords." + Language.GERMAN.getId()));

  protected static Set<String> stopWordFR = NameML.readTextFileLinesSet(new File(CONFIG_DIR, "stopwords." + Language.FRENCH.getId()));

  protected static Set<String> stopWordES = NameML.readTextFileLinesSet(new File(CONFIG_DIR, "stopwords." + Language.SPANISH.getId()));

  protected static Set<String> stopWordEN = NameML.readTextFileLinesSet(new File(CONFIG_DIR, "stopwords." + Language.ENGLISH.getId()));

  protected static Set<String> stopWordIT = NameML.readTextFileLinesSet(new File(CONFIG_DIR, "stopwords." + Language.ITALIAN.getId()));

  // -----------------------------------------------------------------------------------
  // Company Name Suffixes
  // -----------------------------------------------------------------------------------

  /** Contains common company name suffixes (like "Inc") */
  public static final String companyNameSuffix = "(?:" + "[cC][oO]\\.|" + "[cC][oO]\\b|" + "&" + B + "?[cC][oO]\\.|" + "&" + B + "?[cC][oO]\\b|"
      + "\\b[cC][oO][rR][pP]\\.|" + "\\b[cC][oO][rR][pP]\\b|" + "\\bR[cC]orporation\\b|" + "\\b[iI][nN][cC]\\.|" + "\\b[iI][nN][cC]\\b|"
      + "\\b[iI]ncorporated\\b|" + "\\b[iI]ncorporation\\b|" + "\\b[iI]ncorp\\.?|" + "\\b[iI]ncorp\\b|" + "\\b[lL][tT][dD]\\.|"
      + "\\b[lL][tT][dD]\\b|" + "\\b[lL]imited\\b|" + "\\bp\\.l\\.c\\.\\b|" + "\\bPty\\.\\b|" + "\\bLLC\\b|" + "\\bAG\\b|" + "\\bGmbH\\b|"
      + "\\bKG\\b|" + "\\bOHG\\b|" // German ones 
      + "\\bS\\.R\\.L\\.\\b|" + "\\bS\\.p\\.A\\.\\b|" + "\\bS\\.A\\.\\b)"; // French, Spanish

  // see also: http://en.wikipedia.org/wiki/Types_of_business_entity

  public static final Pattern companyNameSuffixPattern = Pattern.compile(companyNameSuffix);

  /** Says whether this String is a company name suffix */
  public static boolean isCompanyNameSuffix(String s) {
    return (companyNameSuffixPattern.matcher(s).matches());
  }

  // -----------------------------------------------------------------------------------
  // Names
  // -----------------------------------------------------------------------------------

  /** team name */
  public static final String teamName = BD + U + "[\\w\\s\\.]+" + BD;

  public static final Pattern teamNamePattern = Pattern.compile(teamName);

  /** Contains prepositions */
  public static final String prep = "(?:on|of|for)";

  /**
   * Contains the pattern for names. Practically everything is a name if it
   * starts with an uppercase letter
   */
  public static final String laxName = BD + U + ".*" + BD;

  /**
   * Contains the pattern for names. Practically everything is a name if it
   * starts with an uppercase letter
   */
  public static final Pattern laxNamePattern = Pattern.compile(laxName);

  /**
   * Contains a pattern that indicates strings that are very likely to be
   * names
   */
  public static final String safeName = BD + U + "(" + H + "[" + U + DG + "]|[" + U + L + DG + "]){2,}" + BD;

  /**
   * Contains a pattern that indicates strings that are very likely to be
   * names
   */
  public static final Pattern safeNamePattern = Pattern.compile(safeName);

  /**
   * Contains a pattern that indicates strings that are very likely to be
   * names
   */
  public static final Pattern safeNamesPattern = Pattern.compile(safeName + optMul(B + opt(prep + B) + safeName));

  /**
   * Contains a pattern that indicates strings that are very likely to be
   * names
   */
  public static final Pattern safeNamesPatternNoPrep = Pattern.compile(safeName + optMul(B + safeName));

  /** Tells whether a String is a name with high probability */
  public static boolean isName(String s) {
    return (safeNamePattern.matcher(s).matches());
  }

  /** Tells whether a String is a sequence of names with high probability */
  public static boolean isNames(String s) {
    return (safeNamesPattern.matcher(s).matches());
  }

  /** Tells whether a String could possibly be a name */
  public static boolean couldBeName(String s) {
    return (laxNamePattern.matcher(s).matches());
  }

  /** TRUE for stopwords */
  public static boolean isStopWord(String w, Language l) {
    if (l == Language.ENGLISH) {
      return stopWordEN.contains(w);
    } else if (l == Language.FRENCH) {
      return stopWordFR.contains(w);

    } else if (l == Language.GERMAN) {
      return stopWordDE.contains(w);

    } else if (l == Language.ITALIAN) {
      return stopWordIT.contains(w);

    } else if (l == Language.SPANISH) {
      return stopWordES.contains(w);

    }
    return stopWordEN.contains(w);
  }

  // -----------------------------------------------------------------------------------
  // Names: Members
  // -----------------------------------------------------------------------------------

  /** Holds the original name */
  protected String original;

  /** Holds the normalized name */
  protected String normalized;

  /** Returns the original name */
  public String toString() {
    return (original);
  }

  /**
   * Returns the letters and digits of the original name (eliminates
   * punctuation)
   */
  public String normalize() {
    if (normalized == null) normalized = original.replaceAll(B, "_").replaceAll("([\\P{L}&&[^\\d]&&[^_]])", "");
    return (normalized);
  }

  /** Constructor (for subclasses only; use Name.of instead!) */
  protected NameML(String s) {
    original = s;
  }

  /** Returns a description */
  public String describe() {
    return ("Name\n" + "  Original: " + original + "\n" + "  Normalized: " + normalize());
  }

  /** Returns the original name */
  public String original() {
    return (original);
  }

  // -----------------------------------------------------------------------------------
  // Abbreviations
  // -----------------------------------------------------------------------------------

  /**
   * Read an entire text file as a list of strings. The strings do not include
   * a terminating new line character.
   * 
   * @param inputFile
   *            text file to open
   * @param encoding
   *            character encoding name (as used by Java) or null for UTF-8
   * @return contents of file
   * @throws IOException
   */
  public static List<String> readTextFileLines(File inputFile, String encoding) throws IOException {
    if (encoding == null) encoding = "UTF-8";
    BufferedReader fi = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), encoding));
    List<String> lines = new ArrayList<String>();
    String line = null;
    while ((line = fi.readLine()) != null) {
      lines.add(line);
    }
    fi.close();
    return lines;
  }

  /**
   * Read set from an UTF-8 text file, ignoring lines starting with "##"
   * 
   * @param inputFile
   * @return set of strings
   */
  public static Set<String> readTextFileLinesSet(File inputFile) {
    List<String> lines;
    try {
      lines = NameML.readTextFileLines(inputFile, "UTF-8");
      // a bit slow to do it this way
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    Set<String> set = new HashSet<String>((int) (lines.size() / 0.75));
    for (String item : lines) {
      item = item.trim();
      if (!item.startsWith("##") && item.length() > 0) set.add(item);
    }
    return set;
  }

  /** Contains the lax pattern for abbreviations */
  public static final Pattern laxAbbreviationPattern = Pattern.compile(BD + U + "[" + U + DG + B + H + "]++" + BD);

  /** Contains the safe pattern for abbreviations */
  public static final Pattern safeAbbreviationPattern = Pattern.compile(BD + U + "[" + U + DG + H + "\\.]++" + BD);

  /** Tells whether a string is an abbreviation with high probability */
  public static boolean isAbbreviation(String word) {
    return (safeAbbreviationPattern.matcher(word).matches());
  }

  /** Tells whether a string could be abbreviation. */
  public static boolean couldBeAbbreviation(String word) {
    return (laxAbbreviationPattern.matcher(word).matches());
  }

  public static class AbbreviationML extends NameML {

    public AbbreviationML(String s) {
      super(s);
      if (!laxAbbreviationPattern.matcher(s).matches()) return;
    }

    public String normalize() {
      if (normalized == null) normalized = super.normalize().toUpperCase();
      return (normalized);
    }

    /** Returns a description */
    public String describe() {
      return ("Abbreviation\n" + "  Original: " + original + "\n" + "  Normalized: " + normalize());
    }

  }

  // -----------------------------------------------------------------------------------
  // Company Names
  // -----------------------------------------------------------------------------------

  /** Contains the pattern for companies */
  public static final Pattern laxCompanyPattern = Pattern.compile("(" + laxNamePattern + ")" + BC + "(" + companyNameSuffix + ")");

  /** Contains the safe pattern for companies */
  public static final Pattern safeCompanyPattern = Pattern.compile("(" + safeNamesPatternNoPrep + opt(opt(B) + "&" + opt(B) + safeNamesPatternNoPrep)
      + ")" + BC + "(" + companyNameSuffix + ")");

  /** Tells if the string is a company name with high probability */
  public static boolean isCompanyName(String s) {
    return (safeCompanyPattern.matcher(s).matches());
  }

  /** Tells if the string could be a company name */
  public static boolean couldBeCompanyName(String s) {
    return (laxCompanyPattern.matcher(s).matches());
  }

  public static class CompanyNameML extends NameML {

    protected String name;

    protected String suffix;

    public CompanyNameML(String s) {
      super(s);
      Matcher m = laxCompanyPattern.matcher(s);
      if (!m.matches()) return;
      name = m.group(1);
      suffix = m.group(2);
    }

    /** Returns the name. */
    public String name() {
      return name;
    }

    /** Returns the suffix. */
    public String suffix() {
      return suffix;
    }

    public String normalize() {
      return (name);
    }

    /** Returns a description */
    public String describe() {
      return ("CompanyName\n" + "  Original: " + original + "\n" + "  Name: " + name + "\n" + "  Suffix: " + suffix + "\n" + "  Normalized: " + normalize());
    }
  }

  // -----------------------------------------------------------------------------------
  // Person Names
  // -----------------------------------------------------------------------------------

  /** A direct family name prefix (such as "Mc") */
  public static final String directFamilyNamePrefix = BD + "(?:(?:al-|Mc|Di|De|Mac|O')" + B + "?)";

  /** The pattern "Name" */
  public static final String personNameComponent = U + L + "+";

  /** The pattern "Name." */
  public static final String givenNameComponent = or(or(personNameComponent + BD, U + L + "*+\\."), U + BD);

  /** The pattern "Name[-Name]" */
  public static final String givenName = BD + mulHyp(givenNameComponent);

  /** The pattern (personNameComponent+B)+ */
  public static final String givenNames = mul(givenName);

  /** Name component with an optional familyNamePrefix and postfix */
  public static final String familyName = BD + mulHyp(opt(directFamilyNamePrefix) + personNameComponent) + BD;

  /** Nickname '...' */
  public static final String nickName = "(?:'[^']')";

  /**
   * @param titlePattern
   * @return pattern for strings that could be person names
   */
  private static Pattern createLaxPersonNamePattern(Pattern titlePattern) {
    return Pattern.compile(c(optMul(titlePattern.pattern() + B)) + c(optMul(givenName + B)) + opt(c(nickName) + B) + opt(c(attributePrefix) + B)
        + opt(c(familyNamePrefix) + B) + c(familyName) + opt(BC + c(familyNameSuffix)) + opt(B + c(roman)) + opt(B + of + B + c(personNameComponent))
        + opt(B + c(nickName)));
  }

  public static final Pattern laxPersonNamePatternEn = createLaxPersonNamePattern(titlePatternEn);

  public static final Pattern laxPersonNamePatternDe = createLaxPersonNamePattern(titlePatternDe);

  public static final Pattern laxPersonNamePatternEs = createLaxPersonNamePattern(titlePatternEs);

  public static final Pattern laxPersonNamePatternFr = createLaxPersonNamePattern(titlePatternFr);

  public static final Pattern laxPersonNamePatternIt = createLaxPersonNamePattern(titlePatternIt);

  /**
   * @param titlePattern
   * @return pattern for strings that are person names with high probability
   */
  private static Pattern createSafePersonNamePattern(Pattern titlePattern) {
    return Pattern.compile(
    // Mr. Bob Carl Miller
        titlePattern.pattern() + B + givenNames + B + opt(familyNamePrefix + B) + familyName + opt(BC + familyNameSuffix) + or +
        // Mr. Miller
            titlePattern.pattern() + B + opt(familyNamePrefix + B) + familyName + opt(BC + familyNameSuffix) + or +
            // Bob XI
            givenName + B + roman + or +
            // Bob Miller Jr.
            givenNames + B + opt(familyNamePrefix + B) + familyName + BC + familyNameSuffix + or +
            // Miller Jr.
            opt(familyNamePrefix + B) + familyName + BC + familyNameSuffix + or +
            // George W. Bush
            givenName + B + U + "\\." + B + opt(familyNamePrefix + B) + familyName + opt(BC + familyNameSuffix) + or +
            // George H. W. Bush
            givenName + B + U + "\\." + B + U + "\\." + B + opt(familyNamePrefix + B) + familyName + opt(BC + familyNameSuffix));
  }

  public static final Pattern safePersonNamePatternEn = createSafePersonNamePattern(titlePatternEn);

  public static final Pattern safePersonNamePatternDe = createSafePersonNamePattern(titlePatternDe);

  public static final Pattern safePersonNamePatternEs = createSafePersonNamePattern(titlePatternEs);

  public static final Pattern safePersonNamePatternFr = createSafePersonNamePattern(titlePatternFr);

  public static final Pattern safePersonNamePatternIt = createSafePersonNamePattern(titlePatternIt);

  /** Returns true if it is possible that the string is a person name */
  public static boolean couldBePersonName(String s, Language lang) {
    if (isCompanyName(s)) return (false);
    if (lang.equals(Language.ENGLISH)) return (laxPersonNamePatternEn.matcher(s).matches());
    else if (lang.equals(Language.GERMAN)) return (laxPersonNamePatternDe.matcher(s).matches());
    else if (lang.equals(Language.SPANISH)) return (laxPersonNamePatternEs.matcher(s).matches());
    else if (lang.equals(Language.FRENCH)) return (laxPersonNamePatternFr.matcher(s).matches());
    else if (lang.equals(Language.ITALIAN)) return (laxPersonNamePatternIt.matcher(s).matches());
    else throw new IllegalArgumentException("Unsupported language.");
  }

  /** Returns true if it is highly probable that the string is a person name. */
  public static boolean isPersonName(String m, Language lang) {
    if (lang.equals(Language.ENGLISH)) return (safePersonNamePatternEn.matcher(m).matches());
    else if (lang.equals(Language.GERMAN)) return (safePersonNamePatternDe.matcher(m).matches());
    else if (lang.equals(Language.SPANISH)) return (safePersonNamePatternEs.matcher(m).matches());
    else if (lang.equals(Language.FRENCH)) return (safePersonNamePatternFr.matcher(m).matches());
    else if (lang.equals(Language.ITALIAN)) return (safePersonNamePatternIt.matcher(m).matches());
    else throw new IllegalArgumentException("Unsupported language.");
  }

  public static class PersonNameML extends NameML {

    protected String myTitles;

    protected String myGivenNames;

    protected String myFamilyNamePrefix;

    protected String myAttributePrefix;

    protected String myFamilyName;

    protected String myAttribute;

    protected String myFamilyNameSuffix;

    protected String myRoman;

    protected String myCity;

    protected String myNickname;

    /** Returns the n-th group or null */
    protected static String getComponent(Matcher m, int n) {
      if (m.group(n) == null || m.group(n).length() == 0) return (null);
      String result = m.group(n);
      if (result.matches(".+" + B)) return (result.substring(0, result.length() - 1));
      if (result.matches(B + ".+")) return (result.substring(1));
      return (result);
    }

    /** Constructs a person name from a String */
    public PersonNameML(String s, Language lang) {
      super(s);
      s = s.replace('_', ' ');
      Matcher m;
      if (lang.equals(Language.ENGLISH)) m = laxPersonNamePatternEn.matcher(s);
      else if (lang.equals(Language.FRENCH)) m = laxPersonNamePatternFr.matcher(s);
      else if (lang.equals(Language.SPANISH)) m = laxPersonNamePatternEs.matcher(s);
      else if (lang.equals(Language.GERMAN)) m = laxPersonNamePatternDe.matcher(s);
      else if (lang.equals(Language.ITALIAN)) m = laxPersonNamePatternIt.matcher(s);
      else throw new IllegalArgumentException("Unsupported language");
      if (!m.matches()) return;
      myTitles = getComponent(m, 1);
      myGivenNames = getComponent(m, 2);
      myNickname = getComponent(m, 3);
      myFamilyName = getComponent(m, 6);
      myFamilyNamePrefix = getComponent(m, 5);
      String attr = getComponent(m, 4);
      if (attr != null) {
        myAttributePrefix = attr;
        myAttribute = myFamilyName;
        myFamilyName = null;
      }
      myFamilyNameSuffix = getComponent(m, 7);
      myRoman = getComponent(m, 8);
      myCity = getComponent(m, 9);
      if (myNickname == null) myNickname = getComponent(m, 10);
      // Postprocessing: If the title applies to given names, make the
      // family name the given name
      Set<String> titlesForGivenNames;
      if (lang.equals(Language.ENGLISH)) titlesForGivenNames = titlesForGivenNamesEn;
      else if (lang.equals(Language.FRENCH)) titlesForGivenNames = titlesForGivenNamesFr;
      else if (lang.equals(Language.SPANISH)) titlesForGivenNames = titlesForGivenNamesEs;
      else if (lang.equals(Language.GERMAN)) titlesForGivenNames = titlesForGivenNamesDe;
      else if (lang.equals(Language.ITALIAN)) titlesForGivenNames = titlesForGivenNamesIt;
      else throw new IllegalArgumentException("Unsupported language");
      if (myGivenNames == null && myTitles != null && titlesForGivenNames.contains(myTitles.toLowerCase())) {
        myGivenNames = myFamilyName;
        myFamilyName = null;
      }
      // Postprocessing: If we have no given name and a roman number,
      // familyname is given name
      if (myGivenNames == null && myRoman != null) {
        myGivenNames = myFamilyName;
        myFamilyName = null;
      }
      // Postprocessing: if familyname is a familynamesuffix, rearrange
      if (myFamilyName != null && myGivenNames != null && familyNameSuffixPattern.matcher(myFamilyName).matches()) {
        String[] g = myGivenNames.split(B);
        myFamilyNameSuffix = myFamilyName;
        myFamilyName = g[g.length - 1];
        myGivenNames = g.length == 1 ? null : myGivenNames.substring(0, myGivenNames.length() - myFamilyName.length());
      }
    }

    /** Returns the first given name or null */
    public String givenName() {
      if (myGivenNames == null) return (null);
      if (myGivenNames.indexOf(' ') == -1) return (myGivenNames);
      return (myGivenNames.substring(0, myGivenNames.indexOf(' ')));
    }

    /** Returns the attribute. */
    public String attribute() {
      return myAttribute;
    }

    /** Returns the attributePrefix. */
    public String attributePrefix() {
      return myAttributePrefix;
    }

    /** Returns the city. */
    public String city() {
      return myCity;
    }

    /** Returns the nickname. */
    public String nickname() {
      return myNickname;
    }

    /** Returns the familyName. */
    public String familyName() {
      return myFamilyName;
    }

    /** Returns the familyNamePrefix. */
    public String familyNamePrefix() {
      return myFamilyNamePrefix;
    }

    /** Returns the familyNameSuffix. */
    public String familyNameSuffix() {
      return myFamilyNameSuffix;
    }

    /** Returns the givenNames. */
    public String givenNames() {
      return myGivenNames;
    }

    /** Returns the roman number. */
    public String roman() {
      return myRoman;
    }

    /** Returns the titles. */
    public String titles() {
      return myTitles;
    }

    /** Normalizes a person name. */
    public String normalize() {
      String given = givenNames();

      // Try the family name
      if (myFamilyName != null) {
        String family = myFamilyName;
        if (myFamilyNameSuffix != null && myFamilyNameSuffix.matches("[jJ].*")) family += ", Jr.";
        else if (myFamilyNameSuffix != null && myFamilyNameSuffix.matches("[sS].*")) family += ", Sr.";
        if (given != null) family = given + ' ' + family;
        return (family);
      }

      // Try the given name
      if (given != null) {
        if (myRoman != null && given != null) given += ' ' + myRoman;
        if (myAttribute != null && given != null) given += ' ' + myAttribute;
        return (given);
      }

      // Return original
      return (original());
    }

    /** Returns a description */
    public String describe() {
      return ("PersonName\n" + "  Original: " + original + "\n" + "  Titles: " + titles() + "\n" + "  Given Name: " + givenName() + "\n"
          + "  Given Names: " + givenNames() + "\n" + "  Nickname: " + nickname() + "\n" + "  Family Name Prefix: " + familyNamePrefix() + "\n"
          + "  Attribute Prefix: " + attributePrefix() + "\n" + "  Family Name: " + familyName() + "\n" + "  Attribute: " + attribute() + "\n"
          + "  Family Name Suffix: " + familyNameSuffix() + "\n" + "  Roman: " + roman() + "\n" + "  City: " + city() + "\n" + "  Normalized: " + normalize());
    }

  }

  // ----------------------------------------------------------------------------
  // American States
  // ----------------------------------------------------------------------------

  public static Map<String, String> usStates = new FinalMap<String, String>("AL", "Alabama", "AK", "Alaska", "AS", "American Samoa", "AZ", "Arizona",
      "AR", "Arkansas", "CA", "California", "CALIF", "California", "CO", "Colorado", "CT", "Connecticut", "DE", "Delaware", "DC",
      "District of Columbia", "FM", "Federated States of Micronesia", "FL", "Florida", "GA", "Georgia", "GU", "Guam", "HI", "Hawaii", "ID", "Idaho",
      "IL", "Illinois", "IN", "Indiana", "IA", "Iowa", "KS", "Kansas", "KY", "Kentucky", "LA", "Louisiana", "ME", "Maine", "MH", "Marshall Islands",
      "MD", "Maryland", "MA", "Massachusetts", "MI", "Michigan", "MN", "Minnesota", "MS", "Mississippi", "MO", "Missouri", "MT", "Montana", "NE",
      "Nebraska", "NV", "Nevada", "NH", "New Hampshire", "NJ", "New Jersey", "NM", "New Mexico", "NY", "New York", "NC", "North Carolina", "ND",
      "North Dakota", "MP", "Northern Mariana Islands", "OH", "Ohio", "OK", "Oklahoma", "OR", "Oregon", "PW", "Palau", "PA", "Pennsylvania", "PR",
      "Puerto Rico", "RI", "Rhode Island", "SC", "South Carolina", "SD", "South Dakota", "TN", "Tennessee", "TX", "Texas", "UT", "Utah", "VT",
      "Vermont", "VI", "Virgin Islands", "VA", "Virginia", "WA", "Washington", "WV", "West Virginia", "WI", "Wisconsin", "WY", "Wyoming");

  /** Returns TRUE for US States */
  public static boolean isUSState(String s) {
    return (usStates.values().contains(s.replace('_', ' ')));
  }

  /** Returns TRUE for US State abbreviations */
  public static boolean isUSStateAbbreviation(String s) {
    if (s.endsWith(".")) s = Char.cutLast(s);
    return (usStates.containsKey(s.toUpperCase()));
  }

  /** Returns the US sate for an abbreviation (or NULL) */
  public static String unabbreviateUSState(String s) {
    if (s.endsWith(".")) s = Char.cutLast(s);
    return (usStates.get(s.toUpperCase()));
  }

  // ----------------------------------------------------------------------------
  // Language names
  // ----------------------------------------------------------------------------

  // FIXME: these have not been internationalized yet

  /** Language codes */
  public static Map<String, String> languageCodes = new FinalMap<String, String>("aa", "Afar", "ab", "Abkhazian", "ae", "Avestan", "af", "Afrikaans",
      "ak", "Akan", "am", "Amharic", "an", "Aragonese", "ar", "Arabic", "as", "Assamese", "av", "Avaric", "ay", "Aymara", "az", "Azerbaijani", "ba",
      "Bashkir", "be", "Belarusian", "bg", "Bulgarian", "bh", "Bihari", "bi", "Bislama", "bm", "Bambara", "bn", "Bengali", "bo", "Tibetan", "br",
      "Breton", "bs", "Bosnian", "ca", "Catalan", "ce", "Chechen", "ch", "Chamorro", "co", "Corsican", "cr", "Cree", "cs", "Czech", "cu", "Church",
      "cv", "Chuvash", "cy", "Welsh", "da", "Danish", "de", "German", "dv", "Divehi", "dz", "Dzongkha", "ee", "Ewe", "el", "Greek", "en", "English",
      "eo", "Esperanto", "es", "Spanish", "et", "Estonian", "eu", "Basque", "fa", "Persian", "ff", "Fulah", "fi", "Finnish", "fj", "Fijian", "fo",
      "Faroese", "fr", "French", "fy", "Western Frisian", "ga", "Irish", "gd", "Scottish", "gl", "Galician", "gn", "Guaran�", "gu", "Gujarati",
      "gv", "Manx", "ha", "Hausa", "he", "Hebrew", "hi", "Hindi", "ho", "Hiri", "hr", "Croatian", "ht", "Haitian", "hu", "Hungarian", "hy",
      "Armenian", "hz", "Herero", "ia", "Interlingua", "id", "Indonesian", "ie", "Interlingue", "ig", "Igbo", "ii", "Sichuan", "ik", "Inupiaq", "io",
      "Ido", "is", "Icelandic", "it", "Italian", "iu", "Inuktitut", "ja", "Japanese", "jv", "Javanese", "ka", "Georgian", "kg", "Kongo", "ki",
      "Kikuyu", "kj", "Kwanyama", "kk", "Kazakh", "kl", "Kalaallisut", "km", "Khmer", "kn", "Kannada", "ko", "Korean", "kr", "Kanuri", "ks",
      "Kashmiri", "ku", "Kurdish", "kv", "Komi", "kw", "Cornish", "ky", "Kirghiz", "la", "Latin", "lb", "Luxembourgish", "lg", "Ganda", "li",
      "Limburgish", "ln", "Lingala", "lo", "Lao", "lt", "Lithuanian", "lu", "Luba-Katanga", "lv", "Latvian", "mg", "Malagasy", "mh", "Marshallese",
      "mi", "Maori", "mk", "Macedonian", "ml", "Malayalam", "mn", "Mongolian", "mo", "Moldavian", "mr", "Marathi", "ms", "Malay", "mt", "Maltese",
      "my", "Burmese", "na", "Nauru", "nb", "Norwegian", "nd", "North", "ne", "Nepali", "ng", "Ndonga", "nl", "Dutch", "nn", "Norwegian", "no",
      "Norwegian", "nr", "South", "nv", "Navajo", "ny", "Chichewa", "oc", "Occitan", "oj", "Ojibwa", "om", "Oromo", "or", "Oriya", "os", "Ossetian",
      "pa", "Panjabi", "pi", "Pali", "pl", "Polish", "ps", "Pashto", "pt", "Portuguese", "qu", "Quechua", "rm", "Raeto-Romance", "rn", "Kirundi",
      "ro", "Romanian", "ru", "Russian", "rw", "Kinyarwanda", "ry", "Rusyn", "sa", "Sanskrit", "sc", "Sardinian", "sd", "Sindhi", "se", "Northern",
      "sg", "Sango", "sh", "Serbo-Croatian", "si", "Sinhalese", "sk", "Slovak", "sl", "Slovenian", "sm", "Samoan", "sn", "Shona", "so", "Somali",
      "sq", "Albanian", "sr", "Serbian", "ss", "Swati", "st", "Sotho", "su", "Sundanese", "sv", "Swedish", "sw", "Swahili", "ta", "Tamil", "te",
      "Telugu", "tg", "Tajik", "th", "Thai", "ti", "Tigrinya", "tk", "Turkmen", "tl", "Tagalog", "tn", "Tswana", "to", "Tonga", "tr", "Turkish",
      "ts", "Tsonga", "tt", "Tatar", "tw", "Twi", "ty", "Tahitian", "ug", "Uighur", "uk", "Ukrainian", "ur", "Urdu", "uz", "Uzbek", "ve", "Venda",
      "vi", "Vietnamese", "vo", "Volap�k", "wa", "Walloon", "wo", "Wolof", "xh", "Xhosa", "yi", "Yiddish", "yo", "Yoruba", "za", "Zhuang", "zh",
      "Chinese", "zu", "Zulu");

  /** Returns TRUE for languages */
  public static boolean isLanguage(String s) {
    return (languageCodes.values().contains(Char.upCaseFirst(s)));
  }

  /** Returns TRUE for language codes */
  public static boolean isLanguageCode(String s) {
    return (languageCodes.containsKey(s.toLowerCase()));
  }

  /** Returns the language for a code (or NULL) */
  public static String languageForCode(String s) {
    return (languageCodes.get(s.toLowerCase()));
  }

  // ----------------------------------------------------------------------------
  // Nationalities
  // ----------------------------------------------------------------------------

  // FIXME: these have not been internationalized yet

  public static Map<String, String> nationality2country = new FinalMap<String, String>("African", "Africa", "Antarctic", "Antarctica", "Americana",
      "Americas", "Asian", "Asia", "Middle Eastern", "Middle East", "Australasian", "Australasia", "Australian", "Australia", "Eurasian", "Eurasia",
      "European", "Europe", "North American", "North America", "Oceanian", "Oceania", "South American", "South America", "Afghan", "Afghanistan",
      "Albanian", "Albania", "Algerian", "Algeria", "American Samoan", "American Samoa", "Andorran", "Andorra", "Angolan", "Angola", "Anguillan",
      "Anguilla", "Antiguan", "Antigua and Barbuda", "Argentine", "Argentina", "Argentinean", "Argentina", "Argentinian", "Argentina", "Armenian",
      "Armenia", "Aruban", "Aruba", "Austrian", "Austria", "Azerbaijani", "Azerbaijan", "Azeri", "Azerbaijan", "Bahamian", "Bahamas", "Bahraini",
      "Bahrain", "Bangladeshi", "Bangladesh", "Barbadian", "Barbados", "Bajan", "Barbados", "Belarusian", "Belarus", "Belgian", "Belgium",
      "Belizean", "Belize", "Beninese", "Benin", "Bermudian", "Bermuda", "Bermudan", "Bermuda", "Bhutanese", "Bhutan", "Bolivian", "Bolivia",
      "Bosnian", "Bosnia and Herzegovina", "Bosniak", "Bosnia and Herzegovina", "Herzegovinian", "Bosnia and Herzegovina", "Botswanan", "Botswana",
      "Brazilian", "Brazil", "British Virgin Island", "British Virgin Islands", "Bruneian", "Brunei", "Bulgarian", "Bulgaria", "Burkinabe",
      "Burkina Fasoa", "Burmese", "Burmab", "Burundian", "Burundi", "Cambodian", "Cambodia", "Cameroonian", "Cameroon", "Canadian", "Canada",
      "Cape Verdean", "Cape Verde", "Caymanian", "Cayman Islands", "Central African", "Central African Republic", "Chadian", "Chad", "Chilean",
      "Chile", "Chinese", "People's Republic of China", "See Taiwan", "Republic of China", "Christmas Island", "Christmas Island", "Cocos Island",
      "Cocos (Keeling) Islands", "Colombian", "Colombia", "Comorian", "Comoros", "Congolese", "Democratic Republic of the Congo", "Cook Island",
      "Cook Islands", "Costa Rican", "Costa Rica", "Ivorian", "C�te d'Ivoire", "Croatian", "Croatia", "Cuban", "Cuba", "Cypriot", "Cyprus",
      "Czech", "Czech Republic", "Danish", "Denmark", "Djiboutian", "Djibouti", "Dominicand", "Dominica", "Dominicane", "Dominican Republic",
      "Timorese", "East Timor", "Ecuadorian", "Ecuador", "Egyptian", "Egypt", "Salvadoran", "El Salvador", "English", "England",
      "Equatorial Guinean", "Equatorial Guinea", "Eritrean", "Eritrea", "Estonian", "Estonia", "Ethiopian", "Ethiopia", "Falkland Island",
      "Falkland Islands", "Faroese", "Faroe Islands", "Fijian", "Fiji", "Finnish", "Finland", "French", "France", "French Guianese", "French Guiana",
      "French Polynesian", "French Polynesia", "Gabonese", "Gabon", "Gambian", "Gambia", "Georgian", "Georgia", "German", "Germany", "Ghanaian",
      "Ghana", "Gibraltar", "Gibraltar", "Greek", "Greece", "Greenlandic", "Greenland", "Grenadian", "Grenada", "Guadeloupe", "Guadeloupe",
      "Guamanian", "Guam", "Guatemalan", "Guatemala", "Guinean", "Guinea", "Guyanese", "Guyana", "Haitian", "Haiti", "Honduran", "Honduras",
      "Hong Kong", "Hong Kong", "Hungarian", "Hungary", "Icelandic", "Iceland", "Indian", "India", "Indonesian", "Indonesia", "Iranian", "Iran",
      "Iraqi", "Iraq", "Manx", "Isle of Man", "Israeli", "Israel", "Italian", "Italy", "Jamaican", "Jamaica", "Japanese", "Japan", "Jordanian",
      "Jordan", "Kazakhstaniz", "Kazakhstan", "Kenyan", "Kenya", "I-Kiribati", "Kiribati", "North Korean", "North Korea", "South Korean",
      "South Korea", "Kosovar", "Kosovo", "Kuwaiti", "Kuwait", "Kyrgyzstani", "Kyrgyzstan", "Laotian", "Laos", "Latvian", "Latvia", "Lebanese",
      "Lebanon", "Basotho", "Lesotho", "Liberian", "Liberia", "Libyan", "Libya", "Liechtenstein", "Liechtenstein", "Lithuanian", "Lithuania",
      "Luxembourg", "Luxembourg", "Macanese", "Macau", "Macedonian", "Republic of Macedonia", "Malagasy", "Madagascar", "Malawian", "Malawi",
      "Malaysian", "Malaysia", "Maldivian", "Maldives", "Malian", "Mali", "Maltese", "Malta", "Marshallese", "Marshall Islands", "Martiniquais",
      "Martinique", "Mauritanian", "Mauritania", "Mauritian", "Mauritius", "Mahoran", "Mayotte", "Mexican", "Mexico", "Micronesian", "Micronesia",
      "Moldovan", "Moldova", "Mon�gasque", "Monaco", "Mongolian", "Mongolia", "Montenegrin", "Montenegro", "Montserratian", "Montserrat",
      "Moroccan", "Morocco", "Mozambican", "Mozambique", "Namibian", "Namibia", "Nauruan", "Nauru", "Nepali", "Nepal", "Dutch", "Netherlands",
      "Dutch Antillean", "Netherlands Antilles", "New Caledonian", "New Caledonia", "New Zealand", "New Zealand", "Nicaraguan", "Nicaragua",
      "Niuean", "Niue", "Nigerien", "Niger", "Nigerian", "Nigeria", "Norwegian", "Norway", "Northern Irish", "Northern Ireland", "Northern Marianan",
      "Northern Marianas", "Omani", "Oman", "Pakistani", "Pakistan", "Palestinian", "Palestinian territories", "Palauan", "Palau", "Panamanian",
      "Panama", "Papua New Guinean", "Papua New Guinea", "Paraguayan", "Paraguay", "Peruvian", "Peru", "Philippine", "Philippines", "Filipino",
      "Philippines", "Pitcairn Island", "Pitcairn Island", "Polish", "Poland", "Portuguese", "Portugal", "Puerto Rican", "Puerto Rico", "Qatari",
      "Qatar", "Irish", "Republic of Ireland", "R�unionese", "R�union", "Romanian", "Romania", "Russian", "Russia", "Rwandan", "Rwanda",
      "St. Helenian", "St. Helena", "Kittitian", "St. Kitts and Nevis", "St. Lucian", "St. Lucia", "Saint-Pierrais", "Saint-Pierre and Miquelon",
      "St. Vincentian", "St. Vincent and the Grenadines", "Samoan", "Samoa", "Sammarinese", "San Marino", "S�o Tom�an",
      "S�o Tom� and Pr�ncipe", "Saudi", "Saudi Arabia", "Scottish", "Scotland", "Senegalese", "Senegal", "Serbian", "Serbia", "Seychellois",
      "Seychelles", "Sierra Leonean", "Sierra Leone", "Singaporean", "Singapore", "Slovak", "Slovakia", "Slovene", "Slovenia", "Slovenian",
      "Slovenia", "Solomon Island", "Solomon Islands", "Somali", "Somalia", "Somaliland", "Somaliland", "South African", "South Africa", "Spanish",
      "Spain", "Sri Lankan", "Sri Lanka", "Sudanese", "Sudan", "Surinamese", "Surinam", "Swazi", "Swaziland", "Swedish", "Sweden", "Swiss",
      "Switzerland", "Syrian", "Syria", "Taiwanese", "Taiwan", "Tajikistani", "Tajikistan", "Tanzanian", "Tanzania", "Thai", "Thailand", "Togolese",
      "Togo", "Tongan", "Tonga", "Trinidadian", "Trinidad and Tobago", "Tunisian", "Tunisia", "Turkish", "Turkey", "Turkmen", "Turkmenistan",
      "Tuvaluan", "Tuvalu", "Ugandan", "Uganda", "Ukrainian", "Ukraine", "Emirati", "United Arab Emirates", "British", "United Kingdom", "American",
      "United States of America", "Uruguayan", "Uruguay", "Uzbekistani", "Uzbekistan", "Uzbek", "Uzbekistan", "Vanuatuan", "Vanuatu", "Venezuelan",
      "Venezuela", "Vietnamese", "Vietnam", "Virgin Island", "Virgin Islands", "Welsh", "Wales", "Wallisian", "Wallis and Futuna", "Sahrawi",
      "Western Sahara", "Yemeni", "Yemen", "Zambian", "Zambia", "Zimbabwean", "Zimbabwe");

  /** Returns TRUE for nations */
  public static boolean isNation(String s) {
    return (nationality2country.values().contains(s));
  }

  /** Returns TRUE for nationalities */
  public static boolean isNationality(String s) {
    return (nationality2country.containsKey(s));
  }

  /** Returns the nation for a nationality (or NULL) */
  public static String nationForNationality(String s) {
    return (nationality2country.get(s));
  }

  // ----------------------------------------------------------------------------
  // Main
  // ----------------------------------------------------------------------------

  /** Factory pattern */
  public static NameML of(String s, Language lang) {
    if (isCompanyName(s)) return (new CompanyNameML(s));
    if (couldBePersonName(s, lang)) return (new PersonNameML(s, lang));
    if (isAbbreviation(s)) return (new AbbreviationML(s));
    return (new NameML(s));
  }

  /** Test routine */
  public static void main(String[] argv) throws Exception {
    for (String s : new FileLines("./testdata/NameParserTestDe.txt")) {
      //D.p(Name.of(s).describe());
      D.p(NameML.of(s, Language.GERMAN).describe());
    }
  }
}
