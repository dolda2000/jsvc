package dolda.jsvc;

public interface ResettableRequest extends Request {
    public boolean canreset();
    public void reset();
}
