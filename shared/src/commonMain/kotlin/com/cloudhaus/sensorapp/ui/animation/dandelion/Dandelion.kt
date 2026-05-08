@file:Suppress("FunctionName")

package com.cloudhaus.sensorapp.ui.animation.dandelion

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

private val PI_F = PI.toFloat()
private const val TWO_PI = 6.2831855f
private const val DEG = 57.29578f

@Suppress("MemberVisibilityCanBePrivate")
data class DandelionPalette(
    val stem: Color = Color(0xFF6B9A5D),
    val stemDark: Color = Color(0xFF527A47),
    val leaf: Color = Color(0xFF7BA86B),
    val leafDark: Color = Color(0xFF5E8A52),
    val seedFluff: Color = Color(0xFFFFFDF8),
    val seedFluffShade: Color = Color(0xFFE8E4D4),
    val seedCore: Color = Color(0xFFC9A876),
    val seedStalk: Color = Color(0xFF8FAE7E),
    val receptacle: Color = Color(0xFF9BB890),
) {
    companion object {
        val Default = DandelionPalette()
    }
}

/**
 * Tunable shape/layout knobs for the dandelion. Pass a custom one through
 * [Dandelion]'s `config` param to change seed counts, leaf sizes, stem
 * thickness, etc. without editing the rig file.
 */
@Suppress("MemberVisibilityCanBePrivate")
data class DandelionConfig(
    /** One [Ring] per concentric seed ring. Default = 6 rings, 99 seeds. */
    val rings: List<Ring> = DefaultRings,
    /** One [Leaf] per drawn leaf at the stem base. */
    val leaves: List<Leaf> = DefaultLeaves,
    /** Stroke width of the main stem path. */
    val stemWidth: Float = 10f,
    /** Stem length as a fraction of the canvas height. */
    val stemLengthFraction: Float = 0.55f,
    /** Sway amplitude in degrees at breath = 0. */
    val swayAmplitudeBase: Float = 1.5f,
    /** Additional sway amplitude in degrees added at full breath (100). */
    val swayAmplitudePerBreath: Float = 4f,
    /** Number of teeth on each side of every leaf. */
    val leafTeeth: Int = 5,
    /** How far each leaf tooth extends from the leaf's central rib. */
    val leafToothDepth: Float = 30f,
    /** Radius of the soft white halo behind the puffball at full fullness. */
    val haloRadius: Float = 240f,
    /** Radius of the fluff disc on a single seed. */
    val seedFluffRadius: Float = 22f,
    /** Number of pappus spokes drawn per seed. */
    val seedSpokes: Int = 11,
    /** Lowest possible breath value at which a seed may auto-detach. */
    val detachThresholdMin: Float = 78f,
    /** Random jitter added to [detachThresholdMin] per seed. */
    val detachThresholdJitter: Float = 20f,
) {
    /** A concentric ring of seeds. `count = 1, radius = 0` is the centre seed. */
    data class Ring(val count: Int, val radius: Float, val phase: Float = 0f)

    /**
     * A single leaf at the stem base. [offsetX]/[offsetY] are pixel offsets
     * from the stem base, [baseAngleDeg] is the resting rotation, and
     * [swayCoefficient] multiplies the global leaf-sway value (use a negative
     * coefficient to make a leaf sway opposite to the others).
     */
    data class Leaf(
        val offsetX: Float,
        val offsetY: Float,
        val baseAngleDeg: Float,
        val swayCoefficient: Float,
        val length: Float,
    )

    companion object {
        val DefaultRings: List<Ring> = listOf(
            Ring(count = 1, radius = 0f),
            Ring(count = 16, radius = 36f),
            Ring(count = 28, radius = 68f, phase = 0.2f),
            Ring(count = 40, radius = 100f, phase = 0.4f),
            Ring(count = 52, radius = 132f, phase = 0.6f),
            Ring(count = 60, radius = 164f, phase = 0.8f),
        )
        val DefaultLeaves: List<Leaf> = listOf(
            Leaf(offsetX = -4f, offsetY = -6f, baseAngleDeg = -50f, swayCoefficient = 1f, length = 170f),
            Leaf(offsetX = 4f, offsetY = -4f, baseAngleDeg = 130f, swayCoefficient = -0.7f, length = 145f),
            Leaf(offsetX = -8f, offsetY = -14f, baseAngleDeg = -110f, swayCoefficient = 0.6f, length = 120f),
        )
        val Default = DandelionConfig()
    }
}

