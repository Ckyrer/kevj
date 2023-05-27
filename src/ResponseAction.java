interface Action {
    boolean response(String resource, String ip, String request);
}

class ResponseAction {
    final boolean isStart;
    final Action func;

    public ResponseAction(Action func, boolean isStart) {
        this.isStart = isStart;
        this.func = func;
    }

    public final boolean response(String resource, String ip, String request) {
        return this.func.response(resource, ip, request);
    }
}