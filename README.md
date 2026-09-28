# EQ Alert

App gratuita y de código abierto para televisores Android (Google TV, Android TV, Sony, Philips, TCL, Hisense, Fire TV). Vigila sismos en segundo plano y, si uno alcanza **alerta roja cerca de tu ubicación**, interrumpe lo que estés viendo con una pantalla roja a pantalla completa, sirena y voz.

> **Aviso importante.** Esta app **no sustituye** a los sistemas oficiales de alerta ni a la protección civil. Usa datos públicos que se publican unos minutos después del sismo, así que el aviso puede llegar cuando las ondas ya llegaron. Es un complemento, no una alerta temprana de segundos.

## Cómo funciona

1. Un servicio en primer plano consulta cada 30 s dos fuentes públicas y sin clave: **USGS** y **EMSC**.
2. Calcula la distancia al sismo y estima la intensidad de sacudida (escala Mercalli) en tu ubicación.
3. Niveles: **informativo** (aviso silencioso), **naranja** (notificación), **roja** (pantalla completa + sirena + voz).
4. Nunca avisa dos veces del mismo sismo, aunque lo publiquen las dos fuentes; sí vuelve a avisar si la magnitud se revisa al alza.
5. Se reinicia sola al encender el televisor.

Región inicial: **Caribe** (República Dominicana, Puerto Rico, Haití, Cuba, Jamaica, Islas Vírgenes). El motor funciona en cualquier lugar; falta ampliar la lista de ciudades y añadir fuentes nacionales.

## Instalar y configurar

1. Instala el APK firmado (por ejemplo con Downloader o `adb install`). Huella SHA-256 del certificado: `4fdf0db7c4b407959b0d4281771a4698fe14436fe0e7e1ea18e3ce0a0405a487`.
2. Abre la app y elige la ciudad donde está el televisor (los televisores no tienen GPS).
2b. La app adivina el país por tu conexión a internet (sin GPS ni permisos) y te lleva directo a elegir la ciudad; si falla o el país no está en la lista, puedes elegirlo a mano con "Cambiar país". La pantalla de alerta muestra la recomendación oficial de protección civil de ese país (COE, PRSN, ODPEM, VITEMA, Defensa Civil o DGPC según corresponda), además de la acción universal: agáchate, cúbrete y sujétate.
3. Activa **"Permitir que la alerta interrumpa la pantalla"**. Si tu televisor no muestra esa pantalla:
   ```
   adb shell appops set org.eqalert.tv SYSTEM_ALERT_WINDOW allow
   ```
4. Pulsa **"Probar la alerta en 10 segundos"** y sal de la app para ver cómo interrumpe. La prueba suena muy bajito a propósito; la alerta real usa el 85 % del volumen de alarma.

## Límites conocidos

- **Volumen final:** la sirena sube el volumen de alarma del dispositivo, pero en un Fire TV Stick u otro receptor HDMI el volumen real lo decide el televisor. Si el televisor está en silencio o muy bajo, la sirena sonará poco; la pantalla roja sí aparece.
- **Reposo profundo:** si el televisor se apaga del todo (standby real), el sistema corta la red y la app no puede despertarlo. Funciona con el televisor encendido y en el reposo ligero que mantiene el Wi-Fi.
- **Samsung (Tizen) y LG (webOS)** necesitan apps aparte; aún no existen.
- La intensidad es una **estimación sencilla** basada en magnitud, profundidad y distancia, no un mapa oficial de sacudida.

## Estado

Versión 1.0.0. Probada en el emulador de Google TV (Android 11) y en un Fire TV Stick real
(Android 9): vigilancia, cambio de país/ciudad, alerta de prueba y cierre limpio verificados
en dispositivo. Ver [CHANGELOG](CHANGELOG.md).

## Publicar en tiendas de apps

Lo que ya está listo en este repositorio:

- ✅ `LICENSE` (Apache-2.0), [`PRIVACY.md`](PRIVACY.md) bilingüe, `CHANGELOG.md`, este README.
- ✅ APK firmado con una llave propia (no depuración), `targetSdk 35` (el mínimo que piden las
  tiendas hoy), sin errores de `lint`, con pruebas automáticas.
