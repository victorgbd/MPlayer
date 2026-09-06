package mplayer;

import java.io.InputStream;
import java.util.Vector;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;

/**
 * Motor de reproduccion de audio basado en MMAPI (JSR-135).
 * Maneja el ciclo de vida del reproductor, volumen, busqueda (seek) y listas de reproduccion.
 */
public class AudioEngine implements PlayerListener {

    public static final int STATE_STOPPED = 0;
    public static final int STATE_PLAYING = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_LOADING = 3;
    public static final int STATE_ERROR = 4;

    public static final int REPEAT_OFF = 0;
    public static final int REPEAT_ONE = 1;
    public static final int REPEAT_ALL = 2;

    private Player player;
    private VolumeControl volumeControl;

    private int state = STATE_STOPPED;
    private int volume = 70; // 0 - 100
    private boolean muted = false;
    private int volumeBeforeMute = 70;
    private int repeatMode = REPEAT_ALL;
    private boolean shuffle = false;

    private final Vector playlist = new Vector();
    private final Vector playbackHistory = new Vector();
    private final Vector shufflePool = new Vector();
    private final java.util.Random random = new java.util.Random();
    private int currentTrackIndex = -1;
    private AudioEngineListener listener;

    private final Object playerLock = new Object();
    private boolean isReleasing = false;

    public AudioEngine() {
        loadSavedPlaylist();
    }

    private void loadSavedPlaylist() {
        try {
            Vector saved = PlaylistStorage.load();
            if (saved != null && !saved.isEmpty()) {
                for (int i = 0; i < saved.size(); i++) {
                    playlist.addElement(saved.elementAt(i));
                }
                int savedIdx = PlaylistStorage.loadCurrentIndex();
                if (savedIdx >= 0 && savedIdx < playlist.size()) {
                    currentTrackIndex = savedIdx;
                } else {
                    currentTrackIndex = 0;
                }
            }
            shuffle = PlaylistStorage.loadShuffle();
            repeatMode = PlaylistStorage.loadRepeatMode();
            if (shuffle) {
                initShufflePool();
            }
        } catch (Throwable t) {
            // ignorar
        }
    }

    public synchronized void savePlaylist() {
        PlaylistStorage.save(playlist, currentTrackIndex, shuffle, repeatMode);
    }

    public void setListener(AudioEngineListener listener) {
        this.listener = listener;
    }

    public int getState() {
        return state;
    }

    private void setState(int newState) {
        this.state = newState;
        if (listener != null) {
            listener.onStateChanged(newState);
        }
    }

    // ==========================================
    // Playlist Management & Multi-Playlists
    // ==========================================

    public synchronized String getActivePlaylistName() {
        return PlaylistStorage.getActivePlaylistName();
    }

    public synchronized Vector getPlaylistNames() {
        return PlaylistStorage.getPlaylistNames();
    }

    public synchronized String[] getPlaylistNamesArray() {
        Vector v = PlaylistStorage.getPlaylistNames();
        if (v == null || v.isEmpty()) {
            return new String[] { "Principal" };
        }
        String[] arr = new String[v.size()];
        for (int i = 0; i < v.size(); i++) {
            arr[i] = (String) v.elementAt(i);
        }
        return arr;
    }

    public synchronized void switchPlaylist(String playlistName) {
        if (playlistName == null || playlistName.equals(getActivePlaylistName())) {
            return;
        }
        savePlaylist();
        stop();

        PlaylistStorage.setActivePlaylist(playlistName);

        playlist.removeAllElements();
        playbackHistory.removeAllElements();
        shufflePool.removeAllElements();
        currentTrackIndex = -1;

        loadSavedPlaylist();
        notifyTrackChanged();
    }

    public synchronized boolean createPlaylist(String name) {
        savePlaylist();
        boolean ok = PlaylistStorage.createPlaylist(name);
        if (ok) {
            stop();
            playlist.removeAllElements();
            playbackHistory.removeAllElements();
            shufflePool.removeAllElements();
            currentTrackIndex = -1;
            loadSavedPlaylist();
            notifyTrackChanged();
        }
        return ok;
    }

    public synchronized boolean deletePlaylist(String name) {
        if (name == null) return false;
        boolean wasActive = name.equals(getActivePlaylistName());
        if (wasActive) {
            stop();
        }
        boolean ok = PlaylistStorage.deletePlaylist(name);
        if (ok && wasActive) {
            playlist.removeAllElements();
            playbackHistory.removeAllElements();
            shufflePool.removeAllElements();
            currentTrackIndex = -1;
            loadSavedPlaylist();
            notifyTrackChanged();
        }
        return ok;
    }

