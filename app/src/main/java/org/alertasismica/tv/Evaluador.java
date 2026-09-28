package org.alertasismica.tv;

/**
 * Estima qué tan fuerte se sentiría un sismo en la ubicación del televisor.
 *
 * La intensidad es una estimación sencilla (escala Mercalli modificada) a partir de la
 * magnitud y la distancia; no reemplaza a un mapa de sacudida oficial. Se calibró con
 * casos típicos: M7 a 20 km ≈ VIII-IX, M6 a 20 km ≈ VII, M5 a 20 km ≈ V, M5 a 100 km ≈ III.
 */
final class Evaluador {
    static final int NINGUNA = 0;
    static final int INFORMATIVA = 1;
    static final int NARANJA = 2;
    static final int ROJA = 3;

    /** Sensibilidades: 0 baja, 1 normal, 2 alta. Umbral de alerta roja en intensidad. */
    static final double[] UMBRAL_ROJO = {7.0, 6.0, 5.0};

    private Evaluador() {}

    static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double f1 = Math.toRadians(lat1), f2 = Math.toRadians(lat2);
        double df = f2 - f1;
        double dl = Math.toRadians(lon2 - lon1);
        double a = Math.sin(df / 2) * Math.sin(df / 2)
                + Math.cos(f1) * Math.cos(f2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * r * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    static double intensidadEstimada(double magnitud, double distanciaKm, double profundidadKm) {
        double prof = Math.max(0, profundidadKm);
        double r = Math.max(10, Math.sqrt(distanciaKm * distanciaKm + prof * prof));
        double mmi = 1.98 + 1.6 * magnitud - 3.6 * Math.log10(r);
        return Math.max(1, Math.min(10, mmi));
    }

    static int nivel(double intensidad, double magnitud, int sensibilidad) {
        int s = Math.max(0, Math.min(2, sensibilidad));
        double rojo = UMBRAL_ROJO[s];
        if (intensidad >= rojo && magnitud >= 4.5) return ROJA;
        if (intensidad >= rojo - 1 && magnitud >= 4.0) return NARANJA;
        if (intensidad >= rojo - 2) return INFORMATIVA;
        return NINGUNA;
    }

    /** Segundos que faltan para que llegue la onda S (la que más se siente); negativo si ya llegó. */
    static double segundosHastaOndaS(double distanciaKm, double profundidadKm, long tiempoSismoMs, long ahoraMs) {
        double r = Math.sqrt(distanciaKm * distanciaKm + Math.max(0, profundidadKm) * Math.max(0, profundidadKm));
        double viaje = r / 3.5;
        return viaje - (ahoraMs - tiempoSismoMs) / 1000.0;
    }

    static String romano(double intensidad) {
        String[] r = {"I", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return r[(int) Math.max(1, Math.min(10, Math.round(intensidad)))];
    }
}
