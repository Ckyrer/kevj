import java.util.ArrayList;
import java.util.Arrays;

public class App {
    public static void main(String[] args) throws Exception {
        Server server = new Server(1623);
        server.addDataTypes("image", new ArrayList<String>(Arrays.asList("jpeg", "jpg", "png", "webm")));
        server.addDataTypes("video", new ArrayList<String>(Arrays.asList("mp4")));

        DataOperator.projectPath = "/home/kvdl/Projects/HomeWebsite/resources/";
        
        server.addRequestHandler("/", (String resource, String ip, String request) -> {
            server.sendResponse(DataOperator.buildPage("main"));
            return true;
        });

        server.start();
    }
}
