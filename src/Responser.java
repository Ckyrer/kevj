import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;

public class Responser {
    private final OutputStream out;
    private final ResponseThread parent;

    public Responser(OutputStream out, ResponseThread parent) {
        this.out = out;
        this.parent = parent;
    }

    // --------------------------------------------------
    // send responses
    // --------------------------------------------------

    // Эта так скажем основа, это база
    private final void sendBaseResponse(String status, String contentType, byte[] content) {
        try {
            try {
                out.write( ("HTTP/2 "+status+"\n").getBytes() );
                out.write( ("Content-Type: "+contentType+"; charset=utf-8\n").getBytes() );
                out.write( "\n".getBytes() );
                out.write( content );
                out.flush();
            } catch (SocketException e) {
                System.out.println("Соединение разорвано");
            }
        } catch (IOException e) {e.printStackTrace();}
    }

    // Отправить заданный статус и тип
    public final void sendResponse(String status, String contentType, String content) {
        sendBaseResponse(status, contentType, content.getBytes());
    }

    // Отправить 200 OK html 
    public final void sendResponse(String content) {
        sendBaseResponse("200 OK", "text/html", content.getBytes());
    }

    // Отправить массив байтов
    public final void sendResponse(String contentType, byte[] content) {
        sendBaseResponse("200 OK", contentType, content);
    }

    // Отправить частями
    public final void sendResponse(String contentType, String path, int bufferSize) {
        try {
            try {
                out.write(("HTTP/2 200 OK\n").getBytes());
                out.write(("Content-Type: "+contentType+"; charset=utf-8\n").getBytes());
                out.write("\n".getBytes());
                
                try ( FileInputStream in = new FileInputStream(DataOperator.projectPath+path) ) {
                    byte[] buffer = new byte[3072];
                
                    int rc = in.read(buffer);
                    while(rc != -1) {
                        out.write(buffer);
                        rc = in.read(buffer); 
                    }
                }
                
                out.flush();
            } catch (SocketException e) {
                System.out.println("Соединение разорвано");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 404
    public final void send404Response(String res, String ip, String req) {
        parent.send404Response(res, ip, req, this);
    }

}
