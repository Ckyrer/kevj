import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

interface onRequestAction {
    boolean response(String resource, String ip, String request);
}

interface onRequestCMDAction {
    boolean response(String ip, String[] args);
}


public class Server {
    final private int port;
    private PrintWriter out;
    private OutputStream outb;
    final private Map<String, onRequestAction> responses = new HashMap<String, onRequestAction>();
    final private Map<String, onRequestCMDAction> commands = new HashMap<String, onRequestCMDAction>();
    final private Map<String, ArrayList<String>> dataTypes = new HashMap<String, ArrayList<String>>();

    public Server(int port) {
        this.port = port;
    }

    public final void addRequestHandler(String requestedResourse, onRequestAction handler) {
        this.responses.put(requestedResourse, handler);
    }

    public final void addRequestCMDHandler(String requestedResourse, onRequestCMDAction handler) {
        this.commands.put(requestedResourse, handler);
    }

    public final void addDataTypes(String type, ArrayList<String> exts) {
        this.dataTypes.put(type, exts);
    }

    public final void start() {
        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            System.out.println("Server started!");
            
            while (true) {
                // ожидаем подключения
                Socket socket = serverSocket.accept();

                // для подключившегося клиента открываем потоки 
                // чтения и записи
                try (
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    PrintWriter output = new PrintWriter(socket.getOutputStream());
                    OutputStream outputb = socket.getOutputStream();
                ) {
                    this.out = output;
                    this.outb = outputb;

                    // ждем первой строки запроса
                    while (!input.ready()) ;

                    // считываем и печатаем все что было отправлено клиентом
                    final String requestText = input.readLine();
                    final String requestType = requestText.split(" ")[0];
                    final String requestedResource = requestText.split(" ")[1];
                    final String ip = socket.getInetAddress().toString().substring(1);
                    
                    System.out.println("Client connected! " + ip + " " + requestType + " " + requestedResource);

                    onRequestAction overwatch;
                    boolean proceed = true;

                    if ((overwatch = this.responses.get("OVERWATCH"))!=null) {
                        if (!overwatch.response(requestedResource, ip, requestText)) {
                            proceed = false;
                        }
                    }

                    if (proceed) {
                        // Если команда
                        if (requestedResource.startsWith("/CMD%3C%3E")) {
                            String cmd = requestedResource.split("%3C%3E")[1];
                            String[] args = requestedResource.substring(requestedResource.indexOf("%3C%3E", 9)+6).split("%3C%3E");
                            onRequestCMDAction res;
                            if ((res = commands.get(cmd))!=null) {
                                res.response(ip, args);
                            } else {
                                output.println("HTTP/2 200 OK");
                                output.println("Content-Type: text/html; charset=utf-8");
                                output.println();
                                output.println("None");
                                output.flush();
                            }
                        } 
                        // Если не команда
                        else {
                            // Если ресурс существует
                            if (this.responses.containsKey(requestedResource)) {
                                // Выполняем обработчик события и отправляем результат
                                this.responses.get(requestedResource).response(requestedResource, ip, requestText);
                            // Если ресурс является медиа
                            } else if (requestedResource.startsWith("/media/")) {
                                String pathToRes = requestedResource.substring(7);
                                
                                // Если файл существует то ищем расширение в списке
                                if (DataOperator.isFileExist(pathToRes)) {
                                    String ext = pathToRes.substring(pathToRes.lastIndexOf('.')+1);
                                    String contentType = "text/html";
                                    for (Map.Entry<String, ArrayList<String>> el : dataTypes.entrySet()) {
                                        if (el.getValue().contains(ext)) {
                                            contentType = el.getKey()+"/"+ext;
                                        }
                                    }
                                    outputb.write(("HTTP/2 200 OK").getBytes());
                                    outputb.write(("Content-Type: "+contentType+"; charset=utf-8").getBytes());
                                    outputb.write("\n".getBytes());
                                    outputb.write(DataOperator.readFileAsBytes(pathToRes));
                                    outputb.flush();
                                // Иначе отправлем 404
                                } else {
                                    send404Response(output, requestedResource, ip, requestText);
                                }
                            // Иначе отправляем 404
                            } else {
                                send404Response(output, requestedResource, ip, requestText);
                            }
                        }
                    }

                    
                    // по окончанию выполнения блока try-with-resources потоки, 
                    // а вместе с ними и соединение будут закрыты
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private final void send404Response(PrintWriter out, String resource, String ip, String request) {
        onRequestAction res;
        if ((res = responses.get("404")) != null) {
            res.response(resource, ip, request);
        } else {
            out.println("HTTP/2 404 Not Found");
            out.println("Content-Type: text/html; charset=utf-8");
            out.println();
            out.println("Error 404");
            out.flush();
        }
    }

    public final void sendResponse(String status, String contentType, String content) {
        out.println("HTTP/2 "+status);
        out.println("Content-Type: "+contentType+"; charset=utf-8");
        out.println();
        out.println(content);
        out.flush();
    }

    public final void sendResponse(String content) {
        out.println("HTTP/2 200 OK");
        out.println("Content-Type: text/html; charset=utf-8");
        out.println();
        out.println(content);
        out.flush();
    }

    public final void sendResponse(String status, String contentType, byte[] content) {
        try {
            outb.write(("HTTP/2 "+status).getBytes());
            outb.write(("Content-Type: "+contentType+"; charset=utf-8").getBytes());
            outb.write("\n".getBytes());
            outb.write(content);
            outb.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public final void sendResponse(String contentType, byte[] content) {
        try {
            outb.write(("HTTP/2 200 OK\n").getBytes());
            outb.write(("Content-Type: "+contentType+"; charset=utf-8\n").getBytes());
            outb.write("\n".getBytes());
            outb.write(content);
            outb.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
