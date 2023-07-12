import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;

interface ActionCMD {
    void response(String ip, String[] args);
}

interface AsyncActionCMD {
    void response(String ip, String[] args, OutputStream out);
}

class ResponseCMDAction {
    final ActionCMD func;
    final AsyncActionCMD asyncFunc;
    final boolean isAsync;

    public ResponseCMDAction(ActionCMD func) {
        this.func = func;
        this.asyncFunc = null;
        this.isAsync=false;
    }

    public ResponseCMDAction(AsyncActionCMD func) {
        this.asyncFunc = func;
        this.func = null;
        this.isAsync = true;
    }

    public final void response(String resource, String ip) {
        String[] args = resource.substring(resource.indexOf("<>", resource.indexOf("CMD")+5)+2).split("<>");
        this.func.response(ip, args);
    }

    public final void response(String resource, String ip, OutputStream out, BufferedReader inp) {
        String[] args = resource.substring(resource.indexOf("<>", resource.indexOf("CMD")+5)+2).split("<>");
        new Thread(new _AsyncResponseCMDAction(asyncFunc, ip, args, out, inp)).start();
    }

}

class _AsyncResponseCMDAction implements Runnable {
    final AsyncActionCMD asyncFunc;
    final OutputStream out;
    final BufferedReader in;
    final String ip;
    final String[] args;

    public _AsyncResponseCMDAction(AsyncActionCMD func, String ip, String[] args, OutputStream out, BufferedReader inp) {
        this.ip = ip;
        this.out = out;
        this.in = inp;
        this.asyncFunc = func;
        this.args = args;
    }

    public final void run() {
        this.asyncFunc.response(ip, args, out);
        try {
            out.close();
            in.close();
        } catch (IOException e) {e.printStackTrace();}
    }

}

