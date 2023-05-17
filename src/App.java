import java.util.ArrayList;
import java.util.Arrays;

public class App {
    public static void main(String[] args) throws Exception {
        Server server = new Server(1623);
        final ArrayList<String> IMAGES_EXTENSIONS = new ArrayList<String>(Arrays.asList("jpeg", "jpg", "png", "webm"));

        DataOperator.projectPath = "/home/kvdl/Projects/HomeWebsite/resources/";
        
        server.addRequestHandler("/", new ResponseAction(false, (String resource, String ip, String request) -> {
            server.sendResponse(DataOperator.buildPage("main"));
            return true;
        }));

        server.addRequestHandler("/media/", new ResponseAction(true, (String res, String ip, String request) -> {
            String pathToRes = res.substring(7);
                                
            // Если файл существует то ищем расширение в списке
            if (DataOperator.isFileExist(pathToRes)) {
                String ext = pathToRes.substring(pathToRes.lastIndexOf('.')+1);
                String contentType = "text/html";
                if (ext.equals("mp4")) {
                    contentType = "video/mp4";
                } else if (IMAGES_EXTENSIONS.contains(ext)) {
                    contentType = "images/"+ext;
                }
                server.sendResponse(contentType, DataOperator.readFileAsBytes(pathToRes));
            } else {
                server.send404Response(res, ip, request);
            }

            return true;
        }));

        server.start();
    }
}
