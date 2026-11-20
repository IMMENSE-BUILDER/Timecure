package com.timecure.app.ui.components

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timecure.app.model.StroopSpec

/**
 * Respects Android's global Animator Duration Scale ("Remove animations" accessibility setting).
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

/**
 * Consistent top bar insets across every screen so titles and icons sit comfortably below
 * status bars and camera notches / display cutouts.
 */
@Composable
fun safeTopBarWindowInsets(): WindowInsets =
    WindowInsets.statusBars.union(WindowInsets.displayCutout).add(WindowInsets(top = 4.dp))

/**
 * Bespoke Timecure AI emblem button: a crisp orbital neural 4-point star drawn in Canvas
 * inside a gradient-bordered pill paired with "AI" label.
 * Reads animation state ONLY in the draw phase (zero 60fps recompositions) and gives instant
 * spring press-down feedback + haptic tick on tap.
 */
@Composable
fun TimecureAiLogoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotion()
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "ai_btn_press"
    )

    val orbitState: State<Float> = if (reducedMotion) {
        rememberUpdatedState(25f)
    } else {
        val inf = rememberInfiniteTransition(label = "ai_emblem_orbit")
        inf.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(9000), RepeatMode.Restart),
            label = "orbit_deg"
        )
    }

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    val labelColor = MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = interaction,
        shape = CircleShape,
        color = container,
        border = BorderStroke(
            width = 1.25.dp,
            brush = Brush.linearGradient(colors = listOf(primary, tertiary))
        ),
        tonalElevation = 2.dp,
        modifier = modifier
            .heightIn(min = 40.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .semantics {
                role = Role.Button
                contentDescription = "Open Timecure AI Vision and Tool Assistant"
            }
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.minDimension / 2f

                // Read orbitState.value strictly inside DrawScope (zero recompositions)
                rotate(degrees = orbitState.value, pivot = Offset(cx, cy)) {
                    drawArc(
                        brush = Brush.sweepGradient(listOf(primary, tertiary, primary)),
                        startAngle = 20f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                val starR = r * 0.62f
                val innerR = r * 0.19f
                val starPath = Path().apply {
                    moveTo(cx, cy - starR)
                    quadraticTo(cx + innerR, cy - innerR, cx + starR, cy)
                    quadraticTo(cx + innerR, cy + innerR, cx, cy + starR)
                    quadraticTo(cx - innerR, cy + innerR, cx - starR, cy)
                    quadraticTo(cx - innerR, cy - innerR, cx, cy - starR)
                    close()
                }
                drawPath(
                    path = starPath,
                    brush = Brush.linearGradient(
                        colors = listOf(primary, tertiary),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = "AI",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = labelColor
            )
        }
    }
}

/**
 * Expressive concentric breathing aura behind the ringing clock.
 * Reads pulse strictly inside Canvas DrawScope (zero recompositions).
 */
@Composable
fun BreathingAuraRing(
    primaryColor: Color,
    accentColor: Color,
    urgent: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val pulseState: State<Float> = if (reducedMotion) {
        rememberUpdatedState(0.55f)
    } else {
        val transition = rememberInfiniteTransition(label = "alarm_aura")
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (urgent) 900 else 1600,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aura_pulse"
        )
    }

    Canvas(modifier = modifier) {
        val pulse = pulseState.value
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f
        val outerR = maxRadius * (0.72f + 0.28f * pulse)
        val midR = maxRadius * (0.56f + 0.22f * (1f - pulse * 0.6f))
        val innerR = maxRadius * 0.48f

        drawCircle(color = accentColor.copy(alpha = 0.14f * (1.2f - pulse * 0.5f)), radius = outerR, center = center)
        drawCircle(color = primaryColor.copy(alpha = 0.22f), radius = midR, center = center)
        drawCircle(
            color = primaryColor.copy(alpha = 0.45f),
            radius = innerR,
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
fun StroopWordBanner(
    stroop: StroopSpec,
    modifier: Modifier = Modifier
) {
    val inkColor = stroopInkColor(stroop.inkColorId)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Word ${stroop.word} displayed in ${stroop.inkColorId.lowercase()} ink"
            }
    ) {
        Text(
            text = stroop.word,
            color = inkColor,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp)
        )
    }
}

fun stroopInkColor(colorId: String): Color = when (colorId.uppercase()) {
    "BLUE" -> Color(0xFF1565C0)
    "ORANGE" -> Color(0xFFE65100)
    "GREEN" -> Color(0xFF2E7D32)
    "RED" -> Color(0xFFC62828)
    "PURPLE" -> Color(0xFF6A1B9A)
    "TEAL" -> Color(0xFF00695C)
    else -> Color(0xFF1565C0)
}

@Composable
fun AnimatedCheckmarkBadge(
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    var revealed by remember { mutableStateOf(reducedMotion) }
    LaunchedEffect(Unit) { revealed = true }

    val progressState = animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = if (reducedMotion) tween(0) else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "check_progress"
    )

    val containerColor = MaterialTheme.colorScheme.primary
    val strokeColor = MaterialTheme.colorScheme.onPrimary

    Box(modifier = modifier.size(80.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progress = progressState.value
            val radius = (size.minDimension / 2f) * progress.coerceIn(0f, 1.05f)
            drawCircle(color = containerColor, radius = radius)

            if (progress > 0.2f) {
                val p = ((progress - 0.2f) / 0.8f).coerceIn(0f, 1f)
                val w = size.width
                val h = size.height
                val start = Offset(w * 0.28f, h * 0.52f)
                val mid = Offset(w * 0.44f, h * 0.67f)
                val end = Offset(w * 0.73f, h * 0.35f)

                val stroke = Stroke(
                    width = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                val path = Path().apply {
                    moveTo(start.x, start.y)
                    if (p < 0.45f) {
                        val t = p / 0.45f
                        lineTo(start.x + (mid.x - start.x) * t, start.y + (mid.y - start.y) * t)
                    } else {
                        lineTo(mid.x, mid.y)
                        val t = (p - 0.45f) / 0.55f
                        lineTo(mid.x + (end.x - mid.x) * t, mid.y + (end.y - mid.y) * t)
                    }
                }
                drawPath(path = path, color = strokeColor, style = stroke)
            }
        }
    }
}
