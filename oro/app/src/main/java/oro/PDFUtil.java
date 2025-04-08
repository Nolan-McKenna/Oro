package oro;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

public class PDFUtil {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java PdfReaderTest <path-to-pdf>");
            return;
        }

        String pdfPath = args[0];
        File pdfFile = new File(pdfPath);

        if (!pdfFile.exists()) {
            System.out.println("File not found: " + pdfPath);
            return;
        }

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            System.out.println(text);
        } catch (IOException e) {
            System.err.println("Error reading PDF: " + e.getMessage());
        }
    }
}

