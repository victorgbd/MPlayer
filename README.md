# 🎵 MPlayer - Java ME Audio Player

<p align="center">
  <img src="src/icons/music_note.png" alt="MPlayer Logo" width="64" height="64" />
</p>

<p align="center">
  <strong>Reproductor multimedia avanzado en pantalla completa para Sony Ericsson K800i y dispositivos J2ME (MIDP 2.0 / CLDC 1.1)</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-J2ME%20%2F%20Java%20ME-orange.svg" alt="Platform" />
  <img src="https://img.shields.io/badge/Profile-MIDP--2.0%20%2F%20CLDC--1.1-blue.svg" alt="Profile" />
  <img src="https://img.shields.io/badge/Resolution-240x320%20QVGA-green.svg" alt="Resolution" />
  <img src="https://img.shields.io/badge/Target-Sony%20Ericsson%20K800i-darkblue.svg" alt="Target" />
  <img src="https://img.shields.io/badge/Audio-MMAPI%20(JSR--135)-red.svg" alt="MMAPI" />
  <img src="https://img.shields.io/badge/Storage-JSR--75%20(FileConnection)-yellow.svg" alt="JSR-75" />
</p>

---

## 📖 Descripción General

**MPlayer** es un reproductor de audio de alto rendimiento diseñado desde cero para teléfonos móviles clásicos, optimizado específicamente para el mítico **Sony Ericsson K800i** (y dispositivos compatibles con pantalla QVGA 240x320). 

A diferencia de las aplicaciones J2ME convencionales que utilizan formularios estándar de `LCDUI`, **MPlayer** está programado al 100% sobre un **Canvas gráfico personalizado en modo pantalla completa**., con efectos visuales animados, navegación ágil por carpetas de la memoria y persistencia total de datos mediante RMS.

---

## ✨ Características Principales

### 🎧 Motor de Audio Potente (MMAPI / JSR-135)
* **Compatibilidad multiformato:** Reproducción de archivos MP3, AAC, M4A, WMA, WAV, MIDI polifónico (hasta 72 voces), AMR, 3GP, XMF, IMY y MMF.
* **Control de reproducción continuo:** Reproducir, pausar, detener, rebobinar (<< 5 seg) y avanzar (>> 5 seg).
* **Control de volumen fino:** Ajuste de nivel 0 a 100 con función de silencio (*Mute*) y recuperación de volumen previo.
* **Modos de reproducción:**
  * **Modo Aleatorio Inteligente (*Shuffle*):** Algoritmo con seguimiento de historial que evita repetir canciones ya sonadas hasta agotar la lista.
  * **Modos de Repetición:** *Repetir Todo*, *Repetir Una Pista* o *Sin Repetición*.
* **Reproducción en Segundo Plano:** Opción de minimizar la aplicación (`Display.setCurrent(null)`) permitiendo que la música continúe mientras se utilizan otras funciones del teléfono.

### 📁 Explorador de Archivos Integrado (JSR-75 FileConnection)
* Exploración en tiempo real del sistema de archivos: **Tarjeta Memory Stick Micro M2** (`/e:/`) y **Memoria Interna** (`/c:/`).
* **Selección múltiple por lotes:** Marca o desmarca pistas individuales (tecla `*` o `1`, o toque táctil) y selecciona todas con `#`.
* **Añadido de carpetas completas:** Agrega todos los archivos de audio de una carpeta de un solo toque.
* **Detección inteligente de duplicados:** Aviso contextual cuando una pista ya existe en la lista de reproducción, con opciones para omitir o duplicar.

### 📋 Gestión de Múltiples Playlists (Persistencia RMS)
* **Varias listas de reproducción:** Crea, renombra, alterna y elimina playlists personalizadas (*Favoritos*, *Gym*, *Chill*, etc.).
* **Lista Principal protegida:** Siempre disponible como base del reproductor.
* **Persistencia total:** Guarda automáticamente en la memoria del teléfono (Record Management System) la lista activa, la pista en reproducción, la posición del cursor, y los estados de *Shuffle* y *Repeat*.

### 🌌 Visualizadores Dinámicos & Salvapantallas
Efectos visuales renderizados en tiempo real

---

## 🕹️ Controles y Atajos de Teclado

Diseñado tanto para teclados numéricos físicos y joystick de 5 direcciones como para pantallas táctiles (emuladores y dispositivos touch):

### Joystick de 5 Direcciones
| Control | Acción |
| :--- | :--- |
| **Centro (OK / Joystick Press)** | Reproducir / Pausar / Aceptar en menús |
| **Arriba (Up)** | Subir volumen (+) |
| **Abajo (Down)** | Bajar volumen (-) |
| **Izquierda (Left)** | Rebobinar 5 segundos (<<) |
| **Derecha (Right)** | Avanzar 5 segundos (>>) |

