package oro;

// PDFBox
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.text.TextPosition;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.util.ImageHelper;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import net.sourceforge.tess4j.ITessAPI;


import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

// CoreNLP
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

// Java standard
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OroPDFDocument {
    private PDDocument document;
    private String filePath;

    public OroPDFDocument(String filePath) throws IOException {
        this.document = PDDocument.load(new File(filePath));
        this.filePath = filePath;
    }

    public String getPDFText() throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        return stripper.getText(document);
    }

    public String getFilePath() {
        return filePath;
    }

    public static void createPDF(String outputPath, String newText) throws IOException {
        try (PDDocument newDoc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            newDoc.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(newDoc, page);
            PDType0Font font = PDType0Font.load(newDoc, new File("src/main/resources/fonts/NotoSans-Regular.ttf"));
            contentStream.setFont(font, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(50, 700);

            // Split text into lines if necessary
            String[] lines = newText.split("\n");
            for (String line : lines) {
                contentStream.showText(line);
                contentStream.newLineAtOffset(0, -15); // Move down for next line
            }

            contentStream.endText();
            contentStream.close();

            newDoc.save(outputPath);
        }
    }


    // redactHIPAA with a reference pdf
    public static void redactHIPAARef(OroPDFDocument pdf, String outputPath, OroPDFDocument template) throws IOException {
        PDDocument filledDoc = pdf.copyDocument();
        PDDocument templateDoc = template.copyDocument();
        PDDocument redactedDoc = new PDDocument();
        ITesseract tesseract = new Tesseract();

        // Optional: set your actual tessdata path if needed
        tesseract.setDatapath("/usr/local/share/tessdata/");
        tesseract.setLanguage("eng");

        PDFRenderer filledRenderer = new PDFRenderer(filledDoc);
        PDFRenderer templateRenderer = new PDFRenderer(templateDoc);

        for (int i = 0; i < filledDoc.getNumberOfPages(); i++) {
            // 1. OCR the filled page
            BufferedImage filledImage = filledRenderer.renderImageWithDPI(i, 300, ImageType.RGB);
            List<Word> words = tesseract.getWords(filledImage, ITessAPI.TessPageIteratorLevel.RIL_WORD);

            // Necessary annotations for Stanford NLP
            StringBuilder fullfilledText = new StringBuilder();
            for (Word word : words) {
                fullfilledText.append(word.getText()).append(" ");
            }

            BufferedImage templateImage = templateRenderer.renderImageWithDPI(i, 300, ImageType.RGB);
            List<Word> templateWords = tesseract.getWords(templateImage, ITessAPI.TessPageIteratorLevel.RIL_WORD);


            List<Word> redactionCandidates = getDifferentWords(words, templateWords);

            // Setup NLP
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

            // Annotate with CoreNLP
            Annotation annotation = new Annotation(fullfilledText.toString());
            pipeline.annotate(annotation);

            // Draw black boxes over PII
            Graphics2D g2d = filledImage.createGraphics();
            g2d.setColor(Color.BLACK);

            for (Word word : redactionCandidates) {
                if (RedactionUtils.isHIPAAPIITemplate(word.getText(), annotation)) {
                    g2d.fillRect(word.getBoundingBox().x, word.getBoundingBox().y, word.getBoundingBox().width, word.getBoundingBox().height);
                }
            }

            g2d.dispose();
            // Add modified image to new PDF
            PDPage newPage = new PDPage(PDRectangle.LETTER);
            redactedDoc.addPage(newPage);
            PDImageXObject pdImage = LosslessFactory.createFromImage(redactedDoc, filledImage);
            try (PDPageContentStream contents = new PDPageContentStream(redactedDoc, newPage)) {
                PDRectangle mediaBox = newPage.getMediaBox();
                float scale = mediaBox.getWidth() / filledImage.getWidth();  // assumes full-page size match
                contents.drawImage(pdImage, 0, 0, filledImage.getWidth() * scale, filledImage.getHeight() * scale);

            }

        }
        redactedDoc.save(outputPath);
        redactedDoc.close();
        filledDoc.close();
        templateDoc.close();
    }

    public static List<Word> getDifferentWords(List<Word> words, List<Word> templateWords) {
        List<Word> newWords = new ArrayList<>();
        List<String> templateGetWords = new ArrayList<>();
        List<Rectangle> templateGetRectangles = new ArrayList<>();

        for (Word word : templateWords) {
            templateGetWords.add(word.getText());
            templateGetRectangles.add(word.getBoundingBox());
        }
        for (Word word : words) {
            if (!(templateGetWords.contains(word.getText()) & templateGetRectangles.contains(word.getBoundingBox()))) {
                newWords.add(word);
            }
        }
        return newWords;
    }






    public static void redactHIPAA(OroPDFDocument pdf, String outputPath) throws IOException {
        PDDocument doc = pdf.copyDocument();
        PDFRenderer renderer = new PDFRenderer(doc);
        PDDocument redactedDoc = new PDDocument();

        ITesseract tesseract = new Tesseract();
        // Optional: set language or data path
        tesseract.setDatapath("/usr/local/share/tessdata/");
        tesseract.setLanguage("eng");

        for (int i = 0; i < doc.getNumberOfPages(); i++) {
            BufferedImage image = renderer.renderImageWithDPI(i, 300, ImageType.RGB);
            List<RedactedWord> piiList = extractWordsWithCoordinates(image);
//            for (Map.Entry<String, Rectangle> entry : extractWordsWithCoordinates(image).entrySet()) {
//                System.out.println("REDACT: " + entry.getKey() + " at " + entry.getValue());
//            }

            // Draw black boxes over PII
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(Color.BLACK);
            for (RedactedWord word : piiList) {
                g2d.fillRect(word.bounds.x, word.bounds.y, word.bounds.width, word.bounds.height);
            }
            g2d.dispose();

            // Add modified image to new PDF
            PDPage newPage = new PDPage(PDRectangle.LETTER);
            redactedDoc.addPage(newPage);
            PDImageXObject pdImage = LosslessFactory.createFromImage(redactedDoc, image);
            try (PDPageContentStream contents = new PDPageContentStream(redactedDoc, newPage)) {
                PDRectangle mediaBox = newPage.getMediaBox();
                float scale = mediaBox.getWidth() / image.getWidth();  // assumes full-page size match
                contents.drawImage(pdImage, 0, 0, image.getWidth() * scale, image.getHeight() * scale);
            }

        }

        redactedDoc.save(outputPath);
        redactedDoc.close();
        doc.close();
    }

    public static List<RedactedWord> extractWordsWithCoordinates(BufferedImage image) {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("/usr/local/share/tessdata/");  // Set the correct path to tessdata
        tesseract.setLanguage("eng");  // Set language

        List<RedactedWord> piiList = new ArrayList<>();

        try {
            // Get all words with their bounding boxes
            List<Word> words = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD);

            // Necessary annotations for Stanford NLP
            StringBuilder fullText = new StringBuilder();
            for (Word word : words) {
                fullText.append(word.getText()).append(" ");
            }

            // Setup NLP
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
            StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

            // Annotate with CoreNLP
            Annotation annotation = new Annotation(fullText.toString());
            pipeline.annotate(annotation);

            // Debug to see the annotation for each token
            System.out.println("NER Token Annotations:");
            for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    String tokenText = token.originalText();
                    String nerTag = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);

                    System.out.println(tokenText + " -> " + nerTag);
                }
            }

            // Iterate over each word and check the current word and the next word for PII
            for (Word word : words) {
                String currentText = word.getText();

                // Check current word for PII
                if (RedactionUtils.isHIPAAPII(currentText, annotation)) {
                    System.out.println(currentText + "should be redacted");
                    piiList.add(new RedactedWord(currentText, word.getBoundingBox()));
                }
            }

        } catch (Exception e) {
            System.err.println("OCR failed: " + e.getMessage());
        }
        return piiList;
    }

    private static Rectangle combineRectangles(Rectangle r1, Rectangle r2) {
        int x = Math.min(r1.x, r2.x);
        int y = Math.min(r1.y, r2.y);
        int width = Math.max(r1.x + r1.width, r2.x + r2.width) - x;
        int height = Math.max(r1.y + r1.height, r2.y + r2.height) - y;
        return new Rectangle(x, y, width, height);
    }

    // Auxilary class for the piiList
    static class RedactedWord {
        String text;
        Rectangle bounds;

        RedactedWord(String text, Rectangle bounds) {
            this.text = text;
            this.bounds = bounds;
        }
    }

    // Auxilary class for HIPAA PII regex matching
    public class RedactionUtils {

        // Patterns for PII (simplified and expandable)
        private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
        private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(?:\\+1[-.\\s]?)?(\\(?\\d{3}\\)?[-.\\s]?)?\\d{3}[-.\\s]?\\d{4}\\b");
        private static final Pattern ALT_PHONE_PATTERN = Pattern.compile("((\\(\\d{3}\\) ?)|(\\d{3}-))?\\d{3}-\\d{4}");
        private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z]{2,6}\\b", Pattern.CASE_INSENSITIVE);
        private static final Pattern DATE_PATTERN = Pattern.compile("\\b\\d{1,2}/\\d{1,2}/\\d{2,4}\\b");
        private static final Pattern MRN_PATTERN = Pattern.compile("\\bMRN\\s*\\d{6,}\\b");
        private static final Pattern ZIP_PATTERN = Pattern.compile("^\\d{5}(?:-\\d{4})?$");

        /**
         * Checks if a given word or phrase matches any HIPAA-sensitive category.
         */
        public static boolean isHIPAAPII(String word, Annotation fullTextAnnotation) {
            String trimmed = word.trim();
            Set<String> whiteList = Set.of("information", "healthcare", "health", "legal", "representative", "medical", "disability", "insurance", "drug", "alcohol", "abuse");

            // Check regex patterns
            if (SSN_PATTERN.matcher(trimmed).find()) return true;
            if (PHONE_PATTERN.matcher(trimmed).find()) return true;
            if (ALT_PHONE_PATTERN.matcher(trimmed).find()) return true;
            if (EMAIL_PATTERN.matcher(trimmed).find()) return true;
            if (DATE_PATTERN.matcher(trimmed).find()) return true;
            if (MRN_PATTERN.matcher(trimmed).find()) return true;
            if (ZIP_PATTERN.matcher(trimmed).find()) return true;

            // Use Stanford NER to check for named entities
            for (CoreMap sentence : fullTextAnnotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    String tokenText = token.originalText();
                    String ner = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);

                    if (tokenText.equalsIgnoreCase(trimmed)) {
                        if ((ner.equals("PERSON") || ner.equals("LOCATION") || ner.equals("DATE") || ner.equals("STATE_OR_PROVINCE") || ner.equals("ORGANIZATION") || ner.equals("CITY")) && !whiteList.contains(word.toLowerCase())) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        public static boolean isHIPAAPIITemplate(String word, Annotation fullTextAnnotation) {
            String trimmed = word.trim();
            Set<String> whiteList = Set.of("information", "healthcare", "health", "legal", "representative", "medical", "disability", "insurance", "drug", "alcohol", "abuse");

            // Check regex patterns
            if (SSN_PATTERN.matcher(trimmed).find()) return true;
            if (PHONE_PATTERN.matcher(trimmed).find()) return true;
            if (ALT_PHONE_PATTERN.matcher(trimmed).find()) return true;
            if (EMAIL_PATTERN.matcher(trimmed).find()) return true;
            if (DATE_PATTERN.matcher(trimmed).find()) return true;
            if (MRN_PATTERN.matcher(trimmed).find()) return true;
            if (ZIP_PATTERN.matcher(trimmed).find()) return true;

            // Use Stanford NER to check for named entities
            for (CoreMap sentence : fullTextAnnotation.get(CoreAnnotations.SentencesAnnotation.class)) {
                for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                    String tokenText = token.originalText();
                    String ner = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                    System.out.println("Word: " + tokenText + "Annotation: " + ner);

                    if (tokenText.equalsIgnoreCase(trimmed)) {
                        if ((ner.equals("PERSON") || ner.equals("LOCATION") || ner.equals("DATE") || ner.equals("STATE_OR_PROVINCE") || ner.equals("ORGANIZATION") || ner.equals("CITY") || ner.equals("NUMBER")) && (!whiteList.contains(word.toLowerCase()))) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }
    }






    public void saveTextAsPDF(String text, String outputPath) throws IOException {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage();
        doc.addPage(page);

        PDPageContentStream stream = new PDPageContentStream(doc, page);
        PDType0Font font = PDType0Font.load(doc, new File("src/main/resources/fonts/NotoSans-Regular.ttf"));
        stream.beginText();
        stream.setFont(font, 12);
        stream.newLineAtOffset(50, 700);

        String[] lines = text.split("\n");
        for (String line : lines) {
            stream.showText(line);
            stream.newLineAtOffset(0, -15);
        }

        stream.endText();
        stream.close();
        doc.save(outputPath);
        doc.close();
    }

    public PDDocument copyDocument() throws IOException {
        // Save to memory and reload to make a copy
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        this.document.save(out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        return PDDocument.load(in);
    }


}