    public synchronized boolean containsTrack(Track track) {
        if (track == null) return false;
        return containsSource(track.getSource()) || containsTitle(track.getTitle());
    }

    public synchronized boolean containsSource(String source) {
        if (source == null) return false;
        for (int i = 0; i < playlist.size(); i++) {
            Track t = (Track) playlist.elementAt(i);
            if (t != null && source.equalsIgnoreCase(t.getSource())) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean containsTitle(String title) {
        if (title == null) return false;
        for (int i = 0; i < playlist.size(); i++) {
            Track t = (Track) playlist.elementAt(i);
            if (t != null && title.equalsIgnoreCase(t.getTitle())) {
                return true;
            }
        }
        return false;
    }

    public synchronized int findTrackIndex(String source) {
        if (source == null) return -1;
        for (int i = 0; i < playlist.size(); i++) {
            Track t = (Track) playlist.elementAt(i);
            if (t != null && source.equalsIgnoreCase(t.getSource())) {
                return i;
            }
        }
        return -1;
    }

    public synchronized int findTrackIndex(Track track) {
        if (track == null) return -1;
        int idx = findTrackIndex(track.getSource());
        if (idx != -1) return idx;
        for (int i = 0; i < playlist.size(); i++) {
            Track t = (Track) playlist.elementAt(i);
            if (t != null && track.getTitle() != null && track.getTitle().equalsIgnoreCase(t.getTitle())) {
                return i;
            }
        }
        return -1;
    }

    public synchronized Vector getDuplicates(Vector candidateTracks) {
        Vector dups = new Vector();
        if (candidateTracks == null) return dups;
        for (int i = 0; i < candidateTracks.size(); i++) {
            Track t = (Track) candidateTracks.elementAt(i);
            if (containsTrack(t)) {
                dups.addElement(t);
            }
        }
        return dups;
    }

    public synchronized Vector getNonDuplicates(Vector candidateTracks) {
        Vector nonDups = new Vector();
        if (candidateTracks == null) return nonDups;
        for (int i = 0; i < candidateTracks.size(); i++) {
            Track t = (Track) candidateTracks.elementAt(i);
            if (!containsTrack(t)) {
                nonDups.addElement(t);
            }
        }
        return nonDups;
    }

    public synchronized void addTrack(Track track) {
        playlist.addElement(track);
        if (currentTrackIndex == -1) {
            currentTrackIndex = 0;
        } else if (shuffle) {
            shufflePool.addElement(new Integer(playlist.size() - 1));
        }
        savePlaylist();
        notifyTrackChanged();
    }

    public synchronized void addTracks(Vector tracks) {
        if (tracks == null || tracks.isEmpty()) return;
        boolean wasEmpty = playlist.isEmpty();
        for (int i = 0; i < tracks.size(); i++) {
            playlist.addElement(tracks.elementAt(i));
        }
        if (wasEmpty) {
            currentTrackIndex = 0;
        }
        if (shuffle) {
            initShufflePool();
        }
        savePlaylist();
        notifyTrackChanged();
    }

    public synchronized void removeTrack(int index) {
        if (index >= 0 && index < playlist.size()) {
            boolean isCurrent = (index == currentTrackIndex);
            playlist.removeElementAt(index);
            cleanHistoryOnRemove(index);
            removeFromShufflePool(index);
            if (playlist.isEmpty()) {
                stop();
                currentTrackIndex = -1;
            } else if (isCurrent) {
                if (currentTrackIndex >= playlist.size()) {
                    currentTrackIndex = 0;
                }
                playTrack(currentTrackIndex);
            } else if (index < currentTrackIndex) {
                currentTrackIndex--;
            }
            savePlaylist();
            notifyTrackChanged();
        }
    }

    public synchronized void clearPlaylist() {
        stop();
        playlist.removeAllElements();
        playbackHistory.removeAllElements();
        shufflePool.removeAllElements();
        currentTrackIndex = -1;
        savePlaylist();
        notifyTrackChanged();
    }

    public synchronized int getPlaylistSize() {
        return playlist.size();
    }

    public synchronized Track getTrack(int index) {
        if (index >= 0 && index < playlist.size()) {
            return (Track) playlist.elementAt(index);
        }
        return null;
    }

    public synchronized int getCurrentTrackIndex() {
        return currentTrackIndex;
    }

    public synchronized Track getCurrentTrack() {
        if (currentTrackIndex >= 0 && currentTrackIndex < playlist.size()) {
            return (Track) playlist.elementAt(currentTrackIndex);
        }
        return null;
    }

    private void notifyTrackChanged() {
        if (listener != null) {
            listener.onTrackChanged(getCurrentTrack(), currentTrackIndex, playlist.size());
        }
    }

    // ==========================================
    // Playback Controls
    // ==========================================

    public void playTrack(final int index) {
        if (index < 0 || index >= playlist.size()) {
            return;
        }
        currentTrackIndex = index;
        if (shuffle) {
            for (int i = shufflePool.size() - 1; i >= 0; i--) {
                if (((Integer) shufflePool.elementAt(i)).intValue() == index) {
                    shufflePool.removeElementAt(i);
                    break;
                }
            }
        }
        savePlaylist();
        notifyTrackChanged();
        setState(STATE_LOADING);

        new Thread(new Runnable() {
            public void run() {
                synchronized (playerLock) {
                    if (isReleasing) return;
                    try {
                        closePlayerInternal();
                        Track track = (Track) playlist.elementAt(index);
                        if (track.isResource()) {
                            InputStream is = getClass().getResourceAsStream(track.getSource());
                            if (is == null) {
                                throw new Exception("Archivo de recurso no encontrado: " + track.getSource());
                            }
                            String mime = getMimeType(track.getSource());
                            player = Manager.createPlayer(is, mime);
                        } else {
                            String url = track.getSource();
                            if (url != null && (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("rtsp://"))) {
                                throw new Exception("Reproducci\u00F3n por Internet deshabilitada");
                            }
                            if (url.indexOf("://") == -1) {
                                url = "file:///" + (url.startsWith("/") ? url.substring(1) : url);
                            }
                            player = Manager.createPlayer(url);
                        }

                        player.addPlayerListener(AudioEngine.this);
                        player.realize();
                        player.prefetch();
                        applyVolumeInternal();
                        player.start();
                        setState(STATE_PLAYING);
                    } catch (Exception ex) {
                        setState(STATE_ERROR);
                        notifyError("Error al cargar pista: " + ex.getMessage());
                    }
                }
            }
        }).start();
    }

    public void play() {
        if (state == STATE_PAUSED && player != null) {
            new Thread(new Runnable() {
                public void run() {
                    synchronized (playerLock) {
                        try {
                            player.start();
                            setState(STATE_PLAYING);
                        } catch (Exception ex) {
                            notifyError("Error al reanudar: " + ex.getMessage());
                        }
                    }
                }
            }).start();
        } else if (state == STATE_STOPPED || state == STATE_ERROR) {
            if (currentTrackIndex >= 0 && currentTrackIndex < playlist.size()) {
                playTrack(currentTrackIndex);
            }
        }
    }

    public void pause() {
        if (state == STATE_PLAYING && player != null) {
            new Thread(new Runnable() {
                public void run() {
                    synchronized (playerLock) {
                        try {
                            player.stop();
                            setState(STATE_PAUSED);
                        } catch (Exception ex) {
                            notifyError("Error al pausar: " + ex.getMessage());
                        }
                    }
                }
            }).start();
        }
    }

    public void togglePlayPause() {
        if (state == STATE_PLAYING) {
            pause();
        } else if (state == STATE_PAUSED) {
            play();
        } else if (state == STATE_STOPPED || state == STATE_ERROR) {
            play();
        }
    }

    public void stop() {
        new Thread(new Runnable() {
            public void run() {
                synchronized (playerLock) {
                    try {
                        if (player != null) {
                            if (player.getState() == Player.STARTED) {
                                player.stop();
                            }
                            player.setMediaTime(0);
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                    setState(STATE_STOPPED);
                }
            }
        }).start();
    }

    public void nextTrack() {
        if (playlist.isEmpty()) return;
        if (playlist.size() == 1) {
            playTrack(0);
            return;
        }

        if (shuffle) {
            if (currentTrackIndex >= 0) {
                playbackHistory.addElement(new Integer(currentTrackIndex));
                if (playbackHistory.size() > 50) {
                    playbackHistory.removeElementAt(0);
                }
            }

            if (shufflePool.isEmpty()) {
                if (repeatMode == REPEAT_ALL) {
                    initShufflePool();
                } else {
                    stop();
                    return;
                }
            }

            if (!shufflePool.isEmpty()) {
                int randIdx = random.nextInt(shufflePool.size());
                int nextIndex = ((Integer) shufflePool.elementAt(randIdx)).intValue();
                shufflePool.removeElementAt(randIdx);
                playTrack(nextIndex);
                return;
            }
        }

        int nextIndex = currentTrackIndex + 1;
        if (nextIndex >= playlist.size()) {
            if (repeatMode == REPEAT_ALL) {
                nextIndex = 0;
            } else {
                stop();
                return;
            }
        }
        playTrack(nextIndex);
    }

    public void prevTrack() {
        if (playlist.isEmpty()) return;
        // Si ya lleva mas de 3 segundos de reproduccion, reiniciar la pista actual
        if (getMediaTime() > 3000000L) {
            seekDelta(-100000000L); // volver al inicio
            return;
        }

        if (shuffle && !playbackHistory.isEmpty()) {
            int prevIdx = ((Integer) playbackHistory.lastElement()).intValue();
            playbackHistory.removeElementAt(playbackHistory.size() - 1);
            if (prevIdx >= 0 && prevIdx < playlist.size()) {
                playTrack(prevIdx);
                return;
            }
        }

        int prevIndex = currentTrackIndex - 1;
        if (prevIndex < 0) {
            if (repeatMode == REPEAT_ALL) {
                prevIndex = playlist.size() - 1;
            } else {
                prevIndex = 0;
            }
        }
        playTrack(prevIndex);
    }

    // ==========================================
    // Seeking
    // ==========================================

    public void seekDelta(final long deltaMicros) {
        if (player == null || state == STATE_STOPPED) return;
        new Thread(new Runnable() {
            public void run() {
                synchronized (playerLock) {
                    try {
                        long now = player.getMediaTime();
                        long dur = player.getDuration();
                        long target = now + deltaMicros;
                        if (target < 0) target = 0;
                        if (dur > 0 && target > dur) target = dur;
                        player.setMediaTime(target);
                    } catch (Exception e) {
                        // Ignorar
                    }
                }
            }
        }).start();
    }

    public void seekToPercent(final int percent) {
        if (player == null || state == STATE_STOPPED) return;
        new Thread(new Runnable() {
            public void run() {
                synchronized (playerLock) {
                    try {
                        long dur = player.getDuration();
                        if (dur > 0) {
                            long target = (dur * percent) / 100L;
                            player.setMediaTime(target);
                        }
                    } catch (Exception e) {
                        // Ignorar
                    }
                }
            }
        }).start();
    }

    public long getMediaTime() {
        if (player != null) {
            try {
                return player.getMediaTime();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    public long getDuration() {
        if (player != null) {
            try {
                return player.getDuration();
            } catch (Exception e) {
                return -1;
            }
        }
        return -1;
    }

    public int getProgressPercent() {
        long dur = getDuration();
        if (dur <= 0) return 0;
        long pos = getMediaTime();
        int pct = (int) ((pos * 100L) / dur);
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;
        return pct;
    }

    // ==========================================
    // Volume & Sound Control
    // ==========================================

    public void setVolume(int level) {
        if (level < 0) level = 0;
        if (level > 100) level = 100;
        this.volume = level;
        this.muted = (level == 0);
        applyVolumeInternal();
        notifyVolumeChanged();
    }

    public int getVolume() {
        return volume;
    }

    public void volumeUp() {
        if (muted) {
            muted = false;
        }
        setVolume(volume + 10);
    }

    public void volumeDown() {
        setVolume(volume - 10);
    }

    public void toggleMute() {
        muted = !muted;
        if (muted) {
            volumeBeforeMute = volume;
            applyVolumeInternal();
        } else {
            volume = (volumeBeforeMute > 0) ? volumeBeforeMute : 50;
            applyVolumeInternal();
        }
        notifyVolumeChanged();
    }

    public boolean isMuted() {
        return muted;
    }

    private void applyVolumeInternal() {
        if (player == null) return;
        try {
            volumeControl = (VolumeControl) player.getControl("VolumeControl");
            if (volumeControl == null) {
                volumeControl = (VolumeControl) player.getControl("javax.microedition.media.control.VolumeControl");
            }
            if (volumeControl != null) {
                if (muted) {
                    volumeControl.setLevel(0);
                } else {
                    volumeControl.setLevel(volume);
                }
            }
        } catch (Exception e) {
            // Algunos dispositivos no implementan VolumeControl
        }
    }

    private void notifyVolumeChanged() {
        if (listener != null) {
            listener.onVolumeChanged(volume, muted);
        }
    }

    // ==========================================
    // Repeat & Shuffle Modes
    // ==========================================

    public void toggleRepeatMode() {
        repeatMode = (repeatMode + 1) % 3;
        savePlaylist();
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    public String getRepeatModeString() {
        switch (repeatMode) {
            case REPEAT_ONE:
                return "1 Vez";
            case REPEAT_ALL:
                return "Todo";
            default:
                return "Off";
        }
    }

    public synchronized boolean isShuffle() {
        return shuffle;
    }

    public synchronized void setShuffle(boolean enabled) {
        this.shuffle = enabled;
        if (this.shuffle) {
            initShufflePool();
        } else {
            shufflePool.removeAllElements();
            playbackHistory.removeAllElements();
        }
        savePlaylist();
    }

    public synchronized void toggleShuffle() {
        setShuffle(!shuffle);
    }

    private synchronized void initShufflePool() {
        shufflePool.removeAllElements();
        int size = playlist.size();
        if (size <= 0) return;
        for (int i = 0; i < size; i++) {
            if (i != currentTrackIndex) {
                shufflePool.addElement(new Integer(i));
            }
        }
    }

    private synchronized void removeFromShufflePool(int index) {
        for (int i = shufflePool.size() - 1; i >= 0; i--) {
            int val = ((Integer) shufflePool.elementAt(i)).intValue();
            if (val == index) {
                shufflePool.removeElementAt(i);
            } else if (val > index) {
                shufflePool.setElementAt(new Integer(val - 1), i);
            }
        }
    }

    private synchronized void cleanHistoryOnRemove(int index) {
        for (int i = playbackHistory.size() - 1; i >= 0; i--) {
            int val = ((Integer) playbackHistory.elementAt(i)).intValue();
            if (val == index) {
                playbackHistory.removeElementAt(i);
            } else if (val > index) {
                playbackHistory.setElementAt(new Integer(val - 1), i);
            }
        }
    }

    // ==========================================
    // MMAPI PlayerListener Callback
    // ==========================================

    public void playerUpdate(Player p, String event, Object eventData) {
        if (PlayerListener.END_OF_MEDIA.equals(event)) {
            new Thread(new Runnable() {
                public void run() {
                    if (repeatMode == REPEAT_ONE) {
                        try {
                            synchronized (playerLock) {
                                if (player != null) {
                                    player.setMediaTime(0);
                                    player.start();
                                    setState(STATE_PLAYING);
                                }
                            }
                        } catch (Exception e) {
                            playTrack(currentTrackIndex);
                        }
                    } else if (repeatMode == REPEAT_ALL) {
                        nextTrack();
                    } else { // REPEAT_OFF
                        if (shuffle) {
                            if (!shufflePool.isEmpty()) {
                                nextTrack();
                            } else {
                                stop();
                            }
                        } else {
                            if (currentTrackIndex + 1 < playlist.size()) {
                                nextTrack();
                            } else {
                                stop();
                            }
                        }
                    }
                }
            }).start();
        } else if (PlayerListener.ERROR.equals(event)) {
            setState(STATE_ERROR);
            notifyError("Error MMAPI: " + eventData);
        }
    }

    private void notifyError(String msg) {
        if (listener != null) {
            listener.onError(msg);
        }
    }

    private void closePlayerInternal() {
        if (player != null) {
            try {
                if (player.getState() == Player.STARTED) {
                    player.stop();
                }
                player.removePlayerListener(this);
                player.deallocate();
                player.close();
            } catch (Exception e) {
                // ignore
            }
            player = null;
            volumeControl = null;
            System.gc();
        }
    }

    public static String getMimeType(String source) {
        if (source == null) return "audio/mpeg";
        String lower = source.toLowerCase();
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".aac")) return "audio/aac";
        if (lower.endsWith(".m4a")) return "audio/mp4";
        if (lower.endsWith(".wma")) return "audio/x-ms-wma";
        if (lower.endsWith(".wav")) return "audio/x-wav";
        if (lower.endsWith(".mid") || lower.endsWith(".midi")) return "audio/midi";
        if (lower.endsWith(".amr")) return "audio/amr";
        if (lower.endsWith(".3gp") || lower.endsWith(".3ga")) return "audio/3gpp";
        if (lower.endsWith(".xmf") || lower.endsWith(".mxmf")) return "audio/xmf";
        if (lower.endsWith(".imy")) return "audio/imelody";
        if (lower.endsWith(".mmf")) return "application/x-smaf";
        return "audio/mpeg";
    }

    public void release() {
        isReleasing = true;
        synchronized (playerLock) {
            closePlayerInternal();
        }
    }
}
