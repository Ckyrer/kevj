import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    final private int port;

    public OutputStream outputb;
    public BufferedReader input;

    final private Map<String, ResponseAction> responses = new HashMap<String, ResponseAction>();
    final private Map<String, ResponseCMDAction> commands = new HashMap<String, ResponseCMDAction>();
    private Overwatch overwatch = null;

    public Server(int port) {
        this.port = port;
    }

    public final void addOverwatchHandler(Overwatch overwatch) {
        this.overwatch = overwatch;
    }

    public final void addRequestHandler(String requestedResourse,  boolean isStart, Action handler) {
        this.responses.put(requestedResourse, new ResponseAction(handler, isStart));
    }

    public final void addRequestCMDHandler(String requestedResourse, ActionCMD handler) {
        this.commands.put(requestedResourse, new ResponseCMDAction(handler));
    }

    public final void start() {
        try {
            try (ServerSocket serverSocket = new ServerSocket(this.port)) {
                
                while (true) {
                    // ожидаем подключения
                    Socket socket = serverSocket.accept();
                    
                    new Thread( new ResponseThread(responses, commands, overwatch, socket.getOutputStream(), socket.getInputStream(), socket.getInetAddress().toString().substring(1)) ).start();
                
                }
            }
        } catch (IOException e) {e.printStackTrace();}
    }
}
