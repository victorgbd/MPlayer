package mplayer;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Pantalla principal de MPlayer optimizada a pantalla completa para Sony Ericsson K800i:
 * - Modo Pantalla Completa nativo (setFullScreenMode(true)) a resolucion exacta 240x320 QVGA.
 * - Barra de softkeys integrada nativa: "Menu" (Softkey Izquierdo) y "Salir" (Softkey Derecho).
 * - Menu flotante in-canvas para acceso rapido a todas las funciones sin salir del reproductor.
 * - Mapeo completo de teclado fisico, joystick de 5 direcciones, teclas laterales de volumen (-11, -12) y tecla 'C' (-8).
 */
public class PlayerCanvas extends Canvas implements AudioEngineListener {

    private final HelloMIDlet midlet;
    private final AudioEngine engine;


    // Timer de animacion
    private Timer animTimer;

    // Espectro visualizador
    private static final int NUM_BARS = 14;
    private final int[] barHeights = new int[NUM_BARS];
    private final int[] barPeaks = new int[NUM_BARS];
    private final Random random = new Random();

    // Desplazamiento de texto largo (Marquee)
    private int marqueeOffset = 0;
    private int marqueePauseTicks = 0;

    // Coordenadas calculadas para la pantalla 240x320
    private int progressBarX, progressBarY, progressBarW, progressBarH;
    private int volBarX, volBarY, volBarW, volBarH;
    private int volBtnMinusX, volBtnMinusY, volBtnPlusX, volBtnPlusY, volBtnSize;
    private int btnPrevX, btnRwdX, btnPlayX, btnStopX, btnFwdX, btnNextX;
    private int btnControlsY, btnSecondaryY, btnSize, btnHeroSize;

    // Estado visual de boton presionado (feedback de tecla fisica)
    private int pressedButton = -1;
    private int pressedTimerTicks = 0;
    private static final int BTN_NONE = -1;
    private static final int BTN_PREV = 0;
    private static final int BTN_RWD = 1;
    private static final int BTN_PLAY = 2;
    private static final int BTN_STOP = 3;
    private static final int BTN_FWD = 4;
    private static final int BTN_NEXT = 5;
    private static final int BTN_VOL_MINUS = 6;
    private static final int BTN_VOL_PLUS = 7;
    private static final int BTN_VOL_SLIDER = 8;
    private static final int BTN_SEEK = 9;

    // Modos de visualizacion estilo Windows Media Player
    public static final int VIZ_BARS = 0;
    public static final int VIZ_SCOPE = 1;
    public static final int VIZ_WARP = 2;
    public static final int VIZ_FIRE = 3;
    public static final int VIZ_CUBE = 4;
    public static final int VIZ_ROAD = 5;
    public static final int VIZ_COUNT = 6;

    private int activeVisualizer = VIZ_BARS;
    private boolean isScreensaverActive = false;

    // Coordenadas del area del visualizador en el deck para deteccion tactil
    private int vizAreaX, vizAreaY, vizAreaW, vizAreaH;

    // Submenu de visualizaciones / screensaver
    private boolean isVizMenuOpen = false;
    private int vizMenuSelectedIndex = 0;
    private static final String[] VIZ_MENU_ITEMS = {
        "1. Barras de Espectro",
        "2. Ondas Laser (Scope)",
        "3. Tunel Estelar (Warp)",
        "4. Fuego & Plasma",
        "5. Cubo 3D Rotativo",
        "6. Pista Infinita Retro",
        "7. Ver Pantalla Completa"
    };

    // Datos matematicos para Ondas Laser (Scope) y 3D
    private int wavePhase = 0;
    private static final int[] SIN_TABLE = new int[360];
    static {
        for (int i = 0; i < 360; i++) {
            SIN_TABLE[i] = (int) (Math.sin(i * Math.PI / 180.0) * 1000);
        }
    }

    private int sinDeg(int deg) {
        deg = deg % 360;
        if (deg < 0) deg += 360;
        return SIN_TABLE[deg];
    }

    private int cosDeg(int deg) {
        int d = (deg + 90) % 360;
        if (d < 0) d += 360;
        return SIN_TABLE[d];
    }

    // Datos para Tunel Estelar 3D (Warp)
    private static final int NUM_STARS = 45;
    private final int[] starX = new int[NUM_STARS];
    private final int[] starY = new int[NUM_STARS];
    private final int[] starZ = new int[NUM_STARS];

    // Datos para Llama de Fuego / Plasma
    private static final int NUM_FLAMES = 18;
    private final int[] flameHeights = new int[NUM_FLAMES];
    private int flameAnimTick = 0;

