package com.rotalog.delivery.shared

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.rotalog.delivery.model.Stop

enum class NavigationApp { GOOGLE_MAPS, WAZE }

/** Launch attempts allow unavailable handlers to recover without package discovery. */
class ExternalActions(
    private val context: Context,
    private val launch: (Intent) -> Unit = { context.startActivity(it) }
) {
    fun navigate(stop: Stop, app: NavigationApp): String? = safely(
        "Aplicativo indisponível. Escolha outro navegador ou copie o endereço."
    ) {
        val address = Uri.encode(stop.address)
        val intent = when (app) {
            NavigationApp.GOOGLE_MAPS -> Intent(Intent.ACTION_VIEW, "https://www.google.com/maps/dir/?api=1&destination=$address".toUri())
                .setPackage("com.google.android.apps.maps")
            NavigationApp.WAZE -> Intent(Intent.ACTION_VIEW, "https://waze.com/ul?q=$address&navigate=yes".toUri())
                .setPackage("com.waze")
        }
        launch(intent)
    }
    fun dial(stop: Stop): String? = safely("Discador indisponível neste aparelho.") {
        launch(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", stop.phone, null)))
    }
    fun copyAddress(stop: Stop): String = try {
        context.getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("Endereço da parada", stop.address))
        "Endereço copiado."
    } catch (_: SecurityException) { "Não foi possível copiar o endereço." }

    private fun safely(failure: String, action: () -> Unit): String? = try {
        action()
        null
    } catch (_: ActivityNotFoundException) { failure }
      catch (_: SecurityException) { failure }
}
