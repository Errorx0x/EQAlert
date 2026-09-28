package org.alertasismica.tv;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Vigilancia continua: consulta las fuentes oficiales cada 30 s y, si el sismo alcanza el nivel
 * de alerta roja en la ubicación elegida, abre la pantalla de alerta sobre lo que se esté viendo.
 */
public class MonitorService extends Service {
    static final String EXTRA_MAG = "mag";
    static final String EXTRA_LUGAR = "lugar";
    static final String EXTRA_DIST = "dist";
    static final String EXTRA_MMI = "mmi";
    static final String EXTRA_PROF = "prof";
    static final String EXTRA_TIEMPO = "tiempo";
    static final String EXTRA_FUENTE = "fuente";
    static final String EXTRA_PRUEBA = "prueba";
    static final String ACCION_PRUEBA = "org.alertasismica.tv.PRUEBA";

    private static final String CANAL_ESTADO = "estado";
    private static final String CANAL_ALERTA = "alerta";
    private static final int ID_ESTADO = 1;
    static final int ID_ALERTA = 2;
    private static final long CADA_MS = 30 * 1000L;
    private static final long EDAD_MAXIMA_MS = 30 * 60 * 1000L;

    static volatile boolean corriendo;

    private HandlerThread hilo;
    private Handler manejador;
    private PowerManager.WakeLock cpu;
    private WifiManager.WifiLock wifi;

    private final Runnable ciclo = new Runnable() {
        @Override
        public void run() {
            try {
                consultar();
            } catch (Exception ignorada) {
            }
            manejador.postDelayed(this, CADA_MS);
        }
    };