    // Datos para Cubo 3D Rotativo
    private int cubeRotX = 24;
    private int cubeRotY = 38;
    private int cubeRotZ = 12;
    private final int[] cubeProjX = new int[8];
    private final int[] cubeProjY = new int[8];
    private final int[] cubeInnerProjX = new int[8];
    private final int[] cubeInnerProjY = new int[8];
    private static final int[][] CUBE_VERTICES = {
        {-100, -100, -100}, // 0
        { 100, -100, -100}, // 1
        { 100,  100, -100}, // 2
        {-100,  100, -100}, // 3
        {-100, -100,  100}, // 4
        { 100, -100,  100}, // 5
        { 100,  100,  100}, // 6
        {-100,  100,  100}  // 7
    };
    private static final int[][] CUBE_FACES = {
        {0, 1, 2, 3}, // Frontal (Z=-100)
        {5, 4, 7, 6}, // Trasera (Z=+100)
        {4, 5, 1, 0}, // Superior (Y=-100)
        {3, 2, 6, 7}, // Inferior (Y=+100)
        {1, 5, 6, 2}, // Derecha (X=+100)
        {4, 0, 3, 7}  // Izquierda (X=-100)
    };
    private static final int[] CUBE_FACE_COLORS = {
        0x003366, // Frente: Azul Cyber
        0x101b2a, // Trasera: Pizarra oscuro
        0x004d40, // Superior: Verde Azulado
        0x311b92, // Inferior: Violeta profundo
        0x4a148c, // Derecha: Purpura neon
        0x006064  // Izquierda: Cian oscuro
    };
    private static final int[][] CUBE_EDGES = {
        {0, 1}, {1, 2}, {2, 3}, {3, 0},
        {4, 5}, {5, 6}, {6, 7}, {7, 4},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    // Datos para Pista Infinita Retro
    private int roadScroll = 0;
    private int roadAnimTick = 0;

    // Menu flotante de pantalla completa
    private boolean isMenuOpen = false;
    private int menuSelectedIndex = 0;
    private static final String[] MENU_ITEMS = {
        "1. Lista de Reproducci\u00F3n",
        "2. Explorar Archivos",
        "3. Modo Aleatorio",
        "4. Modo Repetici\u00F3n",
        "5. Efectos Visuales",
        "6. Ayuda",
        "7. Minimizar",
        "8. Salir"
    };

    public PlayerCanvas(HelloMIDlet midlet, AudioEngine engine) {
        this.midlet = midlet;
        this.engine = engine;
        this.engine.setListener(this);
        initStarfield();

        // Activar pantalla completa para Sony Ericsson K800i (240x320 completo)
        setFullScreenMode(true);
    }

    protected void showNotify() {
        setFullScreenMode(true);
        startAnimation();
    }

    protected void hideNotify() {
        stopAnimation();
    }

    private synchronized void startAnimation() {
        stopAnimation();
        animTimer = new Timer();
        animTimer.schedule(new TimerTask() {
            public void run() {
                updateVisuals();
                repaint();
            }
        }, 50, 50); // ~20 FPS fluido en K800i
    }

    private synchronized void stopAnimation() {
        if (animTimer != null) {
            animTimer.cancel();
            animTimer = null;
        }
    }

    private void initStarfield() {
        for (int i = 0; i < NUM_STARS; i++) {
            resetStar(i);
            starZ[i] = random.nextInt(250) + 5;
        }
    }

    private void resetStar(int i) {
        starX[i] = random.nextInt(200) - 100;
        starY[i] = random.nextInt(200) - 100;
        starZ[i] = 255;
    }

    private void updateVisuals() {
        if (pressedTimerTicks > 0) {
            pressedTimerTicks--;
            if (pressedTimerTicks == 0) {
                pressedButton = BTN_NONE;
            }
        }

        int state = engine.getState();

        // 1. Barras de espectro
        if (state == AudioEngine.STATE_PLAYING) {
            for (int i = 0; i < NUM_BARS; i++) {
                int target = random.nextInt(100);
                if (target > barHeights[i]) {
                    barHeights[i] = target;
                } else {
                    barHeights[i] = Math.max(0, barHeights[i] - 12);
                }
                if (barHeights[i] > barPeaks[i]) {
                    barPeaks[i] = barHeights[i];
                } else {
                    barPeaks[i] = Math.max(0, barPeaks[i] - 3);
                }
            }
        } else if (state == AudioEngine.STATE_PAUSED) {
            // Congelar barras
        } else {
            // STOP / SIN PISTAS: decaer suavemente a cero
            for (int i = 0; i < NUM_BARS; i++) {
                barHeights[i] = Math.max(0, barHeights[i] - 16);
                barPeaks[i] = Math.max(0, barPeaks[i] - 6);
            }
        }

        // 2. Ondas Laser (Scope)
        wavePhase = (wavePhase + (state == AudioEngine.STATE_PLAYING ? 10 : 3)) % 360;

        // 3. Tunel Estelar 3D (Warp)
        int starSpeed = (state == AudioEngine.STATE_PLAYING ? 14 : 3);
        for (int i = 0; i < NUM_STARS; i++) {
            starZ[i] -= starSpeed;
            if (starZ[i] <= 4) {
                resetStar(i);
            }
        }

        // 4. Llama de Fuego / Plasma
        flameAnimTick++;
        if (state == AudioEngine.STATE_PLAYING) {
            for (int i = 0; i < NUM_FLAMES; i++) {
                int deg = ((flameAnimTick * 14) + (i * 360 / NUM_FLAMES)) % 360;
                int waveVal = (SIN_TABLE[deg] + 1000) * 45 / 2000;
                int target = waveVal + random.nextInt(55);
                if (target > flameHeights[i]) {
                    flameHeights[i] = target;
                } else {
                    flameHeights[i] = Math.max(0, flameHeights[i] - 10);
                }
            }
        } else {
            for (int i = 0; i < NUM_FLAMES; i++) {
                flameHeights[i] = Math.max(0, flameHeights[i] - 12);
            }
        }

        // 5. Cubo 3D Rotativo
        int cubeSpeed = (state == AudioEngine.STATE_PLAYING ? 4 : 1);
        cubeRotX = (cubeRotX + cubeSpeed) % 360;
        cubeRotY = (cubeRotY + cubeSpeed * 2) % 360;
        cubeRotZ = (cubeRotZ + cubeSpeed) % 360;

        // 6. Pista Infinita Retro con Auto
        int roadSpeed = (state == AudioEngine.STATE_PLAYING ? 24 : 5);
        roadScroll = (roadScroll + roadSpeed) % 10000;
        roadAnimTick++;

        // Marquee para titulos largos
        Track current = engine.getCurrentTrack();
        if (current != null) {
            Font boldFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
            int textW = boldFont.stringWidth(current.getTitle());
            int lcdInnerW = getWidth() - 32;
            if (textW > lcdInnerW) {
                if (marqueePauseTicks > 0) {
                    marqueePauseTicks--;
                } else {
                    marqueeOffset += 2;
                    if (marqueeOffset > textW + 25) {
                        marqueeOffset = 0;
                        marqueePauseTicks = 20;
                    }
                }
            } else {
                marqueeOffset = 0;
            }
        }
    }

    // ==========================================
    // Renderizado Grafico (240x320 K800i Fullscreen)
    // ==========================================

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        // 0. Modo Screensaver WMP a pantalla completa
        if (isScreensaverActive) {
            paintScreensaver(g, w, h);
            return;
        }

        // 1. Fondo negro Sony Ericsson Cyber-shot / Walkman
        g.setColor(0x0a0f16);
        g.fillRect(0, 0, w, h);

        // 2. Cabecera (Header K800i)
        int headerH = 26;
        g.setColor(0x111924);
        g.fillRect(0, 0, w, headerH);
        g.setColor(0x00d2ff);
        g.drawLine(0, headerH - 1, w, headerH - 1);

        if (IconStore.musicNote != null) {
            g.drawImage(IconStore.musicNote, 5, 2, Graphics.TOP | Graphics.LEFT);
        }

        Font boldSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        g.setFont(boldSmall);
        g.setColor(0x00e5ff);
        g.drawString("MPLAYER", 32, 6, Graphics.TOP | Graphics.LEFT);

        paintStateBadge(g, w - 8, 4);

        // 3. Tarjeta / Deck LCD (Central)
        int cardMargin = 6;
        int cardX = cardMargin;
        int cardY = headerH + 4;
        int cardW = w - (cardMargin * 2); // 228px
        int cardH = 186;

        // Marco y fondo del LCD
        g.setColor(0x06090e);
        g.fillRect(cardX, cardY, cardW, cardH);
        g.setColor(0x1f2e42);
        g.drawRect(cardX, cardY, cardW - 1, cardH - 1);
        g.setColor(0x101824);
        g.drawRect(cardX + 1, cardY + 1, cardW - 3, cardH - 3);

        int innerX = cardX + 7;
        int innerW = cardW - 14;
        int curY = cardY + 5;

        // Si NO hay canciones cargadas en la lista, mostrar pantalla de bienvenida K800i
        if (engine.getPlaylistSize() == 0) {
            paintEmptyState(g, cardX, cardY, cardW, cardH);
        } else {
            // Contenido LCD normal cuando hay musica cargada:
            Track curTrack = engine.getCurrentTrack();
            String fmt = (curTrack != null) ? curTrack.getFormat() : "MP3";

            // Fila 1: Pista actual, formato y Modo de repeticion
            g.setFont(boldSmall);
            g.setColor(0x6b829e);
            int totalTracks = engine.getPlaylistSize();
            int curIndex = engine.getCurrentTrackIndex() + 1;
            String trackStr = "PISTA " + curIndex + "/" + totalTracks + " [" + fmt + "]";
            g.drawString(trackStr, innerX, curY, Graphics.TOP | Graphics.LEFT);

            Image repIcon = getCurrentRepeatIcon();
            if (repIcon != null) {
                g.drawImage(repIcon, innerX + innerW - 20, curY - 2, Graphics.TOP | Graphics.LEFT);
            }

            Image shufIcon = engine.isShuffle() ? IconStore.shuffle : IconStore.shuffleOff;
            if (shufIcon != null) {
                g.drawImage(shufIcon, innerX + innerW - 42, curY - 2, Graphics.TOP | Graphics.LEFT);
            }
            curY += 15;

            // Fila 2: Titulo de la pista (Marquee)
            String title = (curTrack != null) ? curTrack.getTitle() : "";

            Font titleFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
            g.setFont(titleFont);
            g.setColor(0xffffff);

            int oldClipX = g.getClipX();
            int oldClipY = g.getClipY();
            int oldClipW = g.getClipWidth();
            int oldClipH = g.getClipHeight();

            g.setClip(innerX, curY, innerW, titleFont.getHeight() + 2);
            g.drawString(title, innerX - marqueeOffset, curY, Graphics.TOP | Graphics.LEFT);
            g.setClip(oldClipX, oldClipY, oldClipW, oldClipH);
            curY += titleFont.getHeight() + 4;

            // Fila 3: Visualizador WMP (Barras, Laser, Tunel Warp, Fuego)
            // Llenar el contenedor ampliando el area de animacion
            int eqH = (cardY + cardH) - curY - 39;
            if (eqH < 60) eqH = 60;
            vizAreaX = innerX;
            vizAreaY = curY;
            vizAreaW = innerW;
            vizAreaH = eqH;
            paintActiveVisualizer(g, innerX, curY, innerW, eqH);
            curY += eqH + 5;

            // Fila 4: Tiempo digital (Transcurrido / Total)
            long mediaTime = engine.getMediaTime();
            long duration = engine.getDuration();
            String timeStr = formatTime(mediaTime);
            String durStr = formatTime(duration);

            g.setFont(boldSmall);
            g.setColor(0x00e5ff);
            g.drawString(timeStr, innerX, curY, Graphics.TOP | Graphics.LEFT);
            g.setColor(0x5c7b99);
            g.drawString("/ " + durStr, innerX + boldSmall.stringWidth(timeStr) + 4, curY, Graphics.TOP | Graphics.LEFT);

            int pct = engine.getProgressPercent();
            g.setColor(0x00d2ff);
            g.drawString(pct + "%", innerX + innerW, curY, Graphics.TOP | Graphics.RIGHT);
            curY += 14;

            // Fila 5: Barra de progreso
            progressBarX = innerX;
            progressBarY = curY;
            progressBarW = innerW;
            progressBarH = 8;

            g.setColor(0x0e151f);
            g.fillRect(progressBarX, progressBarY, progressBarW, progressBarH);
            g.setColor(0x24354a);
            g.drawRect(progressBarX, progressBarY, progressBarW - 1, progressBarH - 1);

            int fillW = (progressBarW * pct) / 100;
            if (fillW > 0) {
                g.setColor(0x00c4ff);
                g.fillRect(progressBarX + 1, progressBarY + 1, fillW - 2, progressBarH - 2);

                int knobX = progressBarX + fillW - 3;
                if (knobX < progressBarX) knobX = progressBarX;
                if (knobX > progressBarX + progressBarW - 6) knobX = progressBarX + progressBarW - 6;
                g.setColor(0xffffff);
                g.fillRect(knobX, progressBarY - 2, 6, progressBarH + 4);
            }
        }

        // 4. Area de Volumen (Debajo del LCD)
        int volAreaY = cardY + cardH + 5;
        volBtnSize = 20;

        Image speakerImg = engine.isMuted() ? IconStore.volMute : IconStore.volSpeaker;
        if (speakerImg != null) {
            g.drawImage(speakerImg, cardMargin + 2, volAreaY + 1, Graphics.TOP | Graphics.LEFT);
        }

        // Boton Bajar Volumen (-)
        volBtnMinusX = cardMargin + 24;
        volBtnMinusY = volAreaY;
        paintIconButton(g, volBtnMinusX, volBtnMinusY, volBtnSize, volBtnSize, IconStore.volMinus, "-", (pressedButton == BTN_VOL_MINUS), 0x16202e);

        // Barra de nivel de volumen
        volBarX = volBtnMinusX + volBtnSize + 4;
        volBarY = volAreaY + 3;
        volBarW = 104;
        volBarH = 14;

        g.setColor(0x0a1017);
        g.fillRect(volBarX, volBarY, volBarW, volBarH);
        g.setColor(0x223246);
        g.drawRect(volBarX, volBarY, volBarW - 1, volBarH - 1);

        int volLevel = engine.isMuted() ? 0 : engine.getVolume();
        int volFill = (volBarW * volLevel) / 100;
        if (volFill > 0) {
            g.setColor(engine.isMuted() ? 0xaa3333 : 0x00c853);
            g.fillRect(volBarX + 1, volBarY + 1, volFill - 2, volBarH - 2);
        }

        // Boton Subir Volumen (+)
        volBtnPlusX = volBarX + volBarW + 4;
        volBtnPlusY = volAreaY;
        paintIconButton(g, volBtnPlusX, volBtnPlusY, volBtnSize, volBtnSize, IconStore.volPlus, "+", (pressedButton == BTN_VOL_PLUS), 0x16202e);

        g.setFont(boldSmall);
        g.setColor(engine.isMuted() ? 0xff4444 : 0x00d2ff);
        String volText = engine.isMuted() ? "MUTE" : (engine.getVolume() + "%");
        g.drawString(volText, w - cardMargin, volAreaY + 3, Graphics.TOP | Graphics.RIGHT);

        // 5. Panel de Botones de Reproduccion
        btnControlsY = volAreaY + volBtnSize + 7;
        btnSize = 30;
        btnHeroSize = 44;
        btnSecondaryY = btnControlsY + 7;

        btnPrevX = 7;
        btnRwdX  = 43;
        btnPlayX = 79;
        btnStopX = 127;
        btnFwdX  = 163;
        btnNextX = 199;

        paintIconButton(g, btnPrevX, btnSecondaryY, btnSize, btnSize, IconStore.prev, "|<", (pressedButton == BTN_PREV), 0x141d2a);
        paintIconButton(g, btnRwdX,  btnSecondaryY, btnSize, btnSize, IconStore.rwd,  "<<", (pressedButton == BTN_RWD),  0x141d2a);

        // Boton HERO Central Play/Pausa
        boolean isPlaying = (engine.getState() == AudioEngine.STATE_PLAYING);
        Image playPauseIcon = isPlaying ? IconStore.pause : IconStore.play;
        int playBg = isPlaying ? 0x00875a : 0x005ebb;
        paintIconButton(g, btnPlayX, btnControlsY, btnHeroSize, btnHeroSize, playPauseIcon, (isPlaying ? "||" : ">"), (pressedButton == BTN_PLAY), playBg);

        paintIconButton(g, btnStopX, btnSecondaryY, btnSize, btnSize, IconStore.stop, "[]", (pressedButton == BTN_STOP), 0x141d2a);
        paintIconButton(g, btnFwdX,  btnSecondaryY, btnSize, btnSize, IconStore.fwd,  ">>", (pressedButton == BTN_FWD),  0x141d2a);
        paintIconButton(g, btnNextX, btnSecondaryY, btnSize, btnSize, IconStore.next, ">|", (pressedButton == BTN_NEXT), 0x141d2a);

        // 7. Barra de Softkeys Sony Ericsson K800i (Pantalla Completa)
        int softkeyY = h - 22;
        int softkeyH = 22;

        g.setColor(0x0d141e);
        g.fillRect(0, softkeyY, w, softkeyH);
        g.setColor(0x00d2ff);
        // Linea superior corregida: horizontal recta sobre softkeyY (evita raya diagonal)
        g.drawLine(0, softkeyY, w, softkeyY);
        g.setColor(0x192738);
        g.drawLine(0, softkeyY + 1, w, softkeyY + 1);

        Font softkeyFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        g.setFont(softkeyFont);

        if (isMenuOpen || isVizMenuOpen) {
            g.setColor(0x00ff88);
            g.drawString("OK: Elegir", 8, softkeyY + 3, Graphics.TOP | Graphics.LEFT);
            g.setColor(0xff7777);
            g.drawString("Volver", w - 8, softkeyY + 3, Graphics.TOP | Graphics.RIGHT);
        } else {
            g.setColor(0x00d2ff);
            g.drawString("Men\u00FA", 8, softkeyY + 3, Graphics.TOP | Graphics.LEFT);

            g.setColor(0x4b6685);
            g.drawString("MPlayer", w / 2, softkeyY + 3, Graphics.TOP | Graphics.HCENTER);

            g.setColor(0xff7777);
            g.drawString("Salir", w - 8, softkeyY + 3, Graphics.TOP | Graphics.RIGHT);
        }

        // 8. Menu Flotante de Opciones / Submenu de Visualizadores
        if (isMenuOpen) {
            paintOptionsMenu(g, w, h);
        } else if (isVizMenuOpen) {
            paintVizMenu(g, w, h);
        }
    }

