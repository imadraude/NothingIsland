package com.nothingisland.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.nothingisland.app.IslandApplication
import com.nothingisland.app.model.IslandEvent

class BatteryStateReceiver : BroadcastReceiver() {
    private var lastChargingState: Boolean? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

            val percentage = if (level >= 0 && scale > 0) ((level.toFloat() / scale) * 100).toInt() else 0
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            val isFastCharging = chargePlug == BatteryManager.BATTERY_PLUGGED_AC

            // Trigger event when charging state switches to connected
            if (lastChargingState != null && lastChargingState == false && isCharging) {
                IslandApplication.stateManager.postEvent(
                    IslandEvent.Battery(
                        percentage = percentage,
                        isCharging = true,
                        isFastCharging = isFastCharging
                    )
                )
            }
            lastChargingState = isCharging
        }
    }
}
