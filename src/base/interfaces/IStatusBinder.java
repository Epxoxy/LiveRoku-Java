package base.interfaces;

public interface IStatusBinder {
    void onPreparing ();
    void onStreaming ();
    void onWaiting();
    void onStopped ();
}
