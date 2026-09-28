# Cambios

## 1.0.0 · 2026-09-28
Primera versión pensada para publicar.
- Selección de ubicación propia (país → ciudad), con detección automática de país por IP.
- Pantalla de alerta rediseñada: menos texto, la acción a seguir en letra más grande, y la
  recomendación oficial de protección civil del país elegido.
- Corrección de un fallo real: la sirena podía quedar sonando de fondo; ahora se corta al
  instante al cerrar la alerta, y la prueba se apaga sola a los 30 s si no se toca nada.
- Menú principal e interfaz rediseñados en minimalista, con la paleta de ErrorX0X.
- Política de privacidad (`PRIVACY.md`) y materiales listos para publicar en tiendas de apps
  (`store/`): icono, banner promocional y capturas reales del dispositivo.

> El proyecto se llamó "Alerta Sísmica TV" durante el desarrollo inicial; el nombre público es **EQ Alert** desde el primer lanzamiento.

## 0.1.0 · 2026-09-28
Primera versión.
- Vigilancia en segundo plano con USGS y EMSC cada 30 s.
- Alerta roja a pantalla completa con sirena y voz; aviso por notificación en niveles naranja e informativo.
- Región inicial: Caribe (37 ciudades). Interfaz en español e inglés.
- Prueba de alerta en 10 segundos, casi en silencio.
- Arranque automático al encender el televisor.
- Verificada en el emulador de Google TV (Android 11) y en un Fire TV Stick (Android 9): la alerta aparece a pantalla completa sobre otra app a los 10 s y se cierra con OK. La sirena real no se probó en el televisor por ser de noche.