    private void paintEmptyState(Graphics g, int cardX, int cardY, int cardW, int cardH) {
        if (IconStore.musicNote != null) {
            int iconX = cardX + (cardW - IconStore.musicNote.getWidth()) / 2;
            g.drawImage(IconStore.musicNote, iconX, cardY + 22, Graphics.TOP | Graphics.LEFT);
        }

        Font boldFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
        g.setFont(boldFont);
        g.setColor(0xffffff);
        g.drawString("SIN M\u00DASICA CARGADA", cardX + (cardW / 2), cardY + 54, Graphics.TOP | Graphics.HCENTER);

        Font small = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(small);
        g.setColor(0x00d2ff);
        g.drawString("Men\u00FA -> Ayuda para ver teclas", cardX + (cardW / 2), cardY + 80, Graphics.TOP | Graphics.HCENTER);

        g.setColor(0x8ba6c6);
        g.drawString("Pulsa [#] o Joystick para buscar", cardX + (cardW / 2), cardY + 102, Graphics.TOP | Graphics.HCENTER);

        // Caja de sugerencia rapida
        int boxW = cardW - 32;
        int boxH = 26;
        int boxY = cardY + 130;
        g.setColor(0x131f2d);
        g.fillRoundRect(cardX + 16, boxY, boxW, boxH, 6, 6);
        g.setColor(0x273d57);
        g.drawRoundRect(cardX + 16, boxY, boxW, boxH, 6, 6);

        Font boldSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        g.setFont(boldSmall);
        g.setColor(0x66ffb2);
        g.drawString("Pulsa Joystick [OK] para buscar", cardX + (cardW / 2), boxY + 5, Graphics.TOP | Graphics.HCENTER);
    }

    private void paintStateBadge(Graphics g, int rightX, int y) {
        String label = "";
        int bg = 0x223344;
        int fg = 0xffffff;

        switch (engine.getState()) {
            case AudioEngine.STATE_PLAYING:
                label = "PLAY";
                bg = 0x007a48;
                fg = 0x00ff88;
                break;
            case AudioEngine.STATE_PAUSED:
                label = "PAUSA";
                bg = 0x8a6d00;
                fg = 0xffe066;
                break;
            case AudioEngine.STATE_LOADING:
                label = "CARGA";
                bg = 0x005b8a;
                fg = 0x66d9ff;
                break;
            case AudioEngine.STATE_ERROR:
                label = "ERROR";
                bg = 0x8a1a1a;
                fg = 0xff7777;
                break;
            default:
                label = "STOP";
                bg = 0x1f2b38;
                fg = 0x8fa4bb;
                break;
        }

        Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        g.setFont(font);
        int badgeW = font.stringWidth(label) + 12;
        int badgeH = 18;
        int badgeX = rightX - badgeW;

        g.setColor(bg);
        g.fillRoundRect(badgeX, y, badgeW, badgeH, 6, 6);
        g.setColor(fg);
        g.drawString(label, badgeX + (badgeW / 2), y + 2, Graphics.TOP | Graphics.HCENTER);
    }

    private void paintEqualizer(Graphics g, int x, int y, int w, int h) {
        g.setColor(0x06090e);
        g.fillRect(x, y, w, h);

        int barW = (w - (NUM_BARS - 1) * 2) / NUM_BARS;
        if (barW < 4) barW = 4;
        int totalW = NUM_BARS * barW + (NUM_BARS - 1) * 2;
        int startX = x + (w - totalW) / 2;
        int step = (h > 60) ? 5 : 3;
        int segH = (h > 60) ? 4 : 2;

        for (int i = 0; i < NUM_BARS; i++) {
            int bx = startX + i * (barW + 2);
            int barHeightPx = (h * barHeights[i]) / 100;
            int peakHeightPx = (h * barPeaks[i]) / 100;

            int by = y + h - barHeightPx;

            g.setColor(0x0b1118);
            g.fillRect(bx, y, barW, h);

            if (barHeightPx > 0) {
                for (int py = y + h - segH; py >= by; py -= step) {
                    int relH = (y + h - py) * 100 / h;
                    if (relH > 80) {
                        g.setColor(0xff1744); // Rojo neon
                    } else if (relH > 55) {
                        g.setColor(0xffea00); // Amarillo oro
                    } else {
                        g.setColor(0x00e676); // Verde esmeralda
                    }
                    g.fillRect(bx, py, barW, segH);
                }
            }

            if (peakHeightPx > 0) {
                int peakY = y + h - peakHeightPx;
                g.setColor(0xffffff);
                g.fillRect(bx, peakY, barW, segH);
            }
        }
    }

    private void paintScope(Graphics g, int x, int y, int w, int h) {
        g.setColor(0x050910);
        g.fillRect(x, y, w, h);

        int cy = y + (h / 2);
        // Linea central de guia
        g.setColor(0x101b2a);
        g.drawLine(x, cy, x + w, cy);

        int amp = (h * 42) / 100;
        if (engine.getState() != AudioEngine.STATE_PLAYING) {
            amp = 3;
        }

        int prevX = x;
        int prevY1 = cy;
        int prevY2 = cy;
        int prevY3 = cy;
        int step = (w > 200) ? 4 : 3;

        for (int px = x; px <= x + w; px += step) {
            int relX = px - x;
            int deg1 = (wavePhase * 2 + relX * 360 / w * 2) % 360;
            if (deg1 < 0) deg1 += 360;
            int deg2 = (wavePhase * 3 + relX * 360 / w * 3 + 90) % 360;
            if (deg2 < 0) deg2 += 360;
            int deg3 = (wavePhase + relX * 360 / w + 180) % 360;
            if (deg3 < 0) deg3 += 360;

            int py1 = cy + (amp * SIN_TABLE[deg1]) / 1000;
            int py2 = cy + ((amp * 7 / 10) * SIN_TABLE[deg2]) / 1000;
            int py3 = cy + ((amp / 2) * SIN_TABLE[deg3]) / 1000;

            if (px > x) {
                // Capa 3: Verde Neon (profundidad)
                g.setColor(0x009944);
                g.drawLine(prevX, prevY3, px, py3);

                // Capa 2: Magenta WMP
                g.setColor(0xd500f9);
                g.drawLine(prevX, prevY2, px, py2);

                // Capa 1: Cyan Laser brillante
                g.setColor(0x00e5ff);
                g.drawLine(prevX, prevY1, px, py1);
            }
            prevX = px;
            prevY1 = py1;
            prevY2 = py2;
            prevY3 = py3;
        }
    }

    private void paintStarfield(Graphics g, int x, int y, int w, int h) {
        g.setColor(0x020408);
        g.fillRect(x, y, w, h);

        int cx = x + (w / 2);
        int cy = y + (h / 2);
        int scale = (w < h ? w : h) / 2;
        boolean isPlaying = (engine.getState() == AudioEngine.STATE_PLAYING);
        int starSpeed = isPlaying ? 14 : 3;

        for (int i = 0; i < NUM_STARS; i++) {
            int z = starZ[i];
            if (z <= 0) continue;

            int sx = cx + (starX[i] * scale) / z;
            int sy = cy + (starY[i] * scale) / z;

            if (sx >= x && sx < x + w && sy >= y && sy < y + h) {
                int prevZ = z + starSpeed;
                int px = cx + (starX[i] * scale) / prevZ;
                int py = cy + (starY[i] * scale) / prevZ;

                if (z < 65) {
                    // Cercano y ultra rapido: traza de luz cyan y particula blanca
                    g.setColor(0x00d2ff);
                    g.drawLine(px, py, sx, sy);
                    g.setColor(0xffffff);
                    g.fillRect(sx - 1, sy - 1, 3, 3);
                } else if (z < 140) {
                    // Distancia media: traza azul suave
                    g.setColor(0x0077aa);
                    g.drawLine(px, py, sx, sy);
                    g.setColor(0x80d8ff);
                    g.fillRect(sx, sy, 2, 2);
                } else {
                    // Distante: punto azul oscuro
                    g.setColor(0x2d4866);
                    g.fillRect(sx, sy, 1, 1);
                }
            }
        }
    }

