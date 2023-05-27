import java.io.BufferedReader;
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

    public Server(int port) {
        this.port = port;
    }

    public final void addRequestHandler(String requestedResourse,  boolean isStart, Action handler) {
        this.responses.put(requestedResourse, new ResponseAction(handler, isStart));
    }

    public final void addRequestCMDHandler(String requestedResourse, boolean isStart, ActionCMD handler) {
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
                final String requestText = input.readLine();
                final String requestedResource = requestText.split(" ")[1].replace("%20", " ").replace("%3C%3E", "<>").substring(1);
                
                final String ip = socket.getInetAddress().toString().substring(1);
            

                ResponseAction overwatch;
                boolean proceed = true;

                // Подключаем смотрителя, если он есть
                if ((overwatch = this.responses.get("OVERWATCH"))!=null) {
                    if (!overwatch.response(requestedResource, ip, requestText)) {
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
            this.outputb.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public final void send404Response(String resource, String ip, String request) {
        ResponseAction res;
        if ((res = responses.get("404")) != null) {
            res.response(resource, ip, request);
        } else {
            output.println("HTTP/2 404 Not Found");
            output.println("Content-Type: text/html; charset=utf-8");
            output.println();
            output.println("Error 404");
            output.flush();
        }
        closeConnection();
    }

    public final void sendResponse(String status, String contentType, String content) {
        output.println("HTTP/2 "+status);
        output.println("Content-Type: "+contentType+"; charset=utf-8");
        output.println();
        output.println(content);
        output.flush();
        closeConnection();
    }

    public final void sendResponse(String content) {
        output.println("HTTP/2 200 OK");
        output.println("Content-Type: text/html; charset=utf-8");
        output.println();
        output.println(content);
        output.flush();
        closeConnection();
    }

    public final void sendResponse(String status, String contentType, byte[] content) {
        try {
            outputb.write(("HTTP/2 "+status+"\n").getBytes());
            outputb.write(("Content-Type: "+contentType+"; charset=utf-8\n").getBytes());
            outputb.write("\n".getBytes());
            outputb.write(content);
            outputb.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        closeConnection();
    }

    public final void sendResponse(String contentType, byte[] content) {
        try {
            try {
                outputb.write(("HTTP/2 200 OK\n").getBytes());
                outputb.write(("Content-Type: "+contentType+"; charset=utf-8\n").getBytes());
                outputb.write("\n".getBytes());
                outputb.write(content);
                outputb.flush();
            } catch (SocketException e) {
                System.out.println("Connection broken!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        closeConnection();
    }

    public final void sendResponseAsync(String contentType, String filepath) {
        new Thread(new asyncLoader(this, outputb, contentType, DataOperator.projectPath+filepath)).start();
    }

}
