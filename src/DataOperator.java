import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DataOperator {
    // Путь задаётся в главном файле
    public static String projectPath = new File("").getAbsolutePath()+"/";

    public static String readFile(String path) {
        String data = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(projectPath+path));
            String s;

            while((s = reader.readLine())!= null) {
                data+=s+"\n";
            }
            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public static long getFileSize(String path) {
        try {
            return Files.size(new File(projectPath+path).toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
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
            return "You don't supposed to see it. Please report developer about this. (build failed)";
        }
    }

    public static String decodeURL(String url) {
        return URLDecoder.decode(url, StandardCharsets.UTF_8);
    }

    public static String encodeURL(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

}