    private void paintFire(Graphics g, int x, int y, int w, int h) {
        g.setColor(0x090302);
        g.fillRect(x, y, w, h);

        int flameW = (w - (NUM_FLAMES - 1) * 2) / NUM_FLAMES;
        if (flameW < 3) flameW = 3;
        int totalW = NUM_FLAMES * flameW + (NUM_FLAMES - 1) * 2;
        int startX = x + (w - totalW) / 2;
        int step = (h > 60) ? 4 : 2;

        for (int i = 0; i < NUM_FLAMES; i++) {
            int fx = startX + i * (flameW + 2);
            int flameH = (h * flameHeights[i]) / 100;
            if (flameH < 2) flameH = 2;
            int fy = y + h - flameH;

            for (int py = y + h - 1; py >= fy; py -= step) {
                int relH = (y + h - py) * 100 / h;
                if (relH < 18) {
                    g.setColor(0xffffff); // Nucleo blanco incandescente
                } else if (relH < 40) {
                    g.setColor(0xffea00); // Amarillo fuego
                } else if (relH < 70) {
                    g.setColor(0xff6d00); // Naranja plasma
                } else {
                    g.setColor(0xd50000); // Rojo fuego
                }
                g.fillRect(fx, py - (step - 1), flameW, step);
            }

            // Chispas flotantes sobre las columnas mas altas
            if (flameHeights[i] > 35) {
                int sparkY = fy - ((flameAnimTick * 4 + i * 7) % 16) - 2;
                if (sparkY >= y) {
                    g.setColor(0xffea00);
                    g.fillRect(fx + (flameW / 2), sparkY, 2, 2);
                }
            }
        }
    }

    private void paintCube(Graphics g, int x, int y, int w, int h) {
        // Fondo Sci-Fi Cyberpunk
        g.setColor(0x02050b);
        g.fillRect(x, y, w, h);

        int cx = x + (w / 2);
        int cy = y + (h / 2);

        int minDim = (w < h) ? w : h;
        boolean isPlaying = (engine.getState() == AudioEngine.STATE_PLAYING);
        int bass = isPlaying ? ((barHeights[0] + barHeights[1]) / 2) : 0;

        // Anillos radar / ondas sonoras concentricas de fondo
        int ringPhase = (roadAnimTick * 3) % 60;
        int maxR = minDim / 2;
        g.setColor(0x0a1624);
        for (int r = ringPhase; r < maxR; r += 20) {
            g.drawArc(cx - r, cy - r, r * 2, r * 2, 0, 360);
        }
        // Ejes guia cruzados tenues
        g.setColor(0x081320);
        g.drawLine(x, cy, x + w, cy);
        g.drawLine(cx, y, cx, y + h);

        // Escala del cubo segun dimensiones y beat
        // En pantalla pequena (deck, h < 100) le damos una proporcion mayor para que se aprecie nitido y grande
        int baseRadius;
        if (h < 100) {
            baseRadius = (h * 27) / 100;
            if (baseRadius < 18) baseRadius = 18;
        } else {
            baseRadius = (minDim * 26) / 100;
        }
        int beatExpand = isPlaying ? ((bass * baseRadius) / 350) : 0;
        int scale = baseRadius + beatExpand;

        // Matriz de rotacion (X, Y, Z)
        int cosX = cosDeg(cubeRotX), sinX = sinDeg(cubeRotX);
        int cosY = cosDeg(cubeRotY), sinY = sinDeg(cubeRotY);
        int cosZ = cosDeg(cubeRotZ), sinZ = sinDeg(cubeRotZ);

        int camDist = 300;
        int fov = 300;

        // 1. Proyectar los 8 vertices exteriores
        for (int i = 0; i < 8; i++) {
            int vx = (CUBE_VERTICES[i][0] * scale) / 100;
            int vy = (CUBE_VERTICES[i][1] * scale) / 100;
            int vz = (CUBE_VERTICES[i][2] * scale) / 100;

            // Rotar en X
            int y1 = (vy * cosX - vz * sinX) / 1000;
            int z1 = (vy * sinX + vz * cosX) / 1000;
            int x1 = vx;

            // Rotar en Y
            int x2 = (x1 * cosY + z1 * sinY) / 1000;
            int z2 = (-x1 * sinY + z1 * cosY) / 1000;
            int y2 = y1;

            // Rotar en Z
            int x3 = (x2 * cosZ - y2 * sinZ) / 1000;
            int y3 = (x2 * sinZ + y2 * cosZ) / 1000;
            int z3 = z2;

            int zProj = z3 + camDist;
            if (zProj < 10) zProj = 10;

            cubeProjX[i] = cx + (x3 * fov) / zProj;
            cubeProjY[i] = cy + (y3 * fov) / zProj;
        }

        // 2. Dibujar caras solidas visibles (Back-Face Culling)
        for (int f = 0; f < 6; f++) {
            int i0 = CUBE_FACES[f][0];
            int i1 = CUBE_FACES[f][1];
            int i2 = CUBE_FACES[f][2];
            int i3 = CUBE_FACES[f][3];

            int x0 = cubeProjX[i0], y0 = cubeProjY[i0];
            int x1 = cubeProjX[i1], y1 = cubeProjY[i1];
            int x2 = cubeProjX[i2], y2 = cubeProjY[i2];
            int x3 = cubeProjX[i3], y3 = cubeProjY[i3];

            // Producto vectorial 2D para culling
            int cross = (x1 - x0) * (y2 - y1) - (y1 - y0) * (x2 - x1);
            if (cross > 0) {
                // Cara frontal visible: sombreado reactivo
                int faceColor = CUBE_FACE_COLORS[f];
                if (isPlaying && bass > 40) {
                    faceColor = (faceColor + 0x181818) & 0xffffff;
                }
                g.setColor(faceColor);
                g.fillTriangle(x0, y0, x1, y1, x2, y2);
                g.fillTriangle(x0, y0, x2, y2, x3, y3);
            }
        }

        // 3. Proyectar y dibujar nucleo pulsante interior (Beat Core)
        int innerScale = (scale * (35 + (bass * 25 / 100))) / 100;
        int invRotX = (360 - cubeRotX) % 360;
        int invRotY = (360 - cubeRotY) % 360;
        int inCosX = cosDeg(invRotX), inSinX = sinDeg(invRotX);
        int inCosY = cosDeg(invRotY), inSinY = sinDeg(invRotY);

        for (int i = 0; i < 8; i++) {
            int vx = (CUBE_VERTICES[i][0] * innerScale) / 100;
            int vy = (CUBE_VERTICES[i][1] * innerScale) / 100;
            int vz = (CUBE_VERTICES[i][2] * innerScale) / 100;

            int y1 = (vy * inCosX - vz * inSinX) / 1000;
            int z1 = (vy * inSinX + vz * inCosX) / 1000;
            int x1 = vx;

            int x2 = (x1 * inCosY + z1 * inSinY) / 1000;
            int z2 = (-x1 * inSinY + z1 * inCosY) / 1000;
            int y2 = y1;

            int zProj = z2 + camDist;
            if (zProj < 10) zProj = 10;

            cubeInnerProjX[i] = cx + (x2 * fov) / zProj;
            cubeInnerProjY[i] = cy + (y2 * fov) / zProj;
        }

        // Aristas del nucleo interior (Rosa Neon / Magenta electrico)
        g.setColor(isPlaying ? 0xff007f : 0xaa0055);
        for (int e = 0; e < 12; e++) {
            int a = CUBE_EDGES[e][0];
            int b = CUBE_EDGES[e][1];
            g.drawLine(cubeInnerProjX[a], cubeInnerProjY[a], cubeInnerProjX[b], cubeInnerProjY[b]);
        }

        // 4. Dibujar las 12 aristas exteriores con efecto Neon Cyan
        g.setColor(0x00e5ff);
        for (int e = 0; e < 12; e++) {
            int a = CUBE_EDGES[e][0];
            int b = CUBE_EDGES[e][1];
            g.drawLine(cubeProjX[a], cubeProjY[a], cubeProjX[b], cubeProjY[b]);
        }

        // 5. Nodos en vertices brillantes
        g.setColor(0xffffff);
        for (int i = 0; i < 8; i++) {
            g.fillRect(cubeProjX[i] - 1, cubeProjY[i] - 1, 3, 3);
        }

        // Destello central si el beat es muy alto
        if (isPlaying && bass > 65) {
            g.setColor(0x00ffff);
            g.drawRect(cx - 3, cy - 3, 6, 6);
            g.setColor(0xffffff);
            g.fillRect(cx - 1, cy - 1, 2, 2);
        }
    }

