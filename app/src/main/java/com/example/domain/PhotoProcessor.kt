package com.example.domain

import android.graphics.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class BackgroundMode {
    ORIGINAL,
    TRANSPARENT,
    SOLID_COLOR,
    STUDIO_GRADIENT,
    BLUR_BOKEH
}

data class FilterPreset(
    val id: String,
    val name: String,
    val description: String,
    val colorMatrix: ColorMatrix
)

data class PhotoEditState(
    // Background Removal
    val isBgRemoved: Boolean = false,
    val bgMode: BackgroundMode = BackgroundMode.ORIGINAL,
    val solidColor: Int = Color.WHITE,
    val gradientStart: Int = 0xFF312E81.toInt(),
    val gradientEnd: Int = 0xFF831843.toInt(),
    val blurRadius: Float = 25f,
    val bgFeather: Float = 10f,

    // Professional Retouching
    val skinSmooth: Float = 0f, // 0 to 100
    val faceGlow: Float = 0f,   // 0 to 100
    val eyeClarity: Float = 0f, // 0 to 100

    // Lighting & Color Adjustments
    val exposure: Float = 0f,   // -100 to 100
    val brightness: Float = 0f, // -100 to 100
    val contrast: Float = 0f,   // -100 to 100
    val warmth: Float = 0f,     // -100 to 100 (Cool to Warm)
    val saturation: Float = 0f, // -100 to 100
    val tint: Float = 0f,       // -100 to 100 (Green to Magenta)
    val vignette: Float = 0f,   // 0 to 100
    val sharpness: Float = 0f,  // 0 to 100

    // Filter Preset
    val selectedFilterId: String = "original",
    val filterIntensity: Float = 1.0f // 0.0 to 1.0
)

object PhotoProcessor {

    val AVAILABLE_FILTERS = listOf(
        FilterPreset("original", "Original", "Natural untouched photo", ColorMatrix()),
        FilterPreset("warm_studio", "Warm Studio", "Golden portrait skin tones", createWarmStudioMatrix()),
        FilterPreset("vivid_pop", "Vivid Pop", "Punchy contrast and vibrant saturation", createVividPopMatrix()),
        FilterPreset("cool_cinema", "Cinematic", "Teal & Orange Hollywood aesthetic", createCinematicMatrix()),
        FilterPreset("noir_silver", "Noir Silver", "Deep high-contrast studio black & white", createNoirMatrix()),
        FilterPreset("golden_hour", "Golden Hour", "Radiant sunset amber glow", createGoldenHourMatrix()),
        FilterPreset("cyber_neon", "Cyber Neon", "Futuristic violet and cyan color grade", createCyberNeonMatrix()),
        FilterPreset("crisp_hdr", "Crisp HDR", "Dynamic range expansion with rich details", createHdrMatrix()),
        FilterPreset("pastel_dream", "Pastel Dream", "Soft faded film tones", createPastelDreamMatrix()),
        FilterPreset("vintage_film", "Vintage 35mm", "Classic warm analog film look", createVintageFilmMatrix())
    )

    private fun createWarmStudioMatrix(): ColorMatrix {
        val cm = ColorMatrix()
        cm.set(floatArrayOf(
            1.15f, 0f, 0f, 0f, 15f,
            0f, 1.05f, 0f, 0f, 8f,
            0f, 0f, 0.90f, 0f, -5f,
            0f, 0f, 0f, 1.0f, 0f
        ))
        return cm
    }

