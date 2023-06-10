import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Server {
    final private int port;

    public PrintWriter output;
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
        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            
            while (true) {
                // ожидаем подключения
                Socket socket = serverSocket.accept();

                // для подключившегося клиента открываем потоки 
                // чтения и записи
                this.input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                this.outputb = socket.getOutputStream();
                this.output = new PrintWriter(outputb);

                // ждем первой строки запроса
                while (!input.ready()) ;

                // считываем и печатаем все что было отправлено клиентом
                final String requestText = DataOperator.decodeURL(input.readLine());
                final String requestedResource = requestText.split(" ")[1].substring(1);
                
                final String ip = socket.getInetAddress().toString().substring(1);
            
                boolean proceed = true;

                // Подключаем смотрителя, если он есть
                if (overwatch!=null) {
                    if (!overwatch.checkpoint(requestedResource, ip, requestText)) {
                        proceed = false;
                    }
                }

                if (proceed) {
                    // Если команда
                    if (requestedResource.contains("CMD<>")) {
                        String cmd = requestedResource.split("CMD")[0] + requestedResource.split("<>")[1];
                        if (this.commands.containsKey(cmd)) {
                            this.commands.get(cmd).response(requestedResource, ip);
                        } else {
                            sendResponse("NONE");
                        }
                    } 
                    // Если не команда
                    else {
                        // Если ресурс существует
                        if (this.responses.containsKey(requestedResource)) {
                            // Выполняем обработчик события
                            this.responses.get(requestedResource).response(requestedResource, ip, requestText);
                        // Иначе смотрим isStart
                        } else {
                            boolean isFinded = false;

                            // Ищем тот обработчик, с которого начинается запрос
                            for (Map.Entry<String, ResponseAction> el: responses.entrySet()) {
                                if (!el.getKey().equals("") && requestedResource.startsWith(el.getKey())) {
                                    el.getValue().response(requestedResource, ip, requestText);
                                    isFinded = true;
                                    break;
                                }
                            }
                            
                            // Иначе отправляем 404
                            if (!isFinded) {
                                send404Response(requestedResource, ip, requestText);
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public final void closeConnection() {
        this.output.close();
        try {
            this.input.close();
            this.outputb.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final void sendBaseResponse(String status, String contentType, String content) {
        output.println("HTTP/2 "+status);
        output.println("Content-Type: "+contentType+"; charset=utf-8");
        output.println();
        output.println(content);
        output.flush();
        closeConnection();
    }

    private final void sendBaseResponse(String status, String contentType, byte[] content) {
        try {
            try {
                outputb.write(("HTTP/2 "+status+"\n").getBytes());
                outputb.write(("Content-Type: "+contentType+"; charset=utf-8\n").getBytes());
                outputb.write("\n".getBytes());
                outputb.write(content);
                outputb.flush();
            } catch (SocketException e) {
                System.out.println("Connection terminated by client");
            }
        } catch (IOException e) {e.printStackTrace();}
        closeConnection();
    }

    public final void send404Response(String resource, String ip, String request) {
        ResponseAction res;
        if ((res = responses.get("404")) != null) {
            res.response(resource, ip, request);
        } else {
            sendBaseResponse("404 Not Found", "text/html", "Error 404: Not Found");
        }
        closeConnection();
    }

    public final void sendResponse(String status, String contentType, String content) {
        sendBaseResponse(status, contentType, content);
    }

    public final void sendResponse(String content) {
        sendBaseResponse("200 OK", "text/html", content);
    }

    public final void sendResponse(String contentType, byte[] content) {
        sendBaseResponse("200 OK", contentType, content);
    }
    public final void sendResponse(String contentType, String path) {
        new Thread(new _ResponseAction(outputb, contentType, path)).start();
    }
}

final class _ResponseAction implements Runnable {
    final OutputStream output;
    final String path;
    final String contentType;

    public _ResponseAction(OutputStream output, String contentType, String path) {
        this.output = output;
        this.contentType = contentType;
        this.path = path;
    }

    public final void run() {
        try {
            try {
                output.write(("HTTP/2 200 OK\n").getBytes());
                output.write(("Content-Type: "+contentType+"; charset=utf-8\n").getBytes());
                output.write("\n".getBytes());

                byte[] buffer = new byte[3072];
                FileInputStream in = new FileInputStream(DataOperator.projectPath+path);
                int rc = in.read(buffer);
                while(rc != -1) {
                    output.write(buffer);
                    rc = in.read(buffer); 
                }
                in.close();
                
                output.flush();
            } catch (SocketException e) {
                System.out.println("Connection terminated by client");
            }
            output.close();
        } catch (IOException e) {e.printStackTrace();}
    }
}
