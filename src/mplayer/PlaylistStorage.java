package mplayer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Vector;
import javax.microedition.rms.RecordStore;

/**
 * Gestor de persistencia de múltiples listas de reproducción mediante RMS (Record Management System).
 * Soporta varias playlists independientes ("Principal", "Favoritos", etc.),
 * preservando total compatibilidad con la lista existente en k800i_playlist.
 */
public class PlaylistStorage {

    private static final String DEFAULT_STORE = "k800i_playlist";
    private static final String META_STORE = "mp_pl_meta";
    private static final String DEFAULT_PLAYLIST_NAME = "Principal";

    private static Vector playlistNames = null;
    private static Vector playlistStoreNames = null;
    private static String activePlaylistName = null;

    private static synchronized void ensureMetaLoaded() {
        if (playlistNames != null && playlistStoreNames != null && activePlaylistName != null) {
            return;
        }

        playlistNames = new Vector();
        playlistStoreNames = new Vector();
        activePlaylistName = DEFAULT_PLAYLIST_NAME;

        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(META_STORE, false);
            if (rs != null && rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                if (data != null && data.length > 0) {
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    DataInputStream dis = new DataInputStream(bais);

                    activePlaylistName = dis.readUTF();
                    int count = dis.readInt();
                    for (int i = 0; i < count; i++) {
                        String name = dis.readUTF();
                        String store = dis.readUTF();
                        playlistNames.addElement(name);
                        playlistStoreNames.addElement(store);
                    }
                }
            }
        } catch (Throwable t) {
            // Meta store no existe todavía
        } finally {
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception e) {}
            }
        }

        // Si no había metadatos o estaban vacíos, inicializar con lista Principal
        if (playlistNames.isEmpty()) {
            playlistNames.addElement(DEFAULT_PLAYLIST_NAME);
            playlistStoreNames.addElement(DEFAULT_STORE);
            activePlaylistName = DEFAULT_PLAYLIST_NAME;
            saveMeta();
        }

        // Asegurar que activePlaylistName esté en la lista
        boolean found = false;
        for (int i = 0; i < playlistNames.size(); i++) {
            if (activePlaylistName.equals(playlistNames.elementAt(i))) {
                found = true;
                break;
            }
        }
        if (!found) {
            activePlaylistName = (String) playlistNames.elementAt(0);
        }
    }

    private static synchronized void saveMeta() {
        RecordStore rs = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeUTF(activePlaylistName != null ? activePlaylistName : DEFAULT_PLAYLIST_NAME);
            int size = (playlistNames != null) ? playlistNames.size() : 0;
            dos.writeInt(size);

            for (int i = 0; i < size; i++) {
                dos.writeUTF((String) playlistNames.elementAt(i));
                dos.writeUTF((String) playlistStoreNames.elementAt(i));
            }

            dos.flush();
            byte[] data = baos.toByteArray();

            rs = RecordStore.openRecordStore(META_STORE, true);
            if (rs.getNumRecords() == 0) {
                rs.addRecord(data, 0, data.length);
            } else {
                rs.setRecord(1, data, 0, data.length);
            }
        } catch (Throwable t) {
            System.out.println("Aviso al guardar metadatos de playlists: " + t.getMessage());
        } finally {
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception e) {}
            }
        }
    }

    private static synchronized String getActiveStoreName() {
        ensureMetaLoaded();
        for (int i = 0; i < playlistNames.size(); i++) {
            if (activePlaylistName.equals(playlistNames.elementAt(i))) {
                return (String) playlistStoreNames.elementAt(i);
            }
        }
        return DEFAULT_STORE;
    }

    private static synchronized String getStoreNameFor(String playlistName) {
        ensureMetaLoaded();
        if (playlistName == null) return DEFAULT_STORE;
        for (int i = 0; i < playlistNames.size(); i++) {
            if (playlistName.equals(playlistNames.elementAt(i))) {
                return (String) playlistStoreNames.elementAt(i);
            }
        }
        return null;
    }

    public static synchronized Vector getPlaylistNames() {
        ensureMetaLoaded();
        Vector copy = new Vector();
        for (int i = 0; i < playlistNames.size(); i++) {
            copy.addElement(playlistNames.elementAt(i));
        }
        return copy;
    }

    public static synchronized String getActivePlaylistName() {
        ensureMetaLoaded();
        return activePlaylistName;
    }

    public static synchronized void setActivePlaylist(String name) {
        ensureMetaLoaded();
        if (name == null) return;
        for (int i = 0; i < playlistNames.size(); i++) {
            if (name.equals(playlistNames.elementAt(i))) {
                activePlaylistName = name;
                saveMeta();
                return;
            }
        }
    }

    public static synchronized boolean createPlaylist(String name) {
        ensureMetaLoaded();
        if (name == null) return false;
        String cleanName = name.trim();
        if (cleanName.length() == 0) return false;

        for (int i = 0; i < playlistNames.size(); i++) {
            if (cleanName.equalsIgnoreCase((String) playlistNames.elementAt(i))) {
                return false;
            }
        }

        long ts = System.currentTimeMillis() % 10000000L;
        String store = "pl_" + ts + "_" + playlistNames.size();
        if (store.length() > 30) {
            store = store.substring(0, 30);
        }

        saveStore(store, new Vector(), 0, false, 2);

        playlistNames.addElement(cleanName);
        playlistStoreNames.addElement(store);
        activePlaylistName = cleanName;
        saveMeta();
        return true;
    }

    public static synchronized boolean deletePlaylist(String name) {
        ensureMetaLoaded();
        if (name == null || playlistNames.size() <= 1) {
            return false;
        }

        int targetIdx = -1;
        for (int i = 0; i < playlistNames.size(); i++) {
            if (name.equals(playlistNames.elementAt(i))) {
                targetIdx = i;
                break;
            }
        }

        if (targetIdx == -1) return false;

        String store = (String) playlistStoreNames.elementAt(targetIdx);
        try {
            RecordStore.deleteRecordStore(store);
        } catch (Throwable t) {
            // ignorar
        }

        playlistNames.removeElementAt(targetIdx);
        playlistStoreNames.removeElementAt(targetIdx);

        if (name.equals(activePlaylistName)) {
            activePlaylistName = (String) playlistNames.elementAt(0);
        }

        saveMeta();
        return true;
    }

    public static synchronized int getTrackCount(String playlistName) {
        String store = getStoreNameFor(playlistName);
        if (store == null) return 0;
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(store, false);
            if (rs != null && rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                if (data != null && data.length >= 4) {
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    DataInputStream dis = new DataInputStream(bais);
                    return dis.readInt();
                }
            }
        } catch (Throwable t) {
            // ignorar
        } finally {
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception e) {}
            }
        }
        return 0;
    }

    public static synchronized void save(Vector playlist, int currentIndex, boolean shuffle, int repeatMode) {
        saveStore(getActiveStoreName(), playlist, currentIndex, shuffle, repeatMode);
    }

    public static synchronized void save(Vector playlist, int currentIndex) {
        save(playlist, currentIndex, false, 2);
    }

    private static synchronized void saveStore(String storeName, Vector playlist, int currentIndex, boolean shuffle, int repeatMode) {
        RecordStore rs = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            int size = (playlist != null) ? playlist.size() : 0;
            dos.writeInt(size);
            dos.writeInt(currentIndex);

            for (int i = 0; i < size; i++) {
                Track t = (Track) playlist.elementAt(i);
                dos.writeUTF(t.getTitle() != null ? t.getTitle() : "");
                dos.writeUTF(t.getSource() != null ? t.getSource() : "");
                dos.writeBoolean(t.isResource());
            }

            dos.writeBoolean(shuffle);
            dos.writeInt(repeatMode);

            dos.flush();
            byte[] data = baos.toByteArray();

            rs = RecordStore.openRecordStore(storeName, true);
            if (rs.getNumRecords() == 0) {
                rs.addRecord(data, 0, data.length);
            } else {
                rs.setRecord(1, data, 0, data.length);
            }
        } catch (Throwable t) {
            System.out.println("Aviso al guardar playlist en RMS (" + storeName + "): " + t.getMessage());
        } finally {
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception e) {}
            }
        }
    }

    public static synchronized Vector load() {
        return loadStore(getActiveStoreName());
    }

    public static synchronized Vector loadPlaylistTracks(String playlistName) {
        String store = getStoreNameFor(playlistName);
        return (store != null) ? loadStore(store) : new Vector();
    }

    private static synchronized Vector loadStore(String storeName) {
        RecordStore rs = null;
        Vector list = new Vector();
        try {
            rs = RecordStore.openRecordStore(storeName, false);
            if (rs != null && rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                if (data != null && data.length > 0) {
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    DataInputStream dis = new DataInputStream(bais);

                    int count = dis.readInt();
                    dis.readInt(); // savedIndex

                    for (int i = 0; i < count; i++) {
                        String title = dis.readUTF();
                        String source = dis.readUTF();
                        boolean isRes = dis.readBoolean();
                        list.addElement(new Track(title, source, isRes));
                    }
                }
            }
        } catch (Throwable t) {
            // ignorar
        } finally {
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception e) {}
            }
        }
        return list;
    }

    public static synchronized int loadCurrentIndex() {
        String store = getActiveStoreName();
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(store, false);
            if (rs != null && rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                if (data != null && data.length > 0) {
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    DataInputStream dis = new DataInputStream(bais);
                    int count = dis.readInt();
                    int index = dis.readInt();
                    return (index >= 0 && index < count) ? index : 0;
                }
            }
        } catch (Throwable t) {
            // ignorar
        } finally {
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception e) {}
            }
        }
        return 0;
    }

    public static synchronized boolean loadShuffle() {
        String store = getActiveStoreName();
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(store, false);
            if (rs != null && rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                if (data != null && data.length > 0) {
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    DataInputStream dis = new DataInputStream(bais);
                    int count = dis.readInt();
                    dis.readInt(); // index
                    for (int i = 0; i < count; i++) {
                        dis.readUTF();
                        dis.readUTF();
                        dis.readBoolean();
                    }
                    if (dis.available() > 0) {
                        return dis.readBoolean();
                    }
                }
            }
        } catch (Throwable t) {
            // ignorar
        } finally {
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception e) {}
            }
        }
        return false;
    }

    public static synchronized int loadRepeatMode() {
        String store = getActiveStoreName();
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(store, false);
            if (rs != null && rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                if (data != null && data.length > 0) {
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    DataInputStream dis = new DataInputStream(bais);
                    int count = dis.readInt();
                    dis.readInt(); // index
                    for (int i = 0; i < count; i++) {
                        dis.readUTF();
                        dis.readUTF();
                        dis.readBoolean();
                    }
                    if (dis.available() > 0) {
                        dis.readBoolean(); // skip shuffle
                        if (dis.available() >= 4) {
                            return dis.readInt();
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // ignorar
        } finally {
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception e) {}
            }
        }
        return 2; // Default REPEAT_ALL
    }

    public static synchronized void deleteStore() {
        try {
            RecordStore.deleteRecordStore(getActiveStoreName());
        } catch (Throwable t) {
            // ignorar
        }
    }
}
