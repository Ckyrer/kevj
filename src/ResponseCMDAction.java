interface actionCMD {
    boolean response(String ip, String[] args);
}


public class ResponseCMDAction {
    final actionCMD func;

    public ResponseCMDAction(actionCMD func) {
        this.func = func;
    }

    public final boolean response(String resource, String ip) {
        String[] args = resource.substring(resource.indexOf("<>", 5)+2).split("<>");
        return this.func.response(ip, args);
    }
}
