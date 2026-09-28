package org.alertasismica.tv;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Fuentes públicas y oficiales de sismos. Las dos publican en minutos y no piden clave. */
final class Fuentes {
    private static final long VENTANA_MS = 2 * 60 * 60 * 1000L;

    private Fuentes() {}

    private static String iso(long ms) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(ms));
    }

    /** USGS: red sísmica de EE. UU., cubre el Caribe con la Red Sísmica de Puerto Rico. */
    static List<Sismo> usgs(double lat, double lon, long ahoraMs) throws Exception {
        String url = "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson"
                + "&latitude=" + lat + "&longitude=" + lon + "&maxradiuskm=1500"
                + "&starttime=" + iso(ahoraMs - VENTANA_MS) + "&minmagnitude=3&orderby=time&limit=100";
        String cuerpo = Red.descargar(url);
        List<Sismo> r = new ArrayList<>();
        if (cuerpo.isEmpty()) return r;
        JSONArray fs = new JSONObject(cuerpo).getJSONArray("features");
        for (int i = 0; i < fs.length(); i++) {
            JSONObject f = fs.getJSONObject(i);
            JSONObject p = f.getJSONObject("properties");
            if (p.isNull("mag") || !"earthquake".equals(p.optString("type", "earthquake"))) continue;
            JSONArray c = f.getJSONObject("geometry").getJSONArray("coordinates");
            r.add(new Sismo("USGS", f.getString("id"), p.getDouble("mag"),
                    c.getDouble(1), c.getDouble(0), c.optDouble(2, 10),
                    p.getLong("time"), p.optString("place", "")));
        }
        return r;
    }

    /** EMSC: Centro Sismológico Euro-Mediterráneo, recoge boletines de redes de todo el mundo. */
    static List<Sismo> emsc(double lat, double lon, long ahoraMs) throws Exception {
        String url = "https://www.seismicportal.eu/fdsnws/event/1/query?format=json&orderby=time"
                + "&minmag=3&limit=100&start=" + iso(ahoraMs - VENTANA_MS)
                + "&latitude=" + lat + "&longitude=" + lon + "&maxradius=15";
        String cuerpo = Red.descargar(url);
        List<Sismo> r = new ArrayList<>();
        if (cuerpo.isEmpty()) return r;
        JSONArray fs = new JSONObject(cuerpo).getJSONArray("features");
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        for (int i = 0; i < fs.length(); i++) {
            JSONObject o = fs.getJSONObject(i);
            JSONObject p = o.getJSONObject("properties");
            if (p.isNull("mag")) continue;
            String t = p.getString("time");
            long ms = f.parse(t.substring(0, 19)).getTime();
            r.add(new Sismo("EMSC", o.getString("id"), p.getDouble("mag"),
                    p.getDouble("lat"), p.getDouble("lon"), p.optDouble("depth", 10),
                    ms, p.optString("flynn_region", "")));
        }
        return r;
    }
}