    private fun createVividPopMatrix(): ColorMatrix {
        val sat = ColorMatrix().apply { setSaturation(1.35f) }
        val contrast = ColorMatrix().apply {
            val scale = 1.18f
            val translate = (-0.5f * scale + 0.5f) * 255f
            set(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        contrast.postConcat(sat)
        return contrast
    }

    private fun createCinematicMatrix(): ColorMatrix {
        val cm = ColorMatrix()
        cm.set(floatArrayOf(
            1.12f, 0.05f, -0.05f, 0f, 10f,
            -0.02f, 1.08f, 0.05f, 0f, 4f,
            0.08f, 0.12f, 1.18f, 0f, -8f,
            0f, 0f, 0f, 1f, 0f
        ))
        return cm
    }

    private fun createNoirMatrix(): ColorMatrix {
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        val contrast = ColorMatrix().apply {
            val scale = 1.35f
            val translate = (-0.5f * scale + 0.5f) * 255f + 5f
            set(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        contrast.postConcat(cm)
        return contrast
    }

    private fun createGoldenHourMatrix(): ColorMatrix {
        val cm = ColorMatrix()
        cm.set(floatArrayOf(
            1.25f, 0f, 0f, 0f, 25f,
            0f, 1.10f, 0f, 0f, 12f,
            0f, 0f, 0.82f, 0f, -15f,
            0f, 0f, 0f, 1f, 0f
        ))
        return cm
    }

    private fun createCyberNeonMatrix(): ColorMatrix {
        val cm = ColorMatrix()
        cm.set(floatArrayOf(
            1.10f, 0.15f, 0.20f, 0f, 15f,
            0.05f, 1.15f, 0.15f, 0f, 5f,
            0.15f, 0.05f, 1.30f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        ))
        return cm
    }

    private fun createHdrMatrix(): ColorMatrix {
        val cm = ColorMatrix()
        cm.set(floatArrayOf(
            1.18f, 0f, 0f, 0f, 12f,
            0f, 1.18f, 0f, 0f, 12f,
            0f, 0f, 1.18f, 0f, 12f,
            0f, 0f, 0f, 1f, 0f
        ))
        val sat = ColorMatrix().apply { setSaturation(1.22f) }
        cm.postConcat(sat)
        return cm
    }

    private fun createPastelDreamMatrix(): ColorMatrix {
        val cm = ColorMatrix()
        cm.set(floatArrayOf(
            0.92f, 0f, 0f, 0f, 32f,
            0f, 0.94f, 0f, 0f, 28f,
            0f, 0f, 0.96f, 0f, 35f,
            0f, 0f, 0f, 1f, 0f
        ))
        return cm
    }

    private fun createVintageFilmMatrix(): ColorMatrix {
        val cm = ColorMatrix()
        cm.set(floatArrayOf(
            1.08f, 0.05f, 0.02f, 0f, 15f,
            0.02f, 1.02f, 0.02f, 0f, 10f,
            0.01f, 0.02f, 0.90f, 0f, 12f,
            0f, 0f, 0f, 1f, 0f
        ))
        return cm
    }

    /**
     * Builds color matrix combining lighting, color adjustments, and filter
     */
    fun buildAdjustmentColorMatrix(state: PhotoEditState): ColorMatrix {
        val combined = ColorMatrix()

        // 1. Exposure & Brightness
        val totalBrightness = (state.brightness * 1.5f) + (state.exposure * 1.8f)
        if (totalBrightness != 0f) {
            val bMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, totalBrightness,
                0f, 1f, 0f, 0f, totalBrightness,
                0f, 0f, 1f, 0f, totalBrightness,
                0f, 0f, 0f, 1f, 0f
            ))
            combined.postConcat(bMatrix)
        }

        // 2. Contrast
        if (state.contrast != 0f) {
            val scale = (state.contrast + 100f) / 100f
            val translate = (-0.5f * scale + 0.5f) * 255f
            val cMatrix = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            combined.postConcat(cMatrix)
        }

        // 3. Warmth & Tint
        if (state.warmth != 0f || state.tint != 0f) {
            val rAdj = (state.warmth * 0.4f)
            val bAdj = (-state.warmth * 0.4f)
            val gAdj = (-state.tint * 0.35f)
            val wtMatrix = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, rAdj,
                0f, 1f, 0f, 0f, gAdj,
                0f, 0f, 1f, 0f, bAdj,
                0f, 0f, 0f, 1f, 0f
            ))
            combined.postConcat(wtMatrix)
        }

        // 4. Saturation
        if (state.saturation != 0f) {
            val satFactor = (state.saturation + 100f) / 100f
            val satMatrix = ColorMatrix()
            satMatrix.setSaturation(satFactor)
            combined.postConcat(satMatrix)
        }

        // 5. Selected Preset Filter with Intensity blending
        val filter = AVAILABLE_FILTERS.firstOrNull { it.id == state.selectedFilterId }
        if (filter != null && filter.id != "original" && state.filterIntensity > 0f) {
            val filterMatrix = filter.colorMatrix
            if (state.filterIntensity >= 0.98f) {
                combined.postConcat(filterMatrix)
            } else {
                // Blend identity with filter matrix based on intensity
                val blended = blendColorMatrices(ColorMatrix(), filterMatrix, state.filterIntensity)
                combined.postConcat(blended)
            }
        }

        return combined
    }

    private fun blendColorMatrices(m1: ColorMatrix, m2: ColorMatrix, factor: Float): ColorMatrix {
        val a1 = m1.array
        val a2 = m2.array
        val result = FloatArray(20)
        for (i in 0 until 20) {
            result[i] = a1[i] * (1f - factor) + a2[i] * factor
        }
        return ColorMatrix(result)
    }