    @SuppressLint("WakelockTimeout") // la vigilancia debe mantener la CPU despierta mientras el servicio viva
    @Override
    public void onCreate() {
        super.onCreate();
        crearCanales();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        cpu = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "alertasismica:vigilancia");
        cpu.acquire();
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            wifi = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "alertasismica:vigilancia");
            wifi.acquire();
        }
        hilo = new HandlerThread("vigilancia");
        hilo.start();
        manejador = new Handler(hilo.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification n = notificacionEstado(getString(R.string.notif_iniciando));
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(ID_ESTADO, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(ID_ESTADO, n);
        }
        if (!corriendo) {
            corriendo = true;
            manejador.post(ciclo);
        }
        if (intent != null && ACCION_PRUEBA.equals(intent.getAction())) {
            manejador.postDelayed(new Runnable() {
                @Override
                public void run() {
                    long ahora = System.currentTimeMillis();
                    Sismo s = new Sismo("prueba", "prueba", 6.4, 0, 0, 12, ahora - 5000, getString(R.string.lugar_prueba));
                    lanzarAlerta(new Candidato(s, 38, 7.0, Evaluador.ROJA));
                }
            }, 10000);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        corriendo = false;
        manejador.removeCallbacksAndMessages(null);
        hilo.quitSafely();
        if (cpu != null && cpu.isHeld()) cpu.release();
        if (wifi != null && wifi.isHeld()) wifi.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void consultar() throws Exception {
        if (!Prefs.hayUbicacion(this)) return;
        long ahora = System.currentTimeMillis();
        double lat = Prefs.lat(this), lon = Prefs.lon(this);
        List<Sismo> todos = new ArrayList<>();
        boolean usgs = false, emsc = false;
        try {
            todos.addAll(Fuentes.usgs(lat, lon, ahora));
            usgs = true;
        } catch (Exception e) {
            Log.w("AlertaSismica", "USGS: " + e);
        }
        try {
            todos.addAll(Fuentes.emsc(lat, lon, ahora));
            emsc = true;
        } catch (Exception e) {
            Log.w("AlertaSismica", "EMSC: " + e);
        }
        Prefs.guardarEstado(this, ahora, usgs, emsc);
        actualizarEstado(usgs || emsc
                ? getString(R.string.notif_vigilando, Prefs.nombreLugar(this),
                DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(ahora)))
                : getString(R.string.notif_sin_conexion));
        if (!usgs && !emsc) return;

        int sens = Prefs.sensibilidad(this);
        final List<Candidato> candidatos = new ArrayList<>();
        List<Candidato> cercanos = new ArrayList<>();
        for (Sismo s : todos) {
            double dist = Evaluador.distanciaKm(lat, lon, s.lat, s.lon);
            double mmi = Evaluador.intensidadEstimada(s.magnitud, dist, s.profundidadKm);
            Candidato c = new Candidato(s, dist, mmi, Evaluador.nivel(mmi, s.magnitud, sens));
            if (dist <= 500) cercanos.add(c);
            if (c.nivel != Evaluador.NINGUNA && ahora - s.tiempoMs <= EDAD_MAXIMA_MS) candidatos.add(c);
        }
        guardarRecientes(cercanos);

        Collections.sort(candidatos, new Comparator<Candidato>() {
            @Override
            public int compare(Candidato a, Candidato b) {
                if (a.nivel != b.nivel) return b.nivel - a.nivel;
                return Double.compare(b.mmi, a.mmi);
            }
        });
        for (Candidato c : candidatos) {
            if (Prefs.yaAlertado(this, c.sismo, c.nivel)) continue;
            Prefs.registrarAlerta(this, c.sismo, c.nivel, ahora);
            if (c.nivel == Evaluador.ROJA) lanzarAlerta(c);
            else avisar(c);
        }
    }

    private void guardarRecientes(List<Candidato> cercanos) {
        Collections.sort(cercanos, new Comparator<Candidato>() {
            @Override
            public int compare(Candidato a, Candidato b) {
                return Long.compare(b.sismo.tiempoMs, a.sismo.tiempoMs);
            }
        });
        JSONArray a = new JSONArray();
        List<Sismo> vistos = new ArrayList<>();
        for (Candidato c : cercanos) {
            boolean repetido = false;
            for (Sismo v : vistos) {
                if (Math.abs(v.tiempoMs - c.sismo.tiempoMs) < 120000
                        && Evaluador.distanciaKm(v.lat, v.lon, c.sismo.lat, c.sismo.lon) < 200) {
                    repetido = true;
                    break;
                }
            }
            if (repetido) continue;
            vistos.add(c.sismo);
            try {
                a.put(new JSONObject().put("m", c.sismo.magnitud).put("l", c.sismo.lugar)
                        .put("d", Math.round(c.dist)).put("t", c.sismo.tiempoMs).put("i", c.mmi));
            } catch (Exception ignorada) {
            }
            if (a.length() >= 12) break;
        }
        Prefs.guardarRecientes(this, a.toString());
    }

    private Intent intentAlerta(Candidato c) {
        return new Intent(this, AlertaActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_MAG, c.sismo.magnitud)
                .putExtra(EXTRA_LUGAR, c.sismo.lugar)
                .putExtra(EXTRA_DIST, c.dist)
                .putExtra(EXTRA_MMI, c.mmi)
                .putExtra(EXTRA_PROF, c.sismo.profundidadKm)
                .putExtra(EXTRA_TIEMPO, c.sismo.tiempoMs)
                .putExtra(EXTRA_FUENTE, c.sismo.fuente)
                .putExtra(EXTRA_PRUEBA, "prueba".equals(c.sismo.id));
    }

    private void lanzarAlerta(Candidato c) {
        Intent abrir = intentAlerta(c);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(this, ID_ALERTA, abrir, flags);
        Notification.Builder b = constructor(CANAL_ALERTA)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(getString(R.string.alerta_titulo))
                .setContentText(getString(R.string.notif_sismo, c.sismo.magnitud, Math.round(c.dist)))
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_MAX)
                .setContentIntent(pi)
                .setFullScreenIntent(pi, true)
                .setAutoCancel(true);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(ID_ALERTA, b.build());
        try {
            startActivity(abrir);
        } catch (Exception ignorada) {
            // Sin permiso para mostrar sobre otras apps, queda la notificación de pantalla completa.
        }
    }

    private void avisar(Candidato c) {
        Notification.Builder b = constructor(CANAL_ALERTA)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(getString(c.nivel == Evaluador.NARANJA ? R.string.aviso_naranja : R.string.aviso_informativo))
                .setContentText(getString(R.string.notif_sismo_intensidad, c.sismo.magnitud,
                        Math.round(c.dist), Evaluador.romano(c.mmi)))
                .setAutoCancel(true);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify((int) (c.sismo.tiempoMs % 100000) + 10, b.build());
    }

    private Notification.Builder constructor(String canal) {
        return Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, canal) : new Notification.Builder(this);
    }

    private Notification notificacionEstado(String texto) {
        return constructor(CANAL_ESTADO)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(texto)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void actualizarEstado(String texto) {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(ID_ESTADO, notificacionEstado(texto));
    }

    private void crearCanales() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CANAL_ESTADO,
                getString(R.string.canal_estado), NotificationManager.IMPORTANCE_MIN));
        nm.createNotificationChannel(new NotificationChannel(CANAL_ALERTA,
                getString(R.string.canal_alerta), NotificationManager.IMPORTANCE_HIGH));
    }

    private static final class Candidato {
        final Sismo sismo;
        final double dist;
        final double mmi;
        final int nivel;

        Candidato(Sismo sismo, double dist, double mmi, int nivel) {
            this.sismo = sismo;
            this.dist = dist;
            this.mmi = mmi;
            this.nivel = nivel;
        }
    }
}
