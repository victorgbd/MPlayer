package mplayer;

/**
 * Representa una pista de audio en el reproductor.
 * Compatible con CLDC 1.1 / MIDP 2.0 y formatos de Sony Ericsson K800i.
 */
public class Track {
    private String title;
    private String source;
    private boolean isResource;

    public Track(String title, String source, boolean isResource) {
        this.title = title;
        this.source = source;
        this.isResource = isResource;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSource() {
        return source;
    }

    public boolean isResource() {
        return isResource;
    }

    /**
     * Retorna la extensión o formato del archivo (MP3, AAC, WMA, WAV, MID, AMR, etc.)
     */
    public String getFormat() {
        String s = (source != null && source.length() > 0) ? source : title;
        if (s == null) return "AUDIO";
        int dot = s.lastIndexOf('.');
        if (dot != -1 && dot < s.length() - 1) {
            String ext = s.substring(dot + 1).toUpperCase();
            int slash = ext.indexOf('/');
            if (slash != -1) ext = ext.substring(0, slash);
            int qmark = ext.indexOf('?');
            if (qmark != -1) ext = ext.substring(0, qmark);
            if (ext.length() <= 4) {
                return ext;
            }
        }
        return "MP3";
    }

    public String toString() {
        return title;
    }
}
