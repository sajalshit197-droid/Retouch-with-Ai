package com.example.data

import android.graphics.*
import kotlin.math.cos
import kotlin.math.sin

data class SamplePhotoItem(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val defaultPrompt: String
)

object SamplePhotoProvider {

    val SAMPLES = listOf(
        SamplePhotoItem(
            id = "sample_portrait",
            title = "Studio Portrait",
            category = "Portrait",
            description = "Natural lighting, warm skin tones, neutral backdrop",
            defaultPrompt = "High fashion beauty portrait with soft natural lighting"
        ),
        SamplePhotoItem(
            id = "sample_headshot",
            title = "Executive Headshot",
            category = "Professional",
            description = "Clean corporate look with subtle depth of field",
            defaultPrompt = "Professional executive headshot with crisp focus"
        ),
        SamplePhotoItem(
            id = "sample_fashion",
            title = "Urban Fashion",
            category = "Streetwear",
            description = "Moody city background, vibrant styling",
            defaultPrompt = "Stylish urban fashion portrait with city bokeh"
        ),
        SamplePhotoItem(
            id = "sample_product",
            title = "Aesthetic Product",
            category = "E-Commerce",
            description = "Minimalist luxury product on pedestal",
            defaultPrompt = "Clean product photography with smooth shadows"
        )
    )

    fun createSampleBitmap(id: String, width: Int = 800, height: Int = 1000): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        when (id) {
            "sample_portrait" -> drawPortraitSample(canvas, width, height)
            "sample_headshot" -> drawHeadshotSample(canvas, width, height)
            "sample_fashion" -> drawFashionSample(canvas, width, height)
            "sample_product" -> drawProductSample(canvas, width, height)
            else -> drawPortraitSample(canvas, width, height)
        }

