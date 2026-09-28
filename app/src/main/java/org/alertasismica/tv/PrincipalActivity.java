package org.alertasismica.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

/** Ajustes: ubicación, sensibilidad, permiso para interrumpir la pantalla y prueba de alerta. */
public class PrincipalActivity extends Activity {
    private final Handler h = new Handler(Looper.getMainLooper());
    private TextView estado, lugar, recientes;
    private Button btnLugar, btnSensibilidad, btnPermiso;

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
        scroll.setBackgroundColor(Color.parseColor("#0B0909"));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(90, 50, 90, 50);
        scroll.addView(col);

        TextView titulo = texto(col, 36, true, Color.WHITE, 0);
        titulo.setText(R.string.app_name);
        estado = texto(col, 20, false, Color.parseColor("#CFC8C2"), 14);
        lugar = texto(col, 24, true, Color.WHITE, 22);

        btnLugar = boton(col, 14);
        btnLugar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                elegirLugar();
            }
        });
        btnSensibilidad = boton(col, 10);
        btnSensibilidad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.guardarSensibilidad(PrincipalActivity.this, (Prefs.sensibilidad(PrincipalActivity.this) + 1) % 3);
                actualizar();
            }
        });
        btnPermiso = boton(col, 10);
        btnPermiso.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pedirPermisoPantalla();
            }
        });
        Button probar = boton(col, 10);
        probar.setText(R.string.boton_probar);
        probar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                probarAlerta();
            }
        });

        TextView t = texto(col, 22, true, Color.WHITE, 30);
        t.setText(R.string.recientes_titulo);
        recientes = texto(col, 18, false, Color.parseColor("#CFC8C2"), 8);
        TextView aviso = texto(col, 15, false, Color.parseColor("#8E8781"), 30);
        aviso.setText(R.string.aviso_legal);
        setContentView(scroll);

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Prefs.hayUbicacion(this)) {
            elegirLugar();
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
        btnLugar.setText(R.string.boton_ubicacion);
        String[] nombres = getResources().getStringArray(R.array.sensibilidades);
        btnSensibilidad.setText(getString(R.string.boton_sensibilidad, nombres[Prefs.sensibilidad(this)]));
        btnPermiso.setText(getString(R.string.boton_permiso,
                getString(puedeMostrarSobreApps() ? R.string.si : R.string.no)));

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
        recientes.setText(textoRecientes());
    }

    private String textoRecientes() {
        StringBuilder sb = new StringBuilder();
        try {
            JSONArray a = new JSONArray(Prefs.recientes(this));
            long ahora = System.currentTimeMillis();
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                long min = Math.max(0, (ahora - o.getLong("t")) / 60000);
                String cuando = min < 60 ? getString(R.string.hace_min, min) : getString(R.string.hace_horas, min / 60);
                sb.append(getString(R.string.linea_reciente, o.getDouble("m"), o.getString("l"),
                        o.getLong("d"), cuando, Evaluador.romano(o.getDouble("i")))).append("\n");
            }
        } catch (Exception ignorada) {
        }
        return sb.length() == 0 ? getString(R.string.sin_recientes) : sb.toString();
    }

    private void elegirLugar() {
        final String[] nombres = new String[Lugares.TODOS.length];
        for (int i = 0; i < nombres.length; i++) nombres[i] = Lugares.TODOS[i].nombre;
        new AlertDialog.Builder(this)
                .setTitle(R.string.elegir_ubicacion)
                .setItems(nombres, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int i) {
                        Prefs.guardarUbicacion(PrincipalActivity.this, Lugares.TODOS[i]);
                        iniciarServicio();
                        actualizar();
                    }
                })
                .show();
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

    private TextView texto(LinearLayout padre, int sp, boolean negrita, int color, int margenArriba) {
        TextView t = new TextView(this);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (negrita) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = margenArriba;
        padre.addView(t, lp);
        return t;
    }

    private Button boton(LinearLayout padre, int margenArriba) {
        Button b = new Button(this);
        b.setTextSize(20);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
        b.setPadding(40, 24, 40, 24);
        b.setFocusable(true);
        StateListDrawable fondo = new StateListDrawable();
        fondo.addState(new int[]{android.R.attr.state_focused}, forma("#B71C1C"));
        fondo.addState(new int[]{}, forma("#2A2524"));
        b.setBackground(fondo);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = margenArriba;
        padre.addView(b, lp);
        return b;
    }

    private GradientDrawable forma(String color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(color));
        g.setCornerRadius(14);
        return g;
    }
}
