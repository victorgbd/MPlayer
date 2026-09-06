package mplayer;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/**
 * Pantalla de Ayuda en Pantalla Completa (240x320) para Sony Ericsson K800i.
 * Estilo visual Cyber-shot / Walkman con scroll continuo y soporte completo de teclado.
 */
public class HelpCanvas extends Canvas {

    private final HelloMIDlet midlet;

    private int scrollY = 0;
    private int maxScroll = 0;

    private static final String[] HELP_SECTIONS = {
        "FORMATOS SOPORTADOS",
        "* MP3 : MPEG-1/2 Audio Layer 3 (.mp3)",
        "* AAC : Advanced Audio Coding (.aac)",
        "* M4A : AAC / eAAC+ (.m4a)",
        "* WMA : Windows Media Audio (.wma)",
        "* WAV : Audio PCM sin comprimir (.wav)",
        "* MID : Polif\u00F3nico MIDI 72 voces (.mid)",
        "* AMR : Adaptive Multi-Rate (.amr)",
        "* 3GP : Contenedor 3GPP audio (.3gp)",
        "* XMF : Extensible Music Format (.xmf)",
        "* IMY : iMelody Ringtones (.imy)",
        "* MMF : Yamaha SMAF Synthesizer (.mmf)",
        "",
        "JOYSTICK (5 DIRECCIONES)",
        "* Centro (OK) : Play / Pausa / Buscar",
        "* Arriba : Subir volumen (+)",
        "* Abajo : Bajar volumen (-)",
        "* Izquierda : Rebobinar 5 seg (<<)",
        "* Derecha : Avanzar 5 seg (>>)",
        "",
        "TECLADO NUM\u00C9RICO",
        "* [ 5 ] : Reproducir / Pausar",
        "* [ 0 ] : Detener (Stop)",
        "* [ 1 ] : Pista anterior",
        "* [ 3 ] : Pista siguiente",
        "* [ 4 ] : Rebobinar 5 segundos",
        "* [ 6 ] : Avanzar 5 segundos",
        "* [ 2 ] : Subir volumen (+)",
        "* [ 8 ] : Bajar volumen (-)",
        "* [ 7 ] : Modo aleatorio (Shuffle)",
        "* [ 9 ] : Cambiar modo de repetici\u00F3n",
        "* [ * ] : Abrir lista de reproducci\u00F3n",
        "* [ # ] : Explorar Archivos",
        "",
        "MODOS DE REPRODUCCI\u00D3N",
        "* Aleatorio [7] : Reproducci\u00F3n al azar (Shuffle)",
        "* Algoritmo inteligente sin repetir pistas",
        "* Repetici\u00F3n [9] : Todo / 1 Vez / Off",
        "* Pantalla : Toca icono [Azar] o [Rep] en el LCD",
        "",
        "BOTONES F\u00CDSICOS",
        "* Laterales (+ / -) : Control de volumen",
        "* Tecla 'C' (Clear) : Detener / Volver",
        "* Softkey Izquierdo : Men\u00FA de opciones",
        "* Softkey Derecho : Volver / Salir",
        "* Joystick Arriba/Abajo : Desplazar ayuda",
        "",
        "REPRODUCCI\u00D3N EN SEGUNDO PLANO",
        "* Men\u00FA [7] : Minimizar aplicaci\u00F3n",
        "* La m\u00FAsica contin\u00FAa sonando de fondo",
        "* Vuelve a MPlayer desde el men\u00FA de tareas",
        "",
        "M\u00DALTIPLES PLAYLISTS",
        "* Men\u00FA Lista [4] : Cambiar Playlist activa",
        "* Men\u00FA Lista [5] : Crear Nueva Playlist",
        "* Men\u00FA Lista [6] : Eliminar Playlist",
        "* Lista 'Principal' protegida como base",
        "* Guardado autom\u00E1tico e independiente",
        "",
        "EXPLORADOR Y SELECCI\u00D3N",
        "* Tecla [*] o [1] : Marcar/Desmarcar pista [X]",
        "* Tecla [#] : Marcar o desmarcar todas",
        "* Toque en pantalla : Toca la casilla para marcar",
        "* Softkey Izquierdo : A\u00F1adir seleccionadas",
        "* Pistas Repetidas : Alerta autom\u00E1tica con",
        "* opciones de omitir o a\u00F1adir de todas formas",
        "",
        "EFECTOS VISUALES",
        "* Men\u00FA [5] : Efectos Visuales",
        "* 1. Barras de Espectro con picos",
        "* 2. Ondas L\u00E1ser (Osciloscopio)",
        "* 3. T\u00FAnel Estelar 3D (Warp)",
        "* 4. Fuego & Plasma con chispas",
        "* 5. Cubo 3D Rotativo con n\u00FAcleo",
        "* 6. Pista Infinita Retro con Auto",
        "* 7. Ver Pantalla Completa",
        "* En Pantalla : Toca el ecualizador para alternar",
        "* Pantalla Completa : Tecla [5] cambia efecto",
        "* Salir : Cualquier tecla"
    };

