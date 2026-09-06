package mplayer;

import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/**
 * Explorador de Archivos en Pantalla Completa (240x320) para Sony Ericsson K800i.
 * Navegacion por Tarjeta Memory Stick Micro (M2) y memoria interna con JSR-75.
 * Dise\u00F1o Walkman / Cyber-shot con seleccion interactiva, iconos graficos,
 * soporte de atajos fisicos y a\u00F1adido de carpetas completas ("A\u00F1adir Todo").
 */
public class FileBrowserCanvas extends Canvas {

    private final HelloMIDlet midlet;
    private final AudioEngine engine;

    private String currentPath = "";
    private final Vector currentEntries = new Vector();
    private final Vector currentAudioFiles = new Vector();
    private final Vector selectedFiles = new Vector();

    private int selectedIndex = 0;
    private int scrollOffset = 0;

    // Modos de dialogo de pistas duplicadas
    private static final int DIALOG_NONE = 0;
    private static final int DIALOG_SINGLE_DUP = 1;
    private static final int DIALOG_MULTI_DUP = 2;

    private int dialogMode = DIALOG_NONE;
    private Track pendingSingleTrack = null;

    private Vector pendingBatch = null;
    private Vector pendingNonDuplicates = null;
    private int pendingDuplicatesCount = 0;
    private int multiDupOptionIndex = 0;

    public FileBrowserCanvas(HelloMIDlet midlet, AudioEngine engine) {
        this.midlet = midlet;
        this.engine = engine;
        setFullScreenMode(true);
    }

    protected void showNotify() {
        setFullScreenMode(true);
        if (currentEntries.isEmpty()) {
            loadDirectory("");
        }
        repaint();
    }

    public void loadDirectory(String path) {
        this.currentPath = (path == null) ? "" : path;
        currentEntries.removeAllElements();
        currentAudioFiles.removeAllElements();
        selectedFiles.removeAllElements();
        dialogMode = DIALOG_NONE;
        pendingSingleTrack = null;
        pendingBatch = null;
        pendingNonDuplicates = null;
        selectedIndex = 0;
        scrollOffset = 0;

        try {
            if (currentPath.length() == 0) {
                Enumeration roots = FileSystemRegistry.listRoots();
                while (roots.hasMoreElements()) {
                    String root = (String) roots.nextElement();
                    currentEntries.addElement(root);
                }
            } else {
                currentEntries.addElement("..");

                FileConnection fc = (FileConnection) Connector.open("file:///" + currentPath, Connector.READ);
                if (fc.isDirectory()) {
                    Enumeration list = fc.list("*", true);
                    Vector directories = new Vector();

                    while (list.hasMoreElements()) {
                        String name = (String) list.nextElement();
                        if (name.endsWith("/")) {
                            directories.addElement(name);
                        } else if (isAudioFile(name)) {
                            currentAudioFiles.addElement(name);
                        }
                    }
                    fc.close();

                    // 1. Mostrar carpetas primero
                    for (int i = 0; i < directories.size(); i++) {
                        currentEntries.addElement(directories.elementAt(i));
                    }

                    // 2. Mostrar archivos de musica
                    for (int i = 0; i < currentAudioFiles.size(); i++) {
                        currentEntries.addElement(currentAudioFiles.elementAt(i));
                    }
                } else {
                    fc.close();
                }
            }
        } catch (SecurityException se) {
            midlet.showError("Permiso Denegado", "El tel\u00E9fono no autoriz\u00F3 el acceso a los archivos.");
        } catch (Throwable t) {
            midlet.showError("Error de Archivos", "Error al leer: " + t.getMessage());
        }

        ensureVisible();
        repaint();
    }

