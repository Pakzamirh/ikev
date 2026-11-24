package com.sepehr.ikev2.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sepehr.ikev2.ui.theme.*
import com.sepehr.ikev2.vpn.VpnConnectionState

@Composable
fun OrbitalShieldLoader(
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    accentColor: Color = CobaltAccent
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbital")
    val outerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing)
        ),
        label = "outerAngle"
    )
    val innerAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "innerAngle"
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            drawArc(
                color = accentColor.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke)
            )
            drawArc(
                color = accentColor,
                startAngle = outerAngle,
                sweepAngle = 110f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = EmeraldShield,
                startAngle = innerAngle,
                sweepAngle = 85f,
                useCenter = false,
                topLeft = Offset(size.toPx() * 0.16f, size.toPx() * 0.16f),
                size = androidx.compose.ui.geometry.Size(size.toPx() * 0.68f, size.toPx() * 0.68f),
                style = Stroke(width = stroke * 0.85f, cap = StrokeCap.Round)
            )
        }
        Icon(
            imageVector = Icons.Rounded.Shield,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(size * 0.34f)
        )
    }
}

@Composable
fun AnimatedConnectRing(
    state: VpnConnectionState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "connectRing")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "sweep"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val ringColor = when (state) {
        VpnConnectionState.CONNECTED -> EmeraldShield
        VpnConnectionState.CONNECTING -> CobaltAccent
        VpnConnectionState.ERROR -> CrimsonAlert
        VpnConnectionState.DISCONNECTED -> Color(0xFF334155)
    }

    Box(
        modifier = modifier.size(168.dp),
        contentAlignment = Alignment.Center
    ) {
        if (state == VpnConnectionState.CONNECTED || state == VpnConnectionState.CONNECTING) {
            Box(
                modifier = Modifier
                    .size(156.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(ringColor.copy(alpha = 0.12f))
            )
        }

        Canvas(modifier = Modifier.size(148.dp)) {
            drawCircle(
                color = ringColor.copy(alpha = 0.25f),
                style = Stroke(width = 4.dp.toPx())
            )
            if (state == VpnConnectionState.CONNECTING) {
                drawArc(
                    color = CobaltAccent,
                    startAngle = sweepAngle,
                    sweepAngle = 120f,
                    useCenter = false,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
            } else if (state == VpnConnectionState.CONNECTED) {
                drawCircle(
                    color = EmeraldShield,
                    style = Stroke(width = 5.dp.toPx())
                )
            }
        }

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SurfaceElevated, SurfaceCard)
                    )
                )
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PowerSettingsNew,
                contentDescription = "اتصال یا قطع VPN",
                tint = when (state) {
                    VpnConnectionState.CONNECTED -> EmeraldShield
                    VpnConnectionState.CONNECTING -> CobaltAccent
                    else -> TextPrimary
                },
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
fun CountryListShimmer(count: Int = 6) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "shimmerX"
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            SurfaceCard,
            SurfaceElevated,
            SurfaceCard
        ),
        start = Offset(translate, 0f),
        end = Offset(translate + 260f, 0f)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(count) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BorderHairline)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(BorderHairline)
                    )
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(BorderHairline.copy(alpha = 0.6f))
                    )
                }
            }
        }
    }
}
