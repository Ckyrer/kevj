import java.io.OutputStream;

public class TestApp {
    public static void main(String[] args) {
        DataOperator.projectPath+="resources/";
        System.out.println("Dir: "+DataOperator.projectPath);
        Server server = new Server(4444);
        System.out.println("Server started!");

        server.addOverwatchHandler((String res, String ip, String req) -> {
            System.out.println(res);
            return true;
        });

        server.addRequestHandler("", false, (String res, String ip, String req) -> {
            System.out.println(DataOperator.buildPage("pages/main"));
            server.sendResponse(DataOperator.buildPage("pages/main"));
        });

        server.addRequestCMDHandler("test", (String ip, String[] argus) -> {
            System.out.println(argus[1]);
        });

        server.addAsyncRequestHandler("media", true, (String res, String ip, String req, OutputStream out) -> {
            String filepath = "media/"+res.substring(5);
            String contentType = "image";
            String ext = filepath.substring(filepath.lastIndexOf(".")+1);
            if (ext.equals("mp4")) {contentType="video";}
            

            if (DataOperator.isFileExist(filepath)) {
                if (DataOperator.getFileSize(filepath)<10500000) {
                    System.out.println("Small file!" + res);
                    server.sendResponse(contentType+"/"+ext, DataOperator.readFileAsBytes(filepath));
                } else {
                    System.out.println("Big file!" + res);
                    server.sendResponse(contentType+"/"+ext, filepath, 3072, out);
                }
            } else {
                server.send404Response(res, ip, req);
            }
        });

        server.start();

    }
}
