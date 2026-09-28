package org.eqalert.tv;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

final class Prefs {
    private static final long OLVIDAR_MS = 24 * 60 * 60 * 1000L;
    private static final long VENTANA_DUPLICADO_MS = 120 * 1000L;
    private static final double RADIO_DUPLICADO_KM = 200;

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("alerta", Context.MODE_PRIVATE);
    }

    static boolean hayUbicacion(Context c) {
        return sp(c).contains("lat");
    }

    static double lat(Context c) {
        return Double.parseDouble(sp(c).getString("lat", "0"));
    }

    static double lon(Context c) {
        return Double.parseDouble(sp(c).getString("lon", "0"));
    }

    static String nombreLugar(Context c) {
        return sp(c).getString("lugar", "");
    }

    /** Código ISO del país elegido (por ejemplo "DO"), o null si aún no se ha elegido ninguno. */
    static String paisCodigo(Context c) {
        return sp(c).getString("pais", null);
    }

    static void guardarUbicacion(Context c, Paises.Pais p, Paises.Ciudad ciudad) {
        sp(c).edit()
                .putString("lat", String.valueOf(ciudad.lat))
                .putString("lon", String.valueOf(ciudad.lon))
                .putString("lugar", p.nombre + " · " + ciudad.nombre)
                .putString("pais", p.codigo)
                .apply();
    }

    /** 0 baja, 1 normal, 2 alta. */
    static int sensibilidad(Context c) {
        return sp(c).getInt("sensibilidad", 1);
    }

    static void guardarSensibilidad(Context c, int v) {
        sp(c).edit().putInt("sensibilidad", v).apply();
    }

    static void guardarEstado(Context c, long ahoraMs, boolean usgs, boolean emsc) {
        sp(c).edit().putLong("ultima", ahoraMs).putBoolean("usgs", usgs).putBoolean("emsc", emsc).apply();
    }

    static long ultimaConsulta(Context c) {
        return sp(c).getLong("ultima", 0);
    }

    static boolean usgsEnLinea(Context c) {
        return sp(c).getBoolean("usgs", false);
    }

    static boolean emscEnLinea(Context c) {
        return sp(c).getBoolean("emsc", false);
    }

    static void guardarRecientes(Context c, String json) {
        sp(c).edit().putString("recientes", json).apply();
    }

    static String recientes(Context c) {
        return sp(c).getString("recientes", "[]");
    }

    /** ¿Ya se avisó de este sismo (o de uno igual publicado por otra fuente) con este nivel o mayor? */
    static synchronized boolean yaAlertado(Context c, Sismo s, int nivel) {
        try {
            JSONArray a = new JSONArray(sp(c).getString("alertados", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                if (coincide(o, s) && o.getInt("n") >= nivel) return true;
            }
        } catch (Exception ignorada) {
        }
        return false;
    }

    static synchronized void registrarAlerta(Context c, Sismo s, int nivel, long ahoraMs) {
        try {
            JSONArray viejo = new JSONArray(sp(c).getString("alertados", "[]"));
            JSONArray nuevo = new JSONArray();
            boolean actualizado = false;
            for (int i = 0; i < viejo.length(); i++) {
                JSONObject o = viejo.getJSONObject(i);
                if (ahoraMs - o.getLong("t") > OLVIDAR_MS) continue;
                if (coincide(o, s)) {
                    o.put("n", Math.max(o.getInt("n"), nivel));
                    actualizado = true;
                }
                nuevo.put(o);
            }
            if (!actualizado) {
                nuevo.put(new JSONObject().put("t", s.tiempoMs).put("la", s.lat).put("lo", s.lon).put("n", nivel));
            }
            sp(c).edit().putString("alertados", nuevo.toString()).apply();
        } catch (Exception ignorada) {
        }
    }

    private static boolean coincide(JSONObject o, Sismo s) throws Exception {
        return Math.abs(o.getLong("t") - s.tiempoMs) < VENTANA_DUPLICADO_MS
                && Evaluador.distanciaKm(o.getDouble("la"), o.getDouble("lo"), s.lat, s.lon) < RADIO_DUPLICADO_KM;
    }
}
