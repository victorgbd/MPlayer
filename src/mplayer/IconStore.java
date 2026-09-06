package mplayer;

import javax.microedition.lcdui.Image;

/**
 * Almacén central de recursos gráficos e iconos del reproductor.
 * Carga imágenes PNG transparentes desde el paquete /icons/.
 */
public class IconStore {

    public static Image play;
    public static Image pause;
    public static Image stop;
    public static Image prev;
    public static Image next;
    public static Image rwd;
    public static Image fwd;
    public static Image volSpeaker;
    public static Image volMute;
    public static Image volPlus;
    public static Image volMinus;
    public static Image playlist;
    public static Image folder;
    public static Image repeat;
    public static Image repeatAll;
    public static Image repeatOne;
    public static Image repeatOff;
    public static Image musicNote;
    public static Image shuffle;
    public static Image shuffleOff;

    public static void load() {
        try {
            play = Image.createImage("/icons/play.png");
            pause = Image.createImage("/icons/pause.png");
            stop = Image.createImage("/icons/stop.png");
            prev = Image.createImage("/icons/prev.png");
            next = Image.createImage("/icons/next.png");
            rwd = Image.createImage("/icons/rwd.png");
            fwd = Image.createImage("/icons/fwd.png");
            volSpeaker = Image.createImage("/icons/vol_speaker.png");
            volMute = Image.createImage("/icons/vol_mute.png");
            volPlus = Image.createImage("/icons/vol_plus.png");
            volMinus = Image.createImage("/icons/vol_minus.png");
            playlist = Image.createImage("/icons/playlist.png");
            folder = Image.createImage("/icons/folder.png");
            repeat = Image.createImage("/icons/repeat.png");
            repeatAll = Image.createImage("/icons/repeat_all.png");
            repeatOne = Image.createImage("/icons/repeat_one.png");
            repeatOff = Image.createImage("/icons/repeat_off.png");
            musicNote = Image.createImage("/icons/music_note.png");
            shuffle = Image.createImage("/icons/shuffle.png");
            shuffleOff = Image.createImage("/icons/shuffle_off.png");
        } catch (Throwable t) {
            System.out.println("Aviso al cargar iconos: " + t.getMessage());
        }
    }
}
