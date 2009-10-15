package dolda.jsvc;

import java.io.*;
import java.net.URL;
import java.net.SocketAddress;
import java.util.Map;

public interface Request {
    /* Input */
    public URL url();
    public URL rooturl();
    public String method();
    public String path();
    public InputStream input();
    public MultiMap<String, String> inheaders();
    public MultiMap<String, String> params();
    
    /* Output */
    public OutputStream output();
    public void status(int code);
    public void status(int code, String message);
    public MultiMap<String, String> outheaders();
    
    /* Misc. */
    public Map<Object, Object> props();
    public ServerContext ctx();
    public SocketAddress remoteaddr();
    public SocketAddress localaddr();
}
