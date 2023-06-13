interface ActionCMD {
    void response(String ip, String[] args);
}

class ResponseCMDAction {
    final ActionCMD func;

    public ResponseCMDAction(ActionCMD func) {
        this.func = func;
    }

    public final void response(String resource, String ip) {
        String[] args = resource.substring(resource.indexOf("<>", resource.indexOf("CMD")+5)+2).split("<>");
        this.func.response(ip, args);
    }
}
