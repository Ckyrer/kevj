interface Action {
    void response(String resource, String ip, String request);
}

interface Overwatch {
    boolean checkpoint(String resource, String ip, String request);
}

class ResponseAction {
    final boolean isStart;
    final Action func;

    public ResponseAction(Action func, boolean isStart) {
        this.isStart = isStart;
        this.func = func;
    }

    public final void response(String resource, String ip, String request) {
        this.func.response(resource, ip, request);
    }
}
