interface Action {
    void response(String resource, String ip, String request, Responser resp);
}

interface Overwatch {
    boolean checkpoint(String resource, String ip, String request, Responser resp);
}

class ResponseAction  {
    final boolean isStart;
    final Action func;

    public ResponseAction(Action func, boolean isStart) {
        this.isStart = isStart;
        this.func = func;
    }

    public final void response(String resource, String ip, String request, Responser resp) {
        this.func.response(resource, ip, request, resp);
    }
}
