interface ActionCMD {
    boolean response(String ip, String[] args);
}

class ResponseCMDAction {
    final ActionCMD func;

    public ResponseCMDAction(ActionCMD func) {
        this.func = func;
    }

    public final boolean response(String resource, String ip) {
        String[] args = resource.substring(resource.indexOf("<>", 5)+2).split("<>");
        return this.func.response(ip, args);
    }
}
