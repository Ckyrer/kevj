import java.io.OutputStream;

interface ActionCMD {
    void response(String ip, String[] args);
}

interface AsyncActionCMD {
    void response(String ip, String[] args, OutputStream out);
}

class ResponseCMDAction implements Runnable {
    final ActionCMD func;
    final AsyncActionCMD asyncFunc;
    final boolean isAsync;

    private String ip;
    private String[] args;
    private OutputStream out;

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

    public final void response(String resource, String ip, OutputStream out) {
        this.args = resource.substring(resource.indexOf("<>", resource.indexOf("CMD")+5)+2).split("<>");
        this.ip = ip;
        this.out = out;
    }

    public final void run() {
        this.asyncFunc.response(ip, args, out);
    }

}
