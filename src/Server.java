import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
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

    public final void addAsyncRequestHandler(String requestedResourse,  boolean isStart, AsyncAction handler) {
        this.responses.put(requestedResourse, new ResponseAction(handler, isStart));
    }

    public final void addRequestCMDHandler(String requestedResourse, ActionCMD handler) {
        this.commands.put(requestedResourse, new ResponseCMDAction(handler));
    }

    public final void addAsyncRequestCMDHandler(String requestedResourse, AsyncActionCMD handler) {
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

                // ждем первой строки запроса
                while (!input.ready()) ;

                // считываем и печатаем все что было отправлено клиентом
                final String requestText = DataOperator.decodeURL(input.readLine());
                final String requestedResource = requestText.substring(requestText.indexOf(" ")+2, requestText.lastIndexOf(" "));
                
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
                            ResponseCMDAction c = this.commands.get(cmd);
                            if (c.isAsync) {
                                this.commands.get(cmd).response(requestedResource, ip, outputb, input);
                            } else {
                                this.commands.get(cmd).response(requestedResource, ip);
                            }
                        } else {
                            sendResponse("NONE");
                        }
                    } 
                    // Если не команда
                    else {
                        // Если ресурс существует
                        if (this.responses.containsKey(requestedResource)) {
                            // Выполняем обработчик события
                            ResponseAction act = this.responses.get(requestedResource);
                            if (act.isAsync) {
                                act.response(requestedResource, ip, requestText, outputb, input);
                            } else {
                                act.response(requestedResource, ip, requestText);
                            }
                        // Иначе смотрим isStart
                        } else {
                            boolean isFinded = false;

                            // Ищем тот обработчик, с которого начинается запрос
                            for (Map.Entry<String, ResponseAction> el: responses.entrySet()) {
                                if (!el.getKey().equals("") && requestedResource.startsWith(el.getKey())) {
                                    if (el.getValue().isAsync) {
                                        el.getValue().response(requestedResource, ip, requestText, outputb, input);
                                    } else {
                                        el.getValue().response(requestedResource, ip, requestText);
                                    }
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
        try {
            this.input.close();
            this.outputb.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --------------------------------------------------
    // send responses
    // --------------------------------------------------

    // Эта так скажем основа, это база
    private final void sendBaseResponse(String status, String contentType, byte[] content) {
        try {
            try {
                outputb.write( ("HTTP/2 "+status+"\n").getBytes() );
                outputb.write( ("Content-Type: "+contentType+"; charset=utf-8\n").getBytes() );
                outputb.write( "\n".getBytes() );
                outputb.write( content );
                outputb.flush();
            } catch (SocketException e) {
                System.out.println("Соединение разорвано");
            }
        } catch (IOException e) {e.printStackTrace();}
        
        closeConnection();
    }

    // Отправить 404
    public final void send404Response(String resource, String ip, String request) {
        ResponseAction res;
        if ((res = responses.get("404")) != null) {
            res.response(resource, ip, request);
        } else {
            sendBaseResponse("404 Not Found", "text/html", "Error 404: Not Found");
        }
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



    // --------------------------------------------------
    // send async responses
    // --------------------------------------------------
    
    // Отправить частями
    public final void sendResponseAsync(String contentType, String path, int bufferSize, OutputStream out) {
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
                e.printStackTrace();
                System.out.println("Соединение разорвано");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

