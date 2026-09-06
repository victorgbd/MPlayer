package mplayer;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Pantalla de Lista de Reproduccion (Playlist) en Pantalla Completa (240x320).
 * Dise\u00F1o Walkman / Cyber-shot con seleccion interactiva, borrado con tecla 'C',
 * indicadores graficos de reproduccion activa y scrollbar nativa.
 */
public class PlaylistCanvas extends Canvas {

    private final HelloMIDlet midlet;
    private final AudioEngine engine;

    private int selectedIndex = 0;
    private int scrollOffset = 0;

    // Menu flotante de opciones dentro de la playlist
    private boolean isMenuOpen = false;
    private int menuIndex = 0;
    private static final String[] PLAYLIST_MENU = {
        "1. Reproducir Pista",
        "2. Reproducir en Aleatorio",
        "3. Modo Aleatorio",
        "4. Cambiar Playlist",
        "5. Nueva Playlist",
        "6. Eliminar Playlist",
        "7. Eliminar de la Lista",
        "8. Vaciar Lista Completa",
        "9. Explorar Archivos",
        "10. Minimizar",
        "11. Volver al Reproductor"
    };

    // Dialogo de seleccion de Playlist
    private boolean isSwitchPlaylistOpen = false;
    private int switchPlaylistIndex = 0;
    private int switchScrollOffset = 0;

    // Dialogo de confirmacion de eliminacion de Playlist
    private boolean isDeleteConfirmOpen = false;

    public PlaylistCanvas(HelloMIDlet midlet, AudioEngine engine) {
        this.midlet = midlet;
        this.engine = engine;
        setFullScreenMode(true);
    }

    public void refreshList() {
        int total = engine.getPlaylistSize();
        int cur = engine.getCurrentTrackIndex();
        if (cur >= 0 && cur < total) {
            selectedIndex = cur;
        } else if (selectedIndex >= total) {
            selectedIndex = Math.max(0, total - 1);
        }
        ensureVisible();
        repaint();
    }

    protected void showNotify() {
        setFullScreenMode(true);
        refreshList();
    }

