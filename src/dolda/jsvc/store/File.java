package dolda.jsvc.store;

import dolda.jsvc.*;
import java.io.*;

public interface File {
    public InputStream read();
    public OutputStream store();
    public long mtime();
    public void remove();
    public String name();
}
