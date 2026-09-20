package com.elder.desktop.ui.photo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.elder.desktop.data.di.AppContainer
import com.elder.desktop.data.model.PhotoItem
import com.elder.desktop.ui.common.TopBar
import com.elder.desktop.ui.theme.EldBg
import com.elder.desktop.ui.theme.EldWhite
import com.elder.desktop.util.Guard

@Composable
fun PhotoScreen(container: AppContainer, onBack: () -> Unit) {
    val photos = remember { container.photoRepository.photos() }
    var index by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().background(EldBg).padding(horizontal = 22.dp),
    ) {
        TopBar("照片", onBack)

        if (photos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("照片还没准备好", color = EldWhite,
                    style = MaterialTheme.typography.headlineLarge)
            }
            return@Column
        }

        val current = photos[index.coerceIn(0, photos.lastIndex)]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF0A1826)),
        ) {
            AsyncImage(
                model = container.photoRepository.fileFor(current),
                contentDescription = current.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            // 左箭头
            NavArrow(
                Modifier.align(Alignment.CenterStart),
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            ) {
                Guard.tap(context)
                index = if (index == 0) photos.lastIndex else index - 1
            }
            // 右箭头
            NavArrow(
                Modifier.align(Alignment.CenterEnd),
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
            ) {
                Guard.tap(context)
                index = if (index == photos.lastIndex) 0 else index + 1
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            photos.forEachIndexed { i, _ ->
                val active = i == index
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(if (active) 14.dp else 12.dp)
                        .clip(CircleShape)
                        .background(if (active) EldWhite else EldWhite.copy(alpha = 0.35f))
                )
            }
        }
    }
}

@Composable
private fun NavArrow(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x880A1826))
            .size(56.dp, 90.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = EldWhite, modifier = Modifier.size(36.dp))
    }
}
