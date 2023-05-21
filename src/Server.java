import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;



public class Server {
    final private int port;
    private PrintWriter out;
    private OutputStream outb;
    final private Map<String, ResponseAction> responses = new HashMap<String, ResponseAction>();
    final private Map<String, ResponseCMDAction> commands = new HashMap<String, ResponseCMDAction>();

    public Server(int port) {
        this.port = port;
    }

    public final void addRequestHandler(String requestedResourse, ResponseAction handler) {
        this.responses.put(requestedResourse, handler);
    }

    public final void addRequestCMDHandler(String requestedResourse, ResponseCMDAction handler) {
        this.commands.put(requestedResourse, handler);
    }

    public final void start() {
        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            
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
                    final String requestedResource = requestText.split(" ")[1].substring(1);
                    final String ip = socket.getInetAddress().toString().substring(1);
                

                    ResponseAction overwatch;
                    boolean proceed = true;

                    if ((overwatch = this.responses.get("OVERWATCH"))!=null) {
                        if (!overwatch.response(requestedResource, ip, requestText)) {
                            proceed = false;
                        }
                    }

                    if (proceed) {
                        // Если команда
                        if (requestedResource.contains("CMD%3C%3E")) {
                            String cmd = requestedResource.split("%3C%3E")[1];
                            ResponseCMDAction res;
                            if ((res = commands.get(cmd))!=null) {
                                res.response(requestedResource, ip);
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
                            // Иначе отправляем 404
                            } else {
                                boolean isFinded = false;

                                for (Map.Entry<String, ResponseAction> el: responses.entrySet()) {
                                    if (!el.getKey().equals("") && requestedResource.startsWith(el.getKey())) {
                                        el.getValue().response(requestedResource, ip, requestText);
                                        isFinded = true;
                                        break;
                                    }
                                }
                                

                                if (!isFinded) {
                                    send404Response(requestedResource, ip, requestText);
                                }
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

    public final void send404Response(String resource, String ip, String request) {
        ResponseAction res;
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
