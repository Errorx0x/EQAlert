package org.alertasismica.tv;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/** Pantalla completa de alerta roja: imagen que parpadea, sirena y voz. Se cierra con OK. */
public class AlertaActivity extends Activity {
    private static final long BLOQUEO_INICIAL_MS = 2500;
    private static final long CIERRE_AUTOMATICO_MS = 5 * 60 * 1000L;

    private final Handler h = new Handler(Looper.getMainLooper());
    private AudioTrack sirena;
    private TextToSpeech voz;
    private boolean vozLista;
    private ValueAnimator pulso;
    private int volumenAntes = -1;
    private float volumenSirena = 1f;
    private float volumenVoz = 1f;
    private long inicio;
    private FrameLayout raiz;
    private TextView prueba, magnitud, cerca, distancia, sacudida, llegada, consejo, fuente;

    private final Runnable hablar = new Runnable() {
        @Override
        public void run() {
            if (vozLista) {
                Bundle p = new Bundle();
                p.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumenVoz);
                voz.speak(getString(R.string.alerta_voz), TextToSpeech.QUEUE_FLUSH, p, "alerta");
            }
            h.postDelayed(this, 9000);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        inicio = System.currentTimeMillis();
        quitarNotificacion();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT < 27) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        construirPantalla();
        mostrar(getIntent());
        iniciarSirena();
        voz = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int estado) {
                if (estado != TextToSpeech.SUCCESS) return;
                int r = voz.setLanguage(new Locale("es"));
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) return;
                voz.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
                vozLista = true;
            }
        });
        h.postDelayed(hablar, 2000);
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, CIERRE_AUTOMATICO_MS);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        inicio = System.currentTimeMillis();
        quitarNotificacion();
        aplicarVolumen(intent.getBooleanExtra(MonitorService.EXTRA_PRUEBA, false));
        mostrar(intent);
    }

    private TextView texto(LinearLayout padre, int sp, boolean negrita, int margenArriba) {
        TextView t = new TextView(this);
        t.setTextColor(Color.WHITE);
        t.setTextSize(sp);
        t.setGravity(Gravity.CENTER);
        if (negrita) t.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = margenArriba;
        padre.addView(t, lp);
        return t;
    }

    private void construirPantalla() {
        raiz = new FrameLayout(this);
        raiz.setBackgroundColor(Color.parseColor("#B71C1C"));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setPadding(80, 24, 80, 24);
        raiz.addView(col, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        prueba = texto(col, 20, true, 0);
        prueba.setTextColor(Color.parseColor("#FFEB3B"));
        TextView titulo = texto(col, 46, true, 4);
        titulo.setText(R.string.alerta_titulo);
        magnitud = texto(col, 32, true, 10);
        cerca = texto(col, 22, false, 4);
        distancia = texto(col, 22, false, 2);
        sacudida = texto(col, 26, true, 10);
        llegada = texto(col, 22, false, 4);
        consejo = texto(col, 30, true, 14);
        consejo.setText(R.string.alerta_consejo);
        TextView detalle = texto(col, 20, false, 4);
        detalle.setText(R.string.alerta_detalle);

        Button cerrar = new Button(this);
        cerrar.setText(R.string.alerta_cerrar);
        cerrar.setTextSize(20);
        cerrar.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 14;
        col.addView(cerrar, lp);
        cerrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intentarCerrar();
            }
        });
        cerrar.requestFocus();

        fuente = texto(col, 15, false, 10);
        fuente.setAlpha(0.8f);
        setContentView(raiz);

        pulso = ValueAnimator.ofObject(new ArgbEvaluator(), Color.parseColor("#B71C1C"), Color.parseColor("#4A0A0A"));
        pulso.setDuration(500);
        pulso.setRepeatMode(ValueAnimator.REVERSE);
        pulso.setRepeatCount(ValueAnimator.INFINITE);
        pulso.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                raiz.setBackgroundColor((Integer) a.getAnimatedValue());
            }
        });
        pulso.start();
    }

    private void mostrar(Intent i) {
        boolean esPrueba = i.getBooleanExtra(MonitorService.EXTRA_PRUEBA, false);
        double mag = i.getDoubleExtra(MonitorService.EXTRA_MAG, 0);
        double dist = i.getDoubleExtra(MonitorService.EXTRA_DIST, 0);
        double mmi = i.getDoubleExtra(MonitorService.EXTRA_MMI, 0);
        double prof = i.getDoubleExtra(MonitorService.EXTRA_PROF, 10);
        long t = i.getLongExtra(MonitorService.EXTRA_TIEMPO, System.currentTimeMillis());
        String lugar = i.getStringExtra(MonitorService.EXTRA_LUGAR);

        prueba.setText(esPrueba ? getString(R.string.alerta_prueba) : "");
        prueba.setVisibility(esPrueba ? View.VISIBLE : View.GONE);
        magnitud.setText(getString(R.string.alerta_magnitud, mag));
        cerca.setText(lugar == null || lugar.isEmpty() ? "" : getString(R.string.alerta_cerca, lugar));
        distancia.setText(getString(R.string.alerta_distancia, Math.round(dist)));
        sacudida.setText(getString(R.string.alerta_sacudida, Evaluador.romano(mmi)));
        long ahora = System.currentTimeMillis();
        double falta = Evaluador.segundosHastaOndaS(dist, prof, t, ahora);
        if (falta > 1) {
            llegada.setText(getString(R.string.alerta_llegada, Math.round(falta)));
        } else {
            llegada.setText(getString(R.string.alerta_hace, Math.max(0, (ahora - t) / 60000)));
        }
        String f = i.getStringExtra(MonitorService.EXTRA_FUENTE);
        fuente.setText(getString(R.string.alerta_fuente, f == null ? "—" : f));
    }

    /** La notificación de pantalla completa ya cumplió su papel: no debe tapar la alerta. */
    private void quitarNotificacion() {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(MonitorService.ID_ALERTA);
    }

    private void intentarCerrar() {
        if (System.currentTimeMillis() - inicio < BLOQUEO_INICIAL_MS) return;
        finish();
    }

    @Override
    public boolean onKeyDown(int codigo, KeyEvent evento) {
        if (codigo == KeyEvent.KEYCODE_DPAD_CENTER || codigo == KeyEvent.KEYCODE_ENTER
                || codigo == KeyEvent.KEYCODE_NUMPAD_ENTER || codigo == KeyEvent.KEYCODE_BUTTON_A) {
            intentarCerrar();
            return true;
        }
        return super.onKeyDown(codigo, evento);
    }

    @Override
    public void onBackPressed() {
        intentarCerrar();
    }

    private void iniciarSirena() {
        aplicarVolumen(getIntent().getBooleanExtra(MonitorService.EXTRA_PRUEBA, false));

        int tasa = 22050;
        int n = tasa * 2;
        short[] pcm = new short[n];
        double fase = 0;
        for (int i = 0; i < n; i++) {
            double frecuencia = 900 + 350 * Math.sin(2 * Math.PI * i / tasa);
            fase += 2 * Math.PI * frecuencia / tasa;
            pcm[i] = (short) (32767 * 0.9 * (0.75 * Math.sin(fase) + 0.25 * Math.sin(3 * fase)));
        }
        sirena = new AudioTrack(AudioManager.STREAM_ALARM, tasa, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, n * 2, AudioTrack.MODE_STATIC);
        sirena.write(pcm, 0, n);
        sirena.setLoopPoints(0, n, -1);
        sirena.setVolume(volumenSirena);
        sirena.play();
    }

    /**
     * Alerta real: sirena fuerte (volumen de alarma al 85 %, nunca por debajo del que ya tenía).
     * Prueba: casi en silencio, para no despertar a nadie; nunca sube el volumen del televisor.
     */
    private void aplicarVolumen(boolean esPrueba) {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        if (volumenAntes < 0) volumenAntes = am.getStreamVolume(AudioManager.STREAM_ALARM);
        int nuevo;
        if (esPrueba) {
            nuevo = Math.min(volumenAntes, Math.max(1, Math.round(max * 0.15f)));
        } else {
            nuevo = Math.max(volumenAntes, Math.round(max * 0.85f));
        }
        am.setStreamVolume(AudioManager.STREAM_ALARM, nuevo, 0);
        volumenSirena = esPrueba ? 0.12f : 1f;
        volumenVoz = esPrueba ? 0.15f : 1f;
        if (sirena != null) sirena.setVolume(volumenSirena);
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        if (pulso != null) pulso.cancel();
        if (sirena != null) {
            try {
                sirena.stop();
            } catch (IllegalStateException ignorada) {
            }
            sirena.release();
        }
        if (voz != null) voz.shutdown();
        if (volumenAntes >= 0) {
            ((AudioManager) getSystemService(Context.AUDIO_SERVICE))
                    .setStreamVolume(AudioManager.STREAM_ALARM, volumenAntes, 0);
        }
        super.onDestroy();
    }
}
