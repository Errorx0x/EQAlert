package org.eqalert.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

/**
 * Menú minimalista: ubicación, sensibilidad, permiso de pantalla, prueba y sismos recientes.
 * Solo vistas simples de Android (sin imágenes de mapa de bits ni animaciones) para que la
 * pantalla sea ligera. El monitoreo en segundo plano no dibuja nada, así que no usa GPU.
 */
public class PrincipalActivity extends Activity {
    private static final int ESPACIADO = dp(24);

    private final Handler h = new Handler(Looper.getMainLooper());
    private TextView estado, lugar;
    private LinearLayout filaLugar, filaSensibilidad, filaPermiso, filaProbar;
    private TextView valorSensibilidad, valorPermiso;
    private LinearLayout listaRecientes;

    private final Runnable refrescar = new Runnable() {
        @Override
        public void run() {
            actualizar();
            h.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(color(R.color.bg));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(ESPACIADO, ESPACIADO, ESPACIADO, ESPACIADO);
        scroll.addView(col);

        TextView titulo = texto(col, 30, Typeface.BOLD, R.color.texto, 0);
        titulo.setText(R.string.app_name);
        estado = texto(col, 16, Typeface.NORMAL, R.color.texto_tenue, dp(6));
        lugar = texto(col, 22, Typeface.BOLD, R.color.texto, dp(24));

        etiquetaSeccion(col, R.string.seccion_ajustes, dp(4));
        filaLugar = fila(col, R.drawable.ic_pin, R.string.boton_ubicacion, null);
        filaLugar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PrincipalActivity.this, UbicacionActivity.class));
            }
        });
        filaSensibilidad = fila(col, R.drawable.ic_gauge, R.string.sensibilidad_titulo, null);
        valorSensibilidad = (TextView) filaSensibilidad.getChildAt(2);
        filaSensibilidad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.guardarSensibilidad(PrincipalActivity.this, (Prefs.sensibilidad(PrincipalActivity.this) + 1) % 3);
                actualizar();
            }
        });
        filaPermiso = fila(col, R.drawable.ic_pantalla, R.string.permiso_titulo, null);
        valorPermiso = (TextView) filaPermiso.getChildAt(2);
        filaPermiso.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pedirPermisoPantalla();
            }
        });
        filaProbar = fila(col, R.drawable.ic_campana, R.string.boton_probar, null);
        filaProbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                probarAlerta();
            }
        });
        filaLugar.requestFocus();

        etiquetaSeccion(col, R.string.recientes_titulo, dp(28));
        listaRecientes = new LinearLayout(this);
        listaRecientes.setOrientation(LinearLayout.VERTICAL);
        col.addView(listaRecientes, anchoCompleto(0));

        TextView aviso = texto(col, 14, Typeface.NORMAL, R.color.texto_tenue, dp(32));
        aviso.setText(R.string.aviso_legal);
        aviso.setLineSpacing(dp(2), 1f);

        setContentView(scroll);
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Prefs.hayUbicacion(this)) {
            startActivity(new Intent(this, UbicacionActivity.class));
        } else {
            iniciarServicio();
        }
        h.post(refrescar);
    }

    @Override
    protected void onPause() {
        h.removeCallbacks(refrescar);
        super.onPause();
    }

    private void iniciarServicio() {
        Intent s = new Intent(this, MonitorService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(s);
            else startService(s);
        } catch (Exception ignorada) {
        }
    }

    private void actualizar() {
        boolean hay = Prefs.hayUbicacion(this);
        lugar.setText(hay ? getString(R.string.ubicacion_actual, Prefs.nombreLugar(this))
                : getString(R.string.sin_ubicacion));
        String[] nombres = getResources().getStringArray(R.array.sensibilidades);
        valorSensibilidad.setText(nombres[Prefs.sensibilidad(this)]);
        valorPermiso.setText(getString(puedeMostrarSobreApps() ? R.string.si : R.string.no));

        if (!hay) {
            estado.setText(R.string.estado_detenido);
        } else if (Prefs.ultimaConsulta(this) == 0) {
            estado.setText(R.string.estado_iniciando);
        } else {
            estado.setText(getString(R.string.estado_fuentes,
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(Prefs.ultimaConsulta(this))),
                    getString(Prefs.usgsEnLinea(this) ? R.string.en_linea : R.string.sin_respuesta),
                    getString(Prefs.emscEnLinea(this) ? R.string.en_linea : R.string.sin_respuesta)));
        }
        pintarRecientes();
    }

    private void pintarRecientes() {
        listaRecientes.removeAllViews();
        try {
            JSONArray a = new JSONArray(Prefs.recientes(this));
            long ahora = System.currentTimeMillis();
            if (a.length() == 0) {
                TextView vacio = new TextView(this);
                vacio.setText(R.string.sin_recientes);
                vacio.setTextColor(color(R.color.texto_tenue));
                vacio.setTextSize(16);
                listaRecientes.addView(vacio, anchoCompleto(dp(4)));
                return;
            }
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                long min = Math.max(0, (ahora - o.getLong("t")) / 60000);
                String cuando = min < 60 ? getString(R.string.hace_min, min) : getString(R.string.hace_horas, min / 60);
                TextView fila = new TextView(this);
                fila.setText(getString(R.string.linea_reciente, o.getDouble("m"), o.getString("l"),
                        o.getLong("d"), cuando, Evaluador.romano(o.getDouble("i"))));
                fila.setTextColor(color(R.color.texto_tenue));
                fila.setTextSize(15);
                LinearLayout.LayoutParams lp = anchoCompleto(dp(10));
                lp.bottomMargin = dp(10);
                listaRecientes.addView(fila, lp);
                if (i < a.length() - 1) listaRecientes.addView(divisor());
            }
        } catch (Exception ignorada) {
        }
    }

    private boolean puedeMostrarSobreApps() {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
    }

    private void pedirPermisoPantalla() {
        if (puedeMostrarSobreApps()) return;
        Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        if (i.resolveActivity(getPackageManager()) != null) {
            startActivity(i);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.permiso_manual_titulo)
                    .setMessage(getString(R.string.permiso_manual, getPackageName()))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    private void probarAlerta() {
        Intent i = new Intent(this, MonitorService.class).setAction(MonitorService.ACCION_PRUEBA);
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
        } catch (Exception ignorada) {
        }
        Toast.makeText(this, R.string.prueba_en_10, Toast.LENGTH_LONG).show();
    }

    // --- construcción de vistas, sin XML ni mapas de bits: solo texto, iconos vectoriales y formas planas ---

    private void etiquetaSeccion(LinearLayout padre, int textoId, int margenArriba) {
        TextView t = texto(padre, 13, Typeface.BOLD, R.color.texto_tenue, margenArriba);
        t.setText(textoId);
        t.setAllCaps(true);
        t.setLetterSpacing(0.08f);
    }

    /** Una fila de menú: icono + etiqueta + valor a la derecha. El foco pinta un acento a la izquierda. */
    private LinearLayout fila(LinearLayout padre, int iconoId, int etiquetaId, String valorInicial) {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.HORIZONTAL);
        f.setGravity(Gravity.CENTER_VERTICAL);
        f.setFocusable(true);
        f.setBackgroundResource(R.drawable.fila_fondo);
        f.setPadding(dp(14), dp(16), dp(14), dp(16));

        ImageView icono = new ImageView(this);
        icono.setImageResource(iconoId);
        icono.setColorFilter(color(R.color.texto));
        f.addView(icono, new LinearLayout.LayoutParams(dp(22), dp(22)));

        TextView etiqueta = new TextView(this);
        etiqueta.setText(etiquetaId);
        etiqueta.setTextColor(color(R.color.texto));
        etiqueta.setTextSize(18);
        LinearLayout.LayoutParams lpEtiqueta = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lpEtiqueta.leftMargin = dp(16);
        f.addView(etiqueta, lpEtiqueta);

        TextView valor = new TextView(this);
        valor.setText(valorInicial == null ? "" : valorInicial);
        valor.setTextColor(color(R.color.texto_tenue));
        valor.setTextSize(16);
        f.addView(valor);

        LinearLayout.LayoutParams lp = anchoCompleto(0);
        padre.addView(f, lp);
        padre.addView(divisor());
        return f;
    }

    private View divisor() {
        View v = new View(this);
        v.setBackgroundColor(color(R.color.divisor));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        return withParams(v, lp);
    }

    private View withParams(View v, LinearLayout.LayoutParams lp) {
        v.setLayoutParams(lp);
        return v;
    }

    private TextView texto(LinearLayout padre, int sp, int estilo, int colorId, int margenArriba) {
        TextView t = new TextView(this);
        t.setTextSize(sp);
        t.setTextColor(color(colorId));
        t.setTypeface(Typeface.DEFAULT, estilo);
        LinearLayout.LayoutParams lp = anchoCompleto(margenArriba);
        padre.addView(t, lp);
        return t;
    }

    private LinearLayout.LayoutParams anchoCompleto(int margenArriba) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = margenArriba;
        return lp;
    }

    private int color(int id) {
        return getResources().getColor(id);
    }

    private static int dp(int valor) {
        return Math.round(valor * android.content.res.Resources.getSystem().getDisplayMetrics().density);
    }
}
