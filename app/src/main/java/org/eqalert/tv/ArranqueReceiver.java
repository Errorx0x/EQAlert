package org.eqalert.tv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Reanuda la vigilancia cuando el televisor arranca o la app se actualiza. */
public class ArranqueReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String accion = intent == null ? null : intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(accion)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(accion)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(accion)) return;
        if (!Prefs.hayUbicacion(context)) return;
        Intent servicio = new Intent(context, MonitorService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(servicio);
            else context.startService(servicio);
        } catch (Exception ignorada) {
        }
    }
}
