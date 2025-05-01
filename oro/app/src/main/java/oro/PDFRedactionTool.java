/**
 * PDFRedactionTool.java - A tool for intelligent redaction of PHI in medical documents
 * Uses Apache PDFBox for PDF processing and combines regex with contextual rules
 */

package oro;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDFRedactionTool {

    private static final String REDACTION_TEXT = "███████";

    // Standard PHI patterns
    private static final Map<String, Pattern> PHI_PATTERNS = new HashMap<>();
    static {
        // Common PHI regex patterns
        PHI_PATTERNS.put("SSN", Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"));
        PHI_PATTERNS.put("PHONE", Pattern.compile("\\b\\(\\d{3}\\)\\s*\\d{3}-\\d{4}\\b|\\b\\d{3}-\\d{3}-\\d{4}\\b"));
        PHI_PATTERNS.put("DATE", Pattern.compile("\\b\\d{1,2}/\\d{1,2}/\\d{2,4}\\b|\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{1,2},\\s+\\d{4}\\b"));
        PHI_PATTERNS.put("EMAIL", Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}\\b"));
        PHI_PATTERNS.put("ZIPCODE", Pattern.compile("\\b\\d{5}(-\\d{4})?\\b"));
    }

    // Context patterns that indicate patient information
    private static final List<Pattern> PATIENT_CONTEXT_PATTERNS = Arrays.asList(
            Pattern.compile("(?i)patient\\s*name\\s*:?\\s*([^\\n:]{2,50})"),
            Pattern.compile("(?i)patient\\s*address\\s*:?\\s*([^\\n:]{5,100})"),
            Pattern.compile("(?i)patient\\s*id\\s*:?\\s*([^\\n:]{2,30})"),
            Pattern.compile("(?i)date\\s*of\\s*birth\\s*:?\\s*([^\\n:]{6,20})")
    );

    // Context patterns that indicate non-patient information (should not be redacted)
    private static final List<Pattern> NON_REDACTABLE_CONTEXT_PATTERNS = Arrays.asList(
            Pattern.compile("(?i)doctor\\s*name\\s*:?\\s*([^\\n:]{2,50})"),
            Pattern.compile("(?i)physician\\s*:?\\s*([^\\n:]{2,50})"),
            Pattern.compile("(?i)hospital\\s*:?\\s*([^\\n:]{2,100})"),
            Pattern.compile("(?i)clinic\\s*address\\s*:?\\s*([^\\n:]{5,100})"),
            Pattern.compile("(?i)provider\\s*:?\\s*([^\\n:]{2,50})")
    );

    /**
     * Main method to process a PDF document and redact sensitive information
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java PDFRedactionTool <input_pdf> <output_pdf>");
            return;
        }

        String inputPath = args[0];
        String outputPath = args[1];

        try {
            redactDocument(inputPath, outputPath);
            System.out.println("Redaction completed successfully. Output saved to: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error processing PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Process PDF document and apply redactions
     */
    public static void redactDocument(String inputPath, String outputPath) throws IOException {
        File inputFile = new File(inputPath);
        PDDocument document = PDDocument.load(inputFile);

        try {
            // Extract text and identify portions to redact
            List<RedactionTarget> redactionTargets = identifyRedactionTargets(document);

            // Apply redactions
            applyRedactions(document, redactionTargets);

            // Save the redacted document
            document.save(outputPath);
        } finally {
            document.close();
        }
    }

    /**
     * Identifies text that should be redacted based on patterns and context
     */
    private static List<RedactionTarget> identifyRedactionTargets(PDDocument document) throws IOException {
        List<RedactionTarget> targets = new ArrayList<>();

        // First pass: extract all text with position information
        PositionalTextStripper stripper = new PositionalTextStripper();
        stripper.setSortByPosition(true);
        stripper.setStartPage(1);
        stripper.setEndPage(document.getNumberOfPages());

        stripper.getText(document);
        List<TextWithPosition> textPositions = stripper.getTextPositions();

        // Convert text positions to string for context analysis
        StringBuilder fullText = new StringBuilder();
        for (TextWithPosition tp : textPositions) {
            fullText.append(tp.getText());
        }
        String documentText = fullText.toString();

        // Identify non-redactable areas (doctor names, hospital info)
        Set<TextRange> protectedRanges = new HashSet<>();
        for (Pattern pattern : NON_REDACTABLE_CONTEXT_PATTERNS) {
            Matcher matcher = pattern.matcher(documentText);
            while (matcher.find()) {
                if (matcher.groupCount() >= 1) {
                    int start = matcher.start(1);
                    int end = matcher.end(1);
                    protectedRanges.add(new TextRange(start, end));
                }
            }
        }

        // Find patient context information
        Set<TextRange> patientInfoRanges = new HashSet<>();
        for (Pattern pattern : PATIENT_CONTEXT_PATTERNS) {
            Matcher matcher = pattern.matcher(documentText);
            while (matcher.find()) {
                if (matcher.groupCount() >= 1) {
                    int start = matcher.start(1);
                    int end = matcher.end(1);
                    patientInfoRanges.add(new TextRange(start, end));
                }
            }
        }

        // Process standard PHI patterns
        for (Map.Entry<String, Pattern> entry : PHI_PATTERNS.entrySet()) {
            Pattern pattern = entry.getValue();
            Matcher matcher = pattern.matcher(documentText);

            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                TextRange range = new TextRange(start, end);

                // Skip if in a protected range
                boolean isProtected = false;
                for (TextRange protectedRange : protectedRanges) {
                    if (protectedRange.overlaps(range)) {
                        isProtected = true;
                        break;
                    }
                }

                if (!isProtected) {
                    // Find which page and the position on the page
                    for (TextWithPosition textPos : textPositions) {
                        if (textPos.getTextRange().overlaps(range)) {
                            targets.add(new RedactionTarget(
                                    textPos.getPageIndex(),
                                    textPos.getX(),
                                    textPos.getY(),
                                    textPos.getWidth(),
                                    textPos.getHeight(),
                                    textPos.getText()
                            ));
                        }
                    }
                }
            }
        }

        // Process patient-specific information
        for (TextRange patientRange : patientInfoRanges) {
            // Skip if in a protected range
            boolean isProtected = false;
            for (TextRange protectedRange : protectedRanges) {
                if (protectedRange.overlaps(patientRange)) {
                    isProtected = true;
                    break;
                }
            }

            if (!isProtected) {
                for (TextWithPosition textPos : textPositions) {
                    if (textPos.getTextRange().overlaps(patientRange)) {
                        targets.add(new RedactionTarget(
                                textPos.getPageIndex(),
                                textPos.getX(),
                                textPos.getY(),
                                textPos.getWidth(),
                                textPos.getHeight(),
                                textPos.getText()
                        ));
                    }
                }
            }
        }

        return targets;
    }

    /**
     * Apply redactions to the document
     */
    private static void applyRedactions(PDDocument document, List<RedactionTarget> targets) throws IOException {
        // Group redactions by page
        Map<Integer, List<RedactionTarget>> redactionsByPage = new HashMap<>();

        for (RedactionTarget target : targets) {
            redactionsByPage.computeIfAbsent(target.getPageIndex(), k -> new ArrayList<>())
                    .add(target);
        }

        // Process each page
        for (Map.Entry<Integer, List<RedactionTarget>> entry : redactionsByPage.entrySet()) {
            int pageIndex = entry.getKey();
            List<RedactionTarget> pageRedactions = entry.getValue();

            // Check if pageIndex is valid
            if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
                System.err.println("Warning: Skipping invalid page index: " + pageIndex);
                continue;
            }

            PDPage page = document.getPage(pageIndex);
            PDRectangle pageSize = page.getMediaBox();

            PDPageContentStream contentStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true);

            // Set up appearance for redactions
            contentStream.setNonStrokingColor(Color.BLACK);

            // Create a half-transparent graphics state for text redaction
            PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
            graphicsState.setNonStrokingAlphaConstant(1.0f);
            contentStream.setGraphicsStateParameters(graphicsState);

            // Apply each redaction
            for (RedactionTarget redaction : pageRedactions) {
                // Draw a black rectangle over the text
                contentStream.addRect(
                        redaction.getX(),
                        pageSize.getHeight() - redaction.getY() - redaction.getHeight(),
                        redaction.getWidth(),
                        redaction.getHeight()
                );
                contentStream.fill();
            }

            contentStream.close();
        }
    }

    /**
     * Helper class to represent a range of text
     */
    private static class TextRange {
        private final int start;
        private final int end;

        public TextRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public boolean overlaps(TextRange other) {
            return !(this.end <= other.start || this.start >= other.end);
        }
    }

    /**
     * Helper class to store text with position information
     */
    private static class TextWithPosition {
        private final int pageIndex;
        private final float x;
        private final float y;
        private final float width;
        private final float height;
        private final String text;
        private final int textStart;
        private final int textEnd;

        public TextWithPosition(int pageIndex, float x, float y, float width,
                                float height, String text, int textStart) {
            this.pageIndex = pageIndex;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
            this.textStart = textStart;
            this.textEnd = textStart + text.length();
        }

        public int getPageIndex() { return pageIndex; }
        public float getX() { return x; }
        public float getY() { return y; }
        public float getWidth() { return width; }
        public float getHeight() { return height; }
        public String getText() { return text; }

        public TextRange getTextRange() {
            return new TextRange(textStart, textEnd);
        }
    }

    /**
     * Helper class to represent a target for redaction
     */
    private static class RedactionTarget {
        private final int pageIndex;
        private final float x;
        private final float y;
        private final float width;
        private final float height;
        private final String text;

        public RedactionTarget(int pageIndex, float x, float y, float width,
                               float height, String text) {
            this.pageIndex = pageIndex;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
        }

        public int getPageIndex() { return pageIndex; }
        public float getX() { return x; }
        public float getY() { return y; }
        public float getWidth() { return width; }
        public float getHeight() { return height; }
        public String getText() { return text; }
    }

    /**
     * Custom PDFTextStripper that captures positional information
     */
    private static class PositionalTextStripper extends PDFTextStripper {
        private List<TextWithPosition> textPositions = new ArrayList<>();
        private int currentPageIndex = 0;
        private int textOffset = 0;

        public PositionalTextStripper() throws IOException {
            super();
        }

        public List<TextWithPosition> getTextPositions() {
            return textPositions;
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            if (textPositions.isEmpty()) return;

            // Calculate the boundary box for this text
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = 0;
            float maxY = 0;

            for (TextPosition position : textPositions) {
                minX = Math.min(minX, position.getXDirAdj());
                minY = Math.min(minY, position.getYDirAdj());
                maxX = Math.max(maxX, position.getXDirAdj() + position.getWidthDirAdj());
                maxY = Math.max(maxY, position.getYDirAdj() + position.getHeightDir());
            }

            // Create a representation of this text with its position
            this.textPositions.add(new TextWithPosition(
                    currentPageIndex,  // Now correctly using 0-indexed page numbers
                    minX,
                    minY,
                    maxX - minX,
                    maxY - minY,
                    text,
                    textOffset
            ));

            textOffset += text.length();

            super.writeString(text, textPositions);
        }
    }
}