### Teclado Numérico Físico
| Tecla | Función |
| :---: | :--- |
| **`5`** | Reproducir / Pausar (Play / Pause) |
| **`0`** | Detener (Stop) |
| **`1`** | Pista anterior |
| **`3`** | Pista siguiente |
| **`4`** | Rebobinar 5 segundos |
| **`6`** | Avanzar 5 segundos |
| **`2`** | Subir volumen |
| **`8`** | Bajar volumen |
| **`7`** | Alternar Modo Aleatorio (*Shuffle*) |
| **`9`** | Cambiar Modo de Repetición (*All / One / Off*) |
| **`*`** | Abrir Lista de Reproducción (*Playlist*) / Marcar en explorador |
| **`#`** | Abrir Explorador de Archivos / Seleccionar todo |

### Botones Especiales Sony Ericsson & Teléfonos
* **Teclas Laterales (+ / -):** Control de volumen físico nativo (Keycodes `-11` y `-12`).
* **Tecla 'C' (Clear):** Eliminar pista seleccionada de la lista / Volver atrás (Keycode `-8`).
* **Softkey Izquierdo:** Abrir Menú emergente de opciones.
* **Softkey Derecho:** Volver atrás / Salir.

---

## 📱 Dispositivos Compatibles

* **Dispositivo Objetivo:** Sony Ericsson K800i / K810i / K790i (Plataforma DB2020).
* **Compatibilidad Sony Ericsson:** W850i, W880i, K850i, C902, C905, W995 y cualquier modelo con pantalla de resolución 240x320 píxeles.
* **Otros teléfonos J2ME:** Dispositivos Nokia S40 3rd/5th/6th Ed, Nokia S60v3, Motorola, Samsung y LG con soporte MIDP 2.0 y JSR-75.
* **Emuladores recomendados:**
  * [J2ME Loader](https://github.com/nikita36078/J2ME-Loader) (Android)
  * [FreeJ2ME](https://github.com/HEXColorCode/FreeJ2ME) (Multiplataforma)
  * [KEmulator nnmod](https://github.com/lenikita/KEmulator-nnmod) (PC Windows)

---

## 📂 Estructura del Proyecto

```text
MPlayer/
├── dist/
│   ├── MPlayer.jar           # Paquete ejecutable compilado
│   └── MPlayer.jad           # Descriptor de la aplicación J2ME
├── nbproject/                # Configuración de compilación NetBeans / Ant
├── src/
│   ├── icons/                # Iconografía PNG transparente (24x24 / 16x16)
│   └── mplayer/
│       ├── AudioEngine.java          # Motor MMAPI, gestión de estados y volumen
│       ├── AudioEngineListener.java  # Interfaz de eventos de audio
│       ├── FileBrowserCanvas.java    # Explorador JSR-75 en Canvas nativo
│       ├── HelloMIDlet.java          # Ciclo de vida principal del MIDlet
│       ├── HelpCanvas.java           # Pantalla de ayuda y guía de atajos
│       ├── IconStore.java            # Gestor de caché de imágenes y sprites
│       ├── PlayerCanvas.java         # Interfaz del reproductor y visualizadores
│       ├── PlaylistCanvas.java       # Vista de lista de reproducción y menús
│       ├── PlaylistStorage.java      # Persistencia RMS multi-playlist
│       └── Track.java                # Modelo de datos para pistas de audio
├── build.xml                 # Script de compilación Apache Ant
└── README.md                 # Documentación del proyecto
```

---

## 🛠️ Compilación e Instalación

### Requisitos
* **Java Development Kit (JDK):** JDK 8 o superior (con compatibilidad de bytecode para Java 1.3 / 1.4).
* **Java ME SDK / WTK:** Sun Java Wireless Toolkit 2.5.2_01 o Oracle Java ME SDK 3.2.
* **IDE:** NetBeans IDE 8.2 (con soporte Mobility Pack) o cualquier versión moderna con Apache Ant.

### Compilar con Apache Ant
Ejecuta el siguiente comando en la raíz del proyecto:
```bash
ant jar
```
Los binarios generados se encontrarán en la carpeta `dist/`:
* `dist/MPlayer.jar`
* `dist/MPlayer.jad`

### Instalación en un Teléfono Real
1. Conecta tu teléfono mediante cable USB (modo Transferencia de Archivos) o mediante Bluetooth.
2. Copia los archivos `MPlayer.jar` y `MPlayer.jad` a la carpeta `Other` o `Media` del teléfono / Tarjeta M2.
3. En el teléfono, dirígete al **Gestor de Archivos**, selecciona `MPlayer.jar` y pulsa **Instalar**.
4. ¡Listo! MPlayer aparecerá en el menú de **Juegos / Aplicaciones**.

---

## 📜 Licencia y Autor

Desarrollado por **Victor** ([@victorgbd](https://github.com/victorgbd)).  
Creado con pasión por la preservación del software retro y la era dorada de los teléfonos móviles multimedia.