    private void ensureVisible() {
        int rowH = 30;
        int visibleRows = 8; // (296 - 28) / 30 ~ 8 filas visibles
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

        if (IconStore.playlist != null) {
            g.drawImage(IconStore.playlist, 6, 3, Graphics.TOP | Graphics.LEFT);
        }

        Font boldSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        g.setFont(boldSmall);
        g.setColor(0x00e5ff);

        String activeName = engine.getActivePlaylistName();
        String headerTitle = "LISTA: " + (activeName != null ? activeName.toUpperCase() : "PRINCIPAL");
        int maxHeaderW = w - 100;
        if (boldSmall.stringWidth(headerTitle) > maxHeaderW) {
            while (headerTitle.length() > 3 && boldSmall.stringWidth(headerTitle + "..") > maxHeaderW) {
                headerTitle = headerTitle.substring(0, headerTitle.length() - 1);
            }
            headerTitle += "..";
        }
        g.drawString(headerTitle, 30, 6, Graphics.TOP | Graphics.LEFT);

        int totalTracks = engine.getPlaylistSize();
        String badgeText = totalTracks + (totalTracks == 1 ? " pista" : " pistas");
        int badgeW = boldSmall.stringWidth(badgeText) + 12;
        g.setColor(0x00476b);
        g.fillRoundRect(w - badgeW - 6, 4, badgeW, 18, 6, 6);
        g.setColor(0x66d9ff);
        g.drawString(badgeText, w - badgeW - 6 + (badgeW / 2), 6, Graphics.TOP | Graphics.HCENTER);

        if (engine.isShuffle() && IconStore.shuffle != null) {
            g.drawImage(IconStore.shuffle, w - badgeW - 28, 3, Graphics.TOP | Graphics.LEFT);
        }

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
        if (isDeleteConfirmOpen) {
            g.setColor(0xff5555);
            g.drawString("Eliminar", 8, softkeyY + 3, Graphics.TOP | Graphics.LEFT);
            g.setColor(0xff7777);
            g.drawString("Cancelar", w - 8, softkeyY + 3, Graphics.TOP | Graphics.RIGHT);
        } else if (isMenuOpen || isSwitchPlaylistOpen) {
            g.setColor(0x00ff88);
            g.drawString("OK: Elegir", 8, softkeyY + 3, Graphics.TOP | Graphics.LEFT);
            g.setColor(0xff7777);
            g.drawString("Cancelar", w - 8, softkeyY + 3, Graphics.TOP | Graphics.RIGHT);
        } else {
            g.setColor(0x00d2ff);
            g.drawString("Opciones", 8, softkeyY + 3, Graphics.TOP | Graphics.LEFT);

            g.setColor(0x4b6685);
            g.drawString("[C] Borrar", w / 2, softkeyY + 3, Graphics.TOP | Graphics.HCENTER);

            g.setColor(0xff7777);
            g.drawString("Volver", w - 8, softkeyY + 3, Graphics.TOP | Graphics.RIGHT);
        }

        // 4. Contenido de la Lista
        int contentY = headerH + 2;
        int contentH = softkeyY - contentY - 2;

        if (totalTracks == 0) {
            // Estado vacio
            if (IconStore.playlist != null) {
                int iconX = (w - IconStore.playlist.getWidth()) / 2;
                g.drawImage(IconStore.playlist, iconX, contentY + 40, Graphics.TOP | Graphics.LEFT);
            }
            Font fontBold = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
            g.setFont(fontBold);
            g.setColor(0xffffff);
            g.drawString("LISTA VAC\u00CDA", w / 2, contentY + 70, Graphics.TOP | Graphics.HCENTER);

            Font fontPlain = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            g.setFont(fontPlain);
            g.setColor(0x00d2ff);
            g.drawString("Pulsa [OK] o [#] para explorar m\u00FAsica", w / 2, contentY + 95, Graphics.TOP | Graphics.HCENTER);

            g.setColor(0x718ba8);
            g.drawString("En la tarjeta M2 o memoria del tel\u00E9fono", w / 2, contentY + 115, Graphics.TOP | Graphics.HCENTER);

            // Boton sugerencia
            g.setColor(0x132235);
            g.fillRoundRect(30, contentY + 145, w - 60, 28, 6, 6);
            g.setColor(0x00d2ff);
            g.drawRoundRect(30, contentY + 145, w - 60, 28, 6, 6);
            g.setFont(boldSmall);
            g.setColor(0x66ffcc);
            g.drawString("Explorar Tarjeta M2 [ # ]", w / 2, contentY + 152, Graphics.TOP | Graphics.HCENTER);
        } else {
            int oldClipX = g.getClipX();
            int oldClipY = g.getClipY();
            int oldClipW = g.getClipWidth();
            int oldClipH = g.getClipHeight();

            g.setClip(0, contentY, w - 6, contentH);

            int rowH = 30;
            int curActiveTrack = engine.getCurrentTrackIndex();
            Font fontTitle = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            Font fontTitleBold = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);

            for (int i = scrollOffset; i < totalTracks; i++) {
                int rowY = contentY + ((i - scrollOffset) * rowH);
                if (rowY + rowH > contentY + contentH + rowH) break;

                boolean isSelected = (i == selectedIndex);
                boolean isCurrentPlaying = (i == curActiveTrack);

                if (isSelected) {
                    // Fondo de seleccion resplandeciente
                    g.setColor(0x005588);
                    g.fillRoundRect(4, rowY + 1, w - 14, rowH - 2, 6, 6);
                    g.setColor(0x00d2ff);
                    g.drawRoundRect(4, rowY + 1, w - 14, rowH - 2, 6, 6);
                } else {
                    // Linea sutil entre filas
                    g.setColor(0x101824);
                    g.drawLine(6, rowY + rowH - 1, w - 16, rowY + rowH - 1);
                }

                // Icono de estado (Play si suena, o nota musical)
                int iconY = rowY + (rowH - 16) / 2;
                if (isCurrentPlaying) {
                    if (IconStore.play != null) {
                        g.drawImage(IconStore.play, 8, iconY, Graphics.TOP | Graphics.LEFT);
                    }
                } else {
                    if (IconStore.musicNote != null) {
                        g.drawImage(IconStore.musicNote, 8, iconY, Graphics.TOP | Graphics.LEFT);
                    }
                }

                // Titulo de la cancion
                Track t = engine.getTrack(i);
                String title = (t != null) ? t.getTitle() : "Pista " + (i + 1);
                String fmt = (t != null) ? t.getFormat() : "MP3";

                g.setFont(isSelected ? fontTitleBold : fontTitle);
                g.setColor(isSelected ? 0xffffff : (isCurrentPlaying ? 0x00ff88 : 0xb5d0ec));

                // Recortar texto si es largo para dar espacio al badge de formato
                int maxTextW = w - 85;
                String displayTitle = title;
                if (g.getFont().stringWidth(displayTitle) > maxTextW) {
                    while (displayTitle.length() > 3 && g.getFont().stringWidth(displayTitle + "..") > maxTextW) {
                        displayTitle = displayTitle.substring(0, displayTitle.length() - 1);
                    }
                    displayTitle += "..";
                }

                g.drawString((i + 1) + ". " + displayTitle, 28, rowY + 7, Graphics.TOP | Graphics.LEFT);

                // Badge de formato [MP3, AAC, etc.]
                int badgeTagW = 34;
                int badgeTagX = w - 16 - badgeTagW;
                g.setColor(isSelected ? 0x003d5c : 0x131f2d);
                g.fillRoundRect(badgeTagX, rowY + 6, badgeTagW, 16, 4, 4);
                g.setColor(isSelected ? 0x00e5ff : 0x4a6b8c);
                g.drawRoundRect(badgeTagX, rowY + 6, badgeTagW, 16, 4, 4);

                Font tinyFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
                g.setFont(tinyFont);
                g.setColor(isSelected ? 0x00ffff : 0x6e92b8);
                g.drawString(fmt, badgeTagX + (badgeTagW / 2), rowY + 7, Graphics.TOP | Graphics.HCENTER);
            }

            g.setClip(oldClipX, oldClipY, oldClipW, oldClipH);

            // 5. Scrollbar vertical
            if (totalTracks > 8) {
                int sbX = w - 5;
                int sbY = contentY + 2;
                int sbW = 3;
                int sbH = contentH - 4;

                g.setColor(0x131e2b);
                g.fillRect(sbX, sbY, sbW, sbH);

                int thumbH = Math.max(16, (sbH * 8) / totalTracks);
                int thumbY = sbY + (scrollOffset * (sbH - thumbH)) / (totalTracks - 8);

                g.setColor(0x00d2ff);
                g.fillRect(sbX, thumbY, sbW, thumbH);
            }
        }