    private void paintRoad(Graphics g, int x, int y, int w, int h) {
        boolean isPlaying = (engine.getState() == AudioEngine.STATE_PLAYING);
        int bass = isPlaying ? barHeights[0] : 0;

        // 1. Cielo Synthwave nocturno
        int hy = y + (h * 36) / 100;
        int skyH = hy - y;
        if (skyH < 12) {
            hy = y + 12;
            skyH = 12;
        }

        // Gradiente vertical de cielo
        int halfSky = skyH / 2;
        g.setColor(0x090314); // Purpura espacio muy oscuro arriba
        g.fillRect(x, y, w, halfSky);
        g.setColor(0x19082c); // Violeta synthwave
        g.fillRect(x, y + halfSky, w, skyH - halfSky);

        // Estrellas de fondo en el cielo
        g.setColor(0x80d8ff);
        g.fillRect(x + (w * 15 / 100), y + (skyH * 25 / 100), 1, 1);
        g.fillRect(x + (w * 32 / 100), y + (skyH * 60 / 100), 1, 1);
        g.fillRect(x + (w * 78 / 100), y + (skyH * 35 / 100), 1, 1);
        g.fillRect(x + (w * 88 / 100), y + (skyH * 70 / 100), 1, 1);

        // Sol Synthwave retro con ranuras horizontales en el horizonte
        int maxSunR = (h > 100) ? 36 : 18;
        int sunR = Math.min(maxSunR, skyH / 2);
        if (sunR > 6) {
            int sunPulse = isPlaying ? (bass * sunR / 280) : 0;
            int r = sunR + sunPulse;
            int sx = x + (w / 2);
            int sy = hy - 2;

            // Resplandor exterior
            g.setColor(0x660033);
            g.fillArc(sx - r - 2, sy - r - 2, (r + 2) * 2, (r + 2) * 2, 0, 180);

            // Sol amarillo/naranja superior
            g.setColor(0xffaa00);
            g.fillArc(sx - r, sy - r, r * 2, r * 2, 0, 180);

            // Ranuras horizontales synthwave
            g.setColor(0x19082c);
            int slotStep = Math.max(2, r / 4);
            for (int syLine = sy - (r * 2 / 3); syLine < sy; syLine += slotStep) {
                g.fillRect(sx - r, syLine, r * 2, 1);
            }
        }

        // Silueta de montanas distantes en el horizonte
        g.setColor(0x120520);
        int mtnH = Math.min(6, skyH / 3);
        g.drawLine(x, hy, x + w / 4, hy - mtnH);
        g.drawLine(x + w / 4, hy - mtnH, x + w / 2, hy);
        g.drawLine(x + w / 2, hy, x + (w * 3 / 4), hy - mtnH + 2);
        g.drawLine(x + (w * 3 / 4), hy - mtnH + 2, x + w, hy);

        // Linea brillante del horizonte
        g.setColor(0xff007f);
        g.drawLine(x, hy, x + w, hy);

        // 2. Pista en perspectiva 3D
        int groundH = (y + h) - hy;
        if (groundH <= 0) return;

        // Curvatura oscilante de la carretera
        int curveAngle = (roadScroll / 8) % 360;
        int curveFactor = (sinDeg(curveAngle) * (w / 4)) / 1000;

        int step = (groundH > 80) ? 3 : 2;
        int lastRcx = x + w / 2;

        for (int py = hy + 1; py < y + h; py += step) {
            int curH = Math.min(step, (y + h) - py);
            int relY = py - hy;
            // Factor t de 0 (horizonte) a 1000 (pie de pantalla)
            int t = (relY * 1000) / groundH;
            int t2 = (t * t) / 1000;

            int rcx = (x + w / 2) + (curveFactor * t2) / 1000;
            int halfW = (w / 14) + (((w * 40 / 100) * t2) / 1000);
            if (halfW < 6) halfW = 6;

            int seg = ((roadScroll * 2 + relY * 5) / 28) % 2;

            // Terreno a los lados (cyber grid oscuro)
            g.setColor(seg == 0 ? 0x0c0418 : 0x110722);
            g.fillRect(x, py, w, curH);

            // Asfalto
            g.setColor(seg == 0 ? 0x1d1e2b : 0x161722);
            g.fillRect(rcx - halfW, py, halfW * 2, curH);

            // Cordones laterales (curbs / rumble strips)
            int curbW = Math.max(2, halfW / 7);
            g.setColor(seg == 0 ? 0xff0055 : 0xffffff);
            g.fillRect(rcx - halfW, py, curbW, curH);
            g.fillRect(rcx + halfW - curbW, py, curbW, curH);

            // Linea central punteada
            int centerLineW = Math.max(1, halfW / 18);
            if (seg == 0) {
                g.setColor(0xffea00); // Amarillo retro
                g.fillRect(rcx - (centerLineW / 2), py, centerLineW, curH);
            }

            lastRcx = rcx;
        }

        // 3. Postes de luz de neon al borde de la pista (sensacion de velocidad)
        for (int p = 0; p < 3; p++) {
            int pZ = ((roadScroll * 2) + (p * 333)) % 1000;
            if (pZ > 120 && pZ < 940) {
                int pt2 = (pZ * pZ) / 1000;
                int postPy = hy + (groundH * pZ) / 1000;
                int postRcx = (x + w / 2) + (curveFactor * pt2) / 1000;
                int postHalfW = (w / 14) + (((w * 40 / 100) * pt2) / 1000);
                int postH = Math.max(4, (groundH * pZ) / 3800);

                // Poste izquierdo (Cyan)
                int poleLx = postRcx - postHalfW - 3;
                if (poleLx >= x && poleLx < x + w && postPy - postH >= hy) {
                    g.setColor(0x00d2ff);
                    g.drawLine(poleLx, postPy, poleLx, postPy - postH);
                    g.fillRect(poleLx - 1, postPy - postH, 3, 2);
                }

                // Poste derecho (Magenta)
                int poleRx = postRcx + postHalfW + 3;
                if (poleRx >= x && poleRx < x + w && postPy - postH >= hy) {
                    g.setColor(0xff007f);
                    g.drawLine(poleRx, postPy, poleRx, postPy - postH);
                    g.fillRect(poleRx - 1, postPy - postH, 3, 2);
                }
            }
        }

        // 4. El Deportivo Retro (Auto visto desde atras)
        // En pantalla completa dejamos margen suficiente para no ser tapado por la barra inferior del HUD (22px)
        int bottomMargin = isScreensaverActive ? 32 : 4;
        int carW = (h < 100) ? 32 : 54;
        int carH = (h < 100) ? 14 : 26;
        int carY = (y + h) - carH - bottomMargin;
        // Rebote por el ritmo del bajo
        if (isPlaying && bass > 45) {
            carY -= 1;
        }

        // Posicion X centrada con la pista en la posicion exacta de las ruedas del auto
        int carGroundRelY = (carY + carH) - hy;
        int carT = (carGroundRelY * 1000) / groundH;
        int carT2 = (carT * carT) / 1000;
        int carRcx = (x + w / 2) + (curveFactor * carT2) / 1000;
        int carX = carRcx - (carW / 2);

        // Neumaticos anchos y dibujo de llantas
        int tireW = Math.max(5, carW * 14 / 100);
        int tireH = Math.max(5, carH * 28 / 100);
        g.setColor(0x0e0e12);
        g.fillRect(carX + 1, carY + carH - tireH, tireW, tireH);
        g.fillRect(carX + carW - 1 - tireW, carY + carH - tireH, tireW, tireH);
        g.setColor(0x282830);
        g.fillRect(carX + 2, carY + carH - tireH + 1, tireW - 2, tireH - 2);
        g.fillRect(carX + carW - tireW, carY + carH - tireH + 1, tireW - 2, tireH - 2);

        // Difusor trasero inferior (Grafito oscuro)
        int diffW = carW - (tireW * 2) - 2;
        int diffH = Math.max(3, carH * 16 / 100);
        g.setColor(0x18181c);
        g.fillRect(carX + tireW + 1, carY + carH - diffH, diffW, diffH);

        // Tubos de escape cromados
        int exL = carX + tireW + 3;
        int exR = carX + carW - tireW - 6;
        int exW = Math.max(3, carW / 16);
        int exH = Math.max(2, carH / 8);
        g.setColor(0xaaaaaa);
        g.fillRect(exL, carY + carH - exH - 1, exW, exH);
        g.fillRect(exR, carY + carH - exH - 1, exW, exH);

        // Llamas de escape Nitro reactivas con el bajo
        if (isPlaying && bass > 30) {
            int flameLen = Math.max(3, (bass * carH) / 120);
            g.setColor(0x00e5ff); // Llama turbo exterior cian
            g.fillRect(exL - 1, carY + carH, exW + 2, flameLen);
            g.fillRect(exR - 1, carY + carH, exW + 2, flameLen);
            g.setColor(0xffffff); // Nucleo blanco incandescente
            g.fillRect(exL, carY + carH, exW, Math.max(2, flameLen / 2));
            g.fillRect(exR, carY + carH, exW, Math.max(2, flameLen / 2));
        }

        // Chasis principal (Rojo Deportivo Cyber-shot / Rosso Corsa)
        int bodyY = carY + (carH * 32 / 100);
        int bodyH = carH - (carH * 32 / 100) - 2;
        int bodyW = carW - 6;
        int bodyX = carX + 3;

        g.setColor(0xd50000); // Rojo brillante
        g.fillRect(bodyX, bodyY, bodyW, bodyH);
        g.setColor(0x880000); // Sombra inferior del chasis
        g.drawLine(bodyX, bodyY + bodyH - 1, bodyX + bodyW - 1, bodyY + bodyH - 1);

        // Aleron trasero aerodinamico
        int wingY = carY + (carH * 16 / 100);
        int wingH = Math.max(2, carH * 12 / 100);
        g.setColor(0xff1e38);
        g.fillRect(carX + 1, wingY, carW - 2, wingH);
        // Soportes del aleron
        int strutL = carX + (carW * 22 / 100);
        int strutR = carX + carW - (carW * 22 / 100) - 2;
        g.setColor(0x111111);
        int strutH = bodyY - (wingY + wingH) + 1;
        if (strutH > 0) {
            g.fillRect(strutL, wingY + wingH, 2, strutH);
            g.fillRect(strutR, wingY + wingH, 2, strutH);
        }

        // Cabina / Ventana trasera polarizada con reflejos
        int cabW = carW * 52 / 100;
        int cabH = carH * 38 / 100;
        int cabX = carX + (carW - cabW) / 2;
        int cabY = carY;
        g.setColor(0x07111c);
        g.fillRect(cabX, cabY, cabW, cabH);
        // Reflejos de luz en el parabrisas
        g.setColor(0x00d2ff);
        g.drawLine(cabX + 2, cabY + 1, cabX + cabW - 3, cabY + 1);
        if (carH > 18) {
            g.setColor(0x0077aa);
            g.drawLine(cabX + 4, cabY + 3, cabX + cabW - 5, cabY + 3);
        }

        // Luces traseras LED Neon continuas
        int tailY = bodyY + (bodyH * 22 / 100);
        int tailH = Math.max(3, bodyH * 38 / 100);
        int tailW = carW * 24 / 100;
        int tlX = carX + (carW * 12 / 100);
        int trX = carX + carW - (carW * 12 / 100) - tailW;

        g.setColor(0xff0033); // Rojo neon
        g.fillRect(tlX, tailY, tailW, tailH);
        g.fillRect(trX, tailY, tailW, tailH);
        // Nucleo blanco incandescente de las luces
        g.setColor(isPlaying && bass > 40 ? 0xffffff : 0xff8a80);
        g.drawLine(tlX + 1, tailY + (tailH / 2), tlX + tailW - 2, tailY + (tailH / 2));
        g.drawLine(trX + 1, tailY + (tailH / 2), trX + tailW - 2, tailY + (tailH / 2));

        // Matricula / Emblema central
        int plateW = Math.max(6, carW * 18 / 100);
        int plateH = Math.max(3, tailH);
        int plateX = carX + (carW - plateW) / 2;
        g.setColor(0xffd600);
        g.fillRect(plateX, tailY, plateW, plateH);
        g.setColor(0x000000);
        g.drawLine(plateX + 1, tailY + 1, plateX + plateW - 2, tailY + 1);

        // HUD discreto estilo arcade en pantalla completa o deck
        if (h > 100) {
            Font tinyFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
            g.setFont(tinyFont);
            g.setColor(0x00ffcc);
            String speedStr = isPlaying ? "185 KM/H" : "IDLE";
            int hudY = isScreensaverActive ? 32 : (y + 4);
            g.drawString(speedStr, x + w - 6, hudY, Graphics.TOP | Graphics.RIGHT);
        }
    }