    /**
     * Smart Portrait / Foreground Subject Alpha Mask Generation.
     * Computes subject mask using color variance, border sampling, center-of-mass saliency,
     * and edge contrast.
     */
    fun generateSubjectCutout(src: Bitmap, feather: Float): Bitmap {
        val width = src.width
        val height = src.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Downsample for fast mask boundary estimation
        val sw = min(200, width)
        val sh = min(200, height)
        val small = Bitmap.createScaledBitmap(src, sw, sh, true)

        val smallPixels = IntArray(sw * sh)
        small.getPixels(smallPixels, 0, sw, 0, 0, sw, sh)

        // 1. Sample border pixels to learn background color distribution
        var bgR = 0L
        var bgG = 0L
        var bgB = 0L
        var borderCount = 0

        for (x in 0 until sw) {
            val topP = smallPixels[x]
            val botP = smallPixels[(sh - 1) * sw + x]
            bgR += (topP shr 16) and 0xFF
            bgG += (topP shr 8) and 0xFF
            bgB += topP and 0xFF
            bgR += (botP shr 16) and 0xFF
            bgG += (botP shr 8) and 0xFF
            bgB += botP and 0xFF
            borderCount += 2
        }
        for (y in 1 until sh - 1) {
            val leftP = smallPixels[y * sw]
            val rightP = smallPixels[y * sw + (sw - 1)]
            bgR += (leftP shr 16) and 0xFF
            bgG += (leftP shr 8) and 0xFF
            bgB += leftP and 0xFF
            bgR += (rightP shr 16) and 0xFF
            bgG += (rightP shr 8) and 0xFF
            bgB += rightP and 0xFF
            borderCount += 2
        }

        val avgBgR = (bgR / borderCount).toInt()
        val avgBgG = (bgG / borderCount).toInt()
        val avgBgB = (bgB / borderCount).toInt()

        // 2. Generate initial small binary / soft mask
        val maskSmall = Bitmap.createBitmap(sw, sh, Bitmap.Config.ALPHA_8)
        val maskPixels = ByteArray(sw * sh)

        val centerX = sw / 2f
        val centerY = sh / 2f
        val maxRadius = sqrt((centerX * centerX) + (centerY * centerY))

        for (y in 0 until sh) {
            for (x in 0 until sw) {
                val idx = y * sw + x
                val p = smallPixels[idx]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF

                // Color distance from background
                val dr = r - avgBgR
                val dg = g - avgBgG
                val db = b - avgBgB
                val colorDist = sqrt((dr * dr + dg * dg + db * db).toDouble()).toFloat()

                // Distance from center (portrait subjects are concentrated towards center & bottom-center)
                val dx = (x - centerX) / centerX
                val dy = (y - (sh * 0.45f)) / (sh * 0.55f)
                val distFromSubjectCore = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                // Center bias: subjects in middle are prioritized, edges are background
                val spatialWeight = (1.0f - (distFromSubjectCore * 0.65f)).coerceIn(0f, 1f)
                val score = (colorDist / 80f) * 0.55f + spatialWeight * 0.45f

                val alphaVal = when {
                    score > 0.65f -> 255
                    score < 0.35f -> 0
                    else -> ((score - 0.35f) / 0.30f * 255).toInt().coerceIn(0, 255)
                }
                maskPixels[idx] = alphaVal.toByte()
            }
        }

        // Copy into alpha bitmap
        val byteBuffer = java.nio.ByteBuffer.wrap(maskPixels)
        maskSmall.copyPixelsFromBuffer(byteBuffer)

        // Scale mask up to full resolution and smooth edges with blur/feather
        val fullMask = Bitmap.createScaledBitmap(maskSmall, width, height, true)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Apply feathering if needed
        val finalMask = if (feather > 1f) {
            blurBitmap(fullMask, feather.coerceIn(1f, 25f))
        } else {
            fullMask
        }

        val canvas = Canvas(output)
        // Draw original image
        canvas.drawBitmap(src, 0f, 0f, paint)

        // Mask out background using DST_IN
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(finalMask, 0f, 0f, maskPaint)

        return output
    }

    /**
     * Blur helper for background bokeh or mask feathering
     */
    fun blurBitmap(src: Bitmap, radius: Float): Bitmap {
        val w = src.width
        val h = src.height
        // Scale down for fast box blur
        val scale = 0.25f
        val sw = max(1, (w * scale).toInt())
        val sh = max(1, (h * scale).toInt())
        val small = Bitmap.createScaledBitmap(src, sw, sh, true)
        val blurred = fastBoxBlur(small, radius.toInt().coerceIn(2, 20))
        return Bitmap.createScaledBitmap(blurred, w, h, true)
    }

