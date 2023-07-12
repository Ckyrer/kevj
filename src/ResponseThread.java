import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ResponseThread implements Runnable {
    private final OutputStream output;
    private final BufferedReader input;
    private final String ip;

    private final Map<String, ResponseAction> responses;
    private final Map<String, ResponseCMDAction> commands;
    private final Overwatch overwatch;

    public ResponseThread(Map<String, ResponseAction> responses, Map<String, ResponseCMDAction> commands, Overwatch overwatch, OutputStream out, InputStream in, String ip) {
        this.responses = responses;
        this.commands = commands;
        this.overwatch = overwatch;
        this.output = out;
        this.input = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.ip = ip;
    }

    public final void run() {
        try {
            // ждем первой строки запроса
            while (!input.ready()) ;

            // считываем и печатаем все что было отправлено клиентом
            final String requestText = DataOperator.decodeURL(input.readLine());
            final String requestedResource = requestText.substring(requestText.indexOf(" ")+2, requestText.lastIndexOf(" "));
        
            boolean proceed = true;

            // Подключаем смотрителя, если он есть
            if (overwatch!=null) {
                proceed = overwatch.checkpoint(requestedResource, ip, requestText);
            }

            if (proceed) {
                // Создаём ответчика
                Responser resp = new Responser(output, this);

                // Если команда
                if (requestedResource.contains("CMD<>")) {
                    String cmd = requestedResource.split("CMD")[0] + requestedResource.split("<>")[1];
                    if (this.commands.containsKey(cmd)) {
                        this.commands.get(cmd).response(requestedResource, ip, resp);
                    } else {
                        resp.sendResponse("NONE");
                    }
                } 
                // Если не команда
                else {
                    // Если ресурс существует
                    if (this.responses.containsKey(requestedResource)) {
                        // Выполняем обработчик события
                        this.responses.get(requestedResource).response(requestedResource, ip, requestText, resp);
                    // Иначе смотрим isStart
                    } else {
                        boolean isFinded = false;

                        // Ищем тот обработчик, с которого начинается запрос
                        for (Map.Entry<String, ResponseAction> el: responses.entrySet()) {
                            if (!el.getKey().equals("") && requestedResource.startsWith(el.getKey())) {
                                el.getValue().response(requestedResource, ip, requestText, resp);
                                isFinded = true;
                                break;
                            }
                        }
                        
                        // Иначе отправляем 404
                        if (!isFinded) {
                            send404Response(requestedResource, ip, requestText, resp);
                        }
                    }
                }
            }
        } catch (IOException e) {e.printStackTrace();}

        try {this.input.close();} catch (IOException e) {e.printStackTrace();}
        try {this.output.close();} catch (IOException e) {e.printStackTrace();}
    }

    public final void send404Response(String res, String ip, String req, Responser resp) {
        ResponseAction act;
        if ((act = responses.get("404"))!=null) {
            act.response(res, ip, req, resp);
        } else {
            resp.sendResponse("Error 404");
        }
    }
}