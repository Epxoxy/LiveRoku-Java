package base.interfaces;

public interface ILiveDataResolver {
    void onStatusUpdate (boolean isOn);
    void onDurationUpdate (long duration, String timeText);
    void onDownloadSizeUpdate (long totalSize, String sizeText);
    void onBitRateUpdate (long bitRate, String bitRateText);
    void onHotUpdate (long popularity);
}
