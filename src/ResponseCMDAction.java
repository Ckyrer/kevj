interface actionCMD {
    boolean response(String ip, String[] args);
}


public class ResponseCMDAction {
    final actionCMD func;

    public ResponseCMDAction(actionCMD func) {
        this.func = func;
    }

    public final boolean response(String resource, String ip) {
        String[] args = resource.substring(resource.indexOf("%3C%3E", 9)+6).split("%3C%3E");
        return this.func.response(ip, args);
    }
}
