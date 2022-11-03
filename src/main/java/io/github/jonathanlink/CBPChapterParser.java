import io.github.jonathanlink.PDFLayoutTextStripper;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class uses PDFLayoutTextStripper to extract the lines from the PDF - the same line length is 153 characters.
 * This doesnt cover all the characters in the line. The safest method is to capture columns till Article description.
 * There should be some other mechanism to capture "Unit of Quantity" and "Rates of Duty" from the document.
 */
public class CBPChapterParser {

    public static int TOTAL_LINE_LENGTH = 153;
    public static String LAST_TWO_PREFIX = "                       ";
    public static int LAST_TWO_OFFSET = LAST_TWO_PREFIX.length();
    public static String LAST_TWO_PATTERN = "(\\s)+(\\d{2})(.*)";
    public static String FIRST_FOUR_PREFIX = "           ";
    public static int FIRST_FOUR_OFFSET = 0;
    public static int UNIT_OF_QUANTITY_OFFSET = 103;
    public static String FIRST_FOUR_PATTERN = "(\\s)+(\\d{4})((\\s+)(\\(con\\.\\))?)(\\s)+(.*)";
    public static String ABC = "((\\s+\\(con\\.\\))?)";
    public static Pattern FIRST_FOUR_MATCHER = Pattern.compile(FIRST_FOUR_PATTERN);
    //public static String FIRST_SIX_PREFIX = "                               ";
    public static int FIRST_SIX_OFFSET = 0;
    public static String FIRST_SIX_PREFIX_PATTERN = "(\\s)+(.*)";
    public static String FIRST_SIX_PATTERN = "(\\s)+(\\d{4}.\\d{2})(.*)";
    public static String FIRST_EIGHT_PATTERN = "(\\s)+(\\d{4}.\\d{2}.\\d{2})(.*)";
    //public static String FIRST_EIGHT_PREFIX = "                                      ";
    public static int FIRST_EIGHT_OFFSET = 0;
    public static String FIRST_EIGHT_PREFIX_PATTERN = "(\\s)+(.*)";
    public static String ALL_TEN_PATTERN = "(\\s)+(\\d{4}.\\d{2}.\\d{2}\\s{1,10}\\d{2})(.*)";

    //types
    public static String NO_KEY = "NO_KEY";
    public static String FOUR_KEY = "FOUR_KEY";
    public static String SIX_KEY = "SIX_KEY";
    public static String EIGHT_KEY = "EIGHT_KEY";
    public static String TEN_KEY = "TEN_KEY";
    public static String LAST_TWO_KEY = "LAST_TWO_KEY";

    public static void main2(String[] args) {
        String line = " ";
        if (line.matches(ABC)) {
            System.out.println("true");
        }
    }