    public HelpCanvas(HelloMIDlet midlet) {
        this.midlet = midlet;
        setFullScreenMode(true);
    }

    protected void showNotify() {
        setFullScreenMode(true);
        scrollY = 0;
        repaint();
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        // 1. Fondo negro Cyber-shot
        g.setColor(0x0a0f16);
        g.fillRect(0, 0, w, h);

        // 2. Cabecera (Header)
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
        g.drawString("AYUDA", 30, 6, Graphics.TOP | Graphics.LEFT);

        // Badge de formato
        g.setColor(0x005b8a);
        g.fillRoundRect(w - 74, 4, 68, 18, 6, 6);
        g.setColor(0x66d9ff);
        g.drawString("MPlayer", w - 40, 6, Graphics.TOP | Graphics.HCENTER);

        // 3. Barra inferior de Softkeys
        int softkeyY = h - 22;
        int softkeyH = 22;
        g.setColor(0x0d141e);
        g.fillRect(0, softkeyY, w, softkeyH);
        g.setColor(0x00d2ff);
        g.drawLine(0, softkeyY, w, softkeyY);
        g.setColor(0x192738);
        g.drawLine(0, softkeyY + 1, w, softkeyY + 1);

        g.setFont(boldSmall);
        g.setColor(0x00d2ff);
        g.drawString("[OK] Volver", 8, softkeyY + 3, Graphics.TOP | Graphics.LEFT);

        g.setColor(0x4b6685);
        g.drawString("MPlayer", w / 2, softkeyY + 3, Graphics.TOP | Graphics.HCENTER);

        g.setColor(0xff7777);
        g.drawString("Volver", w - 8, softkeyY + 3, Graphics.TOP | Graphics.RIGHT);

        // 4. Area de contenido scrollable
        int contentY = headerH + 2;
        int contentH = softkeyY - contentY - 2;

        int oldClipX = g.getClipX();
        int oldClipY = g.getClipY();
        int oldClipW = g.getClipWidth();
        int oldClipH = g.getClipHeight();

        g.setClip(0, contentY, w - 8, contentH);

        Font titleFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        Font textFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        int lineH = 15;

        int curY = contentY + 4 - scrollY;

        for (int i = 0; i < HELP_SECTIONS.length; i++) {
            String line = HELP_SECTIONS[i];

            if (line.length() == 0) {
                curY += 8;
                continue;
            }

            boolean isSectionHeader = !line.startsWith("*");

            if (isSectionHeader) {
                curY += 4;
                // Barra de seccion
                g.setColor(0x132338);
                g.fillRoundRect(6, curY - 1, w - 24, 18, 4, 4);
                g.setColor(0x00d2ff);
                g.drawRoundRect(6, curY - 1, w - 24, 18, 4, 4);

                g.setFont(titleFont);
                g.setColor(0x00ffff);
                g.drawString(line, 12, curY + 1, Graphics.TOP | Graphics.LEFT);
                curY += 20;
            } else {
                g.setFont(textFont);
                if (line.indexOf(":") != -1) {
                    int colon = line.indexOf(":");
                    String prefix = line.substring(0, colon + 1);
                    String suffix = line.substring(colon + 1);

                    g.setColor(0x66ffb2);
                    g.drawString(prefix, 10, curY, Graphics.TOP | Graphics.LEFT);

                    int offset = textFont.stringWidth(prefix) + 14;
                    g.setColor(0xb0c8e2);
                    g.drawString(suffix, offset, curY, Graphics.TOP | Graphics.LEFT);
                } else {
                    g.setColor(0xb0c8e2);
                    g.drawString(line, 10, curY, Graphics.TOP | Graphics.LEFT);
                }
                curY += lineH;
            }
        }

        int totalContentHeight = (curY + scrollY) - contentY;
        maxScroll = Math.max(0, totalContentHeight - contentH);

        g.setClip(oldClipX, oldClipY, oldClipW, oldClipH);

        // 5. Barra de desplazamiento vertical (Scrollbar)
        if (maxScroll > 0) {
            int sbX = w - 6;
            int sbY = contentY + 2;
            int sbW = 3;
            int sbH = contentH - 4;

            g.setColor(0x141f2e);
            g.fillRect(sbX, sbY, sbW, sbH);

            int thumbH = Math.max(16, (sbH * contentH) / totalContentHeight);
            int thumbY = sbY + (scrollY * (sbH - thumbH)) / maxScroll;

            g.setColor(0x00d2ff);
            g.fillRect(sbX, thumbY, sbW, thumbH);
        }
    }

