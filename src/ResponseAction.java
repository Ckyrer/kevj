interface action {
    boolean response(String resource, String ip, String request);
}

public class ResponseAction {
    final boolean isStart;
    final action func;

    public ResponseAction(boolean isStart, action func) {
        this.isStart = isStart;
        this.func = func;
    }

    public final boolean response(String resource, String ip, String request) {
        return this.func.response(resource, ip, request);
    }
}