package com.elder.desktop.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.elder.desktop.data.di.AppContainer
import com.elder.desktop.data.model.AppEntry
import com.elder.desktop.ui.theme.EldBg
import com.elder.desktop.ui.theme.EldBg2
import com.elder.desktop.ui.theme.EldFamily
import com.elder.desktop.ui.theme.EldPhone
import com.elder.desktop.ui.theme.EldPhoto
import com.elder.desktop.ui.theme.EldSos
import com.elder.desktop.ui.theme.EldSub
import com.elder.desktop.ui.theme.EldWhite
import com.elder.desktop.util.AppLauncher
import com.elder.desktop.util.Guard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenCall: () -> Unit,
    onOpenFamily: () -> Unit,
    onOpenPhoto: () -> Unit,
    onOpenSos: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTick by remember { mutableStateOf(0) }
    // 回到桌面（onResume）时刷新：应用可能被卸载，第三行据此回退占位（功能3）
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var now by remember { mutableStateOf(Date()) }
    // 低频刷新，避免秒级轮询耗电（技术方案 §4.1）
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(30_000)
        }
    }
    val weather = remember { container.weatherRepository.load() }

    // 功能3：桌面第三方应用（六宫格第三行，最多 2 个）
    val apps by produceState<List<AppEntry>>(initialValue = emptyList(), container) {
        container.appRepository.observeApps().collect { value = it }
    }

    val hh = remember(now) { SimpleDateFormat("HH", Locale.CHINA).format(now) }
    val mm = remember(now) { SimpleDateFormat("mm", Locale.CHINA).format(now) }
    val dateStr = remember(now) { SimpleDateFormat("M月d日 · EEEE", Locale.CHINA).format(now) }

    // 秒点闪烁：纯动画，不重建时钟
    val blink by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EldBg)
            .padding(horizontal = 22.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 时钟
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(hh, color = EldWhite, fontSize = MaterialTheme.typography.displayLarge.fontSize,
                fontWeight = FontWeight.Black)
            Text(":", color = EldWhite.copy(alpha = blink),
                fontSize = MaterialTheme.typography.displayLarge.fontSize,
                fontWeight = FontWeight.Black)
            Text(mm, color = EldWhite, fontSize = MaterialTheme.typography.displayLarge.fontSize,
                fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(6.dp))
        Text(dateStr, color = EldWhite, fontSize = MaterialTheme.typography.headlineMedium.fontSize,
            fontWeight = FontWeight.Medium)
        if (weather != null && weather.condition.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 26.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val w = listOfNotNull(weather.city, weather.condition, weather.temp).joinToString(" · ")
                Text(w, color = EldWhite, fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(18.dp))

        // 六宫格：3 行 × 2 列（电话/家人、照片/SOS、应用/应用）
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()) {
                BigTile(Modifier.weight(1f), "电话", Icons.Filled.Phone, EldPhone, onOpenCall)
                BigTile(Modifier.weight(1f), "家人", Icons.Filled.People, EldFamily, onOpenFamily)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()) {
                BigTile(Modifier.weight(1f), "照片", Icons.Filled.PhotoLibrary, EldPhoto, onOpenPhoto)
                BigTile(Modifier.weight(1f), "SOS", Icons.Filled.Warning, EldSos, onOpenSos)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()) {
                AppTile(Modifier.weight(1f), apps.getOrNull(0), context, container, resumeTick) {
                    Guard.tap(context)
                    AppLauncher.launch(context, it.packageName)
                }
                AppTile(Modifier.weight(1f), apps.getOrNull(1), context, container, resumeTick) {
                    Guard.tap(context)
                    AppLauncher.launch(context, it.packageName)
                }
            }
        }
    }
}

@Composable
private fun BigTile(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    bg: Color,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .height(132.dp)
            .scale(if (pressed) 0.96f else 1f)
            .clip(RoundedCornerShape(28.dp))
            .background(bg)
            .clickable(interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = EldWhite, modifier = Modifier.size(46.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, color = EldWhite, fontSize = MaterialTheme.typography.labelLarge.fontSize,
                fontWeight = FontWeight.Black)
        }
    }
}

/**
 * 桌面第三方应用格（功能3）。未勾选或应用已卸载时不显示应用，回退为「应用」占位。
 */
@Composable
private fun AppTile(
    modifier: Modifier = Modifier,
    entry: AppEntry?,
    context: android.content.Context,
    container: AppContainer,
    refreshKey: Int,
    onLaunch: (AppEntry) -> Unit,
) {
    val visible = remember(entry, refreshKey) {
        entry != null && container.appRepository.isInstalled(entry.packageName)
    }
    if (!visible || entry == null) {
        Box(
            modifier = modifier
                .height(132.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(EldBg2),
            contentAlignment = Alignment.Center,
        ) {
            Text("应用", color = EldSub, fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                fontWeight = FontWeight.Bold)
        }
        return
    }
    val icon by produceState<ImageBitmap?>(initialValue = null, entry) {
        value = AppLauncher.loadIcon(context, entry.packageName)
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .height(132.dp)
            .scale(if (pressed) 0.96f else 1f)
            .clip(RoundedCornerShape(28.dp))
            .background(EldBg2)
            .clickable(interaction, indication = null) { onLaunch(entry) },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                androidx.compose.foundation.Image(
                    bitmap = icon!!,
                    contentDescription = entry.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(entry.label.take(1), color = EldWhite,
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(entry.label, color = EldWhite,
                fontSize = MaterialTheme.typography.labelLarge.fontSize,
                fontWeight = FontWeight.Black)
        }
    }
}
