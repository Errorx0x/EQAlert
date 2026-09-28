package org.eqalert.tv;

/** Un sismo publicado por una fuente oficial. */
final class Sismo {
    final String fuente;
    final String id;
    final double magnitud;
    final double lat;
    final double lon;
    final double profundidadKm;
    final long tiempoMs;
    final String lugar;

    Sismo(String fuente, String id, double magnitud, double lat, double lon,
          double profundidadKm, long tiempoMs, String lugar) {
        this.fuente = fuente;
        this.id = id;
        this.magnitud = magnitud;
        this.lat = lat;
        this.lon = lon;
        this.profundidadKm = profundidadKm;
        this.tiempoMs = tiempoMs;
        this.lugar = lugar;
    }
}