        return bitmap
    }

    private fun drawPortraitSample(canvas: Canvas, w: Int, h: Int) {
        // Studio Neutral Textured Backdrop
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                Color.parseColor("#E2D9D2"),
                Color.parseColor("#B0A69D"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Soft studio spotlight behind subject
        val spotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.5f, h * 0.4f, w * 0.45f,
                Color.argb(120, 255, 250, 240),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(w * 0.5f, h * 0.4f, w * 0.45f, spotPaint)

        // Torso / Shoulders (Warm terracotta fabric)
        val clothPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, h * 0.65f, 0f, h.toFloat(),
                Color.parseColor("#C26D53"),
                Color.parseColor("#8E4731"),
                Shader.TileMode.CLAMP
            )
        }
        val torsoPath = Path().apply {
            moveTo(w * 0.15f, h.toFloat())
            quadTo(w * 0.32f, h * 0.68f, w * 0.42f, h * 0.66f)
            lineTo(w * 0.58f, h * 0.66f)
            quadTo(w * 0.68f, h * 0.68f, w * 0.85f, h.toFloat())
            close()
        }
        canvas.drawPath(torsoPath, clothPaint)

        // Neck
        val neckPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0A98B")
        }
        canvas.drawRect(w * 0.43f, h * 0.52f, w * 0.57f, h * 0.68f, neckPaint)

        // Neck shadow
        val neckShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 70, 30, 10)
        }
        canvas.drawRect(w * 0.43f, h * 0.52f, w * 0.57f, h * 0.57f, neckShadow)

        // Face Oval (Warm radiant skin tone)
        val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.48f, h * 0.38f, w * 0.28f,
                Color.parseColor("#F7CEB6"),
                Color.parseColor("#DE9E7F"),
                Shader.TileMode.CLAMP
            )
        }
        val faceOval = RectF(w * 0.32f, h * 0.24f, w * 0.68f, h * 0.58f)
        canvas.drawOval(faceOval, facePaint)

        // Hair (Flowing chestnut brown)
        val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, h * 0.12f, 0f, h * 0.70f,
                Color.parseColor("#38231A"),
                Color.parseColor("#1B110B"),
                Shader.TileMode.CLAMP
            )
        }
        val hairPath = Path().apply {
            moveTo(w * 0.26f, h * 0.62f)
            cubicTo(w * 0.20f, h * 0.35f, w * 0.26f, h * 0.16f, w * 0.50f, h * 0.15f)
            cubicTo(w * 0.74f, h * 0.16f, w * 0.80f, h * 0.35f, w * 0.74f, h * 0.62f)
            cubicTo(w * 0.68f, h * 0.45f, w * 0.64f, h * 0.26f, w * 0.50f, h * 0.26f)
            cubicTo(w * 0.36f, h * 0.26f, w * 0.32f, h * 0.45f, w * 0.26f, h * 0.62f)
            close()
        }
        canvas.drawPath(hairPath, hairPaint)

        // Eyes (Subtle elegant curves)
        val featurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3B2219")
            strokeWidth = w * 0.008f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        // Left Eye
        canvas.drawArc(RectF(w * 0.39f, h * 0.38f, w * 0.46f, h * 0.42f), 190f, 160f, false, featurePaint)
        // Right Eye
        canvas.drawArc(RectF(w * 0.54f, h * 0.38f, w * 0.61f, h * 0.42f), 190f, 160f, false, featurePaint)

        // Eyebrows
        val browPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2D1B14")
            strokeWidth = w * 0.007f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(RectF(w * 0.38f, h * 0.35f, w * 0.47f, h * 0.38f), 200f, 140f, false, browPaint)
        canvas.drawArc(RectF(w * 0.53f, h * 0.35f, w * 0.62f, h * 0.38f), 200f, 140f, false, browPaint)

        // Nose line
        val nosePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 150, 80, 40)
            strokeWidth = w * 0.006f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(w * 0.50f, h * 0.41f, w * 0.49f, h * 0.47f, nosePaint)
        canvas.drawLine(w * 0.49f, h * 0.47f, w * 0.52f, h * 0.47f, nosePaint)

        // Lips (Natural rose)
        val lipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C85B58")
            style = Paint.Style.FILL
        }
        val lipPath = Path().apply {
            moveTo(w * 0.44f, h * 0.515f)
            quadTo(w * 0.50f, h * 0.508f, w * 0.56f, h * 0.515f)
            quadTo(w * 0.50f, h * 0.535f, w * 0.44f, h * 0.515f)
            close()
        }
        canvas.drawPath(lipPath, lipPaint)

        // Cheeks soft blush
        val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.38f, h * 0.44f, w * 0.08f,
                Color.argb(55, 235, 110, 100),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(w * 0.38f, h * 0.44f, w * 0.08f, blushPaint)
        val blushRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.62f, h * 0.44f, w * 0.08f,
                Color.argb(55, 235, 110, 100),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(w * 0.62f, h * 0.44f, w * 0.08f, blushRight)
    }

    private fun drawHeadshotSample(canvas: Canvas, w: Int, h: Int) {
        // Deep Navy / Slate Office Backdrop
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                Color.parseColor("#1E293B"),
                Color.parseColor("#0F172A"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Blurred light bokeh circles in background
        val bokehPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(35, 148, 163, 184)
        }
        canvas.drawCircle(w * 0.2f, h * 0.25f, w * 0.12f, bokehPaint)
        canvas.drawCircle(w * 0.85f, h * 0.35f, w * 0.16f, bokehPaint)

        // Navy Business Suit
        val suitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#172554")
        }
        val suitPath = Path().apply {
            moveTo(w * 0.10f, h.toFloat())
            lineTo(w * 0.35f, h * 0.65f)
            lineTo(w * 0.42f, h * 0.64f)
            lineTo(w * 0.42f, h.toFloat())
            lineTo(w * 0.10f, h.toFloat())
        }
        canvas.drawPath(suitPath, suitPaint)

        val rightSuitPath = Path().apply {
            moveTo(w * 0.90f, h.toFloat())
            lineTo(w * 0.65f, h * 0.65f)
            lineTo(w * 0.58f, h * 0.64f)
            lineTo(w * 0.58f, h.toFloat())
            lineTo(w * 0.90f, h.toFloat())
        }
        canvas.drawPath(rightSuitPath, suitPaint)

        // White Shirt V-neck
        val shirtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
        }
        val shirtPath = Path().apply {
            moveTo(w * 0.42f, h * 0.64f)
            lineTo(w * 0.58f, h * 0.64f)
            lineTo(w * 0.50f, h * 0.82f)
            close()
        }
        canvas.drawPath(shirtPath, shirtPaint)

        // Crimson Tie
        val tiePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#991B1B")
        }
        val tiePath = Path().apply {
            moveTo(w * 0.48f, h * 0.65f)
            lineTo(w * 0.52f, h * 0.65f)
            lineTo(w * 0.54f, h * 0.85f)
            lineTo(w * 0.50f, h * 0.88f)
            lineTo(w * 0.46f, h * 0.85f)
            close()
        }
        canvas.drawPath(tiePath, tiePaint)

        // Headshot Face
        val neckPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DF9D7A")
        }
        canvas.drawRect(w * 0.43f, h * 0.50f, w * 0.57f, h * 0.66f, neckPaint)

        val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.50f, h * 0.36f, w * 0.25f,
                Color.parseColor("#F6CEB4"),
                Color.parseColor("#D98F6C"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawOval(RectF(w * 0.34f, h * 0.22f, w * 0.66f, h * 0.56f), facePaint)

        // Clean Short Haircut
        val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1917")
        }
        val hairPath = Path().apply {
            moveTo(w * 0.33f, h * 0.35f)
            cubicTo(w * 0.30f, h * 0.20f, w * 0.40f, h * 0.14f, w * 0.50f, h * 0.14f)
            cubicTo(w * 0.60f, h * 0.14f, w * 0.70f, h * 0.20f, w * 0.67f, h * 0.35f)
            lineTo(w * 0.65f, h * 0.25f)
            cubicTo(w * 0.58f, h * 0.20f, w * 0.42f, h * 0.20f, w * 0.35f, h * 0.25f)
            close()
        }
        canvas.drawPath(hairPath, hairPaint)

        // Professional Smile & Eyes
        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            strokeWidth = w * 0.008f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(RectF(w * 0.40f, h * 0.36f, w * 0.46f, h * 0.39f), 200f, 140f, false, eyePaint)
        canvas.drawArc(RectF(w * 0.54f, h * 0.36f, w * 0.60f, h * 0.39f), 200f, 140f, false, eyePaint)

        // Confident smile
        val smilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#991B1B")
            style = Paint.Style.STROKE
            strokeWidth = w * 0.008f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(RectF(w * 0.44f, h * 0.46f, w * 0.56f, h * 0.51f), 20f, 140f, false, smilePaint)
    }

    private fun drawFashionSample(canvas: Canvas, w: Int, h: Int) {
        // Sunset Neon / Cyber Street Mood
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                Color.parseColor("#4C1D95"),
                Color.parseColor("#BE185D"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Neon Glow accents
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.8f, h * 0.2f, w * 0.35f,
                Color.argb(160, 244, 114, 182),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(w * 0.8f, h * 0.2f, w * 0.35f, glowPaint)

        // Denim / Streetwear Jacket
        val jacketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E3A8A")
        }
        val jacketPath = Path().apply {
            moveTo(w * 0.12f, h.toFloat())
            quadTo(w * 0.25f, h * 0.65f, w * 0.40f, h * 0.62f)
            lineTo(w * 0.60f, h * 0.62f)
            quadTo(w * 0.75f, h * 0.65f, w * 0.88f, h.toFloat())
            close()
        }
        canvas.drawPath(jacketPath, jacketPaint)

        // Hood / Yellow Beanie
        val beaniePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EAB308")
        }
        canvas.drawOval(RectF(w * 0.33f, h * 0.14f, w * 0.67f, h * 0.30f), beaniePaint)

        // Face
        val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.50f, h * 0.36f, w * 0.22f,
                Color.parseColor("#F5D0B5"),
                Color.parseColor("#D49673"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawOval(RectF(w * 0.35f, h * 0.24f, w * 0.65f, h * 0.55f), facePaint)

        // Cool Sunglasses
        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
        }
        val frameRect1 = RectF(w * 0.38f, h * 0.32f, w * 0.48f, h * 0.39f)
        val frameRect2 = RectF(w * 0.52f, h * 0.32f, w * 0.62f, h * 0.39f)
        canvas.drawRoundRect(frameRect1, 12f, 12f, glassPaint)
        canvas.drawRoundRect(frameRect2, 12f, 12f, glassPaint)
        canvas.drawRect(w * 0.48f, h * 0.34f, w * 0.52f, h * 0.36f, glassPaint)

        // Sunglasses Reflection Highlight
        val reflectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 244, 114, 182)
            strokeWidth = 6f
        }
        canvas.drawLine(w * 0.40f, h * 0.38f, w * 0.45f, h * 0.33f, reflectPaint)
        canvas.drawLine(w * 0.54f, h * 0.38f, w * 0.59f, h * 0.33f, reflectPaint)
    }

    private fun drawProductSample(canvas: Canvas, w: Int, h: Int) {
        // Minimalist Luxury Studio Background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                Color.parseColor("#F1F5F9"),
                Color.parseColor("#CBD5E1"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Cylindrical Pedestal
        val pedestalTop = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
        }
        val pedestalBase = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                w * 0.25f, 0f, w * 0.75f, 0f,
                Color.parseColor("#94A3B8"),
                Color.parseColor("#CBD5E1"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(w * 0.25f, h * 0.72f, w * 0.75f, h * 0.90f, pedestalBase)
        canvas.drawOval(RectF(w * 0.25f, h * 0.68f, w * 0.75f, h * 0.76f), pedestalTop)

        // Pedestal soft shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 50, 60, 80)
        }
        canvas.drawOval(RectF(w * 0.20f, h * 0.88f, w * 0.80f, h * 0.93f), shadowPaint)

        // Perfume Bottle Glass Body
        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                w * 0.35f, 0f, w * 0.65f, 0f,
                Color.parseColor("#FDE047"),
                Color.parseColor("#F59E0B"),
                Shader.TileMode.CLAMP
            )
        }
        val bottleRect = RectF(w * 0.36f, h * 0.42f, w * 0.64f, h * 0.72f)
        canvas.drawRoundRect(bottleRect, 18f, 18f, glassPaint)

        // Golden Cap
        val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                w * 0.42f, 0f, w * 0.58f, 0f,
                Color.parseColor("#FBBF24"),
                Color.parseColor("#B45309"),
                Shader.TileMode.CLAMP
            )
        }
        val capRect = RectF(w * 0.43f, h * 0.30f, w * 0.57f, h * 0.42f)
        canvas.drawRoundRect(capRect, 8f, 8f, capPaint)

        // Glass Highlight Sheen
        val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(140, 255, 255, 255)
        }
        canvas.drawRoundRect(RectF(w * 0.38f, h * 0.44f, w * 0.41f, h * 0.70f), 4f, 4f, sheenPaint)

        // Elegant Minimal Label
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        canvas.drawRoundRect(RectF(w * 0.42f, h * 0.52f, w * 0.58f, h * 0.63f), 4f, 4f, labelPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = w * 0.025f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("LUMINA", w * 0.50f, h * 0.57f, textPaint)
        textPaint.textSize = w * 0.016f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("EAU DE PARFUM", w * 0.50f, h * 0.60f, textPaint)
    }
}
