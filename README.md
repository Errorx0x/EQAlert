# Alerta Sísmica TV (Quake Alert)

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
3. Activa **"Permitir que la alerta interrumpa la pantalla"**. Si tu televisor no muestra esa pantalla:
   ```
   adb shell appops set org.alertasismica.tv SYSTEM_ALERT_WINDOW allow
   ```
4. Pulsa **"Probar la alerta en 10 segundos"** y sal de la app para ver cómo interrumpe. La prueba suena muy bajito a propósito; la alerta real usa el 85 % del volumen de alarma.

## Límites conocidos

- **Reposo profundo:** si el televisor se apaga del todo (standby real), el sistema corta la red y la app no puede despertarlo. Funciona con el televisor encendido y en el reposo ligero que mantiene el Wi-Fi.
- **Samsung (Tizen) y LG (webOS)** necesitan apps aparte; aún no existen.
- La intensidad es una **estimación sencilla** basada en magnitud, profundidad y distancia, no un mapa oficial de sacudida.

## Estado

Versión 0.1.0. Probada en el emulador de Google TV; falta probarla en televisores reales. Ver [CHANGELOG](CHANGELOG.md).

## Compilar

Requiere JDK 17 y Android SDK 35.

```
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Estructura

| Archivo | Función |
|---|---|
| `MonitorService` | Vigilancia continua, decide y lanza las alertas |
| `Fuentes` | Consultas a USGS y EMSC |
| `Evaluador` | Distancia, intensidad estimada y niveles |
| `AlertaActivity` | Pantalla roja, sirena y voz |
| `PrincipalActivity` | Ubicación, sensibilidad, permiso y prueba |
| `ArranqueReceiver` | Reanuda la vigilancia al encender |

## Licencia

Apache-2.0. Contribuciones bienvenidas, sobre todo fuentes oficiales de otros países.
