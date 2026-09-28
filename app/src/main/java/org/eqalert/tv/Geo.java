package org.eqalert.tv;

import org.json.JSONObject;

/**
 * Adivina el país por la IP de la conexión, ya que un televisor no tiene GPS. Es solo una
 * ayuda para preseleccionar el país al configurar por primera vez; el usuario siempre puede
 * cambiarlo a mano. Si el servicio falla, no pasa nada: se usa la selección manual normal.
 */
final class Geo {
    private Geo() {}

    /** Código de país ISO 3166-1 (por ejemplo "DO"), o null si no se pudo saber. */
    static String detectarPaisPorIp() {
        try {
            JSONObject r = new JSONObject(Red.descargar("https://ipwho.is/"));
            if (!r.optBoolean("success", true)) return null;
            String c = r.optString("country_code", null);
            return c == null || c.isEmpty() ? null : c;
        } catch (Exception ignorada) {
            return null;
        }
    }
}
