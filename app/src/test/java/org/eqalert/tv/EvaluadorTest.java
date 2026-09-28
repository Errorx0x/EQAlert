package org.eqalert.tv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EvaluadorTest {
    private static int nivel(double mag, double distKm, double profKm, int sensibilidad) {
        double mmi = Evaluador.intensidadEstimada(mag, distKm, profKm);
        return Evaluador.nivel(mmi, mag, sensibilidad);
    }

    @Test
    public void sismoFuerteCercanoEsAlertaRoja() {
        assertEquals(Evaluador.ROJA, nivel(6.5, 40, 10, 1));
        assertEquals(Evaluador.ROJA, nivel(7.0, 20, 15, 1));
    }

    @Test
    public void sismoPequenoNoInterrumpe() {
        assertEquals(Evaluador.NINGUNA, nivel(3.4, 19, 10, 1));
        assertTrue(nivel(4.8, 60, 10, 1) < Evaluador.ROJA);
    }

    @Test
    public void sismoGrandeLejanoNoEsRoja() {
        assertTrue(nivel(6.0, 400, 10, 1) < Evaluador.ROJA);
        assertTrue(nivel(7.5, 900, 20, 1) < Evaluador.ROJA);
    }

    @Test
    public void laSensibilidadAltaAvisaAntes() {
        assertTrue(nivel(6.0, 45, 10, 2) >= nivel(6.0, 45, 10, 1));
        assertTrue(nivel(6.0, 45, 10, 1) >= nivel(6.0, 45, 10, 0));
    }

    @Test
    public void laIntensidadBajaConLaDistanciaYSubeConLaMagnitud() {
        assertTrue(Evaluador.intensidadEstimada(6, 20, 10) > Evaluador.intensidadEstimada(6, 100, 10));
        assertTrue(Evaluador.intensidadEstimada(7, 50, 10) > Evaluador.intensidadEstimada(6, 50, 10));
    }

    @Test
    public void distanciaSantoDomingoASantiagoEsRazonable() {
        double d = Evaluador.distanciaKm(18.4861, -69.9312, 19.4517, -70.6970);
        assertTrue(d > 120 && d < 160);
    }

    @Test
    public void ondaSLlegaDespuesSiElAvisoEsRapido() {
        assertTrue(Evaluador.segundosHastaOndaS(70, 10, 0, 5000) > 10);
        assertTrue(Evaluador.segundosHastaOndaS(70, 10, 0, 120000) < 0);
    }
}