    private void paintActiveVisualizer(Graphics g, int x, int y, int w, int h) {
        int oldClipX = g.getClipX();
        int oldClipY = g.getClipY();
        int oldClipW = g.getClipWidth();
        int oldClipH = g.getClipHeight();
        g.setClip(x, y, w, h);

        switch (activeVisualizer) {
            case VIZ_SCOPE:
                paintScope(g, x, y, w, h);
                break;
            case VIZ_WARP:
                paintStarfield(g, x, y, w, h);
                break;
            case VIZ_FIRE:
                paintFire(g, x, y, w, h);
                break;
            case VIZ_CUBE:
                paintCube(g, x, y, w, h);
                break;
            case VIZ_ROAD:
                paintRoad(g, x, y, w, h);
                break;
            case VIZ_BARS:
            default:
                paintEqualizer(g, x, y, w, h);
                break;
        }

        if (!isScreensaverActive) {
            g.setColor(0x152233);
            g.drawRect(x, y, w - 1, h - 1);
        }
        g.setClip(oldClipX, oldClipY, oldClipW, oldClipH);
    }

    private void paintScreensaver(Graphics g, int w, int h) {
        // 1. Fondo visualizador completo (240x320 QVGA)
        paintActiveVisualizer(g, 0, 0, w, h);

        // 2. Barra superior HUD Cyber-shot
        int topBarH = 26;
        g.setColor(0x060c14);
        g.fillRect(0, 0, w, topBarH);
        g.setColor(0x00d2ff);
        g.drawLine(0, topBarH - 1, w, topBarH - 1);

        Font boldSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        g.setFont(boldSmall);

        // Titulo de la cancion o estado
        Track track = engine.getCurrentTrack();
        String title = (track != null) ? track.getTitle() : "MPlayer Pantalla Completa";
        g.setColor(0xffffff);
        int oldClipX = g.getClipX();
        int oldClipY = g.getClipY();
        int oldClipW = g.getClipWidth();
        int oldClipH = g.getClipHeight();
        g.setClip(8, 0, w - 85, topBarH);
        g.drawString(title, 8, 6, Graphics.TOP | Graphics.LEFT);
        g.setClip(oldClipX, oldClipY, oldClipW, oldClipH);

        // Badge de estado (PLAY / PAUSE / STOP)
        paintStateBadge(g, w - 6, 4);

        // 3. Barra inferior HUD
        int btmBarH = 22;
        int btmBarY = h - btmBarH;
        g.setColor(0x060c14);
        g.fillRect(0, btmBarY, w, btmBarH);
        g.setColor(0x00d2ff);
        g.drawLine(0, btmBarY, w, btmBarY);

        g.setColor(0x00ffcc);
        g.drawString("[5] " + getVisualizerName(activeVisualizer), 8, btmBarY + 3, Graphics.TOP | Graphics.LEFT);
        g.setColor(0xff7777);
        g.drawString("Salir", w - 8, btmBarY + 3, Graphics.TOP | Graphics.RIGHT);
    }

    private String getVisualizerName(int viz) {
        switch (viz) {
            case VIZ_SCOPE: return "Laser";
            case VIZ_WARP:  return "Tunel 3D";
            case VIZ_FIRE:  return "Fuego";
            case VIZ_CUBE:  return "Cubo 3D";
            case VIZ_ROAD:  return "Pista 3D";
            case VIZ_BARS:
            default:        return "Espectro";
        }
    }

    private void paintVizMenu(Graphics g, int w, int h) {
        int menuW = 216;
        int itemH = 20;
        int titleH = 24;
        int footerH = 18;
        int menuH = titleH + (VIZ_MENU_ITEMS.length * itemH) + footerH + 10;
        int menuX = (w - menuW) / 2;
        int menuY = (h - menuH) / 2;

        // Sombra exterior
        g.setColor(0x000000);
        g.fillRect(menuX - 2, menuY - 2, menuW + 4, menuH + 4);

        // Fondo principal del menu
        g.setColor(0x0b131e);
        g.fillRect(menuX, menuY, menuW, menuH);

        // Borde Cyber-shot
        g.setColor(0x00d2ff);
        g.drawRect(menuX, menuY, menuW - 1, menuH - 1);
        g.drawRect(menuX + 1, menuY + 1, menuW - 3, menuH - 3);

        // Cabecera del menu
        g.setColor(0x132337);
        g.fillRect(menuX + 2, menuY + 2, menuW - 4, titleH);
        g.setColor(0x00d2ff);
        g.drawLine(menuX + 2, menuY + titleH + 2, menuX + menuW - 3, menuY + titleH + 2);

        Font boldFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        g.setFont(boldFont);
        g.setColor(0x00e5ff);
        g.drawString("EFECTOS VISUALES", menuX + (menuW / 2), menuY + 6, Graphics.TOP | Graphics.HCENTER);

        // Opciones del menu
        int itemY = menuY + titleH + 5;
        Font itemFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(itemFont);

        for (int i = 0; i < VIZ_MENU_ITEMS.length; i++) {
            int curItemY = itemY + (i * itemH);
            boolean isSelected = (i == vizMenuSelectedIndex);
            boolean isCurrentActive = (i < VIZ_COUNT && i == activeVisualizer);

            if (isSelected) {
                g.setColor(0x005b8a);
                g.fillRoundRect(menuX + 5, curItemY, menuW - 10, itemH - 2, 4, 4);
                g.setColor(0x00d2ff);
                g.drawRoundRect(menuX + 5, curItemY, menuW - 10, itemH - 2, 4, 4);

                g.setColor(0x00ffcc);
                g.drawString(">", menuX + 10, curItemY + 3, Graphics.TOP | Graphics.LEFT);
                g.setColor(0xffffff);
                g.drawString(VIZ_MENU_ITEMS[i], menuX + 22, curItemY + 3, Graphics.TOP | Graphics.LEFT);
            } else {
                g.setColor(0x8faecf);
                g.drawString(VIZ_MENU_ITEMS[i], menuX + 22, curItemY + 3, Graphics.TOP | Graphics.LEFT);
            }

            if (isCurrentActive) {
                g.setColor(0x00e676);
                g.drawString("[*]", menuX + menuW - 24, curItemY + 3, Graphics.TOP | Graphics.LEFT);
            }
        }

        // Pie de ayuda del menu
        int footerY = itemY + (VIZ_MENU_ITEMS.length * itemH) + 2;
        g.setColor(0x132337);
        g.fillRect(menuX + 2, footerY, menuW - 4, 18);
        g.setColor(0x425872);
        g.drawLine(menuX + 2, footerY, menuX + menuW - 3, footerY);

        Font tinyFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(tinyFont);
        g.setColor(0x5c7f9f);
        g.drawString("[OK] Elegir   [C] Volver", menuX + (menuW / 2), footerY + 2, Graphics.TOP | Graphics.HCENTER);
    }

    private void executeVizMenuItem(int index) {
        isVizMenuOpen = false;
        if (index >= 0 && index < VIZ_COUNT) {
            activeVisualizer = index;
        } else if (index == VIZ_COUNT) {
            isScreensaverActive = true;
        }
        repaint();
    }

    private void paintIconButton(Graphics g, int x, int y, int w, int h, Image icon, String fallback, boolean pressed, int bgColor) {
        if (pressed) {
            g.setColor(0x00d2ff);
            g.fillRoundRect(x, y, w, h, 8, 8);
            g.setColor(0x00172d);
        } else {
            g.setColor(bgColor);
            g.fillRoundRect(x, y, w, h, 8, 8);
            g.setColor(0x273b52);
            g.drawRoundRect(x, y, w - 1, h - 1, 8, 8);
            g.setColor(0xffffff);
        }

        if (icon != null) {
            int ix = x + (w - icon.getWidth()) / 2;
            int iy = y + (h - icon.getHeight()) / 2;
            g.drawImage(icon, ix, iy, Graphics.TOP | Graphics.LEFT);
        } else if (fallback != null && fallback.length() > 0) {
            Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
            g.setFont(font);
            int tx = x + (w / 2);
            int ty = y + (h - font.getHeight()) / 2;
            g.drawString(fallback, tx, ty, Graphics.TOP | Graphics.HCENTER);
        }
    }

    private String getMenuItemText(int index) {
        switch (index) {
            case 0: return "1. Lista de Reproducci\u00F3n";
            case 1: return "2. Explorar Archivos";
            case 2: return "3. Aleatorio: " + (engine.isShuffle() ? "Activado" : "Desactivado");
            case 3: return "4. Repetir: " + engine.getRepeatModeString();
            case 4: return "5. Efectos Visuales";
            case 5: return "6. Ayuda";
            case 6: return "7. Minimizar";
            case 7: return "8. Salir";
            default: return "";
        }
    }