@Composable
fun Dandelion(
    breath: Float,
    endTrigger: Int,
    modifier: Modifier = Modifier,
    palette: DandelionPalette = DandelionPalette.Default,
    config: DandelionConfig = DandelionConfig.Default,
    windStrength: Float = 0.4f,
) {
    val seeds = remember(config) {
        val list = mutableListOf<Seed>()
        for (ring in config.rings) {
            val threshold = { config.detachThresholdMin + Random.nextFloat() * config.detachThresholdJitter }
            if (ring.count <= 1) {
                list.add(Seed(homeAngle = 0f, homeRadius = 0f, detachThreshold = threshold()))
            } else {
                for (i in 0 until ring.count) {
                    val angle = i.toFloat() / ring.count * TWO_PI + ring.phase
                    list.add(Seed(homeAngle = angle, homeRadius = ring.radius, detachThreshold = threshold()))
                }
            }
        }
        list
    }

    val swayPhase = remember { mutableFloatStateOf(Random.nextFloat() * TWO_PI) }
    val burstFlash = remember { mutableFloatStateOf(0f) }

    val frameTime = remember { mutableFloatStateOf(0f) }
    val lastTime = remember { mutableFloatStateOf(-1f) }
    val dtState = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var startNanos = 0L
        while (true) {
            withFrameNanos { now ->
                if (startNanos == 0L) startNanos = now
                val tSec = (now - startNanos) / 1_000_000_000f
                val prev = lastTime.floatValue
                val dt = if (prev < 0f) 0f else (tSec - prev).coerceAtMost(0.05f)
                lastTime.floatValue = tSec
                dtState.floatValue = dt
                frameTime.floatValue = tSec
                burstFlash.floatValue *= 0.92f
            }
        }
    }

    LaunchedEffect(endTrigger) {
        if (endTrigger > 0) {
            burstFlash.floatValue = 1f
            // Stagger like the JSX original's setTimeout(release, i*8) — each
            // seed waits its own absolute delay concurrently, so the burst
            // completes in ~0.8s. (Sequential delay(i*8L) here would
            // accumulate to ~39s and freeze mid-release.)
            coroutineScope {
                seeds.forEachIndexed { i, s ->
                    if (s.attached) {
                        launch {
                            delay(i * 8L)
                            s.release(force = 1.2f)
                        }
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        val now = frameTime.floatValue
        val dt = dtState.floatValue

        val w = size.width
        val h = size.height
        val breathC = breath.coerceIn(0f, 100f)
        val breathN = breathC / 100f

        swayPhase.floatValue += dt * (0.5f + breathN * 0.9f)
        val sp = swayPhase.floatValue
        val sway = sin(sp) * (config.swayAmplitudeBase + breathN * config.swayAmplitudePerBreath)
        val stemAngle = sway * (PI_F / 180f)

        val foreshorten = 1f + breathN * 0.04f
        val baseX = w / 2f
        val baseY = h - 80f
        val stemLen = h * config.stemLengthFraction * foreshorten

        val headOffsetX = sin(stemAngle) * stemLen
        val headOffsetY = -cos(stemAngle) * stemLen
        val headX = baseX + headOffsetX
        val headY = baseY + headOffsetY
        val ctrl1X = baseX + headOffsetX * 0.15f
        val ctrl1Y = baseY - stemLen * 0.4f
        val ctrl2X = baseX + headOffsetX * 0.6f
        val ctrl2Y = baseY - stemLen * 0.85f + sin(sp * 1.3f) * 1.5f

        seeds.forEach { s ->
            if (s.attached && breathC >= s.detachThreshold) {
                val over = breathC - s.detachThreshold
                val p = (over * 0.0004f).coerceAtMost(0.018f) * (0.5f + breathN * 0.6f)
                if (Random.nextFloat() < p) s.release(0.7f + breathN * 0.6f)
            }
            s.update(
                dt = dt,
                t = now,
                breath = breathC,
                headX = headX,
                headY = headY,
                headTilt = stemAngle,
                wind = windStrength * (0.5f + breathN * 0.8f),
                gravity = 0.04f,
            )
        }

        drawStemAndLeaves(
            baseX = baseX, baseY = baseY,
            c1x = ctrl1X, c1y = ctrl1Y,
            c2x = ctrl2X, c2y = ctrl2Y,
            headX = headX, headY = headY,
            breathN = breathN, swayPhase = sp,
            p = palette, config = config,
        )
        drawSeedHead(
            headX = headX, headY = headY,
            stemAngle = stemAngle,
            p = palette, config = config, seeds = seeds,
        )
    }
}

private class Seed(
    val homeAngle: Float,
    val homeRadius: Float,
    val detachThreshold: Float,
) {
    var attached = true
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var rot = homeAngle + PI_F / 2f
    var vrot = 0f
    var life = 1f
    var scale = 1f

    val jitterPhase = Random.nextFloat() * TWO_PI
    val jitterAmp = 0.4f + Random.nextFloat() * 0.8f

    fun update(
        dt: Float,
        t: Float,
        breath: Float,
        headX: Float,
        headY: Float,
        headTilt: Float,
        wind: Float,
        gravity: Float,
    ) {
        if (attached) {
            val breathN = breath / 100f
            val tremble = breathN.pow(1.4f) * jitterAmp
            val wobbleX = sin(t * 4f + jitterPhase) * tremble * 1.6f
            val wobbleY = cos(t * 3.3f + jitterPhase) * tremble * 1.4f

            val depthScale = 1f - breathN * 0.18f
            val radius = homeRadius * depthScale * (1f + breathN * 0.08f)
            val angle = homeAngle + headTilt * 0.15f
            x = headX + cos(angle) * radius + wobbleX
            y = headY + sin(angle) * radius + wobbleY
            rot = angle + PI_F / 2f + sin(t * 3f + jitterPhase) * 0.15f
        } else {
            val turb = sin(t * 1.6f + jitterPhase) * 8f +
                cos(t * 2.4f + jitterPhase * 1.7f) * 6f
            vx += (wind + turb * 0.05f) * dt
            vy += gravity * dt
            vx *= 0.985f
            vy *= 0.985f
            x += vx * dt * 60f
            y += vy * dt * 60f
            rot += vrot * dt
            life -= dt * 0.18f
            scale = (life * 1.4f).coerceIn(0f, 1f)
        }
    }

    fun release(force: Float = 1f) {
        if (!attached) return
        attached = false
        val a = homeAngle
        val speed = (1.2f + Random.nextFloat() * 1.4f) * force
        vx = cos(a) * speed * 0.6f + (Random.nextFloat() - 0.3f) * 0.6f
        vy = sin(a) * speed * 0.6f - (0.6f + Random.nextFloat() * 0.8f) * force
        vrot = (Random.nextFloat() - 0.5f) * 4f
        life = 1f
    }
}

private fun DrawScope.drawStemAndLeaves(
    baseX: Float, baseY: Float,
    c1x: Float, c1y: Float,
    c2x: Float, c2y: Float,
    headX: Float, headY: Float,
    breathN: Float, swayPhase: Float,
    p: DandelionPalette,
    config: DandelionConfig,
) {
    drawOval(
        color = Color.Black.copy(alpha = 0.08f),
        topLeft = Offset(baseX - 28f, baseY - 1f),
        size = Size(56f, 10f),
    )

    val stem = Path().apply {
        moveTo(baseX, baseY)
        cubicTo(c1x, c1y, c2x, c2y, headX, headY)
    }
    drawPath(
        path = stem,
        color = p.stem,
        style = Stroke(width = config.stemWidth, cap = StrokeCap.Round),
    )

    val highlight = Path().apply {
        moveTo(baseX - 2f, baseY)
        cubicTo(c1x - 2f, c1y, c2x - 2f, c2y, headX - 2f, headY)
    }
    drawPath(
        path = highlight,
        color = p.seedFluff.copy(alpha = 0.25f),
        style = Stroke(width = 1.6f, cap = StrokeCap.Round),
    )

    val leafSway = sin(swayPhase * 1.1f) * (1f + breathN * 4f)
    config.leaves.forEach { leaf ->
        drawLeaf(
            x = baseX + leaf.offsetX,
            y = baseY + leaf.offsetY,
            angleDeg = leaf.baseAngleDeg + leafSway * leaf.swayCoefficient,
            len = leaf.length,
            teeth = config.leafTeeth,
            toothDepth = config.leafToothDepth,
            p = p,
        )
    }
}

private fun DrawScope.drawLeaf(
    x: Float, y: Float, angleDeg: Float, len: Float,
    teeth: Int,
    toothDepth: Float,
    p: DandelionPalette,
) {
    withTransform({
        translate(x, y)
        rotate(angleDeg, pivot = Offset.Zero)
    }) {
        val path = Path().apply {
            moveTo(0f, 0f)
            for (i in 0..teeth) {
                val tt = i.toFloat() / teeth
                val px = tt * len
                val py = -sin(tt * PI_F) * toothDepth - (i % 2) * 3f
                lineTo(px, py)
            }
            lineTo(len, 0f)
            for (i in teeth downTo 0) {
                val tt = i.toFloat() / teeth
                val px = tt * len
                val py = sin(tt * PI_F) * toothDepth + (i % 2) * 3f
                lineTo(px, py)
            }
            close()
        }
        drawPath(path = path, color = p.leaf)
        drawLine(
            color = p.leafDark,
            start = Offset(0f, 0f),
            end = Offset(len * 0.95f, 0f),
            strokeWidth = 1.2f,
        )
    }
}

private fun DrawScope.drawSeedHead(
    headX: Float, headY: Float, stemAngle: Float,
    p: DandelionPalette, config: DandelionConfig, seeds: List<Seed>,
) {
    withTransform({
        translate(headX, headY)
        rotate(stemAngle * DEG, pivot = Offset.Zero)
    }) {
        drawOval(
            color = p.receptacle,
            topLeft = Offset(-14f, -9f),
            size = Size(28f, 18f),
        )
    }

    var attachedCount = 0
    for (s in seeds) if (s.attached) attachedCount++
    val fullness = attachedCount.toFloat() / seeds.size
    if (fullness > 0.05f) {
        val haloR = config.haloRadius * fullness.pow(0.6f)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to p.seedFluff.copy(alpha = 0.75f),
                    0.5f to p.seedFluff.copy(alpha = 0.4f),
                    1f to p.seedFluff.copy(alpha = 0f),
                ),
                center = Offset(headX, headY),
                radius = haloR,
            ),
            radius = haloR,
            center = Offset(headX, headY),
        )
    }

    for (s in seeds) {
        if (s.attached) {
            drawLine(
                color = p.seedStalk.copy(alpha = 0.55f),
                start = Offset(headX, headY),
                end = Offset(s.x, s.y),
                strokeWidth = 0.8f,
            )
            drawSeed(s, p, config, floating = false)
        }
    }
    for (s in seeds) {
        if (!s.attached && s.life > 0f) drawSeed(s, p, config, floating = true)
    }
}

