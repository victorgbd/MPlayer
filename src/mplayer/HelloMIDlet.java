package mplayer;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.midlet.MIDlet;

/**
 * MIDlet Principal de MPlayer optimizado para Sony Ericsson K800i (240x320 QVGA).
 * Toda la suite de pantallas corre en modo Pantalla Completa nativo (Canvas).
 */
public class HelloMIDlet extends MIDlet {

    private boolean isInitialized = false;

    private AudioEngine engine;
    private PlayerCanvas playerCanvas;
    private PlaylistCanvas playlistCanvas;
    private FileBrowserCanvas fileBrowserCanvas;
    private HelpCanvas helpCanvas;
    private Displayable currentScreen;

    public HelloMIDlet() {
    }

    protected void startApp() {
        if (!isInitialized) {
            IconStore.load();
            engine = new AudioEngine();

            playerCanvas = new PlayerCanvas(this, engine);
            playlistCanvas = new PlaylistCanvas(this, engine);
            fileBrowserCanvas = new FileBrowserCanvas(this, engine);
            helpCanvas = new HelpCanvas(this);

            isInitialized = true;
            showPlayer();
        } else {
            if (currentScreen != null) {
                Display.getDisplay(this).setCurrent(currentScreen);
            } else {
                showPlayer();
            }
        }
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) {
        if (engine != null) {
            engine.savePlaylist();
            engine.release();
        }
    }

    public void exitMIDlet() {
        destroyApp(true);
        notifyDestroyed();
    }

    public void minimizeApp() {
        try {
            Display.getDisplay(this).setCurrent(null);
        } catch (Throwable t) {
        }
    }

    // ==========================================
    // Navegacion entre Pantallas (Todas Fullscreen)
    // ==========================================

    public void showPlayer() {
        currentScreen = playerCanvas;
        Display.getDisplay(this).setCurrent(playerCanvas);
    }

    public void showPlaylist() {
        currentScreen = playlistCanvas;
        playlistCanvas.refreshList();
        Display.getDisplay(this).setCurrent(playlistCanvas);
    }

    public void showFileBrowser() {
        try {
            currentScreen = fileBrowserCanvas;
            fileBrowserCanvas.loadDirectory("");
            Display.getDisplay(this).setCurrent(fileBrowserCanvas);
        } catch (Throwable t) {
            showError("No Soportado", "El dispositivo no soporta JSR-75 (FileConnection).");
        }
    }


    public void showHelp() {
        currentScreen = helpCanvas;
        Display.getDisplay(this).setCurrent(helpCanvas);
    }

    public void promptCreatePlaylist() {
        int count = engine.getPlaylistNames().size();
        final javax.microedition.lcdui.TextBox tb = new javax.microedition.lcdui.TextBox(
                "Nueva Playlist", "Lista " + (count + 1), 24, javax.microedition.lcdui.TextField.ANY);
        final javax.microedition.lcdui.Command cmdOk = new javax.microedition.lcdui.Command("Guardar", javax.microedition.lcdui.Command.OK, 1);
        final javax.microedition.lcdui.Command cmdCancel = new javax.microedition.lcdui.Command("Cancelar", javax.microedition.lcdui.Command.CANCEL, 2);
        tb.addCommand(cmdOk);
        tb.addCommand(cmdCancel);
        tb.setCommandListener(new javax.microedition.lcdui.CommandListener() {
            public void commandAction(javax.microedition.lcdui.Command c, javax.microedition.lcdui.Displayable d) {
                if (c == cmdOk) {
                    String name = tb.getString();
                    if (name != null && name.trim().length() > 0) {
                        boolean created = engine.createPlaylist(name.trim());
                        if (!created) {
                            showError("Aviso", "Ya existe una playlist con ese nombre.");
                            return;
                        }
                    }
                }
                showPlaylist();
            }
        });
        Display.getDisplay(this).setCurrent(tb);
    }

    public void showError(String title, String message) {
        Alert alert = new Alert(title, message, null, AlertType.ERROR);
        alert.setTimeout(3500);
        Displayable current = Display.getDisplay(this).getCurrent();
        Display.getDisplay(this).setCurrent(alert, current != null ? current : playerCanvas);
    }
}
