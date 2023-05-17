import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DataOperator {
    // Путь задаётся в главном файле
    public static String projectPath;

    public static String readFile(String path) {
        String data = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(projectPath+path));
            String s;

            while((s = reader.readLine())!= null) {
                data+=s;
            }
            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public static byte[] readFileAsBytes(String file_path) {
        Path path = Paths.get(projectPath+file_path);
        byte[] data;
        try {
            data = Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
            data = null;
        }
        return data;
    }

    public static boolean isFileExist(String path) {
        File res = new File(projectPath+path);
        if (res.exists() && !res.isDirectory()) {
            return true;
        }
        return false;
    }

    public static String buildPage(String path) {
        File t = new File(projectPath+path);
        if (t.exists() && t.isDirectory()) {
            String result = readFile(path+"/index.html");
            if (isFileExist(path+"/main.js")) {
                int edge = result.lastIndexOf("</body>");
                result = result.substring(0, edge)+"<script>"+readFile(path+"/main.js")+"</script>"+result.substring(edge);
            }

            if (isFileExist(path+"/style.css")) {
                int edge = result.lastIndexOf("</head>");
                result = result.substring(0, edge)+"<style>"+readFile(path+"/style.css")+"</style>"+result.substring(edge);
            }
            
            return result;
        } else {
            return "You don't supposed to see it. Please report developer about this.";
        }
    }

}
