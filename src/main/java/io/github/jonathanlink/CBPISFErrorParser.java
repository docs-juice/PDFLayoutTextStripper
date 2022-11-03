import io.github.jonathanlink.PDFLayoutTextStripper;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;


/**
 * This class uses PDFLayoutTextStripper to extract the lines from the PDF - the same line length is 153 characters.
 * This doesnt cover all the characters in the line. The safest method is to capture columns till Article description.
 * There should be some other mechanism to capture "Unit of Quantity" and "Rates of Duty" from the document.
 */
public class CBPISFErrorParser {

    public static String LAST_TWO_PREFIX = "                       ";
    public static int LAST_TWO_OFFSET = LAST_TWO_PREFIX.length();
    public static String LAST_TWO_PATTERN = "(\\s)+(\\d{2})(.*)";
    public static String CODE_START_PREFIX = "              ";
    public static int FIRST_FOUR_OFFSET = 0;
    public static int UNIT_OF_QUANTITY_OFFSET = 103;
    public static String CODE_START_PATTERN = "(\\s)+(\\d{3})(.*)";
    public static String ABC = "((\\s+\\(con\\.\\))?)";
    public static Pattern FIRST_FOUR_MATCHER = Pattern.compile(CODE_START_PATTERN);
    //public static String FIRST_SIX_PREFIX = "                               ";
    public static String MORE_THAN_ONE_SPACE = "(.*)(\\s{3,})(.*)";


    public static void main2(String[] args) {
        String line = " ";
        if (line.matches(ABC)) {
            System.out.println("true");
        }
    }

	public static void main(String[] args) {
		String string = null;
        try {
            PDFParser pdfParser = new PDFParser(new RandomAccessFile(new File("/Users/ashishmehrotra/Documents/fastisf/catair-isf-error-codes.pdf"), "r"));
            pdfParser.parse();
            PDDocument pdDocument = new PDDocument(pdfParser.getDocument());
            PDFTextStripper pdfTextStripper = new PDFLayoutTextStripper();
            string = pdfTextStripper.getText(pdDocument);
            String[] lines = string.split("\n");
            List<String> codes = new ArrayList<>();
            List<String> smallDesc = new ArrayList<>();
            List<String> bigDesc = new ArrayList<>();

            int i=0;
            /*for (i = 0; i < lines.length; i++) {
                System.out.println(lines[i].length() + " " + lines[i]);
            }*/

            String desc = null;
            String small = null;
            for (String line: lines) {
                if (line.matches(CODE_START_PATTERN)) {
                    if (desc != null) {
                        bigDesc.add(desc);
                        smallDesc.add(small);
                    }
                    String val = line.replaceAll(CODE_START_PATTERN, "$2");
                    codes.add(val);
                    String text = line.replaceAll(CODE_START_PATTERN, "$3").trim();
                    int currBlankMax = 0;
                    for (int j=0; j < text.length(); j++) {
                        if (text.charAt(j) == ' ') {
                            //if (text.charAt(j-1))
                        }
                    }

                    int index = line.indexOf(val);
                    String code = line.substring(index, index+3);
                    System.out.println(code);
                    i++;
                } else {
                    desc += " " + line.trim();
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        };
        //System.out.println(string);
	}

}