    private boolean isAudioFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".aac") ||
               lower.endsWith(".m4a") || lower.endsWith(".wma") ||
               lower.endsWith(".wav") || lower.endsWith(".mid") ||
               lower.endsWith(".midi") || lower.endsWith(".amr") ||
               lower.endsWith(".3gp") || lower.endsWith(".3ga") ||
               lower.endsWith(".xmf") || lower.endsWith(".mxmf") ||
               lower.endsWith(".imy") || lower.endsWith(".mmf");
    }

    private void ensureVisible() {
        int visibleRows = 9; // (296 - 28) / 28 ~ 9 filas
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }
        if (scrollOffset < 0) scrollOffset = 0;
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

        if (IconStore.folder != null) {
            g.drawImage(IconStore.folder, 6, 3, Graphics.TOP | Graphics.LEFT);
        }

        Font boldSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        g.setFont(boldSmall);
        g.setColor(0x00e5ff);

        // Titulo de ruta actual
        String title = (currentPath.length() == 0) ? "UNIDADES" : currentPath;
        int maxHeaderW = w - 90;
        if (boldSmall.stringWidth(title) > maxHeaderW) {
            while (title.length() > 3 && boldSmall.stringWidth(title + "..") > maxHeaderW) {
                title = title.substring(1); // recortar de la izquierda
            }
            title = ".." + title;
        }
        g.drawString(title, 28, 6, Graphics.TOP | Graphics.LEFT);

        // Badge de elementos
        int total = currentEntries.size();
        String badgeStr = total + " elem";
        int badgeW = boldSmall.stringWidth(badgeStr) + 12;
        g.setColor(0x00476b);
        g.fillRoundRect(w - badgeW - 6, 4, badgeW, 18, 6, 6);
        g.setColor(0x66d9ff);
        g.drawString(badgeStr, w - badgeW - 6 + (badgeW / 2), 6, Graphics.TOP | Graphics.HCENTER);

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
        if (dialogMode == DIALOG_SINGLE_DUP) {
            g.setColor(0x00ff88);
            g.drawString("A\u00F1adir", 8, softkeyY + 3, Graphics.TOP | Graphics.LEFT);
            g.setColor(0xffaa00);
            g.drawString("[1] Reproducir", w / 2, softkeyY + 3, Graphics.TOP | Graphics.HCENTER);
            g.setColor(0xff7777);
            g.drawString("Cancelar", w - 8, softkeyY + 3, Graphics.TOP | Graphics.RIGHT);
        } else if (dialogMode == DIALOG_MULTI_DUP) {
            g.setColor(0x00ff88);
            g.drawString("Elegir", 8, softkeyY + 3, Graphics.TOP | Graphics.LEFT);
            g.setColor(0xaaaaaa);
            g.drawString("[1-3] Atajo", w / 2, softkeyY + 3, Graphics.TOP | Graphics.HCENTER);
            g.setColor(0xff7777);
            g.drawString("Cancelar", w - 8, softkeyY + 3, Graphics.TOP | Graphics.RIGHT);
        } else {
            if (!selectedFiles.isEmpty()) {
                g.setColor(0x00ff88);
                g.drawString("A\u00F1adir (" + selectedFiles.size() + ")", 8, softkeyY + 3, Graphics.TOP | Graphics.LEFT);
                g.setColor(0x00d2ff);
                g.drawString("[*] Marcar", w / 2, softkeyY + 3, Graphics.TOP | Graphics.HCENTER);
            } else if (!currentAudioFiles.isEmpty()) {
                g.setColor(0x00ff88);
                g.drawString("A\u00F1adir Todo", 8, softkeyY + 3, Graphics.TOP | Graphics.LEFT);
                g.setColor(0x4b6685);
                g.drawString("[#] Todo", w / 2, softkeyY + 3, Graphics.TOP | Graphics.HCENTER);
            } else {
                g.setColor(0x00d2ff);
                g.drawString("OK: Abrir", 8, softkeyY + 3, Graphics.TOP | Graphics.LEFT);
                g.setColor(0x4b6685);
                g.drawString("MPlayer", w / 2, softkeyY + 3, Graphics.TOP | Graphics.HCENTER);
            }

            g.setColor(0xff7777);
            g.drawString("Volver", w - 8, softkeyY + 3, Graphics.TOP | Graphics.RIGHT);
        }

        // 4. Area de Archivos y Carpetas
        int contentY = headerH + 2;
        int contentH = softkeyY - contentY - 2;

        if (total == 0) {
            Font fontBold = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
            g.setFont(fontBold);
            g.setColor(0xffffff);
            g.drawString("CARPETA VAC\u00CDA", w / 2, contentY + 60, Graphics.TOP | Graphics.HCENTER);

            Font fontPlain = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            g.setFont(fontPlain);
            g.setColor(0x8faecf);
            g.drawString("No hay canciones compatibles", w / 2, contentY + 85, Graphics.TOP | Graphics.HCENTER);
            g.setColor(0x00d2ff);
            g.drawString("Pulsa [Volver] o tecla 'C' para subir", w / 2, contentY + 105, Graphics.TOP | Graphics.HCENTER);
        } else {
            int oldClipX = g.getClipX();
            int oldClipY = g.getClipY();
            int oldClipW = g.getClipWidth();
            int oldClipH = g.getClipHeight();

            g.setClip(0, contentY, w - 6, contentH);

            int rowH = 28;
            Font fontPlain = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            Font fontBold = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);

            for (int i = scrollOffset; i < total; i++) {
                int rowY = contentY + ((i - scrollOffset) * rowH);
                if (rowY + rowH > contentY + contentH + rowH) break;

                boolean isSelected = (i == selectedIndex);
                String entry = (String) currentEntries.elementAt(i);
                boolean isUp = entry.equals("..");
                boolean isDir = entry.endsWith("/") || isUp;
                boolean isAudio = !isDir && isAudioFile(entry);
                boolean isChecked = isAudio && selectedFiles.contains(entry);

                if (isSelected) {
                    g.setColor(0x005588);
                    g.fillRoundRect(4, rowY + 1, w - 14, rowH - 2, 6, 6);
                    g.setColor(0x00d2ff);
                    g.drawRoundRect(4, rowY + 1, w - 14, rowH - 2, 6, 6);
                } else {
                    g.setColor(0x101824);
                    g.drawLine(6, rowY + rowH - 1, w - 16, rowY + rowH - 1);
                }

                // Icono
                int iconY = rowY + (rowH - 16) / 2;
                if (isDir) {
                    if (IconStore.folder != null) {
                        g.drawImage(IconStore.folder, 8, iconY, Graphics.TOP | Graphics.LEFT);
                    }
                } else {
                    if (IconStore.musicNote != null) {
                        g.drawImage(IconStore.musicNote, 8, iconY, Graphics.TOP | Graphics.LEFT);
                    }
                }

                // Checkbox para canciones
                if (isAudio) {
                    int cbY = rowY + (rowH - 14) / 2;
                    g.setColor(isChecked ? 0x004d73 : 0x141f2d);
                    g.fillRect(26, cbY, 14, 14);
                    g.setColor(isChecked ? 0x00e5ff : 0x4a6b8c);
                    g.drawRect(26, cbY, 13, 13);
                    if (isChecked) {
                        g.setColor(0x00ff88);
                        g.drawLine(28, cbY + 7, 31, cbY + 10);
                        g.drawLine(31, cbY + 10, 37, cbY + 3);
                        g.drawLine(28, cbY + 8, 31, cbY + 11);
                        g.drawLine(31, cbY + 11, 37, cbY + 4);
                    }
                }

                // Etiqueta legible adaptada para Sony Ericsson K800i
                String label = entry;
                if (isUp) {
                    label = ".. (Subir nivel)";
                } else if (currentPath.length() == 0) {
                    if (entry.equalsIgnoreCase("card/")) {
                        label = "Tarjeta M2 (card/)";
                    } else if (entry.equalsIgnoreCase("other/")) {
                        label = "Memoria Tel\u00E9fono (other/)";
                    }
                }

                g.setFont(isSelected ? fontBold : fontPlain);
                g.setColor(isSelected ? 0xffffff : (isDir ? 0xffd54f : (isChecked ? 0x00ffcc : 0xb5d0ec)));

                int labelX = isAudio ? 44 : 28;
                int maxTextW = isAudio ? (w - 95) : (w - 75);
                String displayStr = label;
                if (g.getFont().stringWidth(displayStr) > maxTextW) {
                    while (displayStr.length() > 3 && g.getFont().stringWidth(displayStr + "..") > maxTextW) {
                        displayStr = displayStr.substring(0, displayStr.length() - 1);
                    }
                    displayStr += "..";
                }
                g.drawString(displayStr, labelX, rowY + 6, Graphics.TOP | Graphics.LEFT);

                // Badge derecho: [DIR] o Formato [MP3], [AAC], etc.
                int badgeTagW = 34;
                int badgeTagX = w - 16 - badgeTagW;
                String tagStr = "DIR";
                if (!isDir) {
                    int dot = entry.lastIndexOf('.');
                    if (dot != -1 && dot < entry.length() - 1) {
                        tagStr = entry.substring(dot + 1).toUpperCase();
                    }
                }

                g.setColor(isSelected ? 0x003d5c : 0x131f2d);
                g.fillRoundRect(badgeTagX, rowY + 5, badgeTagW, 16, 4, 4);
                g.setColor(isSelected ? 0x00e5ff : (isDir ? 0x8a6d00 : 0x4a6b8c));
                g.drawRoundRect(badgeTagX, rowY + 5, badgeTagW, 16, 4, 4);

                Font tinyFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
                g.setFont(tinyFont);
                g.setColor(isSelected ? 0x00ffff : (isDir ? 0xffe066 : 0x6e92b8));
                g.drawString(tagStr, badgeTagX + (badgeTagW / 2), rowY + 6, Graphics.TOP | Graphics.HCENTER);
            }

            g.setClip(oldClipX, oldClipY, oldClipW, oldClipH);

            // 5. Scrollbar vertical
            if (total > 9) {
                int sbX = w - 5;
                int sbY = contentY + 2;
                int sbW = 3;
                int sbH = contentH - 4;

                g.setColor(0x131e2b);
                g.fillRect(sbX, sbY, sbW, sbH);

                int thumbH = Math.max(16, (sbH * 9) / total);
                int thumbY = sbY + (scrollOffset * (sbH - thumbH)) / (total - 9);

                g.setColor(0x00d2ff);
                g.fillRect(sbX, thumbY, sbW, thumbH);
            }
        }

        // 6. Dialogos de confirmacion de canciones repetidas
        if (dialogMode == DIALOG_SINGLE_DUP) {
            paintSingleDupDialog(g, w, h);
        } else if (dialogMode == DIALOG_MULTI_DUP) {
            paintMultiDupDialog(g, w, h);
        }
    }

    private void paintSingleDupDialog(Graphics g, int w, int h) {
        int boxW = 216;
        int boxH = 155;
        int boxX = (w - boxW) / 2;
        int boxY = (h - boxH) / 2;

        g.setColor(0x000000);
        g.fillRect(boxX - 2, boxY - 2, boxW + 4, boxH + 4);

        g.setColor(0x141005);
        g.fillRect(boxX, boxY, boxW, boxH);
        g.setColor(0xffaa00);
        g.drawRect(boxX, boxY, boxW - 1, boxH - 1);
        g.drawRect(boxX + 1, boxY + 1, boxW - 3, boxH - 3);

        int titleH = 22;
        g.setColor(0x2d1f05);
        g.fillRect(boxX + 2, boxY + 2, boxW - 4, titleH);
        g.setColor(0xffaa00);
        g.drawLine(boxX + 2, boxY + titleH + 2, boxX + boxW - 3, boxY + titleH + 2);

        Font boldSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        Font plainSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(boldSmall);
        g.setColor(0xffcc00);
        g.drawString("CANCION REPETIDA", boxX + (boxW / 2), boxY + 5, Graphics.TOP | Graphics.HCENTER);

        g.setFont(plainSmall);
        g.setColor(0xffffff);
        g.drawString("Esta pista ya est\u00E1 en la lista:", boxX + (boxW / 2), boxY + 34, Graphics.TOP | Graphics.HCENTER);

        g.setFont(boldSmall);
        g.setColor(0x00e5ff);
        String songTitle = (pendingSingleTrack != null) ? pendingSingleTrack.getTitle() : "Pista";
        if (boldSmall.stringWidth(songTitle) > boxW - 20) {
            while (songTitle.length() > 3 && boldSmall.stringWidth(songTitle + "..") > boxW - 20) {
                songTitle = songTitle.substring(0, songTitle.length() - 1);
            }
            songTitle += "..";
        }
        g.drawString(songTitle, boxX + (boxW / 2), boxY + 52, Graphics.TOP | Graphics.HCENTER);

        g.setFont(plainSmall);
        g.setColor(0xdddddd);
        g.drawString("\u00BFDeseas a\u00F1adirla de todas formas?", boxX + (boxW / 2), boxY + 72, Graphics.TOP | Graphics.HCENTER);

        g.setColor(0x1e2c3d);
        g.fillRoundRect(boxX + 10, boxY + 92, boxW - 20, 20, 4, 4);
        g.setColor(0x00d2ff);
        g.drawRoundRect(boxX + 10, boxY + 92, boxW - 20, 20, 4, 4);
        g.setFont(boldSmall);
        g.setColor(0x00ff88);
        g.drawString("[OK] S\u00ED, a\u00F1adir de nuevo", boxX + (boxW / 2), boxY + 95, Graphics.TOP | Graphics.HCENTER);

        int footerY = boxY + boxH - 24;
        g.setColor(0x221703);
        g.fillRect(boxX + 2, footerY, boxW - 4, 22);
        g.setColor(0x553805);
        g.drawLine(boxX + 2, footerY, boxX + boxW - 3, footerY);

        g.setFont(boldSmall);
        g.setColor(0xffaa00);
        g.drawString("[1] Reproducir", boxX + 10, footerY + 4, Graphics.TOP | Graphics.LEFT);
        g.setColor(0xff7777);
        g.drawString("[C] Cancelar", boxX + boxW - 10, footerY + 4, Graphics.TOP | Graphics.RIGHT);
    }

    private void paintMultiDupDialog(Graphics g, int w, int h) {
        int boxW = 216;
        int boxH = 175;
        int boxX = (w - boxW) / 2;
        int boxY = (h - boxH) / 2;

        g.setColor(0x000000);
        g.fillRect(boxX - 2, boxY - 2, boxW + 4, boxH + 4);

        g.setColor(0x141005);
        g.fillRect(boxX, boxY, boxW, boxH);
        g.setColor(0xffaa00);
        g.drawRect(boxX, boxY, boxW - 1, boxH - 1);
        g.drawRect(boxX + 1, boxY + 1, boxW - 3, boxH - 3);

        int titleH = 22;
        g.setColor(0x2d1f05);
        g.fillRect(boxX + 2, boxY + 2, boxW - 4, titleH);
        g.setColor(0xffaa00);
        g.drawLine(boxX + 2, boxY + titleH + 2, boxX + boxW - 3, boxY + titleH + 2);

        Font boldSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        Font plainSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(boldSmall);
        g.setColor(0xffcc00);
        g.drawString("CANCIONES REPETIDAS", boxX + (boxW / 2), boxY + 5, Graphics.TOP | Graphics.HCENTER);

        int totalSelected = (pendingBatch != null) ? pendingBatch.size() : 0;
        int nonDupCount = (pendingNonDuplicates != null) ? pendingNonDuplicates.size() : 0;

        g.setFont(plainSmall);
        g.setColor(0xffffff);
        String msg = pendingDuplicatesCount + " de " + totalSelected + " canciones";
        g.drawString(msg, boxX + (boxW / 2), boxY + 29, Graphics.TOP | Graphics.HCENTER);
        g.setColor(0xdddddd);
        g.drawString("ya est\u00E1n en la playlist actual.", boxX + (boxW / 2), boxY + 45, Graphics.TOP | Graphics.HCENTER);

        int itemStartY = boxY + 64;
        int itemH = 24;

        String[] optLabels = {
            "1. Omitir (" + nonDupCount + " nuevas)",
            "2. A\u00F1adir todas (" + totalSelected + ")",
            "3. Cancelar"
        };

        for (int i = 0; i < 3; i++) {
            int curY = itemStartY + (i * itemH);
            boolean isSel = (i == multiDupOptionIndex);

            if (isSel) {
                g.setColor(0x005588);
                g.fillRoundRect(boxX + 6, curY, boxW - 12, itemH - 2, 4, 4);
                g.setColor(0x00d2ff);
                g.drawRoundRect(boxX + 6, curY, boxW - 12, itemH - 2, 4, 4);
                g.setFont(boldSmall);
                g.setColor(0xffffff);
            } else {
                g.setFont(plainSmall);
                g.setColor(0xbed4e8);
            }

            g.drawString(optLabels[i], boxX + 12, curY + 4, Graphics.TOP | Graphics.LEFT);
        }

        int footerY = boxY + boxH - 20;
        g.setColor(0x221703);
        g.fillRect(boxX + 2, footerY, boxW - 4, 18);
        g.setColor(0x553805);
        g.drawLine(boxX + 2, footerY, boxX + boxW - 3, footerY);

        Font tinyFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(tinyFont);
        g.setColor(0xaaaaaa);
        g.drawString("[OK] Elegir   [1-3] Atajo   [C] Cancelar", boxX + (boxW / 2), footerY + 2, Graphics.TOP | Graphics.HCENTER);
    }

    private void executeMultiDupOption(int optIndex) {
        switch (optIndex) {
            case 0: // Omitir repetidas (añadir solo no-duplicadas)
                dialogMode = DIALOG_NONE;
                if (pendingNonDuplicates != null && !pendingNonDuplicates.isEmpty()) {
                    int startIndex = engine.getPlaylistSize();
                    engine.addTracks(pendingNonDuplicates);
                    engine.playTrack(startIndex);
                }
                selectedFiles.removeAllElements();
                pendingBatch = null;
                pendingNonDuplicates = null;
                midlet.showPlayer();
                break;
            case 1: // Añadir todas de todas formas
                dialogMode = DIALOG_NONE;
                if (pendingBatch != null && !pendingBatch.isEmpty()) {
                    int startIndex = engine.getPlaylistSize();
                    engine.addTracks(pendingBatch);
                    engine.playTrack(startIndex);
                }
                selectedFiles.removeAllElements();
                pendingBatch = null;
                pendingNonDuplicates = null;
                midlet.showPlayer();
                break;
            case 2: // Cancelar
            default:
                dialogMode = DIALOG_NONE;
                pendingBatch = null;
                pendingNonDuplicates = null;
                repaint();
                break;
        }
    }

    private void requestAddTracks(Vector fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            midlet.showError("Aviso", "No hay archivos de m\u00FAsica seleccionados.");
            return;
        }

        Vector batch = new Vector();
        for (int i = 0; i < fileNames.size(); i++) {
            String file = (String) fileNames.elementAt(i);
            batch.addElement(new Track(file, "file:///" + currentPath + file, false));
        }

        Vector duplicates = engine.getDuplicates(batch);
        Vector nonDuplicates = engine.getNonDuplicates(batch);

        if (duplicates.isEmpty()) {
            // No hay pistas repetidas, añadir y reproducir directamente
            int startIndex = engine.getPlaylistSize();
            engine.addTracks(batch);
            engine.playTrack(startIndex);
            selectedFiles.removeAllElements();
            midlet.showPlayer();
        } else {
            // Hay pistas repetidas, abrir dialogo de confirmacion
            pendingBatch = batch;
            pendingNonDuplicates = nonDuplicates;
            pendingDuplicatesCount = duplicates.size();
            multiDupOptionIndex = 0;
            dialogMode = DIALOG_MULTI_DUP;
            repaint();
        }
    }

    private void toggleFileSelection(String filename) {
        if (isAudioFile(filename)) {
            if (selectedFiles.contains(filename)) {
                selectedFiles.removeElement(filename);
            } else {
                selectedFiles.addElement(filename);
            }
            repaint();
        }
    }

    private void toggleSelectAll() {
        if (currentAudioFiles.isEmpty()) return;

        if (selectedFiles.size() == currentAudioFiles.size()) {
            selectedFiles.removeAllElements();
        } else {
            selectedFiles.removeAllElements();
            for (int i = 0; i < currentAudioFiles.size(); i++) {
                selectedFiles.addElement(currentAudioFiles.elementAt(i));
            }
        }
        repaint();
    }

    private void selectCurrent() {
        if (selectedIndex >= 0 && selectedIndex < currentEntries.size()) {
            String entry = (String) currentEntries.elementAt(selectedIndex);
            if (entry.equals("..")) {
                goUp();
            } else if (entry.endsWith("/")) {
                loadDirectory(currentPath + entry);
            } else {
                // Si el usuario ya inicio seleccion multiple con casillas, pulsar OK marca/desmarca
                if (!selectedFiles.isEmpty()) {
                    toggleFileSelection(entry);
                    return;
                }

                // Reproducir archivo individual: verificar si esta repetido
                String fullPath = "file:///" + currentPath + entry;
                Track track = new Track(entry, fullPath, false);

                if (engine.containsTrack(track)) {
                    pendingSingleTrack = track;
                    dialogMode = DIALOG_SINGLE_DUP;
                    repaint();
                } else {
                    engine.addTrack(track);
                    engine.playTrack(engine.getPlaylistSize() - 1);
                    midlet.showPlayer();
                }
            }
        }
    }

    private void goUp() {
        if (currentPath.length() == 0) {
            midlet.showPlayer();
            return;
        }

        String path = currentPath;
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        int lastSlash = path.lastIndexOf('/');
        if (lastSlash != -1) {
            loadDirectory(path.substring(0, lastSlash + 1));
        } else {
            loadDirectory("");
        }
    }

    protected void keyPressed(int keyCode) {
        int gameAction = 0;
        try {
            gameAction = getGameAction(keyCode);
        } catch (Throwable t) {
            gameAction = 0;
        }

        // 1. Manejo cuando el dialogo de pista repetida individual esta abierto
        if (dialogMode == DIALOG_SINGLE_DUP) {
            if (gameAction == FIRE || keyCode == -5 || keyCode == -6 || keyCode == -21 || keyCode == 10 || keyCode == 32) {
                if (pendingSingleTrack != null) {
                    engine.addTrack(pendingSingleTrack);
                    engine.playTrack(engine.getPlaylistSize() - 1);
                }
                dialogMode = DIALOG_NONE;
                pendingSingleTrack = null;
                midlet.showPlayer();
                return;
            }
            if (keyCode == KEY_NUM1) {
                if (pendingSingleTrack != null) {
                    int existingIdx = engine.findTrackIndex(pendingSingleTrack);
                    if (existingIdx >= 0) {
                        engine.playTrack(existingIdx);
                    }
                }
                dialogMode = DIALOG_NONE;
                pendingSingleTrack = null;
                midlet.showPlayer();
                return;
            }
            if (keyCode == -7 || keyCode == -22 || keyCode == -8 || keyCode == 8 || keyCode == 27 || keyCode == KEY_NUM0) {
                dialogMode = DIALOG_NONE;
                pendingSingleTrack = null;
                repaint();
                return;
            }
            return;
        }

        // 2. Manejo cuando el dialogo de multiples pistas repetidas esta abierto
        if (dialogMode == DIALOG_MULTI_DUP) {
            if (gameAction == UP || keyCode == -1 || keyCode == 38) {
                multiDupOptionIndex = (multiDupOptionIndex - 1 + 3) % 3;
                repaint();
                return;
            }
            if (gameAction == DOWN || keyCode == -2 || keyCode == 40) {
                multiDupOptionIndex = (multiDupOptionIndex + 1) % 3;
                repaint();
                return;
            }
            if (gameAction == FIRE || keyCode == -5 || keyCode == -6 || keyCode == -21 || keyCode == 10 || keyCode == 32) {
                executeMultiDupOption(multiDupOptionIndex);
                return;
            }
            if (keyCode == KEY_NUM1) {
                executeMultiDupOption(0);
                return;
            }
            if (keyCode == KEY_NUM2) {
                executeMultiDupOption(1);
                return;
            }
            if (keyCode == KEY_NUM3) {
                executeMultiDupOption(2);
                return;
            }
            if (keyCode == -7 || keyCode == -22 || keyCode == -8 || keyCode == 8 || keyCode == 27 || keyCode == KEY_NUM0) {
                executeMultiDupOption(2);
                return;
            }
            return;
        }

        // 3. Manejo normal del explorador de archivos
        int total = currentEntries.size();

        // Alternar seleccion con Tecla '*' o Tecla '1'
        if (keyCode == KEY_STAR || keyCode == KEY_NUM1 || keyCode == 42) {
            if (selectedIndex >= 0 && selectedIndex < total) {
                String entry = (String) currentEntries.elementAt(selectedIndex);
                toggleFileSelection(entry);
            }
            return;
        }

        // Alternar marcar todo / desmarcar con Tecla '#'
        if (keyCode == KEY_POUND || keyCode == 35) {
            toggleSelectAll();
            return;
        }

        // Navegacion Arriba / Abajo
        if (gameAction == UP || keyCode == -1 || keyCode == 38 || keyCode == KEY_NUM2) {
            if (total > 0) {
                selectedIndex = (selectedIndex - 1 + total) % total;
                ensureVisible();
                repaint();
            }
            return;
        }

        if (gameAction == DOWN || keyCode == -2 || keyCode == 40 || keyCode == KEY_NUM8) {
            if (total > 0) {
                selectedIndex = (selectedIndex + 1) % total;
                ensureVisible();
                repaint();
            }
            return;
        }

        // Abrir / Seleccionar (Joystick OK, Enter, Space, Tecla 5)
        if (gameAction == FIRE || keyCode == -5 || keyCode == 10 || keyCode == 32 || keyCode == KEY_NUM5) {
            selectCurrent();
            return;
        }

        // Subir nivel con Tecla 'C' de Sony Ericsson (-8), Backspace o Delete
        if (keyCode == -8 || keyCode == 8 || keyCode == 127) {
            if (!selectedFiles.isEmpty()) {
                selectedFiles.removeAllElements();
                repaint();
            } else {
                goUp();
            }
            return;
        }

        // Softkey Izquierdo: Añadir seleccionadas o Añadir Todo
        if (keyCode == -6 || keyCode == -21) {
            if (!selectedFiles.isEmpty()) {
                requestAddTracks(selectedFiles);
            } else if (!currentAudioFiles.isEmpty()) {
                requestAddTracks(currentAudioFiles);
            } else {
                selectCurrent();
            }
            return;
        }

        // Softkey Derecho / Tecla 0: Volver
        if (keyCode == -7 || keyCode == -22 || keyCode == 27 || keyCode == KEY_NUM0) {
            if (currentPath.length() > 0) {
                goUp();
            } else {
                midlet.showPlayer();
            }
            return;
        }
    }

    protected void keyRepeated(int keyCode) {
        keyPressed(keyCode);
    }

    protected void pointerPressed(int x, int y) {
        int w = getWidth();
        int h = getHeight();

        // 1. Si dialogo de duplicado individual esta abierto
        if (dialogMode == DIALOG_SINGLE_DUP) {
            int boxW = 216;
            int boxH = 155;
            int boxX = (w - boxW) / 2;
            int boxY = (h - boxH) / 2;
            int footerY = boxY + boxH - 24;

            if (x >= boxX + 10 && x <= boxX + boxW - 10 && y >= boxY + 92 && y <= boxY + 114) {
                if (pendingSingleTrack != null) {
                    engine.addTrack(pendingSingleTrack);
                    engine.playTrack(engine.getPlaylistSize() - 1);
                }
                dialogMode = DIALOG_NONE;
                pendingSingleTrack = null;
                midlet.showPlayer();
                return;
            }

            if (y >= footerY && y <= boxY + boxH) {
                if (x <= boxX + (boxW / 2)) {
                    if (pendingSingleTrack != null) {
                        int idx = engine.findTrackIndex(pendingSingleTrack);
                        if (idx >= 0) engine.playTrack(idx);
                    }
                    dialogMode = DIALOG_NONE;
                    pendingSingleTrack = null;
                    midlet.showPlayer();
                    return;
                } else {
                    dialogMode = DIALOG_NONE;
                    pendingSingleTrack = null;
                    repaint();
                    return;
                }
            }

            if (x < boxX || x > boxX + boxW || y < boxY || y > boxY + boxH) {
                dialogMode = DIALOG_NONE;
                pendingSingleTrack = null;
                repaint();
                return;
            }
            return;
        }

        // 2. Si dialogo de multiples duplicados esta abierto
        if (dialogMode == DIALOG_MULTI_DUP) {
            int boxW = 216;
            int boxH = 175;
            int boxX = (w - boxW) / 2;
            int boxY = (h - boxH) / 2;
            int itemStartY = boxY + 64;
            int itemH = 24;
            int footerY = boxY + boxH - 20;

            if (x >= boxX + 6 && x <= boxX + boxW - 6 && y >= itemStartY && y < itemStartY + (3 * itemH)) {
                int clickedOpt = (y - itemStartY) / itemH;
                if (clickedOpt >= 0 && clickedOpt < 3) {
                    executeMultiDupOption(clickedOpt);
                    return;
                }
            }

            if (y >= footerY && y <= boxY + boxH) {
                executeMultiDupOption(2);
                return;
            }

            if (x < boxX || x > boxX + boxW || y < boxY || y > boxY + boxH) {
                executeMultiDupOption(2);
                return;
            }
            return;
        }

        // 3. Softkeys inferiores
        int softkeyY = h - 22;
        if (y >= softkeyY) {
            if (x <= 75) {
                if (!selectedFiles.isEmpty()) {
                    requestAddTracks(selectedFiles);
                } else if (!currentAudioFiles.isEmpty()) {
                    requestAddTracks(currentAudioFiles);
                } else {
                    selectCurrent();
                }
                return;
            } else if (x > 75 && x < w - 75) {
                if (!currentAudioFiles.isEmpty()) {
                    toggleSelectAll();
                }
                return;
            } else if (x >= w - 75) {
                if (currentPath.length() > 0) {
                    goUp();
                } else {
                    midlet.showPlayer();
                }
                return;
            }
        }

        // 4. Toques en las filas
        int contentY = 28;
        int rowH = 28;
        int total = currentEntries.size();

        if (y >= contentY && y < softkeyY) {
            int clickedRow = scrollOffset + ((y - contentY) / rowH);
            if (clickedRow >= 0 && clickedRow < total) {
                String entry = (String) currentEntries.elementAt(clickedRow);
                boolean isAudio = !entry.endsWith("/") && !entry.equals("..") && isAudioFile(entry);

                if (isAudio && x >= 18 && x <= 46) {
                    selectedIndex = clickedRow;
                    toggleFileSelection(entry);
                    return;
                }

                if (clickedRow == selectedIndex) {
                    selectCurrent();
                } else {
                    selectedIndex = clickedRow;
                    repaint();
                }
            }
        }
    }

}
