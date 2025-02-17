// package oro;

// // TODO: Setup build/dependencies with install for Apache PDFTextStripper
// import org.apache.pdfbox.pdmodel.PDDocument;
// import org.apache.pdfbox.text.PDFTextStripper;
// import java.io.File;
// import java.io.IOException;

// public class PDFUtil {
//     public static String extractText(String filePath) {
//         File file = new File(filePath);
//         if (!file.exists()) {
//             throw new RuntimeException("File not found: " + filePath);
//         }
//         try (PDDocument document = PDDocument.load(file)) {
//             return new PDFTextStripper().getText(document);
//         } catch (IOException e) {
//             throw new RuntimeException("Error reading PDF: " + e.getMessage());
//         }
//     }
// }