    private void paintOptionsMenu(Graphics g, int w, int h) {
        int menuW = 216;
        int menuH = 228;
        int menuX = (w - menuW) / 2;
        int menuY = (h - menuH) / 2;

        // Sombra exterior
        g.setColor(0x000000);
        g.fillRect(menuX - 2, menuY - 2, menuW + 4, menuH + 4);

        // Fondo principal del menu
        g.setColor(0x0b131e);
        g.fillRect(menuX, menuY, menuW, menuH);

        // Borde Cyber-shot
        g.setColor(0x00d2ff);
        g.drawRect(menuX, menuY, menuW - 1, menuH - 1);
        g.drawRect(menuX + 1, menuY + 1, menuW - 3, menuH - 3);

        // Cabecera del menu
        int titleH = 24;
        g.setColor(0x132337);
        g.fillRect(menuX + 2, menuY + 2, menuW - 4, titleH);
        g.setColor(0x00d2ff);
        g.drawLine(menuX + 2, menuY + titleH + 2, menuX + menuW - 3, menuY + titleH + 2);

        Font boldFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        g.setFont(boldFont);
        g.setColor(0x00e5ff);
        g.drawString("OPCIONES MPLAYER", menuX + (menuW / 2), menuY + 6, Graphics.TOP | Graphics.HCENTER);

        // Opciones del menu
        int itemY = menuY + titleH + 6;
        int itemH = 22;
        Font itemFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(itemFont);

        for (int i = 0; i < MENU_ITEMS.length; i++) {
            int curItemY = itemY + (i * itemH);
            String itemText = getMenuItemText(i);
            boolean isHighlighted = (i == menuSelectedIndex);
            if (isHighlighted) {
                // Fondo resaltado con borde brillante
                g.setColor(0x005b8a);
                g.fillRoundRect(menuX + 5, curItemY, menuW - 10, itemH - 2, 4, 4);
                g.setColor(0x00d2ff);
                g.drawRoundRect(menuX + 5, curItemY, menuW - 10, itemH - 2, 4, 4);

                // Indicador de flecha y texto blanco brillante
                g.setColor(0x00ffcc);
                g.drawString(">", menuX + 10, curItemY + 3, Graphics.TOP | Graphics.LEFT);
                g.setColor(0xffffff);
                g.drawString(itemText, menuX + 22, curItemY + 3, Graphics.TOP | Graphics.LEFT);
            } else {
                g.setColor(0x8faecf);
                g.drawString(itemText, menuX + 22, curItemY + 3, Graphics.TOP | Graphics.LEFT);
            }

            if (i == 2 && engine.isShuffle()) {
                g.setColor(0x00e676);
                g.drawString("[*]", menuX + menuW - 24, curItemY + 3, Graphics.TOP | Graphics.LEFT);
            } else if (i == 3 && engine.getRepeatMode() != AudioEngine.REPEAT_OFF) {
                g.setColor(0x00e676);
                g.drawString("[*]", menuX + menuW - 24, curItemY + 3, Graphics.TOP | Graphics.LEFT);
            }
        }

        // Pie de ayuda del menu
        int footerY = itemY + (MENU_ITEMS.length * itemH) + 2;
        g.setColor(0x132337);
        g.fillRect(menuX + 2, footerY, menuW - 4, 18);
        g.setColor(0x425872);
        g.drawLine(menuX + 2, footerY, menuX + menuW - 3, footerY);

        Font tinyFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(tinyFont);
        g.setColor(0x5c7f9f);
        g.drawString("[OK] Elegir   [C] Volver", menuX + (menuW / 2), footerY + 2, Graphics.TOP | Graphics.HCENTER);
    }

    private static String formatTime(long microSec) {
        if (microSec < 0) return "--:--";
        long totalSec = microSec / 1000000L;
        long min = totalSec / 60L;
        long sec = totalSec % 60L;
        return (min < 10 ? "0" : "") + min + ":" + (sec < 10 ? "0" : "") + sec;
    }

    private Image getCurrentRepeatIcon() {
        switch (engine.getRepeatMode()) {
            case AudioEngine.REPEAT_ONE:
                return (IconStore.repeatOne != null) ? IconStore.repeatOne : IconStore.repeat;
            case AudioEngine.REPEAT_ALL:
                return (IconStore.repeatAll != null) ? IconStore.repeatAll : IconStore.repeat;
            default:
                return (IconStore.repeatOff != null) ? IconStore.repeatOff : IconStore.repeat;
        }
    }

    private void triggerButtonFeedback(int btnCode) {
        pressedButton = btnCode;
        pressedTimerTicks = 4; // dura ~200ms
        repaint();
    }

    private void executeMenuItem(int index) {
        switch (index) {
            case 0:
                isMenuOpen = false;
                repaint();
                midlet.showPlaylist();
                break;
            case 1:
                isMenuOpen = false;
                repaint();
                midlet.showFileBrowser();
                break;
            case 2:
                engine.toggleShuffle();
                repaint();
                break;
            case 3:
                engine.toggleRepeatMode();
                repaint();
                break;
            case 4:
                isMenuOpen = false;
                isVizMenuOpen = true;
                vizMenuSelectedIndex = activeVisualizer;
                repaint();
                break;
            case 5:
                isMenuOpen = false;
                repaint();
                midlet.showHelp();
                break;
            case 6:
                isMenuOpen = false;
                repaint();
                midlet.minimizeApp();
                break;
            case 7:
                isMenuOpen = false;
                midlet.exitMIDlet();
                break;
            default:
                break;
        }
    }

    // ==========================================
    // Control de Teclado Fisico y Joystick K800i
    // ==========================================

    protected void keyPressed(int keyCode) {
        // Obtenemos gameAction de forma segura con try/catch para evitar IllegalArgumentException
        int gameAction = 0;
        try {
            gameAction = getGameAction(keyCode);
        } catch (Throwable t) {
            gameAction = 0;
        }

        // Manejo especial cuando el Screensaver WMP esta activo
        if (isScreensaverActive) {
            if (keyCode == KEY_NUM5 || keyCode == -4 || keyCode == 39) {
                // Tecla 5 o flecha derecha: alternar efecto dentro del screensaver
                activeVisualizer = (activeVisualizer + 1) % VIZ_COUNT;
                repaint();
                return;
            }
            // Cualquier otra tecla: salir del screensaver
            isScreensaverActive = false;
            repaint();
            return;
        }

        // Manejo especial cuando el submenu de Efectos Visuales esta abierto
        if (isVizMenuOpen) {
            // 1. Navegacion Arriba
            if (gameAction == UP || keyCode == -1 || keyCode == 38 || keyCode == -11) {
                vizMenuSelectedIndex = (vizMenuSelectedIndex - 1 + VIZ_MENU_ITEMS.length) % VIZ_MENU_ITEMS.length;
                repaint();
                return;
            }

            // 2. Navegacion Abajo
            if (gameAction == DOWN || keyCode == -2 || keyCode == 40 || keyCode == -12) {
                vizMenuSelectedIndex = (vizMenuSelectedIndex + 1) % VIZ_MENU_ITEMS.length;
                repaint();
                return;
            }

            // 3. Confirmar / Elegir
            if (gameAction == FIRE || keyCode == -5 || keyCode == -6 || keyCode == -21 || keyCode == 10 || keyCode == 32) {
                executeVizMenuItem(vizMenuSelectedIndex);
                return;
            }

            // 4. Cancelar / Volver
            if (keyCode == -7 || keyCode == -22 || keyCode == -8 || keyCode == 8 || keyCode == 27 || keyCode == KEY_NUM0) {
                isVizMenuOpen = false;
                repaint();
                return;
            }

            // 5. Atajos directos con el teclado numerico 1..7
            if (keyCode == KEY_NUM1) { executeVizMenuItem(0); return; }
            if (keyCode == KEY_NUM2) { executeVizMenuItem(1); return; }
            if (keyCode == KEY_NUM3) { executeVizMenuItem(2); return; }
            if (keyCode == KEY_NUM4) { executeVizMenuItem(3); return; }
            if (keyCode == KEY_NUM5) { executeVizMenuItem(4); return; }
            if (keyCode == KEY_NUM6) { executeVizMenuItem(5); return; }
            if (keyCode == KEY_NUM7) { executeVizMenuItem(6); return; }
            return;
        }

        // Manejo especial cuando el menu de pantalla completa esta abierto
        if (isMenuOpen) {
            // 1. Navegacion Arriba (Joystick, flechas PC, teclas laterales o 2)
            if (gameAction == UP || keyCode == -1 || keyCode == 38 || keyCode == -11) {
                menuSelectedIndex = (menuSelectedIndex - 1 + MENU_ITEMS.length) % MENU_ITEMS.length;
                repaint();
                return;
            }

            // 2. Navegacion Abajo (Joystick, flechas PC, teclas laterales o 8)
            if (gameAction == DOWN || keyCode == -2 || keyCode == 40 || keyCode == -12) {
                menuSelectedIndex = (menuSelectedIndex + 1) % MENU_ITEMS.length;
                repaint();
                return;
            }

            // 3. Confirmar / Elegir (Centro Joystick, Left Softkey, Enter, Barra espaciadora)
            if (gameAction == FIRE || keyCode == -5 || keyCode == -6 || keyCode == -21 || keyCode == 10 || keyCode == 32) {
                executeMenuItem(menuSelectedIndex);
                return;
            }

            // 4. Cancelar / Cerrar menu (Right Softkey, tecla C, Delete, Escape, tecla 0)
            if (keyCode == -7 || keyCode == -22 || keyCode == -8 || keyCode == 8 || keyCode == 27 || keyCode == KEY_NUM0) {
                isMenuOpen = false;
                repaint();
                return;
            }

            // 5. Atajos directos con el teclado numerico 1..7
            if (keyCode == KEY_NUM1) { executeMenuItem(0); return; }
            if (keyCode == KEY_NUM2) { executeMenuItem(1); return; }
            if (keyCode == KEY_NUM3) { executeMenuItem(2); return; }
            if (keyCode == KEY_NUM4) { executeMenuItem(3); return; }
            if (keyCode == KEY_NUM5) { executeMenuItem(4); return; }
            if (keyCode == KEY_NUM6) { executeMenuItem(5); return; }
            if (keyCode == KEY_NUM7) { executeMenuItem(6); return; }
            if (keyCode == KEY_NUM8) { executeMenuItem(7); return; }
            return;
        }

        // 0. Softkeys Sony Ericsson K800i en pantalla completa
        if (keyCode == -6 || keyCode == -21) {
            // Softkey Izquierdo: Abrir Menu
            isMenuOpen = true;
            menuSelectedIndex = 0;
            repaint();
            return;
        }

        if (keyCode == -7 || keyCode == -22) {
            // Softkey Derecho: Salir
            midlet.exitMIDlet();
            return;
        }

        // 1. Joystick K800i (Centro / Direccion) y teclas de flechas PC
        if (gameAction == FIRE || keyCode == -5 || keyCode == 10 || keyCode == 32) {
            if (engine.getPlaylistSize() == 0) {
                midlet.showFileBrowser();
            } else {
                triggerButtonFeedback(BTN_PLAY);
                engine.togglePlayPause();
            }
            return;
        }

        if (gameAction == UP || keyCode == -1 || keyCode == 38 || keyCode == -11) {
            // Arriba en joystick, flecha arriba en PC o tecla lateral volumen (+) en K800i
            triggerButtonFeedback(BTN_VOL_PLUS);
            engine.volumeUp();
            return;
        }

        if (gameAction == DOWN || keyCode == -2 || keyCode == 40 || keyCode == -12) {
            // Abajo en joystick, flecha abajo en PC o tecla lateral volumen (-) en K800i
            triggerButtonFeedback(BTN_VOL_MINUS);
            engine.volumeDown();
            return;
        }

        if (gameAction == LEFT || keyCode == -3 || keyCode == 37) {
            // Izquierda en joystick o flecha izquierda en PC: rebobinar 5s
            triggerButtonFeedback(BTN_RWD);
            engine.seekDelta(-5000000L);
            return;
        }

        if (gameAction == RIGHT || keyCode == -4 || keyCode == 39) {
            // Derecha en joystick o flecha derecha en PC: avanzar 5s
            triggerButtonFeedback(BTN_FWD);
            engine.seekDelta(5000000L);
            return;
        }

        // 2. Teclas especiales Sony Ericsson
        if (keyCode == -8 || keyCode == 8 || keyCode == 127) {
            // Tecla 'C' (Clear) de Sony Ericsson / Backspace: Detener musica
            triggerButtonFeedback(BTN_STOP);
            engine.stop();
            return;
        }

        // 3. Teclado Numerico K800i
        switch (keyCode) {
            case KEY_NUM5:
                if (engine.getPlaylistSize() == 0) {
                    midlet.showFileBrowser();
                } else {
                    triggerButtonFeedback(BTN_PLAY);
                    engine.togglePlayPause();
                }
                break;
            case KEY_NUM0:
                triggerButtonFeedback(BTN_STOP);
                engine.stop();
                break;
            case KEY_NUM2:
                triggerButtonFeedback(BTN_VOL_PLUS);
                engine.volumeUp();
                break;
            case KEY_NUM8:
                triggerButtonFeedback(BTN_VOL_MINUS);
                engine.volumeDown();
                break;
            case KEY_NUM4:
                triggerButtonFeedback(BTN_RWD);
                engine.seekDelta(-5000000L);
                break;
            case KEY_NUM6:
                triggerButtonFeedback(BTN_FWD);
                engine.seekDelta(5000000L);
                break;
            case KEY_NUM1:
                triggerButtonFeedback(BTN_PREV);
                engine.prevTrack();
                break;
            case KEY_NUM3:
                triggerButtonFeedback(BTN_NEXT);
                engine.nextTrack();
                break;
            case KEY_NUM7:
                engine.toggleShuffle();
                repaint();
                break;
            case KEY_NUM9:
                engine.toggleRepeatMode();
                repaint();
                break;
            case KEY_STAR:
                midlet.showPlaylist();
                break;
            case KEY_POUND:
                midlet.showFileBrowser();
                break;
            default:
                break;
        }
    }

