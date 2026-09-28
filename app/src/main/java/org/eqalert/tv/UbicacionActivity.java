package org.eqalert.tv;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Selección de ubicación integrada a la app (sin el cuadro nativo del sistema): primero el
 * país, con el de la conexión actual ya elegido cuando se puede adivinar por la IP, y luego
 * la ciudad dentro de ese país, en una lista alfabética fácil de recorrer con el mando.
 */
public class UbicacionActivity extends Activity {
    private static final long TIEMPO_MAX_DETECCION_MS = 4000;

    private final Handler h = new Handler(Looper.getMainLooper());
    private ScrollView scroll;
    private LinearLayout col;
    private boolean resuelto;
    private boolean enPasoCiudad;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        scroll = new ScrollView(this);
        scroll.setBackgroundColor(color(R.color.bg));
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(24), dp(24), dp(24), dp(24));
        scroll.addView(col);
        setContentView(scroll);

        String actual = Prefs.paisCodigo(this);
        Paises.Pais pais = Paises.porCodigo(actual);
        if (pais != null) {
            mostrarCiudades(pais);
        } else {
            mostrarDetectando();
            detectarPaisEnSegundoPlano();
        }
    }

    private void detectarPaisEnSegundoPlano() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String codigo = Geo.detectarPaisPorIp();
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        if (resuelto || isFinishing()) return;
                        resuelto = true;
                        Paises.Pais p = Paises.porCodigo(codigo);
                        if (p != null) mostrarCiudades(p);
                        else mostrarPaises();
                    }
                });
            }
        }).start();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (resuelto || isFinishing()) return;
                resuelto = true;
                mostrarPaises();
            }
        }, TIEMPO_MAX_DETECCION_MS);
    }

    private void mostrarDetectando() {
        col.removeAllViews();
        TextView t = new TextView(this);
        t.setText(R.string.detectando_pais);
        t.setTextColor(color(R.color.texto_tenue));
        t.setTextSize(18);
        col.addView(t);
    }

    private void mostrarPaises() {
        enPasoCiudad = false;
        col.removeAllViews();
        titulo(R.string.elegir_pais_titulo);
        for (final Paises.Pais p : Paises.TODOS) {
            fila(p.nombre, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mostrarCiudades(p);
                }
            });
        }
    }

    private void mostrarCiudades(final Paises.Pais pais) {
        enPasoCiudad = true;
        col.removeAllViews();
        titulo(getString(R.string.elegir_ciudad_titulo, pais.nombre));
        for (final Paises.Ciudad ciudad : pais.ciudades) {
            fila(ciudad.nombre, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Prefs.guardarUbicacion(UbicacionActivity.this, pais, ciudad);
                    finish();
                }
            });
        }
        View cambiarPais = fila(getString(R.string.cambiar_pais), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarPaises();
            }
        });
        ((TextView) ((LinearLayout) cambiarPais).getChildAt(0)).setTextColor(color(R.color.texto_tenue));
    }

    private void titulo(int textoId) {
        titulo(getString(textoId));
    }

    private void titulo(String texto) {
        TextView t = new TextView(this);
        t.setText(texto);
        t.setTextColor(color(R.color.texto));
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextSize(24);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(16);
        col.addView(t, lp);
    }

    private View fila(String texto, View.OnClickListener clic) {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.HORIZONTAL);
        f.setGravity(Gravity.CENTER_VERTICAL);
        f.setFocusable(true);
        f.setBackgroundResource(R.drawable.fila_fondo);
        f.setPadding(dp(14), dp(16), dp(14), dp(16));
        TextView t = new TextView(this);
        t.setText(texto);
        t.setTextColor(color(R.color.texto));
        t.setTextSize(18);
        f.addView(t);
        f.setOnClickListener(clic);
        col.addView(f, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        View div = new View(this);
        div.setBackgroundColor(color(R.color.divisor));
        col.addView(div, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        return f;
    }

    @Override
    public void onBackPressed() {
        if (enPasoCiudad) {
            mostrarPaises();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private int color(int id) {
        return getResources().getColor(id);
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}
