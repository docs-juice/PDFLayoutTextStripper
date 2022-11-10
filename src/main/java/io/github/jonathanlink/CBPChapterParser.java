package io.github.jonathanlink;

import io.github.jonathanlink.PDFLayoutTextStripper;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
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

    public static void main11(String[] args) {
        String line = "0303.41.00.00 : Fish, frozen, excluding fish fillets and other fish meat of heading 04: : Tunas (of the genus Thunnus), skipjack tuna (stripe-bellied bonito) (Katsuwonus pelamis), excluding edible fish offal of subheadings 0303:91 to 0303:99: Albacore or longfinned tunas (Thunnus alalunga): kg";
        //if (line.matches(ABC)) {
        if (line.contains(": : ")) {
            System.out.println(line);
            line = line.replaceAll(": : ", ": ");
            line = line.replaceAll(": ", "->");
            System.out.println(line);
        }
    }

    /*public static void main(String[] args) {
        try {
            File folder = new File("/Users/ashishmehrotra/Downloads/cbp chapters/");
            for (final File fileEntry : folder.listFiles()) {
                System.out.println("File processing... " + fileEntry.getAbsolutePath());
                if (!fileEntry.isDirectory() && fileEntry.getName().endsWith("pdf")) {
                    String txtFileName = fileEntry.getAbsolutePath().replace(".pdf", ".sql");
                    boolean fileExists = new File(txtFileName).exists();
                    if (fileEntry.getName().endsWith("99.pdf") || fileExists)
                        continue;
                    String value = processFile(fileEntry.getAbsolutePath());

                    BufferedWriter writer = new BufferedWriter(new FileWriter(txtFileName));
                    writer.write(value);
                    writer.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    public static void main(String[] args) {
        try {

            String value = processFile("/Users/ashishmehrotra/Downloads/cbp chapters/Chapter 3.pdf");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String processFile(String fileName) {
        String string = null;
        try {
            PDFParser pdfParser = new PDFParser(new RandomAccessFile(new File(fileName), "r"));
            pdfParser.parse();
            PDDocument pdDocument = new PDDocument(pdfParser.getDocument());
            PDFTextStripper pdfTextStripper = new PDFLayoutTextStripper();
            string = pdfTextStripper.getText(pdDocument);
            String[] lines = string.split("\n");
            int i=0;
            String lastHTS = null;

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
            Stack<String> contentKeyTypes = new Stack<>();

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
                    contentKeyTypes.clear();
                    while (!(line.startsWith(FIRST_FOUR_PREFIX) && line.matches(FIRST_FOUR_PATTERN))) {
                        if (i == lines.length - 1)
                            break;
                        line = lines[++i];
                    }
                }
                if (i == lines.length - 1)
                    break;

                if (line.trim().length() == 0 || line.trim().equals("(con.)")) { //nothing to add in the line, keep moving
                    continue;
                }
                if (line.startsWith(FIRST_FOUR_PREFIX) && line.matches(ALL_TEN_PATTERN)) {
                    htsVal = line.replaceAll(ALL_TEN_PATTERN, "$2").trim();
                    htsVal = htsVal.replaceAll("\\s+", ".");
                    if (htsVal.equals("0306.17.00.29")) {
                        System.out.println("1");
                    }
                    //pop the stack based on the values in HTS

                    val = line.replaceAll(ALL_TEN_PATTERN, "$3").trim();
                    contentOffset = line.indexOf(val);
                    //val = val.replaceAll("\\(con.\\)", "");
                    currType = TEN_KEY;
                    currKey = htsVal;
                } else if (line.startsWith(FIRST_FOUR_PREFIX) && line.matches(FIRST_EIGHT_PATTERN)) {
                    order = 30;
                    eightHTSVal = line.replaceAll(FIRST_EIGHT_PATTERN, "$2").trim();
                    val = line.replaceAll(FIRST_EIGHT_PATTERN, "$3").trim();
                    contentOffset = line.indexOf(val);
                    //val = val.replaceAll("\\(con.\\)", "");
                    currType = EIGHT_KEY;
                    currKey = eightHTSVal;
                } else if (line.startsWith(LAST_TWO_PREFIX) && line.matches(LAST_TWO_PATTERN)) {//starting of first 4
                    order = 40;
                    String totalLine = lines[i].substring(line.length());
                    String lastTwoVal = line.replaceAll(LAST_TWO_PATTERN, "$2").trim();
                    if (eightHTSVal.equals("0306.17.00") && lastTwoVal.equals("29")) {
                        System.out.println("1");
                    }
                    htsVal = eightHTSVal + "."+ lastTwoVal;
                    val = line.replaceAll(LAST_TWO_PATTERN, "$3").trim();
                    contentOffset = line.indexOf(val);
                    //val = val.replaceAll("\\(con.\\)", "");
                    currType = LAST_TWO_KEY;
                    currKey = htsVal;
                } else if (line.startsWith(FIRST_FOUR_PREFIX) && line.matches(FIRST_SIX_PATTERN)) {
                    order = 20;
                    firtSixVal = line.replaceAll(FIRST_SIX_PATTERN, "$2").trim();
                    val = line.replaceAll(FIRST_SIX_PATTERN, "$3").trim();
                    contentOffset = line.indexOf(val);
                    //val = val.replaceAll("\\(con.\\)", "");
                    currType = SIX_KEY;
                    currKey = firtSixVal;
                } else if (line.startsWith(FIRST_FOUR_PREFIX) && line.matches(FIRST_FOUR_PATTERN)) {
                    contentValues.clear();
                    contentOffsets.clear();
                    contentKeyTypes.clear();
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
                    //val = val.replaceAll("\\(con.\\)", "");
                    if (contentOffset > prevContentOffset) {
                        order = prevOrder + 2; //it could be next 4digit or 8 digit
                    } /*else if (contentOffset == prevContentOffset) {
                        currType = prevType;
                    }*/
                    currType = null;
                }
                prevOrder = order;

                val = val.trim();
                String prev = contentValues.size() > 0 ? contentValues.peek() : "";
                if (contentValues.isEmpty() || contentOffsets.peek() < contentOffset) {
                    contentOffsets.push(contentOffset);
                    contentValues.push(prev + " " + val);
                    contentKeyTypes.push(currType);
                    //contentValues.push(val);
                } else if (contentOffsets.peek() == contentOffset) {
                    //String v = hasHtsInLine? contentValues.peek() : contentValues.pop();
                    prev = contentValues.size() > 0 ? contentValues.peek() : "";
                    String v = contentValues.peek();
                    //NOTE - use contains and not ends with because units get parsed sometimes too
                    if (!v.equals("")) {
                        if (v.endsWith(":") || (v.length() > 8 && v.substring(v.length()-8).contains("..."))) {
                            if (currType != null && contentOffset != contentOffsets.peek()) {
                                contentValues.pop();
                                contentValues.push(prev + " " + val);
                                contentOffsets.pop();
                                contentOffsets.push(contentOffset);
                            } else {
                                String oldVal = contentValues.pop();
                                contentOffsets.pop();
                                prev = contentValues.size() > 0 ? contentValues.peek() : "";
                                if (prev.length() == 0) {
                                    prev = oldVal;
                                }
                                contentValues.push(prev + " " + val);
                                contentOffsets.push(contentOffset);
                            }
                            contentKeyTypes.pop();
                            contentKeyTypes.push(currType);
                        } else {
                            v += " " + val;
                            contentValues.pop();
                            contentValues.push(v);
                        }
                    }
                    if (currType == null) {
                        currKey = prevKey;
                    }
                } else if (contentOffsets.peek() > contentOffset)  {
                    while (!contentOffsets.isEmpty() && contentOffsets.peek() >= contentOffset) {
                        contentOffsets.pop();
                        contentValues.pop();
                        contentKeyTypes.pop();
                    }
                    contentOffsets.push(contentOffset);
                    prev = contentValues.size() > 0 ? contentValues.peek() + " :" : "";
                    contentValues.push(prev + " " + val);
                    contentKeyTypes.push(currKey);
                    currKey = null;
                }

                if (line.matches(ALL_TEN_PATTERN) || (line.startsWith(LAST_TWO_PREFIX) && line.matches(LAST_TWO_PATTERN))) {
                    String value = contentValues.peek();
                    //since the line has ... means it is ending the value and there's no follow up content in next line, so safe to pop it
                    if (value.endsWith("...") || (value.length() > 8 && value.substring(value.length()-8).contains("..."))) {
                        while (contentOffsets.size() > 0 && contentOffsets.peek() >= contentOffset) {
                            contentValues.pop();
                            contentOffsets.pop();
                            contentKeyTypes.pop();
                        }
                    }

                    //System.out.println(htsVal + " : " + value);
                    value = value.replaceAll(": : ", ": ");
                    value = value.replaceAll(": ", "->");
                    boolean skip = false;
                    if (lastHTS != null && !lastHTS.equals(htsVal)) {
                        String[] lastParts = lastHTS.split("\\.");
                        String[] parts = htsVal.split("\\.");
                        if (Integer.parseInt(parts[3]) <= Integer.parseInt(lastParts[3])
                                && Integer.parseInt(parts[2]) <= Integer.parseInt(lastParts[2])
                                && Integer.parseInt(parts[1]) <= Integer.parseInt(lastParts[1])
                                && Integer.parseInt(parts[0]) <= Integer.parseInt(lastParts[0])) {
                            skip = true;
                        }
                    }
                    if (!skip) {
                        value = value.replaceAll("\\(con.\\)", "");
                        value = value.replaceAll("\\.{3,}", "->").trim();
                        if (value.endsWith("->")) {
                            value = value.substring(0, value.length()-2);
                        }
                        finalHTSMap.put(htsVal, value);
                        lastHTS = htsVal;
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
                    //if (vals.length <= 4) {
                    if (vals.length == 4) {
                        v = vals[0].trim();
                        key = htsVal.substring(0, 4);
                        /*if (!fourLetterMaps.containsValue(v)) {
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
                        currKey = key;*/
                    }
                } else if (line.startsWith(FIRST_FOUR_PREFIX) && line.matches(FIRST_EIGHT_PATTERN)) {
                    String value = contentValues.peek();
                    //value = value.replaceAll("\\.+", ":").trim();
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
                    //value = value.replaceAll("\\.+", ":").trim();
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
                    //value = value.replaceAll("\\.+", ":").trim();
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
                    boolean finish = false;
                    if (value.contains("....") && ((currType != null && (currType.equals(TEN_KEY) || currType.equals(LAST_TWO_KEY))) ||
                                    (currType == null && prevType != null && (prevType.equals(TEN_KEY) || prevType.equals(LAST_TWO_KEY))))) {
                        finish = true;
                    }
                    //value = value.replaceAll("\\.+", ":").trim();
                    /*if (value.endsWith(":")) {
                        value = value.substring(0, value.length() - 1);
                    }*/
                    if (value.endsWith(": No")) {
                        value = value.substring(0, value.length() - 4);
                    }
                    if (finish) {
                        boolean skip = false;
                        if (lastHTS != null && !lastHTS.equals(htsVal)) {
                            String[] lastParts = lastHTS.split("\\.");
                            String[] parts = htsVal.split("\\.");
                            if (Integer.parseInt(parts[3]) <= Integer.parseInt(lastParts[3])
                                    && Integer.parseInt(parts[2]) <= Integer.parseInt(lastParts[2])
                                    && Integer.parseInt(parts[1]) <= Integer.parseInt(lastParts[1])
                                    && Integer.parseInt(parts[0]) <= Integer.parseInt(lastParts[0])) {
                                skip = true;
                            }
                        }
                        if (!skip) {
                            value = value.replaceAll("\\(con.\\)", "");
                            value = value.replaceAll("\\.{3,}", "->").trim();
                            value = value.replaceAll(":", "->").trim();
                            if (value.endsWith("->")) {
                                value = value.substring(0, value.length()-2);
                            }
                            finalHTSMap.put(htsVal, value);
                            lastHTS = htsVal;
                        }
                    }
                    //System.out.println(prevType + " : " + currType);

                    /*if (currKey != null) {
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
                    }*/

                }

                prevContentOffset = contentOffset;
                if (currType != null ) {
                    prevType = currType;
                }
                prevKey = currKey;
            }
            Iterator<String> iter = finalHTSMap.keySet().iterator();
            StringBuilder builder = new StringBuilder();
            while (iter.hasNext()) {
                String key = iter.next();
                System.out.println(key + " : " + finalHTSMap.get(key));
                builder.append("INSERT INTO `fastisf-prod`.hs_codes(COMPLETE_HTS_CODE, DESCRIPTION) VALUES(\"" + key + "\", \"" + finalHTSMap.get(key) + "\");").append(System.lineSeparator());
            }
            System.out.println(finalHTSMap.size());
            return builder.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        };
        return null;
    }
}