    protected void keyRepeated(int keyCode) {
        keyPressed(keyCode);
    }

    // ==========================================
    // Compatibilidad Tactil (Para emulador de PC)
    // ==========================================

    protected void pointerPressed(int x, int y) {
        int w = getWidth();
        int h = getHeight();

        // 0. Si el Screensaver WMP esta activo
        if (isScreensaverActive) {
            if (y >= h - 28 && x <= 130) {
                // Toque en "[5] Efecto"
                activeVisualizer = (activeVisualizer + 1) % VIZ_COUNT;
                repaint();
                return;
            }
            // Cualquier otro toque sale del screensaver
            isScreensaverActive = false;
            repaint();
            return;
        }

        // 1. Si el submenu de Efectos Visuales esta abierto
        if (isVizMenuOpen) {
            int menuW = 216;
            int itemH = 20;
            int titleH = 24;
            int footerH = 18;
            int menuH = titleH + (VIZ_MENU_ITEMS.length * itemH) + footerH + 10;
            int menuX = (w - menuW) / 2;
            int menuY = (h - menuH) / 2;
            int itemY = menuY + titleH + 5;

            if (x >= menuX && x <= menuX + menuW && y >= itemY && y <= itemY + VIZ_MENU_ITEMS.length * itemH) {
                int clickedIndex = (y - itemY) / itemH;
                if (clickedIndex >= 0 && clickedIndex < VIZ_MENU_ITEMS.length) {
                    executeVizMenuItem(clickedIndex);
                    return;
                }
            }
            isVizMenuOpen = false;
            repaint();
            return;
        }

        // 2. Si el menu principal esta abierto, manejar seleccion
        if (isMenuOpen) {
            int menuW = 216;
            int menuH = 228;
            int menuX = (w - menuW) / 2;
            int menuY = (h - menuH) / 2;
            int titleH = 24;
            int itemY = menuY + titleH + 6;
            int itemH = 22;

            if (x >= menuX && x <= menuX + menuW && y >= itemY && y <= itemY + MENU_ITEMS.length * itemH) {
                int clickedIndex = (y - itemY) / itemH;
                if (clickedIndex >= 0 && clickedIndex < MENU_ITEMS.length) {
                    executeMenuItem(clickedIndex);
                    return;
                }
            }
            // Toque fuera del menu lo cierra
            isMenuOpen = false;
            repaint();
            return;
        }

        // Toques en la barra de softkeys inferior
        int softkeyY = h - 22;
        if (y >= softkeyY) {
            if (x <= 75) {
                // Toque en softkey izquierdo
                if (isMenuOpen || isVizMenuOpen) {
                    if (isMenuOpen) executeMenuItem(menuSelectedIndex);
                    else executeVizMenuItem(vizMenuSelectedIndex);
                } else {
                    isMenuOpen = true;
                    menuSelectedIndex = 0;
                }
                repaint();
                return;
            } else if (x >= w - 75) {
                // Toque en softkey derecho
                if (isMenuOpen) {
                    isMenuOpen = false;
                    repaint();
                    return;
                } else if (isVizMenuOpen) {
                    isVizMenuOpen = false;
                    repaint();
                    return;
                } else {
                    midlet.exitMIDlet();
                    return;
                }
            }
        }

        if (engine.getPlaylistSize() == 0) {
            midlet.showFileBrowser();
            return;
        }

        // Toque en iconos de estado en el LCD Deck (Repeticion y Aleatorio)
        int cardMargin = 6;
        int cardW = w - (cardMargin * 2);
        int innerX = cardMargin + 7;
        int innerW = cardW - 14;
        int topRowY = 35;
        if (y >= topRowY - 6 && y <= topRowY + 18) {
            if (x >= innerX + innerW - 22 && x <= innerX + innerW) {
                engine.toggleRepeatMode();
                repaint();
                return;
            } else if (x >= innerX + innerW - 46 && x < innerX + innerW - 22) {
                engine.toggleShuffle();
                repaint();
                return;
            }
        }

        // Toque en el area del visualizador en el LCD deck para cambiar de efecto
        if (x >= vizAreaX && x <= vizAreaX + vizAreaW &&
            y >= vizAreaY && y <= vizAreaY + vizAreaH) {
            activeVisualizer = (activeVisualizer + 1) % VIZ_COUNT;
            repaint();
            return;
        }

        if (x >= progressBarX && x <= progressBarX + progressBarW &&
            y >= progressBarY - 6 && y <= progressBarY + progressBarH + 6) {
            pressedButton = BTN_SEEK;
            int pct = ((x - progressBarX) * 100) / progressBarW;
            engine.seekToPercent(pct);
            repaint();
            return;
        }

        // Toque en icono de altavoz para silenciar / activar
        if (x >= cardMargin && x <= volBtnMinusX - 2 && y >= volBtnMinusY && y <= volBtnMinusY + volBtnSize) {
            engine.toggleMute();
            repaint();
            return;
        }

        if (x >= volBarX && x <= volBarX + volBarW &&
            y >= volBarY - 2 && y <= volBarY + volBarH + 2) {
            pressedButton = BTN_VOL_SLIDER;
            int pct = ((x - volBarX) * 100) / volBarW;
            engine.setVolume(pct);
            repaint();
            return;
        }

        if (x >= volBtnMinusX && x <= volBtnMinusX + volBtnSize &&
            y >= volBtnMinusY && y <= volBtnMinusY + volBtnSize) {
            pressedButton = BTN_VOL_MINUS;
            engine.volumeDown();
            repaint();
            return;
        }
        if (x >= volBtnPlusX && x <= volBtnPlusX + volBtnSize &&
            y >= volBtnPlusY && y <= volBtnPlusY + volBtnSize) {
            pressedButton = BTN_VOL_PLUS;
            engine.volumeUp();
            repaint();
            return;
        }

        if (x >= btnPlayX && x <= btnPlayX + btnHeroSize &&
            y >= btnControlsY && y <= btnControlsY + btnHeroSize) {
            pressedButton = BTN_PLAY;
            engine.togglePlayPause();
            repaint();
            return;
        }

        if (y >= btnSecondaryY && y <= btnSecondaryY + btnSize) {
            if (x >= btnPrevX && x <= btnPrevX + btnSize) {
                pressedButton = BTN_PREV;
                engine.prevTrack();
            } else if (x >= btnRwdX && x <= btnRwdX + btnSize) {
                pressedButton = BTN_RWD;
                engine.seekDelta(-5000000L);
            } else if (x >= btnStopX && x <= btnStopX + btnSize) {
                pressedButton = BTN_STOP;
                engine.stop();
            } else if (x >= btnFwdX && x <= btnFwdX + btnSize) {
                pressedButton = BTN_FWD;
                engine.seekDelta(5000000L);
            } else if (x >= btnNextX && x <= btnNextX + btnSize) {
                pressedButton = BTN_NEXT;
                engine.nextTrack();
            }
            repaint();
            return;
        }
    }

    protected void pointerDragged(int x, int y) {
        if (isMenuOpen || isVizMenuOpen || isScreensaverActive) return;

        if (pressedButton == BTN_SEEK) {
            int pct = ((x - progressBarX) * 100) / progressBarW;
            if (pct < 0) pct = 0;
            if (pct > 100) pct = 100;
            engine.seekToPercent(pct);
            repaint();
        } else if (pressedButton == BTN_VOL_SLIDER) {
            int pct = ((x - volBarX) * 100) / volBarW;
            if (pct < 0) pct = 0;
            if (pct > 100) pct = 100;
            engine.setVolume(pct);
            repaint();
        }
    }

    protected void pointerReleased(int x, int y) {
        pressedButton = BTN_NONE;
        repaint();
    }


    // ==========================================
    // Callbacks del Motor de Audio
    // ==========================================

    public void onStateChanged(int state) {
        repaint();
    }

    public void onTrackChanged(Track track, int index, int total) {
        marqueeOffset = 0;
        marqueePauseTicks = 15;
        repaint();
    }

    public void onVolumeChanged(int volume, boolean isMuted) {
        repaint();
    }

    public void onError(String message) {
        midlet.showError("Audio", message);
        repaint();
    }
}
