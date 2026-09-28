package org.alertasismica.tv;

/** Ciudades del Caribe para elegir la ubicación del televisor (los televisores no tienen GPS). */
final class Lugares {
    static final class Lugar {
        final String nombre;
        final double lat;
        final double lon;

        Lugar(String nombre, double lat, double lon) {
            this.nombre = nombre;
            this.lat = lat;
            this.lon = lon;
        }
    }

    static final Lugar[] TODOS = {
        new Lugar("República Dominicana · Santo Domingo", 18.4861, -69.9312),
        new Lugar("República Dominicana · Santiago de los Caballeros", 19.4517, -70.6970),
        new Lugar("República Dominicana · Puerto Plata", 19.7934, -70.6884),
        new Lugar("República Dominicana · Sosúa", 19.7500, -70.5167),
        new Lugar("República Dominicana · Samaná", 19.2058, -69.3363),
        new Lugar("República Dominicana · Nagua", 19.3833, -69.8500),
        new Lugar("República Dominicana · San Francisco de Macorís", 19.3009, -70.2526),
        new Lugar("República Dominicana · La Vega", 19.2209, -70.5289),
        new Lugar("República Dominicana · Jarabacoa", 19.1167, -70.6333),
        new Lugar("República Dominicana · Bonao", 18.9420, -70.4092),
        new Lugar("República Dominicana · Mao", 19.5519, -71.0783),
        new Lugar("República Dominicana · Montecristi", 19.8489, -71.6461),
        new Lugar("República Dominicana · Dajabón", 19.5499, -71.7086),
        new Lugar("República Dominicana · Higüey / Punta Cana", 18.6150, -68.7080),
        new Lugar("República Dominicana · La Romana", 18.4273, -68.9728),
        new Lugar("República Dominicana · San Pedro de Macorís", 18.4539, -69.3086),
        new Lugar("República Dominicana · Baní", 18.2795, -70.3319),
        new Lugar("República Dominicana · Azua", 18.4530, -70.7344),
        new Lugar("República Dominicana · San Juan de la Maguana", 18.8060, -71.2299),
        new Lugar("República Dominicana · Barahona", 18.2085, -71.1008),
        new Lugar("República Dominicana · Jimaní", 18.4917, -71.8500),
        new Lugar("República Dominicana · Pedernales", 18.0384, -71.7443),
        new Lugar("Puerto Rico · San Juan", 18.4655, -66.1057),
        new Lugar("Puerto Rico · Arecibo", 18.4725, -66.7157),
        new Lugar("Puerto Rico · Aguadilla", 18.4275, -67.1541),
        new Lugar("Puerto Rico · Mayagüez", 18.2013, -67.1452),
        new Lugar("Puerto Rico · Guánica", 17.9719, -66.9080),
        new Lugar("Puerto Rico · Ponce", 18.0111, -66.6141),
        new Lugar("Puerto Rico · Fajardo", 18.3258, -65.6524),
        new Lugar("Islas Vírgenes de EE. UU. · Charlotte Amalie", 18.3419, -64.9307),
        new Lugar("Haití · Puerto Príncipe", 18.5944, -72.3074),
        new Lugar("Haití · Cabo Haitiano", 19.7578, -72.2044),
        new Lugar("Haití · Jacmel", 18.2342, -72.5347),
        new Lugar("Haití · Les Cayes", 18.2000, -73.7500),
        new Lugar("Cuba · La Habana", 23.1136, -82.3666),
        new Lugar("Cuba · Santiago de Cuba", 20.0247, -75.8219),
        new Lugar("Cuba · Guantánamo", 20.1444, -75.2092),
        new Lugar("Jamaica · Kingston", 17.9970, -76.7936),
    };

    private Lugares() {}
}