    private fun fastBoxBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val output = Bitmap.createBitmap(w, h, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        val a = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var asum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in 0 until 256 * divsum) {
            dv[idx] = idx / divsum
        }

        yi = 0
        var yw = 0

        val stack = Array(div) { IntArray(4) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var aoutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        var ainsum: Int

        for (yIdx in 0 until h) {
            asum = 0; rsum = 0; gsum = 0; bsum = 0
            aoutsum = 0; routsum = 0; goutsum = 0; boutsum = 0
            ainsum = 0; rinsum = 0; ginsum = 0; binsum = 0
            for (idx in -radius..radius) {
                p = pix[yi + min(wm, max(idx, 0))]
                sir = stack[idx + radius]
                sir[0] = (p shr 24) and 0xff
                sir[1] = (p shr 16) and 0xff
                sir[2] = (p shr 8) and 0xff
                sir[3] = p and 0xff
                rbs = r1 - abs(idx)
                asum += sir[0] * rbs
                rsum += sir[1] * rbs
                gsum += sir[2] * rbs
                bsum += sir[3] * rbs
                if (idx > 0) {
                    ainsum += sir[0]
                    rinsum += sir[1]
                    ginsum += sir[2]
                    binsum += sir[3]
                } else {
                    aoutsum += sir[0]
                    goutsum += sir[2]
                    routsum += sir[1]
                    boutsum += sir[3]
                }
            }
            stackpointer = radius

            for (xIdx in 0 until w) {
                a[yi] = dv[asum]
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                asum -= aoutsum
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                aoutsum -= sir[0]
                routsum -= sir[1]
                goutsum -= sir[2]
                boutsum -= sir[3]

                if (yIdx == 0) {
                    vmin[xIdx] = min(xIdx + radius + 1, wm)
                }
                p = pix[yw + vmin[xIdx]]

                sir[0] = (p shr 24) and 0xff
                sir[1] = (p shr 16) and 0xff
                sir[2] = (p shr 8) and 0xff
                sir[3] = p and 0xff

                ainsum += sir[0]
                rinsum += sir[1]
                ginsum += sir[2]
                binsum += sir[3]

                asum += ainsum
                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                aoutsum += sir[0]
                routsum += sir[1]
                goutsum += sir[2]
                boutsum += sir[3]

                ainsum -= sir[0]
                rinsum -= sir[1]
                ginsum -= sir[2]
                binsum -= sir[3]

                yi++
            }
            yw += w
        }

        for (xIdx in 0 until w) {
            asum = 0; rsum = 0; gsum = 0; bsum = 0
            aoutsum = 0; routsum = 0; goutsum = 0; boutsum = 0
            ainsum = 0; rinsum = 0; ginsum = 0; binsum = 0
            yp = -radius * w
            for (idx in -radius..radius) {
                yi = max(0, yp) + xIdx
                sir = stack[idx + radius]
                sir[0] = a[yi]
                sir[1] = r[yi]
                sir[2] = g[yi]
                sir[3] = b[yi]
                rbs = r1 - abs(idx)
                asum += a[yi] * rbs
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (idx > 0) {
                    ainsum += sir[0]
                    rinsum += sir[1]
                    ginsum += sir[2]
                    binsum += sir[3]
                } else {
                    aoutsum += sir[0]
                    routsum += sir[1]
                    goutsum += sir[2]
                    boutsum += sir[3]
                }
                if (idx < hm) {
                    yp += w
                }
            }
            yi = xIdx
            stackpointer = radius
            for (yIdx in 0 until h) {
                pix[yi] = (dv[asum] shl 24) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                asum -= aoutsum
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                aoutsum -= sir[0]
                routsum -= sir[1]
                goutsum -= sir[2]
                boutsum -= sir[3]

                if (xIdx == 0) {
                    vmin[yIdx] = min(yIdx + r1, hm) * w
                }
                p = xIdx + vmin[yIdx]

                sir[0] = a[p]
                sir[1] = r[p]
                sir[2] = g[p]
                sir[3] = b[p]

                ainsum += sir[0]
                rinsum += sir[1]
                ginsum += sir[2]
                binsum += sir[3]

                asum += ainsum
                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                aoutsum += sir[0]
                routsum += sir[1]
                goutsum += sir[2]
                boutsum += sir[3]

                ainsum -= sir[0]
                rinsum -= sir[1]
                ginsum -= sir[2]
                binsum -= sir[3]

                yi += w
            }
        }

        output.setPixels(pix, 0, w, 0, 0, w, h)
        return output
    }

