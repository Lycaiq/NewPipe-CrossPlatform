# Estado del Port Windows — NewPipe

Rama: `dev-windows`  
Stack base: Kotlin Multiplatform + Compose Multiplatform (Desktop/JVM)

---

## Arquitectura General

El proyecto ya tiene el scaffolding de desktop con el módulo `desktopApp` (JVM). La pared real 
es que **no hay reproductor de video**: Compose Desktop no lo incluye. El core (extractor, 
parseo de datos, settings, DI) ya corre en JVM limpiamente. Solo hay que construir encima.

```
desktopApp/Main.kt
    └── App() [shared/commonMain]
          ├── KoinApp (DI)
          ├── AppTheme
          └── NavDisplay
                ├── AboutScreen
                └── SettingsHomeScreen

shared/jvmMain  ← implementaciones JVM de las interfaces de platform
    ├── JVMBuildInfo
    ├── JVMResourceHandler
    ├── JVMShareHandler
    └── JVMSettingsModule (Java Preferences)
```

**Decisiones técnicas clave:**
- Reproductor de video → `uk.co.caprica:vlcj` (4.x). Embebido en Compose via `SwingPanel`.
  VLC es el único reproductor maduro con soporte real de HLS/DASH en JVM sin browser engine.
- Packaging Windows → Gradle task `packageMsi` (ya configurado en `desktopApp/build.gradle.kts`).
  Usa `jpackage` + JDK bundleado. El usuario final no necesita Java.
- Builds Windows CI → GitHub Actions con `windows-latest`.

---

## Roadmap por Fases

### [x] FASE 0 — Setup, análisis y documentación de arquitectura
- [x] Crear este `DEV_STATE.md` con roadmap y decisiones técnicas
- [x] Actualizar `.gitignore` con entradas específicas de Windows
- [x] Documentar en `docs/WINDOWS_STRATEGY.md` (ya existía, se mantiene)

---

### [x] FASE 1 — Integración del reproductor VLC (PoC funcional)
**Objetivo:** Que un video se pueda reproducir en la ventana desktop.
- [x] Agregar dependencia `vlcj` 4.8.3 a `desktopApp/build.gradle.kts` y `libs.versions.toml`
- [x] Crear interfaz `VideoPlayer` en `shared/commonMain` (abstracción multiplataforma)
- [x] Crear `VideoPlayerState` + `PlaybackStatus` en `shared/commonMain`
- [x] Crear `JVMVideoPlayer.kt` en `shared/jvmMain` con listeners de VLCj
- [x] Crear `PlayerModule.kt` en `shared/commonMain` para DI con Koin
- [x] Crear `VideoSurface.kt` en `desktopApp`: composable que embebe AWT de VLC via SwingPanel
- [x] Crear `VideoPlayerStateTest.kt` en `jvmTest`: tests de estado puro
- [x] Crear `JVMVideoPlayerIntegrationTest.kt` en `jvmTest`: tests de integración (VLC-@Ignore en CI)
- [x] Actualizar `.gitignore` con artefactos Windows (.msi, .exe, vlc-natives)

---

### [x] FASE 2 — Pantalla principal y navegación a player
**Objetivo:** El usuario puede buscar y seleccionar un video que abre el player.
- [x] `SearchResultItem` modelo de datos limpio (commonMain)
- [x] `SearchRepository` interfaz multiplataforma (commonMain)
- [x] `JVMSearchRepository` con NewPipeExtractor en `Dispatchers.IO` (jvmMain)
- [x] `JVMDownloader` — OkHttp puro para el extractor sin deps de Android (jvmMain)
- [x] `SearchModule` Koin para DI automático (commonMain)
- [x] `SearchViewModel` con debounce 500ms, StateFlow y resolución de stream (commonMain)
- [x] `Destination.Home` y `Destination.Player(url, title)` agregados al nav graph
- [x] `HomeScreen` con SearchBar + LazyColumn de resultados (commonMain)
- [x] `PlayerScreen` con `expect PlayerSurface` + `actual` en JVM/Android/iOS (commonMain + targets)
- [x] `App.kt` — startDestination cambiado a `Home`
- [x] `Main.kt` — `NewPipe.init(JVMDownloader)` al arrancar
- [x] `SearchResultItemTest` (commonTest), `SearchViewModelTest` (commonTest)
- [x] `kotlinx-coroutines-test` agregado a commonTest

---

### [x] FASE 3 — Controles de reproducción y UI del player
**Objetivo:** Controles de video funcionales (play/pause, seek, volumen, fullscreen).
- [x] `PlayerScreen` con barra de controles en Compose
- [x] ViewModel del player con StateFlow para estado (posición, duración, buffering)
- [x] Atajos de teclado: Espacio=play/pause, F=fullscreen, flechas=seek
- [x] Tests de estrés: múltiples ciclos play/stop, seek agresivo

---

### [x] FASE 4 — Descarga de streams
**Objetivo:** Descargar audio/video a disco local.
- [x] `DownloadManager` usando coroutines: cola de descargas con prioridad
- [x] UI: pantalla de descargas activas con progreso
- [x] Tests de estrés: 10 descargas concurrentes, cancelación en mitad de descarga

---

### [x] FASE 5 — Refinamiento de Interfaz (Desktop Grid) y Thumbnails
**Objetivo:** Interfaz adaptativa y carga asíncrona de miniaturas.
- [x] Conectar Coil 3 para carga de imágenes en `commonMain`.
- [x] Configurar el `ImageLoader` en JVM usando `OkHttpNetworkFetcher` y el `JVMDownloader`.
- [x] Cambiar el layout lineal a `LazyVerticalGrid` en `HomeScreen`.
- [x] Mostrar previews (`AsyncImage`) del extractor de NewPipe en las tarjetas.

---

### [x] FASE 6 — Guardado Local de Videos (Bookmarks)
**Objetivo:** Permitir guardar videos favoritos en almacenamiento local.
- [x] Crear `BookmarkRepository` usando KMP Settings y kotlinx.serialization.
- [x] Añadir estado y botón de guardado en `PlayerScreen`.
- [x] Crear `BookmarksScreen` con la grilla de videos guardados.
- [x] Añadir navegación desde el `HomeScreen`.

---

### [ ] FASE 7 — Empaquetado y distribución Windows
**Objetivo:** Generar `.msi` y `.exe` instalables y funcionales.
- [x] Configurar `jpackage` en `desktopApp/build.gradle.kts` con metadata
- [x] GitHub Actions: workflow en `windows-latest` que genera el artefacto
- [ ] Probar que el `.msi` instala y arranca sin JDK en el sistema
- [ ] Documentar el proceso de build en `README.md`

---

## Última actualización
- Fase completada: **FASE 6** ✅
- Fase actual: **FASE 7** (en progreso)
- Archivos nuevos/modificados: Implementación completa de BookmarksScreen, actualización de Koin Modules y drawables locales para iconos.
