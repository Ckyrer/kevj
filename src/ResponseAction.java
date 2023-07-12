import java.io.BufferedReader;
import java.io.IOException;
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

class ResponseAction  {
    final boolean isStart;
    final Action func;
    final AsyncAction asyncFunc;
    final boolean isAsync;

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
        this.func.response(resource, ip, request);
    }

    public final void response(String resource, String ip, String request, OutputStream out, BufferedReader inp) {
        new Thread( new _AsyncResponseAction(asyncFunc, ip, resource, request, out, inp) ).start();
    }

}

class _AsyncResponseAction implements Runnable {
    final AsyncAction asyncFunc;
    final OutputStream out;
    final BufferedReader in;
    final String ip;
    final String resource;
    final String request;

    public _AsyncResponseAction(AsyncAction func, String ip, String res, String req, OutputStream out, BufferedReader inp) {
        this.out = out;
        this.in = inp;
        this.asyncFunc = func;

        this.ip = ip;
        this.resource = res;
        this.request = req;
    }

    public final void run() {
        this.asyncFunc.response(resource, ip, request, out);
        try {
            out.close();
            in.close();
        } catch (IOException e) {e.printStackTrace();}
    }

}
