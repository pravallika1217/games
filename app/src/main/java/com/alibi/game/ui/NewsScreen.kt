package com.alibi.game.ui

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alibi.game.GameViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

/** Tomorrow's front page, starring the detective. */
@Composable
fun NewsScreen(vm: GameViewModel) {
    val page = vm.page ?: return
    val context = LocalContext.current
    val spin = remember { Animatable(-540f) }
    val grow = remember { Animatable(0.1f) }
    LaunchedEffect(Unit) {
        val easing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.2f)
        launch { spin.animateTo(-1f, tween(900, easing = easing)) }
        grow.animateTo(1f, tween(900, easing = easing))
    }

    Page {
        Column(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    rotationZ = spin.value
                    scaleX = grow.value
                    scaleY = grow.value
                }
                .shadow(18.dp, RoundedCornerShape(3.dp))
                .background(Noir.Newsprint, RoundedCornerShape(3.dp))
                .padding(horizontal = 14.dp, vertical = 16.dp),
        ) {
            Text("The Daily Detective", color = Noir.NewsInk, fontFamily = Noir.News, fontWeight = FontWeight.Black, fontSize = 30.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            HorizontalDivider(color = Noir.NewsInk, thickness = 1.dp, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                val small = 11.sp
                Text(LocalDate.now().plusDays(1).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), color = Noir.NewsInk, fontFamily = Noir.Typewriter, fontSize = small)
                Text("CITY EDITION", color = Noir.NewsInk, fontFamily = Noir.Typewriter, fontSize = small)
                Text("₹5", color = Noir.NewsInk, fontFamily = Noir.Typewriter, fontSize = small)
            }
            HorizontalDivider(color = Noir.NewsInk, thickness = 3.dp)

            Text(
                page.headline.uppercase(),
                color = Noir.NewsInk,
                fontFamily = Noir.News,
                fontWeight = FontWeight.Black,
                fontSize = 27.sp,
                lineHeight = 30.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
            )
            Text(page.subhead, color = Noir.NewsInk, fontFamily = Noir.Typewriter, fontSize = 14.sp, lineHeight = 19.sp)

            Column(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF2A2620))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // A black-and-white "photo".
                Text(page.photo, fontSize = 46.sp, modifier = Modifier.graphicsLayer { alpha = 0.92f })
                Text(page.caption, color = Noir.Newsprint, fontFamily = Noir.Typewriter, fontSize = 11.sp, textAlign = TextAlign.Center)
            }

            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                page.columns.forEach {
                    Text(it, color = Noir.NewsInk, fontFamily = Noir.News, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f))
                }
            }
            HorizontalDivider(color = Noir.NewsInk, thickness = 1.dp, modifier = Modifier.padding(top = 12.dp))
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Rating: ${page.verdict.title} · ${page.verdict.points}/100", color = Noir.NewsInk, fontFamily = Noir.Typewriter, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("★".repeat(page.verdict.stars) + "☆".repeat(3 - page.verdict.stars), color = Noir.NewsInk, fontSize = 22.sp, letterSpacing = 3.sp)
            }
        }

        val promoted = vm.promotedTo
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Noir.Night2)
                .border(1.dp, if (promoted != null) Noir.Lamp else Noir.Night3, RoundedCornerShape(14.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (promoted != null) "🎖️" else "🗂️", fontSize = 32.sp)
            Text(
                if (promoted != null) "Promoted! You are now ${promoted.title}. Keep solving to climb the ranks."
                else "Rank: ${vm.careerRank.title} · ${vm.solvedCount} case${if (vm.solvedCount == 1) "" else "s"} solved · 🌆 ${vm.streak}-day streak",
                color = Noir.Fg,
                fontSize = 15.sp,
                lineHeight = 21.sp,
            )
        }

        LampButton("Share on WhatsApp 📤", onClick = {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, page.shareText)
            }
            context.startActivity(Intent.createChooser(send, "Share your front page"))
        })
        GhostButton("Back to your desk", onClick = vm::backToDesk, modifier = Modifier.fillMaxWidth())
        Box(Modifier.height(8.dp))
    }
}