- ✅ Icono de la app (vector, se ve nítido en cualquier tamaño) y banner de TV (320×180dp) en
  `app/src/main/res/`.
- ✅ Materiales para la ficha de la tienda en [`store/`](store/): icono de 512×512
  (`icono-512.png`) y banner promocional de 1024×500 (`banner-promocional-1024x500.png`).
- ✅ La app funciona igual si el usuario no concede el permiso de superposición: la alerta
  también llega como notificación de pantalla completa (`USE_FULL_SCREEN_INTENT`), que no
  necesita ese permiso en Android 10 o superior.

Lo que falta y **solo lo puede hacer quien publique la app**, porque exige una cuenta de
desarrollador propia (identidad, pago o impuestos, y formularios que Google/Amazon no dejan
rellenar por API):

- **Amazon Appstore (recomendado primero):** encaja mejor porque la app nace para Fire TV.
  El registro de desarrollador es gratis. Hay que subir el APK, el icono y el banner de
  `store/`, y responder el cuestionario de privacidad usando el contenido de `PRIVACY.md`
  (una vez el repositorio esté público, la URL cruda de ese archivo en GitHub sirve como
  enlace de política de privacidad).
- **Google Play (Android TV / Google TV):** el registro cuesta 25 USD, de pago único. Aviso
  importante: el permiso "mostrar sobre otras apps" (`SYSTEM_ALERT_WINDOW`) que usa esta app
  para la alerta está entre los que Google revisa con más cuidado, y no siempre lo aprueba
  para casos como este. Como la app ya funciona sin ese permiso gracias a la notificación de
  pantalla completa, si Play lo rechaza se puede quitar esa única línea del manifiesto y volver
  a enviarla sin perder la función principal.
- El cuestionario de clasificación de contenido y el formulario de seguridad de datos
  ("Data safety") de cada tienda los debe rellenar quien tenga la cuenta; el contenido de
  `PRIVACY.md` tiene las respuestas (qué datos se usan, para qué, y que nunca se venden ni
  comparten).

## Compilar

Requiere JDK 17 y Android SDK 35.

```
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Diseño

Interfaz minimalista y ligera a propósito:

- **Sin XML ni imágenes de mapa de bits en las pantallas** (solo texto, formas planas e iconos vectoriales finos) — nada que decodificar ni mantener en memoria como bitmap.
- **El monitoreo en segundo plano no dibuja nada**: `MonitorService` no tiene ventana ni vista, así que no consume GPU mientras la app no está abierta (comprobado con `dumpsys gfxinfo`: 0 fotogramas, 0% CPU en reposo).
- **La pantalla de alerta no parpadea**: fondo rojo sólido de una sola pintada; solo un punto pequeño "en vivo" se anima, así que se redibuja un área mínima en vez de toda la pantalla. Más liviano y sin riesgo para personas fotosensibles.
- Paleta de marca ErrorX0X (negro, vino, crema) con foco de mando marcado por una franja de color a la izquierda de cada fila, sin fondos ni bordes decorativos de más.

## Estructura

| Archivo | Función |
|---|---|
| `MonitorService` | Vigilancia continua, decide y lanza las alertas |
| `Fuentes` | Consultas a USGS y EMSC |
| `Evaluador` | Distancia, intensidad estimada y niveles |
| `AlertaActivity` | Pantalla roja, sirena y voz |
| `PrincipalActivity` | Menú principal: estado, ajustes y sismos recientes |
| `UbicacionActivity` | Selección de país y ciudad, con detección por IP |
| `Paises` | Datos de países y ciudades, y su agencia de protección civil |
| `Geo` | Detección del país por la IP de la conexión |
| `Prefs` | Todo lo que se guarda en el televisor |
| `ArranqueReceiver` | Reanuda la vigilancia al encender |

## Licencia

Apache-2.0. Contribuciones bienvenidas, sobre todo fuentes oficiales de otros países.
