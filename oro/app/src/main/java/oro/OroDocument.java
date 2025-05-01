// Oro .txt document class

package oro;

import java.io.IOException;
import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;
import java.lang.StringBuilder;

public class OroDocument {
    private String filePath;
    private Map<String, Object> metadata;
    private String textContent;


    
    public OroDocument(String filePath) throws IOException {
        this.filePath = filePath;
        this.metadata = new HashMap<>();
        this.textContent = "";
    }

    public OroDocument() throws IOException {
        this.filePath = null;
        this.metadata = new HashMap<>();
        this.textContent = "";
    }

    public String getDocText(String filePath){
        File file = new File(filePath);
        StringBuilder sb = new StringBuilder();
        try (Scanner scanner = new Scanner(file)){
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                sb.append(line);
            }
            return sb.toString();
        }
        catch (FileNotFoundException e) {
            return "Error: File not found " + e.getMessage();
        }
    }

    // Not really needed, since redacted documents serve as copies, not replace the original file
    public int replaceDocText(String filePath, String replacementText){
        File file = new File(filePath);
        List<String> lines = new ArrayList<>();
        try{
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                lines.add(line.replaceAll(line, replacementText));
            }
            scanner.close();
        }
        catch (FileNotFoundException e){
            throw new RuntimeError("Error: File not found " + e.getMessage());
        }

        try {
            FileWriter writer = new FileWriter(file);
            for (String line : lines) {
                writer.write(line + System.lineSeparator());
            }
            writer.close();
            return 1;
        }
        catch (IOException e){
            throw new RuntimeError("Error: " + e.getMessage());
        }
    }

    // Create a document with the provided filepath and text ==> For creating redacted documents mainly
    public static int createDoc(String filePath, String newText){
        File file = new File(filePath);
        
        try{
            file.createNewFile();
            FileWriter fileWriter = new FileWriter(filePath);
            fileWriter.write(newText);
            fileWriter.close();
            return 1;
        }
        catch(IOException e){
            throw new RuntimeError("Error: " + e.getMessage());
        }

    }


    public String getFilePath(){
        return filePath;
    }


}
