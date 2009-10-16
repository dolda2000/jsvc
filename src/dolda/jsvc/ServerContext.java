package dolda.jsvc;

public interface ServerContext {
    public long starttime();
    public String sysconfig(String key, String def);
    public String libconfig(String key, String def);
    public String name();
}
