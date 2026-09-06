package mplayer;

/**
 * Interfaz para escuchar eventos del motor de audio (cambio de estado, volumen, pista, error).
 */
public interface AudioEngineListener {
    void onStateChanged(int state);
    void onTrackChanged(Track track, int index, int total);
    void onVolumeChanged(int volume, boolean isMuted);
    void onError(String message);
}