    public static void main(String[] args) {
        String string = null;
        try {
            PDFParser pdfParser = new PDFParser(new RandomAccessFile(new File("/Users/ashishmehrotra/Downloads/cbp chapters/Chapter 94.pdf"), "r"));
            pdfParser.parse();
            PDDocument pdDocument = new PDDocument(pdfParser.getDocument());
            PDFTextStripper pdfTextStripper = new PDFLayoutTextStripper();
            string = pdfTextStripper.getText(pdDocument);
            String[] lines = string.split("\n");
            int i=0;

            for (String line: lines) {
                if (line.startsWith(FIRST_FOUR_PREFIX) && line.matches(FIRST_FOUR_PATTERN)) {
                    String val = line.replaceAll(FIRST_FOUR_PATTERN, "$5");
                    FIRST_FOUR_OFFSET = line.indexOf(val);
                    break;
                } else if ((line.startsWith(FIRST_FOUR_PREFIX) && line.matches(ALL_TEN_PATTERN))
                        || (line.startsWith(FIRST_FOUR_PREFIX) && line.matches(FIRST_EIGHT_PATTERN))
                        || (line.startsWith(FIRST_FOUR_PREFIX) && line.matches(FIRST_SIX_PATTERN))) {
                    System.out.println("first 6 pattern");
                    break;
                }
                i++;
            }
            int startLine = i;
            int contentOffset = 0, prevContentOffset = -1;
            Stack<Integer> contentOffsets = new Stack<>();
            Stack<String> contentValues = new Stack<>();

            String val = null;
            String htsVal = null;
            String eightHTSVal = null;
            String firtFourVal = null;
            String firtSixVal = null;
            Map<String, String> fourLetterMaps = new LinkedHashMap<>();
            Map<String, String> sixLetterMaps = new LinkedHashMap<>();
            Map<String, String> eightLetterMaps = new LinkedHashMap<>();
            Map<String, String> tenLetterMaps = new LinkedHashMap<>();
            int prevOrder = -1, order = -1;
            Map<String, String> finalHTSMap = new LinkedHashMap<>();
            String currKey = null,prevKey = null;
            String currType = null, prevType = null;
            //List<String> htsValList = new ArrayList<>();
            //List<String> descList = new ArrayList<>();
            for (i = startLine; i < lines.length; i++) {
                boolean hasHtsInLine = true;
                String line = lines[i].substring(0, UNIT_OF_QUANTITY_OFFSET);
                if (line.trim().startsWith("Harmonized")) {
                    contentValues.clear();
                    contentOffsets.clear();
                    while (!(line.startsWith(FIRST_FOUR_PREFIX) && line.matches(FIRST_FOUR_PATTERN))) {
                        if (i == lines.length - 1)
                            break;
                        line = lines[++i];
                    }
                }
                if (i == lines.length - 1)
                    break;


                if (line.startsWith(FIRST_FOUR_PREFIX) && line.matches(ALL_TEN_PATTERN)) {
                    htsVal = line.replaceAll(ALL_TEN_PATTERN, "$2").trim();
                    htsVal = htsVal.replaceAll("\\s+", ".");
                    //pop the stack based on the values in HTS

                    val = line.replaceAll(ALL_TEN_PATTERN, "$3").trim();
                    contentOffset = line.indexOf(val);
                    val = val.replaceAll("\\(con.\\)", "");
                    currType = TEN_KEY;
                    currKey = htsVal;
                } else if (line.startsWith(FIRST_FOUR_PREFIX) && line.matches(FIRST_EIGHT_PATTERN)) {
                    order = 30;
                    eightHTSVal = line.replaceAll(FIRST_EIGHT_PATTERN, "$2").trim();
                    val = line.replaceAll(FIRST_EIGHT_PATTERN, "$3").trim();
                    contentOffset = line.indexOf(val);
                    val = val.replaceAll("\\(con.\\)", "");
                    if (eightHTSVal.equals("9401.39.00")) {
                        System.out.println("found");
                    }
                    currType = EIGHT_KEY;
                    currKey = eightHTSVal;
                } else if (line.startsWith(LAST_TWO_PREFIX) && line.matches(LAST_TWO_PATTERN)) {//starting of first 4
                    order = 40;
                    String totalLine = lines[i].substring(line.length());
                    String lastTwoVal = line.replaceAll(LAST_TWO_PATTERN, "$2").trim();
                    htsVal = eightHTSVal + "."+ lastTwoVal;
                    val = line.replaceAll(LAST_TWO_PATTERN, "$3").trim();
                    contentOffset = line.indexOf(val);
                    val = val.replaceAll("\\(con.\\)", "");
                    if (val.equals("75")) {
                        System.out.println("75");
                    }
                    currType = LAST_TWO_KEY;
                    currKey = htsVal;
                } else if (line.startsWith(FIRST_FOUR_PREFIX) && line.matches(FIRST_SIX_PATTERN)) {
                    order = 20;
                    firtSixVal = line.replaceAll(FIRST_SIX_PATTERN, "$2").trim();
                    val = line.replaceAll(FIRST_SIX_PATTERN, "$3").trim();
                    contentOffset = line.indexOf(val);
                    val = val.replaceAll("\\(con.\\)", "");
                    currType = SIX_KEY;
                    currKey = firtSixVal;
                } else if (line.startsWith(FIRST_FOUR_PREFIX) && line.matches(FIRST_FOUR_PATTERN)) {
                    contentValues.clear();
                    contentOffsets.clear();
                    order = 10;
                    firtFourVal = line.replaceAll(FIRST_FOUR_PATTERN, "$2").trim();
                    val = line.replaceAll(FIRST_FOUR_PATTERN, "$7").trim();
                    contentOffset = line.indexOf(val);
                    val = val.replaceAll("\\(con.\\)", "");
                    currType = FOUR_KEY;
                    currKey = firtFourVal;
                } else if (line.startsWith(FIRST_FOUR_PREFIX) && line.matches(FIRST_EIGHT_PREFIX_PATTERN)) {
                    hasHtsInLine = false;
                    val = line.replaceAll(FIRST_EIGHT_PREFIX_PATTERN, "$2").trim();
                    contentOffset = line.indexOf(val);
                    if (val.length() == 0) {
                        continue;
                    }
                    val = val.replaceAll("\\(con.\\)", "");
                    if (contentOffset > prevContentOffset) {
                        order = prevOrder + 5; //it could be next 4digit or 8 digit
                    } else if (contentOffset == prevContentOffset) {
                        currType = prevType;
                    }
                }
                prevOrder = order;

                val = val.trim();
                String prev = contentValues.size() > 0 ? contentValues.peek() : "";
                if (contentValues.isEmpty() || contentOffsets.peek() < contentOffset) {
                    contentOffsets.push(contentOffset);
                    contentValues.push(prev + " " + val);
                } else if (contentOffsets.peek() == contentOffset) {
                    //String v = hasHtsInLine? contentValues.peek() : contentValues.pop();
                    prev = contentValues.size() > 0 ? contentValues.peek() : "";
                    String v = contentValues.pop();
                    //NOTE - use contains and not ends with because units get parsed sometimes too
                    if (v.endsWith(":") || v.contains("...")) {
                        contentValues.push(prev + " " + val);
                        contentOffsets.pop();
                        contentOffsets.push(contentOffset);
                    } else {
                        v += " " + val;
                        contentValues.push(v);
                    }
                    if (currType == null) {
                        currKey = prevKey;
                    }
                } else if (contentOffsets.peek() > contentOffset)  {
                    while (!contentOffsets.isEmpty() && contentOffsets.peek() >= contentOffset) {
                        contentOffsets.pop();
                        contentValues.pop();
                    }
                    contentOffsets.push(contentOffset);
                    prev = contentValues.size() > 0 ? contentValues.peek() + " :" : "";
                    contentValues.push(prev + " " + val);
                    currKey = null;
                }

                if (line.matches(ALL_TEN_PATTERN) || (line.startsWith(LAST_TWO_PREFIX) && line.matches(LAST_TWO_PATTERN))) {
                    String value = contentValues.peek();
                    if (value.endsWith("...") || (value.length() > 10 && value.substring(0, value.length()-6).contains("..."))) {
                        contentValues.pop();
                        contentOffsets.pop();
                    }
                    value = value.replaceAll("\\.+", ":").trim();
                    if (value.endsWith(":")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    if (value.endsWith(": No")) {
                        value = value.substring(0, value.length() - 4);
                    }
                    System.out.println(htsVal + " : " + value);
                    finalHTSMap.put(htsVal, value);
                    String[] tempVals = value.split(":");
                    List<String> tempList = new ArrayList<>();

                    for (int j = 0; j < tempVals.length; j++) {
                        String s = tempVals[j];
                        if (s.trim().length() > 0) {
                            tempList.add(s.trim());
                        }
                    }
                    String[] vals = tempList.toArray(new String[0]);
                    String key = null; String v = null;
                    //if (vals.length <= 4) {
                    if (vals.length == 4) {
                        v = vals[0].trim();
                        key = htsVal.substring(0, 4);
                        if (!fourLetterMaps.containsValue(v)) {
                            fourLetterMaps.put(key, v);
                        }
                        key = htsVal.substring(0, 7);
                        v = vals[0] + "->" + vals[1];
                        if (!sixLetterMaps.containsValue(v)) {
                            sixLetterMaps.put(key, v);
                        }
                        key = htsVal.substring(0, 10);
                        v = vals[0] + "->" + vals[1] + "->" + vals[2];
                        if (!eightLetterMaps.containsValue(v)) {
                            eightLetterMaps.put(key, v);
                        }

                        v = vals[0] + "->" + vals[1] + "->" + vals[2] + "->" + vals[3];
                        if (!tenLetterMaps.containsValue(v)) {
                            tenLetterMaps.put(htsVal, v);
                        }
                        currKey = key;
                    }
                } else if (line.startsWith(FIRST_FOUR_PREFIX) && line.matches(FIRST_EIGHT_PATTERN)) {
                    String value = contentValues.peek();
                    value = value.replaceAll("\\.+", ":").trim();
                    if (value.endsWith(":")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    if (value.endsWith(": No")) {
                        value = value.substring(0, value.length() - 4);
                    }
                    String[] tempVals = value.split(":");
                    List<String> tempList = new ArrayList<>();

                    for (int j = 0; j < tempVals.length; j++) {
                        String s = tempVals[j];
                        if (s.trim().length() > 0) {
                            tempList.add(s.trim());
                        }
                    }
                    String[] vals = tempList.toArray(new String[0]);
                    String key = null; String v = null;

                    if (vals.length == 3) {
                        v = vals[0].trim();
                        key = eightHTSVal.substring(0, 4);
                        if (!fourLetterMaps.containsValue(v)) {
                            fourLetterMaps.put(key, v);
                        }
                        /*key = eightHTSVal.substring(0, 7);
                        v = vals[0] + "->" + vals[1];
                        if (!sixLetterMaps.containsValue(v)) {
                            sixLetterMaps.put(key, v);
                        }*/
                        key = eightHTSVal;
                        v = vals[0] + "->" + vals[1] + "->" + vals[2];
                        if (!eightLetterMaps.containsValue(v)) {
                            eightLetterMaps.put(key, v);
                        }
                        currKey = key;
                    }
                } else if (line.startsWith(FIRST_FOUR_PREFIX) && line.matches(FIRST_SIX_PATTERN)) {
                    String value = contentValues.peek();
                    value = value.replaceAll("\\.+", ":").trim();
                    if (value.endsWith(":")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    if (value.endsWith(": No")) {
                        value = value.substring(0, value.length() - 4);
                    }
                    String[] tempVals = value.split(":");
                    List<String> tempList = new ArrayList<>();

                    for (int j = 0; j < tempVals.length; j++) {
                        String s = tempVals[j];
                        if (s.trim().length() > 0) {
                            tempList.add(s.trim());
                        }
                    }
                    String[] vals = tempList.toArray(new String[0]);
                    String key = null; String v = null;

                    if (vals.length == 2) {
                        v = vals[0].trim();
                        key = firtSixVal.substring(0, 4);
                        if (!fourLetterMaps.containsValue(v)) {
                            fourLetterMaps.put(key, v);
                        }
                        key = firtSixVal;
                        v = vals[0] + "->" + vals[1];
                        if (!sixLetterMaps.containsValue(v)) {
                            sixLetterMaps.put(key, v);
                        }
                    }
                    currKey = key;
                } else if (line.startsWith(FIRST_FOUR_PREFIX) && line.matches(FIRST_FOUR_PATTERN)) {
                    String value = contentValues.peek();
                    value = value.replaceAll("\\.+", ":").trim();
                    if (value.endsWith(":")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    if (value.endsWith(": No")) {
                        value = value.substring(0, value.length() - 4);
                    }
                    String[] tempVals = value.split(":");
                    List<String> tempList = new ArrayList<>();

                    for (int j = 0; j < tempVals.length; j++) {
                        String s = tempVals[j];
                        if (s.trim().length() > 0) {
                            tempList.add(s.trim());
                        }
                    }
                    String[] vals = tempList.toArray(new String[0]);
                    String key = null; String v = null;

                    if (vals.length == 1) {
                        v = vals[0].trim();
                        key = firtFourVal;
                        if (!fourLetterMaps.containsValue(v)) {
                            fourLetterMaps.put(key, v);
                        }
                    }
                    currKey = key;
                } else if (line.startsWith(FIRST_FOUR_PREFIX) && line.matches(FIRST_EIGHT_PREFIX_PATTERN)) {
                    String value = contentValues.peek();
                    value = value.replaceAll("\\.+", ":").trim();
                    if (value.endsWith(":")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    if (value.endsWith(": No")) {
                        value = value.substring(0, value.length() - 4);
                    }
                    System.out.println(prevType + " : " + currType);

                    if (currKey != null && prevType.equals(currType)) {
                        switch (currType) {
                            case "FOUR_KEY": fourLetterMaps.put(currKey, value);
                                            break;
                            case "SIX_KEY": sixLetterMaps.put(currKey, value);
                                            break;
                            case "EIGHT_KEY": eightLetterMaps.put(currKey, value);
                                            break;
                            case "LAST_TWO_KEY":
                            case "TEN_KEY": tenLetterMaps.put(currKey, value);
                                            break;
                        }
                    }

                }
                System.out.println("1");
                    /*else if (vals.length == 3) {
                        vals[0] = vals[0].trim();
                        if (fourLetterMaps.containsValue(vals[0])) {
                            //if the first 2 wordsets are not in
                            key = htsVal.substring(0, 10);
                            if (!eightLetterMaps.containsValue(vals[0] + "->" + vals[1])) {
                                if (!sixLetterMaps.containsValue(vals[0] + "->" + vals[1])) {
                                    eightLetterMaps.put(key, vals[0] + "->" + vals[1]);
                                }
                            }
                            if (sixLetterMaps.containsValue(vals[0] + "->" + vals[1])) {
                                tenLetterMaps.put(htsVal, vals[0] + "->" + vals[1] + "->" + vals[2]);
                            } else if (eightLetterMaps.containsValue(vals[0] + "->" + vals[1])) {
                                tenLetterMaps.put(htsVal, vals[0] + "->" + vals[1] + "->" + vals[2]);
                            }
                        }

                        key = htsVal.substring(0, 7);
                        if (!sixLetterMaps.containsValue(vals[0] + "->" + vals[1])) {
                            if (!eightLetterMaps.containsValue(vals[0] + "->" + vals[1])) {
                                sixLetterMaps.put(key, vals[0] + "->" + vals[1]);
                            }
                        }
                    } else if (vals.length == 2) {
                        vals[0] = vals[0].trim();
                        key = htsVal.substring(0, 10);
                        if (!fourLetterMaps.containsValue(vals[0]) && !sixLetterMaps.containsValue(vals[0]) && !eightLetterMaps.containsValue(vals[0])) {
                            eightLetterMaps.put(key, vals[0]);
                        } else if (sixLetterMaps.containsValue(vals[0])) {
                            tenLetterMaps.put(htsVal, vals[0] + "->" + vals[1]);
                        } else if (eightLetterMaps.containsValue(vals[0])) {
                            tenLetterMaps.put(htsVal, vals[0] + "->" + vals[1]);
                        } else if (fourLetterMaps.containsValue(vals[0])) {
                            tenLetterMaps.put(htsVal, vals[0] + "->" + vals[1]);
                        }
                    } else if (vals.length > 4) {
                        /*vals[0] = vals[0].trim();
                        key = htsVal.substring(0, 4);
                        if (!fourLetterMaps.containsValue(vals[0])) {
                            fourLetterMaps.put(key, vals[0]);
                        }
                        key = htsVal.substring(0, 7);
                        vals[1] = vals[0] + "->" + vals[1];
                        if (!sixLetterMaps.containsValue(vals[1])) {
                            sixLetterMaps.put(key, vals[1]);
                        }
                        key = htsVal.substring(0, 10);
                        vals[2] = vals[1] + "->" + vals[2];
                        if (!eightLetterMaps.containsValue(vals[2])) {
                            eightLetterMaps.put(key, vals[2]);
                        }

                        String[] subarray = Arrays.asList(vals)
                                .subList(3, vals.length)
                                .toArray(new String[0]);
                        String v = String.join("->", subarray);

                        if (!tenLetterMaps.containsValue(v)) {
                            tenLetterMaps.put(htsVal, v);
                        }
                        System.out.println(htsVal);
                        //}
                    }*/
                prevContentOffset = contentOffset;
                prevType = currType;
                prevKey = currKey;
            }
            System.out.println(finalHTSMap.size());
            Iterator<String> iter = finalHTSMap.keySet().iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                if (tenLetterMaps.containsKey(key))
                    continue;

                String[] parts = key.split("\\.");
                String threeParts =  parts[0] + "." + parts[1] + "." + parts[2];
                String twoParts = parts[0] + "." + parts[1];
                String onePart = parts[0];
                String v = finalHTSMap.get(key);
                if (eightLetterMaps.containsKey(threeParts)) {
                    tenLetterMaps.put(key, eightLetterMaps.get(threeParts) + "->" + v);
                } else if (sixLetterMaps.containsKey(twoParts)) {
                    tenLetterMaps.put(key, sixLetterMaps.get(twoParts) + "->" + v);
                } else if (fourLetterMaps.containsKey(onePart)) {
                    tenLetterMaps.put(key, fourLetterMaps.get(onePart) + "->" + v);
                }
                System.out.println(key + " : " + tenLetterMaps.get(key));
            }
            System.out.println(finalHTSMap.size());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        };
        //System.out.println(string);
    }
}

