import java.io.OutputStream;

interface Action {
    void response(String resource, String ip, String request);
}

interface AsyncAction {
    void response(String resource, String ip, String request, OutputStream out);
}

interface Overwatch {
    boolean checkpoint(String resource, String ip, String request);
}

class ResponseAction implements Runnable {
    final boolean isStart;
    final Action func;
    final AsyncAction asyncFunc;
    final boolean isAsync;

    private String resource;
    private String ip;
    private String request;
    private OutputStream output;

    public ResponseAction(Action func, boolean isStart) {
        this.isStart = isStart;
        this.func = func;
        this.asyncFunc = null;
        isAsync=false;
    }

    public ResponseAction(AsyncAction func, boolean isStart) {
        this.isStart = isStart;
        this.asyncFunc = func;
        this.func = null;
        isAsync=true;
    }

    public final void response(String resource, String ip, String request) {
        if (func==null) {
            System.out.println("Error! Invalid method used in ResponseAction!");
            return;
        }
        this.func.response(resource, ip, request);
    }

    public final void response(String resource, String ip, String request, OutputStream out) {
        this.resource = resource;
        this.ip = ip;
        this.request = request;
        this.output = out;
    }

    public final void run() {
        this.asyncFunc.response(resource, ip, request, output);
    }

}
