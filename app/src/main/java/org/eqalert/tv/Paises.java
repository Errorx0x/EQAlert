package org.eqalert.tv;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

/**
 * Países y ciudades del Caribe con sus ciudades ordenadas alfabéticamente, para que sean
 * fáciles de encontrar. El código de país es el estándar ISO 3166-1 (el mismo que usa la
 * detección automática por IP), y "agencia" es el organismo de protección civil de ese país
 * cuya recomendación se muestra en la pantalla de alerta.
 */
final class Paises {
    static final class Ciudad {
        final String nombre;
        final double lat;
        final double lon;

        Ciudad(String nombre, double lat, double lon) {
            this.nombre = nombre;
            this.lat = lat;
            this.lon = lon;
        }
    }

    static final class Pais {
        final String codigo;
        final String nombre;
        final int agenciaId;
        final Ciudad[] ciudades;

        Pais(String codigo, String nombre, int agenciaId, Ciudad[] ciudades) {
            this.codigo = codigo;
            this.nombre = nombre;
            this.agenciaId = agenciaId;
            this.ciudades = ordenadas(ciudades);
        }

        private static Ciudad[] ordenadas(Ciudad[] c) {
            Ciudad[] copia = Arrays.copyOf(c, c.length);
            final Collator col = Collator.getInstance(new Locale("es"));
            Arrays.sort(copia, new Comparator<Ciudad>() {
                @Override
                public int compare(Ciudad a, Ciudad b) {
                    return col.compare(a.nombre, b.nombre);
                }
            });
            return copia;
        }
    }

    static final Pais[] TODOS = {
        new Pais("DO", "República Dominicana", R.string.agencia_do, new Ciudad[]{
            new Ciudad("Azua", 18.4530, -70.7344),
            new Ciudad("Baní", 18.2795, -70.3319),
            new Ciudad("Barahona", 18.2085, -71.1008),
            new Ciudad("Bonao", 18.9420, -70.4092),
            new Ciudad("Dajabón", 19.5499, -71.7086),
            new Ciudad("Higüey / Punta Cana", 18.6150, -68.7080),
            new Ciudad("Jarabacoa", 19.1167, -70.6333),
            new Ciudad("Jimaní", 18.4917, -71.8500),
            new Ciudad("La Romana", 18.4273, -68.9728),
            new Ciudad("La Vega", 19.2209, -70.5289),
            new Ciudad("Mao", 19.5519, -71.0783),
            new Ciudad("Montecristi", 19.8489, -71.6461),
            new Ciudad("Nagua", 19.3833, -69.8500),
            new Ciudad("Pedernales", 18.0384, -71.7443),
            new Ciudad("Puerto Plata", 19.7934, -70.6884),
            new Ciudad("Samaná", 19.2058, -69.3363),
            new Ciudad("San Francisco de Macorís", 19.3009, -70.2526),
            new Ciudad("San Juan de la Maguana", 18.8060, -71.2299),
            new Ciudad("San Pedro de Macorís", 18.4539, -69.3086),
            new Ciudad("Santiago de los Caballeros", 19.4517, -70.6970),
            new Ciudad("Santo Domingo", 18.4861, -69.9312),
            new Ciudad("Sosúa", 19.7500, -70.5167),
        }),
        new Pais("PR", "Puerto Rico", R.string.agencia_pr, new Ciudad[]{
            new Ciudad("Aguadilla", 18.4275, -67.1541),
            new Ciudad("Arecibo", 18.4725, -66.7157),
            new Ciudad("Fajardo", 18.3258, -65.6524),
            new Ciudad("Guánica", 17.9719, -66.9080),
            new Ciudad("Mayagüez", 18.2013, -67.1452),
            new Ciudad("Ponce", 18.0111, -66.6141),
            new Ciudad("San Juan", 18.4655, -66.1057),
        }),
        new Pais("HT", "Haití", R.string.agencia_ht, new Ciudad[]{
            new Ciudad("Cabo Haitiano", 19.7578, -72.2044),
            new Ciudad("Jacmel", 18.2342, -72.5347),
            new Ciudad("Les Cayes", 18.2000, -73.7500),
            new Ciudad("Puerto Príncipe", 18.5944, -72.3074),
        }),
        new Pais("CU", "Cuba", R.string.agencia_cu, new Ciudad[]{
            new Ciudad("Guantánamo", 20.1444, -75.2092),
            new Ciudad("La Habana", 23.1136, -82.3666),
            new Ciudad("Santiago de Cuba", 20.0247, -75.8219),
        }),
        new Pais("JM", "Jamaica", R.string.agencia_jm, new Ciudad[]{
            new Ciudad("Kingston", 17.9970, -76.7936),
        }),
        new Pais("VI", "Islas Vírgenes de EE. UU.", R.string.agencia_vi, new Ciudad[]{
            new Ciudad("Charlotte Amalie", 18.3419, -64.9307),
        }),
    };

    static Pais porCodigo(String codigo) {
        if (codigo == null) return null;
        for (Pais p : TODOS) if (p.codigo.equalsIgnoreCase(codigo)) return p;
        return null;
    }

    private Paises() {}
}