        // 6. Dialogos y Menu flotante de Opciones
        if (isMenuOpen) {
            paintOptionsMenu(g, w, h);
        } else if (isSwitchPlaylistOpen) {
            paintSwitchPlaylist(g, w, h);
        } else if (isDeleteConfirmOpen) {
            paintDeleteConfirm(g, w, h);
        }
    }

    private void paintOptionsMenu(Graphics g, int w, int h) {
        int menuW = 216;
        int menuH = 264;
        int menuX = (w - menuW) / 2;
        int menuY = (h - menuH) / 2;

        g.setColor(0x000000);
        g.fillRect(menuX - 2, menuY - 2, menuW + 4, menuH + 4);

        g.setColor(0x0b131e);
        g.fillRect(menuX, menuY, menuW, menuH);

        g.setColor(0x00d2ff);
        g.drawRect(menuX, menuY, menuW - 1, menuH - 1);
        g.drawRect(menuX + 1, menuY + 1, menuW - 3, menuH - 3);

        int titleH = 22;
        g.setColor(0x132337);
        g.fillRect(menuX + 2, menuY + 2, menuW - 4, titleH);
        g.setColor(0x00d2ff);
        g.drawLine(menuX + 2, menuY + titleH + 2, menuX + menuW - 3, menuY + titleH + 2);

        Font boldFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        g.setFont(boldFont);
        g.setColor(0x00e5ff);
        g.drawString("OPCIONES PLAYLIST", menuX + (menuW / 2), menuY + 5, Graphics.TOP | Graphics.HCENTER);

        int itemY = menuY + titleH + 4;
        int itemH = 20;
        Font itemFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(itemFont);

        for (int i = 0; i < PLAYLIST_MENU.length; i++) {
            int curItemY = itemY + (i * itemH);
            String itemText = (i == 2) ? ("3. Aleatorio: " + (engine.isShuffle() ? "Activado" : "Desactivado")) : PLAYLIST_MENU[i];
            if (i == menuIndex) {
                g.setColor(0x005b8a);
                g.fillRoundRect(menuX + 5, curItemY, menuW - 10, itemH - 2, 4, 4);
                g.setColor(0x00d2ff);
                g.drawRoundRect(menuX + 5, curItemY, menuW - 10, itemH - 2, 4, 4);

                g.setColor(0x00ffcc);
                g.drawString(">", menuX + 8, curItemY + 2, Graphics.TOP | Graphics.LEFT);
                g.setColor(0xffffff);
                g.drawString(itemText, menuX + 18, curItemY + 2, Graphics.TOP | Graphics.LEFT);
            } else {
                g.setColor(0x8faecf);
                g.drawString(itemText, menuX + 18, curItemY + 2, Graphics.TOP | Graphics.LEFT);
            }

            if (i == 2 && engine.isShuffle()) {
                g.setColor(0x00e676);
                g.drawString("[*]", menuX + menuW - 24, curItemY + 2, Graphics.TOP | Graphics.LEFT);
            }
        }

        int footerY = itemY + (PLAYLIST_MENU.length * itemH) + 2;
        g.setColor(0x132337);
        g.fillRect(menuX + 2, footerY, menuW - 4, 18);
        g.setColor(0x425872);
        g.drawLine(menuX + 2, footerY, menuX + menuW - 3, footerY);

        Font tinyFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(tinyFont);
        g.setColor(0x5c7f9f);
        g.drawString("[OK] Elegir   [C] Cancelar", menuX + (menuW / 2), footerY + 2, Graphics.TOP | Graphics.HCENTER);
    }

    private void paintSwitchPlaylist(Graphics g, int w, int h) {
        String[] playlists = engine.getPlaylistNamesArray();
        if (playlists == null || playlists.length == 0) {
            playlists = new String[] { "Principal" };
        }
        int totalPl = playlists.length;
        if (switchPlaylistIndex >= totalPl) switchPlaylistIndex = totalPl - 1;
        if (switchPlaylistIndex < 0) switchPlaylistIndex = 0;

        int boxW = 216;
        int maxVisible = 6;
        int itemH = 26;
        int listH = Math.min(totalPl, maxVisible) * itemH;
        int boxH = 30 + listH + 24;
        int boxX = (w - boxW) / 2;
        int boxY = (h - boxH) / 2;

        if (switchPlaylistIndex < switchScrollOffset) {
            switchScrollOffset = switchPlaylistIndex;
        } else if (switchPlaylistIndex >= switchScrollOffset + maxVisible) {
            switchScrollOffset = switchPlaylistIndex - maxVisible + 1;
        }
        if (switchScrollOffset < 0) switchScrollOffset = 0;

        g.setColor(0x000000);
        g.fillRect(boxX - 2, boxY - 2, boxW + 4, boxH + 4);

        g.setColor(0x0b131e);
        g.fillRect(boxX, boxY, boxW, boxH);
        g.setColor(0x00d2ff);
        g.drawRect(boxX, boxY, boxW - 1, boxH - 1);
        g.drawRect(boxX + 1, boxY + 1, boxW - 3, boxH - 3);

        int titleH = 22;
        g.setColor(0x132337);
        g.fillRect(boxX + 2, boxY + 2, boxW - 4, titleH);
        g.setColor(0x00d2ff);
        g.drawLine(boxX + 2, boxY + titleH + 2, boxX + boxW - 3, boxY + titleH + 2);

        Font boldSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        Font plainSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(boldSmall);
        g.setColor(0x00e5ff);
        g.drawString("CAMBIAR PLAYLIST", boxX + (boxW / 2), boxY + 5, Graphics.TOP | Graphics.HCENTER);

        int itemStartY = boxY + titleH + 4;
        String curActive = engine.getActivePlaylistName();

        int count = Math.min(totalPl - switchScrollOffset, maxVisible);
        for (int i = 0; i < count; i++) {
            int idx = switchScrollOffset + i;
            String name = playlists[idx];
            int curY = itemStartY + (i * itemH);
            boolean isSel = (idx == switchPlaylistIndex);
            boolean isAct = (curActive != null && curActive.equalsIgnoreCase(name));

            if (isSel) {
                g.setColor(0x005b8a);
                g.fillRoundRect(boxX + 5, curY, boxW - 10, itemH - 2, 4, 4);
                g.setColor(0x00d2ff);
                g.drawRoundRect(boxX + 5, curY, boxW - 10, itemH - 2, 4, 4);
            }

            g.setFont(isSel ? boldSmall : plainSmall);
            g.setColor(isSel ? 0xffffff : (isAct ? 0x00ff88 : 0xbed4e8));

            String displayName = (idx + 1) + ". " + name;
            int maxNameW = boxW - 70;
            if (g.getFont().stringWidth(displayName) > maxNameW) {
                while (displayName.length() > 3 && g.getFont().stringWidth(displayName + "..") > maxNameW) {
                    displayName = displayName.substring(0, displayName.length() - 1);
                }
                displayName += "..";
            }
            g.drawString(displayName, boxX + 10, curY + 5, Graphics.TOP | Graphics.LEFT);

            if (isAct) {
                g.setFont(boldSmall);
                g.setColor(0x00ff88);
                g.drawString("[ACTIVA]", boxX + boxW - 10, curY + 5, Graphics.TOP | Graphics.RIGHT);
            }
        }

        int footerY = boxY + boxH - 20;
        g.setColor(0x132337);
        g.fillRect(boxX + 2, footerY, boxW - 4, 18);
        g.setColor(0x425872);
        g.drawLine(boxX + 2, footerY, boxX + boxW - 3, footerY);

        Font tinyFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(tinyFont);
        g.setColor(0x5c7f9f);
        g.drawString("[OK] Elegir   [C] Cancelar", boxX + (boxW / 2), footerY + 2, Graphics.TOP | Graphics.HCENTER);
    }

    private void paintDeleteConfirm(Graphics g, int w, int h) {
        int boxW = 216;
        int boxH = 145;
        int boxX = (w - boxW) / 2;
        int boxY = (h - boxH) / 2;

        String curName = engine.getActivePlaylistName();
        boolean isPrincipal = (curName != null && curName.equalsIgnoreCase("Principal"));

        g.setColor(0x000000);
        g.fillRect(boxX - 2, boxY - 2, boxW + 4, boxH + 4);

        g.setColor(0x120a0d);
        g.fillRect(boxX, boxY, boxW, boxH);
        g.setColor(0xff4455);
        g.drawRect(boxX, boxY, boxW - 1, boxH - 1);
        g.drawRect(boxX + 1, boxY + 1, boxW - 3, boxH - 3);

        int titleH = 22;
        g.setColor(0x280e14);
        g.fillRect(boxX + 2, boxY + 2, boxW - 4, titleH);
        g.setColor(0xff4455);
        g.drawLine(boxX + 2, boxY + titleH + 2, boxX + boxW - 3, boxY + titleH + 2);

        Font boldSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        Font plainSmall = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        g.setFont(boldSmall);
        g.setColor(0xff6677);
        g.drawString("ELIMINAR PLAYLIST", boxX + (boxW / 2), boxY + 5, Graphics.TOP | Graphics.HCENTER);

        if (isPrincipal) {
            g.setFont(plainSmall);
            g.setColor(0xffffff);
            g.drawString("La lista 'Principal' no", boxX + (boxW / 2), boxY + 38, Graphics.TOP | Graphics.HCENTER);
            g.drawString("se puede eliminar.", boxX + (boxW / 2), boxY + 56, Graphics.TOP | Graphics.HCENTER);
            g.setColor(0xaaaaaa);
            g.drawString("(Es la lista predeterminada)", boxX + (boxW / 2), boxY + 76, Graphics.TOP | Graphics.HCENTER);
        } else {
            g.setFont(plainSmall);
            g.setColor(0xffffff);
            g.drawString("\u00BFEliminar la playlist actual?", boxX + (boxW / 2), boxY + 36, Graphics.TOP | Graphics.HCENTER);
            g.setFont(boldSmall);
            g.setColor(0x00ffff);
            String showName = "\"" + curName + "\"";
            if (boldSmall.stringWidth(showName) > boxW - 20) {
                showName = showName.substring(0, 14) + "..\"";
            }
            g.drawString(showName, boxX + (boxW / 2), boxY + 56, Graphics.TOP | Graphics.HCENTER);
            g.setFont(plainSmall);
            g.setColor(0xaaaaaa);
            g.drawString("No se borrar\u00E1n las canciones", boxX + (boxW / 2), boxY + 76, Graphics.TOP | Graphics.HCENTER);
        }

        int footerY = boxY + boxH - 22;
        g.setColor(0x280e14);
        g.fillRect(boxX + 2, footerY, boxW - 4, 20);
        g.setColor(0x551a24);
        g.drawLine(boxX + 2, footerY, boxX + boxW - 3, footerY);

        g.setFont(boldSmall);
        if (isPrincipal) {
            g.setColor(0x00d2ff);
            g.drawString("[OK / C] Aceptar", boxX + (boxW / 2), footerY + 3, Graphics.TOP | Graphics.HCENTER);
        } else {
            g.setColor(0xff5555);
            g.drawString("[OK] S\u00ED, Borrar", boxX + 10, footerY + 3, Graphics.TOP | Graphics.LEFT);
            g.setColor(0xaaaaaa);
            g.drawString("[C] Cancelar", boxX + boxW - 10, footerY + 3, Graphics.TOP | Graphics.RIGHT);
        }
    }

    private void executeMenuItem(int idx) {
        switch (idx) {
            case 0: // 1. Reproducir Pista
                isMenuOpen = false;
                repaint();
                playSelected();
                break;
            case 1: // 2. Reproducir en Aleatorio
                isMenuOpen = false;
                repaint();
                engine.setShuffle(true);
                playSelected();
                break;
            case 2: // 3. Modo Aleatorio
                engine.toggleShuffle();
                repaint();
                break;
            case 3: // 4. Cambiar Playlist
                isMenuOpen = false;
                isSwitchPlaylistOpen = true;
                switchPlaylistIndex = 0;
                switchScrollOffset = 0;
                repaint();
                break;
            case 4: // 5. Nueva Playlist
                isMenuOpen = false;
                repaint();
                midlet.promptCreatePlaylist();
                break;
            case 5: // 6. Eliminar Playlist
                isMenuOpen = false;
                isDeleteConfirmOpen = true;
                repaint();
                break;
            case 6: // 7. Eliminar de la Lista
                isMenuOpen = false;
                deleteSelected();
                break;
            case 7: // 8. Vaciar Lista Completa
                isMenuOpen = false;
                engine.clearPlaylist();
                refreshList();
                break;
            case 8: // 9. Explorar Archivos
                isMenuOpen = false;
                repaint();
                midlet.showFileBrowser();
                break;
            case 9: // 10. Minimizar
                isMenuOpen = false;
                repaint();
                midlet.minimizeApp();
                break;
            case 10: // 11. Volver al Reproductor
                isMenuOpen = false;
                repaint();
                midlet.showPlayer();
                break;
            default:
                isMenuOpen = false;
                repaint();
                break;
        }
    }

    private void playSelected() {
        if (selectedIndex >= 0 && selectedIndex < engine.getPlaylistSize()) {
            engine.playTrack(selectedIndex);
            midlet.showPlayer();
        }
    }

    private void deleteSelected() {
        if (selectedIndex >= 0 && selectedIndex < engine.getPlaylistSize()) {
            engine.removeTrack(selectedIndex);
            refreshList();
        }
    }

    protected void keyPressed(int keyCode) {
        int gameAction = 0;
        try {
            gameAction = getGameAction(keyCode);
        } catch (Throwable t) {
            gameAction = 0;
        }

        // Manejo cuando la confirmacion de eliminacion esta abierta
        if (isDeleteConfirmOpen) {
            String curName = engine.getActivePlaylistName();
            boolean isPrincipal = (curName != null && curName.equalsIgnoreCase("Principal"));

            if (gameAction == FIRE || keyCode == -5 || keyCode == -6 || keyCode == -21 || keyCode == 10 || keyCode == 32) {
                if (!isPrincipal) {
                    engine.deletePlaylist(curName);
                }
                isDeleteConfirmOpen = false;
                refreshList();
                return;
            }
            if (keyCode == -7 || keyCode == -22 || keyCode == -8 || keyCode == 8 || keyCode == 27 || keyCode == KEY_NUM0) {
                isDeleteConfirmOpen = false;
                repaint();
                return;
            }
            return;
        }

        // Manejo cuando el dialogo de cambiar playlist esta abierto
        if (isSwitchPlaylistOpen) {
            String[] playlists = engine.getPlaylistNamesArray();
            int totalPl = (playlists != null) ? playlists.length : 1;

            if (gameAction == UP || keyCode == -1 || keyCode == 38 || keyCode == KEY_NUM2) {
                if (totalPl > 0) {
                    switchPlaylistIndex = (switchPlaylistIndex - 1 + totalPl) % totalPl;
                    repaint();
                }
                return;
            }
            if (gameAction == DOWN || keyCode == -2 || keyCode == 40 || keyCode == KEY_NUM8) {
                if (totalPl > 0) {
                    switchPlaylistIndex = (switchPlaylistIndex + 1) % totalPl;
                    repaint();
                }
                return;
            }
            if (gameAction == FIRE || keyCode == -5 || keyCode == -6 || keyCode == -21 || keyCode == 10 || keyCode == 32 || keyCode == KEY_NUM5) {
                if (playlists != null && switchPlaylistIndex >= 0 && switchPlaylistIndex < playlists.length) {
                    engine.switchPlaylist(playlists[switchPlaylistIndex]);
                }
                isSwitchPlaylistOpen = false;
                refreshList();
                return;
            }
            if (keyCode == -7 || keyCode == -22 || keyCode == -8 || keyCode == 8 || keyCode == 27 || keyCode == KEY_NUM0) {
                isSwitchPlaylistOpen = false;
                repaint();
                return;
            }
            if (keyCode >= KEY_NUM1 && keyCode <= KEY_NUM9) {
                int numIdx = keyCode - KEY_NUM1;
                if (playlists != null && numIdx >= 0 && numIdx < playlists.length) {
                    switchPlaylistIndex = numIdx;
                    engine.switchPlaylist(playlists[switchPlaylistIndex]);
                    isSwitchPlaylistOpen = false;
                    refreshList();
                }
                return;
            }
            return;
        }

        // Manejo cuando el menu emergente esta abierto
        if (isMenuOpen) {
            if (gameAction == UP || keyCode == -1 || keyCode == 38 || keyCode == -11) {
                menuIndex = (menuIndex - 1 + PLAYLIST_MENU.length) % PLAYLIST_MENU.length;
                repaint();
                return;
            }
            if (gameAction == DOWN || keyCode == -2 || keyCode == 40 || keyCode == -12) {
                menuIndex = (menuIndex + 1) % PLAYLIST_MENU.length;
                repaint();
                return;
            }
            if (gameAction == FIRE || keyCode == -5 || keyCode == -6 || keyCode == -21 || keyCode == 10 || keyCode == 32) {
                executeMenuItem(menuIndex);
                return;
            }
            if (keyCode == -7 || keyCode == -22 || keyCode == -8 || keyCode == 8 || keyCode == 27 || keyCode == KEY_NUM0) {
                isMenuOpen = false;
                repaint();
                return;
            }
            if (keyCode == KEY_NUM1) { executeMenuItem(0); return; }
            if (keyCode == KEY_NUM2) { executeMenuItem(1); return; }
            if (keyCode == KEY_NUM3) { executeMenuItem(2); return; }
            if (keyCode == KEY_NUM4) { executeMenuItem(3); return; }
            if (keyCode == KEY_NUM5) { executeMenuItem(4); return; }
            if (keyCode == KEY_NUM6) { executeMenuItem(5); return; }
            if (keyCode == KEY_NUM7) { executeMenuItem(6); return; }
            if (keyCode == KEY_NUM8) { executeMenuItem(7); return; }
            if (keyCode == KEY_NUM9) { executeMenuItem(8); return; }
            if (keyCode == KEY_NUM0) { executeMenuItem(9); return; }
            if (keyCode == KEY_POUND) { executeMenuItem(10); return; }
            return;
        }

        // Manejo normal de la lista de canciones
        int total = engine.getPlaylistSize();

        // 0. Tecla '7' para alternar modo aleatorio
        if (keyCode == KEY_NUM7) {
            engine.toggleShuffle();
            repaint();
            return;
        }

        // 1. Navegacion Arriba / Abajo
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

        // 2. Reproducir pista (Joystick OK, Enter, Space, Tecla 5)
        if (gameAction == FIRE || keyCode == -5 || keyCode == 10 || keyCode == 32 || keyCode == KEY_NUM5) {
            if (total == 0) {
                midlet.showFileBrowser();
            } else {
                playSelected();
            }
            return;
        }

        // 3. Borrar cancion con Tecla 'C' de Sony Ericsson (-8), Backspace o Delete
        if (keyCode == -8 || keyCode == 8 || keyCode == 127) {
            deleteSelected();
            return;
        }

        // 4. Tecla '#' para ir a Explorar M2
        if (keyCode == KEY_POUND) {
            midlet.showFileBrowser();
            return;
        }

        // 5. Softkey Izquierdo: Abrir menu de opciones de Playlist
        if (keyCode == -6 || keyCode == -21) {
            isMenuOpen = true;
            menuIndex = 0;
            repaint();
            return;
        }

        // 6. Softkey Derecho / Tecla 0: Volver a PlayerCanvas
        if (keyCode == -7 || keyCode == -22 || keyCode == 27 || keyCode == KEY_NUM0) {
            midlet.showPlayer();
            return;
        }
    }

    protected void keyRepeated(int keyCode) {
        keyPressed(keyCode);
    }

    protected void pointerPressed(int x, int y) {
        int w = getWidth();
        int h = getHeight();

        // 1. Si el dialogo de confirmacion de eliminacion esta abierto
        if (isDeleteConfirmOpen) {
            String curName = engine.getActivePlaylistName();
            boolean isPrincipal = (curName != null && curName.equalsIgnoreCase("Principal"));
            int boxW = 216;
            int boxH = 145;
            int boxX = (w - boxW) / 2;
            int boxY = (h - boxH) / 2;
            int footerY = boxY + boxH - 22;

            if (y >= footerY && y <= boxY + boxH) {
                if (isPrincipal) {
                    isDeleteConfirmOpen = false;
                    repaint();
                    return;
                }
                if (x <= boxX + (boxW / 2)) {
                    engine.deletePlaylist(curName);
                    isDeleteConfirmOpen = false;
                    refreshList();
                    return;
                } else {
                    isDeleteConfirmOpen = false;
                    repaint();
                    return;
                }
            }
            if (x < boxX || x > boxX + boxW || y < boxY || y > boxY + boxH) {
                isDeleteConfirmOpen = false;
                repaint();
                return;
            }
            return;
        }

        // 2. Si el dialogo de cambiar playlist esta abierto
        if (isSwitchPlaylistOpen) {
            String[] playlists = engine.getPlaylistNamesArray();
            if (playlists == null || playlists.length == 0) playlists = new String[] { "Principal" };
            int totalPl = playlists.length;
            int boxW = 216;
            int maxVisible = 6;
            int itemH = 26;
            int listH = Math.min(totalPl, maxVisible) * itemH;
            int boxH = 30 + listH + 24;
            int boxX = (w - boxW) / 2;
            int boxY = (h - boxH) / 2;
            int titleH = 22;
            int itemStartY = boxY + titleH + 4;
            int footerY = boxY + boxH - 20;

            int count = Math.min(totalPl - switchScrollOffset, maxVisible);
            if (y >= itemStartY && y < itemStartY + (count * itemH)) {
                int clicked = switchScrollOffset + ((y - itemStartY) / itemH);
                if (clicked >= 0 && clicked < totalPl) {
                    if (clicked == switchPlaylistIndex) {
                        engine.switchPlaylist(playlists[switchPlaylistIndex]);
                        isSwitchPlaylistOpen = false;
                        refreshList();
                        return;
                    } else {
                        switchPlaylistIndex = clicked;
                        repaint();
                        return;
                    }
                }
            }
            if (y >= footerY && y <= boxY + boxH) {
                if (x <= boxX + (boxW / 2)) {
                    if (switchPlaylistIndex >= 0 && switchPlaylistIndex < totalPl) {
                        engine.switchPlaylist(playlists[switchPlaylistIndex]);
                    }
                    isSwitchPlaylistOpen = false;
                    refreshList();
                    return;
                } else {
                    isSwitchPlaylistOpen = false;
                    repaint();
                    return;
                }
            }
            if (x < boxX || x > boxX + boxW || y < boxY || y > boxY + boxH) {
                isSwitchPlaylistOpen = false;
                repaint();
                return;
            }
            return;
        }

        // 3. Si el menu flotante esta abierto
        if (isMenuOpen) {
            int menuW = 216;
            int menuH = 264;
            int menuX = (w - menuW) / 2;
            int menuY = (h - menuH) / 2;
            int titleH = 22;
            int itemY = menuY + titleH + 4;
            int itemH = 20;

            if (x >= menuX && x <= menuX + menuW && y >= itemY && y <= itemY + PLAYLIST_MENU.length * itemH) {
                int clicked = (y - itemY) / itemH;
                if (clicked >= 0 && clicked < PLAYLIST_MENU.length) {
                    executeMenuItem(clicked);
                    return;
                }
            }
            isMenuOpen = false;
            repaint();
            return;
        }

        // 4. Toques en softkeys inferiores
        int softkeyY = h - 22;
        if (y >= softkeyY) {
            if (x <= 75) {
                isMenuOpen = true;
                menuIndex = 0;
                repaint();
                return;
            } else if (x >= w - 75) {
                midlet.showPlayer();
                return;
            }
        }

        // 5. Toques en las filas de canciones
        int contentY = 28;
        int rowH = 30;
        int total = engine.getPlaylistSize();

        if (total == 0) {
            midlet.showFileBrowser();
            return;
        }

        if (y >= contentY && y < softkeyY) {
            int clickedRow = scrollOffset + ((y - contentY) / rowH);
            if (clickedRow >= 0 && clickedRow < total) {
                if (clickedRow == selectedIndex) {
                    playSelected();
                } else {
                    selectedIndex = clickedRow;
                    repaint();
                }
            }
        }
    }

}