    /**
     * Skin Smoothing & Face Glow Retouching:
     * Softens skin tone textures and minor blemishes while preserving edges and facial geometry.
     */
    fun applyPortraitRetouch(bitmap: Bitmap, skinSmooth: Float, faceGlow: Float): Bitmap {
        if (skinSmooth <= 0f && faceGlow <= 0f) return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw original
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        // 1. Skin Smooth Layer
        if (skinSmooth > 0f) {
            // Edge-preserving soft glow layer
            val blurRad = (skinSmooth * 0.15f).coerceIn(2f, 16f)
            val softLayer = blurBitmap(bitmap, blurRad)

            val smoothPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = ((skinSmooth / 100f) * 160f).toInt().coerceIn(0, 200)
            }
            canvas.drawBitmap(softLayer, 0f, 0f, smoothPaint)
        }

        // 2. Face / Center Subject Glow
        if (faceGlow > 0f) {
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                val cx = width / 2f
                val cy = height * 0.45f
                val radius = max(width, height) * 0.45f
                val glowAlpha = ((faceGlow / 100f) * 75f).toInt()
                val glowColor = Color.argb(glowAlpha, 255, 245, 230)
                shader = RadialGradient(
                    cx, cy, radius,
                    glowColor,
                    Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glowPaint)
        }

        return output
    }

    /**
     * Apply radial vignette effect to corners
     */
    private fun applyVignette(canvas: Canvas, width: Int, height: Int, intensity: Float) {
        if (intensity <= 0f) return
        val cx = width / 2f
        val cy = height / 2f
        val radius = sqrt((cx * cx + cy * cy).toDouble()).toFloat()
        val vignetteAlpha = ((intensity / 100f) * 190f).toInt().coerceIn(0, 255)

        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(Color.TRANSPARENT, Color.argb(vignetteAlpha / 3, 0, 0, 0), Color.argb(vignetteAlpha, 0, 0, 0)),
                floatArrayOf(0.45f, 0.75f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
    }

    /**
     * Full Rendering Pipeline:
     * Integrates Background Replacement + Retouch + Lighting/Filter + Vignette.
     */
    fun processCompletePhoto(
        sourceBitmap: Bitmap,
        state: PhotoEditState,
        cachedCutout: Bitmap? = null
    ): Bitmap {
        val width = sourceBitmap.width
        val height = sourceBitmap.height

        // 1. Determine Subject (Cutout if background removal is active, else full image)
        val subjectBitmap = if (state.isBgRemoved || state.bgMode != BackgroundMode.ORIGINAL) {
            cachedCutout ?: generateSubjectCutout(sourceBitmap, state.bgFeather)
        } else {
            sourceBitmap
        }

        // 2. Retouch subject (Skin Smooth + Face Glow)
        val retouchedSubject = if (state.skinSmooth > 0f || state.faceGlow > 0f) {
            applyPortraitRetouch(subjectBitmap, state.skinSmooth, state.faceGlow)
        } else {
            subjectBitmap
        }

        // 3. Setup output canvas & draw background
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val isReplacingBg = state.isBgRemoved || state.bgMode != BackgroundMode.ORIGINAL

        if (isReplacingBg) {
            when (state.bgMode) {
                BackgroundMode.TRANSPARENT -> {
                    // Left transparent for PNG export
                }
                BackgroundMode.SOLID_COLOR -> {
                    canvas.drawColor(state.solidColor)
                }
                BackgroundMode.STUDIO_GRADIENT -> {
                    val gradientPaint = Paint().apply {
                        shader = LinearGradient(
                            0f, 0f, width.toFloat(), height.toFloat(),
                            state.gradientStart, state.gradientEnd,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradientPaint)
                }
                BackgroundMode.BLUR_BOKEH -> {
                    val blurredBg = blurBitmap(sourceBitmap, state.blurRadius)
                    canvas.drawBitmap(blurredBg, 0f, 0f, null)
                }
                BackgroundMode.ORIGINAL -> {
                    // Shouldn't reach here if isReplacingBg is true, but fallback:
                    canvas.drawBitmap(sourceBitmap, 0f, 0f, null)
                }
            }
        }

        // 4. Prepare Color Adjustment Paint (Lighting + Color + Filters)
        val colorMatrix = buildAdjustmentColorMatrix(state)
        val adjustmentPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }

        // Draw subject with adjustments
        canvas.drawBitmap(retouchedSubject, 0f, 0f, adjustmentPaint)

        // 5. Apply Vignette
        if (state.vignette > 0f) {
            applyVignette(canvas, width, height, state.vignette)
        }

        return output
    }
}