private fun DrawScope.drawSeed(s: Seed, p: DandelionPalette, config: DandelionConfig, floating: Boolean) {
    if (s.scale <= 0f) return
    val alpha = if (floating) s.life.coerceIn(0f, 1f) else 1f
    withTransform({
        translate(s.x, s.y)
        rotate(s.rot * DEG, pivot = Offset.Zero)
        scale(s.scale, s.scale, pivot = Offset.Zero)
    }) {
        val fluffR = config.seedFluffRadius
        drawCircle(
            color = p.seedFluff.copy(alpha = 0.7f * alpha),
            radius = fluffR * 0.85f,
            center = Offset.Zero,
        )
        val spokes = config.seedSpokes
        for (i in 0 until spokes) {
            val a = i.toFloat() / spokes * TWO_PI
            drawLine(
                color = p.seedFluff.copy(alpha = alpha),
                start = Offset.Zero,
                end = Offset(cos(a) * fluffR, sin(a) * fluffR),
                strokeWidth = 1.6f,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(
            color = p.seedFluff.copy(alpha = alpha),
            radius = 2f,
            center = Offset.Zero,
        )
        drawOval(
            color = p.seedCore.copy(alpha = alpha),
            topLeft = Offset(-1.6f, fluffR * 0.3f - 3.5f),
            size = Size(3.2f, 7f),
        )
    }
}
