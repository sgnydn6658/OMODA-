package com.aracekran.video.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import com.aracekran.video.AppState
import com.aracekran.video.Prefs
import com.aracekran.video.R

/**
 * Araç ekranındaki ana görünüm: arka planda web sayfası (WebSurfaceRenderer çizer),
 * üstte Android Auto'nun kendi düğmeleri.
 */
class BrowserScreen(
    carContext: CarContext,
    private val renderer: WebSurfaceRenderer
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        // Navigasyon şablonu üst şeritte en fazla 1 yazılı düğmeye izin verir; diğerleri simge.
        val actions = ActionStrip.Builder()
            .addAction(iconAction(R.drawable.ic_back) { renderer.goBack() })
            .addAction(iconAction(R.drawable.ic_home) { renderer.goHome() })
            .addAction(iconAction(R.drawable.ic_refresh) { renderer.reload() })
            .addAction(textAction(R.string.car_sites) { screenManager.push(SitesScreen(carContext)) })
            .build()

        // Sağdaki küçük düğmeler: kaydırma modu (PAN) + sayfa yukarı/aşağı
        val mapActions = ActionStrip.Builder()
            .addAction(Action.PAN)
            .addAction(iconAction(R.drawable.ic_up) { renderer.scrollPage(-1) })
            .addAction(iconAction(R.drawable.ic_down) { renderer.scrollPage(1) })
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(actions)
            .setMapActionStrip(mapActions)
            .build()
    }

    private fun textAction(titleRes: Int, onClick: () -> Unit): Action =
        Action.Builder()
            .setTitle(carContext.getString(titleRes))
            .setOnClickListener { onClick() }
            .build()

    private fun iconAction(iconRes: Int, onClick: () -> Unit): Action =
        Action.Builder()
            .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, iconRes)).build())
            .setOnClickListener { onClick() }
            .build()
}

/** Telefonda kayıtlı sitelerin listesi (araç ekranından seçilir). */
class SitesScreen(carContext: CarContext) : Screen(carContext) {

    @Suppress("DEPRECATION")
    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
        val bookmarks = Prefs.bookmarks(carContext)
        if (bookmarks.isEmpty()) {
            list.addItem(Row.Builder().setTitle("Telefondan site ekleyin").build())
        }
        bookmarks.forEach { bm ->
            list.addItem(
                Row.Builder()
                    .setTitle(bm.name)
                    .addText(bm.url)
                    .setOnClickListener {
                        AppState.openUrl(bm.url)
                        screenManager.pop()
                    }
                    .build()
            )
        }
        return ListTemplate.Builder()
            .setTitle(carContext.getString(R.string.car_sites_title))
            .setHeaderAction(Action.BACK)
            .setSingleList(list.build())
            .build()
    }
}