    private void scrollDelta(int delta) {
        scrollY += delta;
        if (scrollY < 0) scrollY = 0;
        if (scrollY > maxScroll) scrollY = maxScroll;
        repaint();
    }

    protected void keyPressed(int keyCode) {
        int gameAction = 0;
        try {
            gameAction = getGameAction(keyCode);
        } catch (Throwable t) {
            gameAction = 0;
        }

        // Navegacion Arriba
        if (gameAction == UP || keyCode == -1 || keyCode == 38 || keyCode == KEY_NUM2 || keyCode == -11) {
            scrollDelta(-24);
            return;
        }

        // Navegacion Abajo
        if (gameAction == DOWN || keyCode == -2 || keyCode == 40 || keyCode == KEY_NUM8 || keyCode == -12) {
            scrollDelta(24);
            return;
        }

        // Pagina Arriba / Pagina Abajo
        if (keyCode == KEY_NUM1 || keyCode == KEY_NUM4) {
            scrollDelta(-120);
            return;
        }
        if (keyCode == KEY_NUM3 || keyCode == KEY_NUM6) {
            scrollDelta(120);
            return;
        }

        // Volver (Softkeys, Clear, OK, Escape)
        if (keyCode == -7 || keyCode == -22 || keyCode == -6 || keyCode == -21 ||
            keyCode == -8 || keyCode == 8 || keyCode == 27 || keyCode == KEY_NUM0 ||
            gameAction == FIRE || keyCode == -5 || keyCode == 10 || keyCode == 32) {
            midlet.showPlayer();
        }
    }

    protected void keyRepeated(int keyCode) {
        keyPressed(keyCode);
    }

    protected void pointerPressed(int x, int y) {
        int h = getHeight();
        // Barra inferior de volver
        if (y >= h - 26) {
            midlet.showPlayer();
            return;
        }

        // Toque en mitad superior sube, mitad inferior baja
        if (y < h / 2) {
            scrollDelta(-40);
        } else {
            scrollDelta(40);
        }
    }

}
