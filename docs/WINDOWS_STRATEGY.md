# Estrategia Técnica: NewPipe para Windows

Esta es la hoja de ruta para poner a andar NewPipe en Windows sin reescribir la rueda, aprovechando que ya estamos sobre Kotlin Multiplatform (KMP).

## 1. Interfaz de Usuario (UI)
Vamos a seguir usando **Compose Multiplatform (Desktop)**.
*   **¿Por qué?** Porque ya lo tenemos en el módulo `desktopApp`. Todo el código que está en `shared` (pantallas, componentes, temas) se va a renderizar nativo en Windows usando Skia debajo del capó.
*   **Ventaja:** No hay que tocar ni un componente visual si ya funciona en Android o JVM (Linux/macOS).

## 2. Reutilización del Core (Lógica)
El core se queda intacto en el módulo `shared`.
*   **Extractor y red:** La lógica de consumo (NewPipeExtractor) es puro Kotlin/Java y corre nativo en la JVM de Windows sin problema.
*   **Gestión del estado:** ViewModels, Koin (DI), Settings y Kermit (Logging) ya están configurados para compilar en JVM. Todo el puente de datos está cubierto.

## 3. El Cuello de Botella: Reproducción de Video
Aquí es donde hay que meter mano de verdad:
*   Compose Multiplatform no tiene un reproductor de video oficial robusto (todavía).
*   **Estrategia:** Necesitamos integrar una librería externa en el módulo `desktopApp`.
*   **Candidato principal:** `vlcj` (VLC for Java). Vamos a tener que crear un wrapper (usando `SwingPanel` en Compose Desktop) para embeber la ventana de reproducción nativa de VLC dentro de la UI de Compose.

## 4. Compilación y Ejecutable (Packaging)
El plugin de Compose ya nos hace el trabajo pesado.
*   El archivo `desktopApp/build.gradle.kts` ya tiene configurado el bloque `nativeDistributions` con `TargetFormat.Msi`.
*   **Build Pipeline:** Para compilar el instalador, ejecutamos el task de Gradle:
    ```bash
    ./gradlew :desktopApp:packageMsi
    ```
    Esto usa `jpackage` por detrás, agarra la JDK embebida, compila los JARs y genera el `.msi` instalable listo para distribuir (sin que el usuario final necesite instalar Java).

## Siguientes Pasos
1. Investigar y montar la prueba de concepto (PoC) de `vlcj` dentro del `desktopApp`.
2. Mapear los atajos de teclado y comportamientos específicos de escritorio (ej: full screen mode).
3. Configurar GitHub Actions para que compile automáticamente el `.msi` en runners de Windows (`windows-latest`).
