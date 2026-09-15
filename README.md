# NewPipe Multiplataforma

Este es el repositorio del cliente multiplataforma de NewPipe, un reproductor de medios ligero y de código abierto. Si ya conoces NewPipe para Android, esta es la evolución usando **Kotlin Multiplatform (KMP)** y **Compose Multiplatform** para llevarlo no solo a Android, sino también a iOS y Escritorio (Windows/macOS/Linux).

El objetivo es el mismo: poder ver y descargar videos de plataformas como YouTube, PeerTube, SoundCloud o Bandcamp sin necesidad de cuentas, rastreadores ni anuncios, pero ahora en más dispositivos compartiendo la misma base de código.

## ¿Qué resuelve?
Básicamente, te ahorra tener que escribir la lógica de negocio y la interfaz tres veces. Usamos KMP para compartir toda la lógica posible (reproducción, interacción con la API interna, gestión de listas, descargas) y Compose Multiplatform para que la UI sea la misma en todas partes. Todo esto apoyado en [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) para sacar la información de los servicios.

## Stack y Arquitectura

La arquitectura está pensada para reutilizar lo máximo posible sin perder la esencia nativa en cada plataforma.

### Tecnologías principales:
* **Kotlin Multiplatform (KMP):** El motor principal.
* **Compose Multiplatform:** UI compartida.
* **Koin:** Inyección de dependencias.
* **Kermit:** Logging multiplataforma.
* **Multiplatform Settings:** Guardar preferencias en todos los OS.

### Estructura de módulos:
* `shared/`: Aquí vive casi todo el proyecto. Tiene la UI en Compose, la lógica de presentación (ViewModels), los modelos de datos y la integración con el extractor.
* `app/`: El wrapper para Android. Solo tiene lo mínimo indispensable (como la Activity principal y configuraciones específicas del sistema).
* `desktopApp/`: El wrapper de escritorio (JVM). Lanza la ventana y configura el entorno.
* `iosApp/`: Proyecto en Xcode (Swift/SwiftUI). Instancia el framework compilado de Kotlin y levanta la UI de Compose dentro del ecosistema de iOS.

**Flujo de datos:**
La UI de Compose (en `shared`) dispara eventos a los ViewModels (usando Koin para inyección). Estos ViewModels llaman a la capa de datos o directamente al NewPipe Extractor para conseguir los videos. Una vez llega la info, actualizan los estados que la UI escucha de forma reactiva.

## Cómo levantarlo (Setup)

Necesitas tener instalado:
* JDK 21 (o superior).
* Android Studio (versión reciente que soporte KMP) o IntelliJ IDEA.
* Xcode (solo si vas a correr o compilar la versión de iOS en macOS).

### 1. Android
Abre el proyecto en Android Studio, selecciona la configuración `app` y dale a Run (Shift + F10). 

### 2. Desktop (JVM)
Puedes ejecutarlo directo desde la terminal con Gradle:
```bash
./gradlew :desktopApp:run
```
O configurar una ejecución en tu IDE que apunte a ese task.

### 3. iOS
Tienes que instalar las herramientas de línea de comandos de Xcode. Abre la carpeta `iosApp/` con Xcode y compila desde ahí, o usa el plugin de KMP en Android Studio/IntelliJ (necesitas macOS). La primera vez puede tardar un poco mientras Kotlin compila el framework de iOS.

## Contribuir
Cualquier PR es bienvenido. Intenta seguir el estilo de código del proyecto y asegurarte de que las cosas sigan funcionando en todas las plataformas si tocas `shared`. Si vas a meterle mano al extractor, revisa primero el repositorio de `NewPipeExtractor`.

> **Ojo:** Este proyecto está en constante movimiento. Las cosas pueden romperse de un commit a otro, así que si encuentras un bug feo, abre un issue.
