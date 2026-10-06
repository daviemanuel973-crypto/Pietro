package com.example.visual

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.model.CoverSettings
import com.example.model.CoverTemplate
import java.io.File
import java.io.FileOutputStream

object CoverArtGenerator {

    /**
     * Renders a high-resolution cover image according to the CoverSettings and aspect ratio.
     * Dimensions:
     * - "16:9" -> 1920x1080
     * - "1:1"  -> 1080x1080
     * - "9:16" -> 1080x1920
     */
    suspend fun generateCoverBitmap(
        context: Context,
        settings: CoverSettings,
        targetWidth: Int = 1920,
        targetHeight: Int = 1080
    ): Bitmap = withContext(Dispatchers.Default) {
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Background drawing based on template
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (settings.template) {
            CoverTemplate.NEON -> {
                val gradient = LinearGradient(
                    0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(),
                    intArrayOf(Color.parseColor("#090A0F"), Color.parseColor("#180E29"), Color.parseColor("#002830")),
                    null, Shader.TileMode.CLAMP
                )
                bgPaint.shader = gradient
                canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), bgPaint)

                // Neon glow ring or accent line
                val neonGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor(settings.primaryColorHex)
                    strokeWidth = 14f
                    style = Paint.Style.STROKE
                    alpha = 180
                }
                canvas.drawCircle(targetWidth * 0.5f, targetHeight * 0.42f, targetHeight * 0.28f, neonGlowPaint)
            }
            CoverTemplate.RAP -> {
                val gradient = LinearGradient(
                    0f, 0f, 0f, targetHeight.toFloat(),
                    intArrayOf(Color.parseColor("#121212"), Color.parseColor("#1F140E"), Color.parseColor("#080808")),
                    null, Shader.TileMode.CLAMP
                )
                bgPaint.shader = gradient
                canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), bgPaint)

                // Gold border accent
                val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FFB300")
                    strokeWidth = 8f
                    style = Paint.Style.STROKE
                }
                canvas.drawRect(40f, 40f, targetWidth - 40f, targetHeight - 40f, goldPaint)
            }
            CoverTemplate.DARK -> {
                bgPaint.color = Color.parseColor("#08080C")
                canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), bgPaint)

                // Red/crimson subtle accent beam
                val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FF2A55")
                    alpha = 60
                }
                canvas.drawRect(0f, targetHeight * 0.48f, targetWidth.toFloat(), targetHeight * 0.52f, beamPaint)
            }
            CoverTemplate.CINEMATIC -> {
                val gradient = LinearGradient(
                    0f, 0f, targetWidth.toFloat(), 0f,
                    intArrayOf(Color.parseColor("#050811"), Color.parseColor("#111F38"), Color.parseColor("#050811")),
                    null, Shader.TileMode.CLAMP
                )
                bgPaint.shader = gradient
                canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), bgPaint)

                // Cinematic letterbox bars
                val barPaint = Paint().apply { color = Color.BLACK }
                canvas.drawRect(0f, 0f, targetWidth.toFloat(), 70f, barPaint)
                canvas.drawRect(0f, targetHeight - 70f, targetWidth.toFloat(), targetHeight.toFloat(), barPaint)
            }
            CoverTemplate.CLEAN, CoverTemplate.MINIMAL -> {
                val gradient = LinearGradient(
                    0f, 0f, 0f, targetHeight.toFloat(),
                    intArrayOf(Color.parseColor("#1E222D"), Color.parseColor("#12141A")),
                    null, Shader.TileMode.CLAMP
                )
                bgPaint.shader = gradient
                canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), bgPaint)
            }
        }

        // Apply darkness overlay if requested
        if (settings.darkness > 0f) {
            val dimPaint = Paint().apply {
                color = Color.BLACK
                alpha = (settings.darkness * 255).toInt().coerceIn(0, 255)
            }
            canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), dimPaint)
        }

        // 2. Render Artist and Title Typography
        val title = if (settings.title.isNotBlank()) settings.title else "UNTITLED TRACK"
        val artist = if (settings.artist.isNotBlank()) settings.artist.uppercase() else "VOXFRAME STUDIO"
        val subtitle = settings.subtitle

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = when {
                targetHeight >= 1080 -> 84f
                else -> 56f
            }
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(16f, 0f, 4f, Color.argb(180, 0, 0, 0))
        }

        val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = try {
                Color.parseColor(settings.primaryColorHex)
            } catch (e: Exception) {
                Color.parseColor("#00E5FF")
            }
            textSize = when {
                targetHeight >= 1080 -> 40f
                else -> 28f
            }
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.25f
            setShadowLayer(8f, 0f, 2f, Color.BLACK)
        }

        val centerY = targetHeight * 0.5f

        // Draw Artist above Title
        canvas.drawText(artist, targetWidth * 0.5f, centerY - 40f, artistPaint)

        // Draw Title
        canvas.drawText(title, targetWidth * 0.5f, centerY + 50f, titlePaint)

        // Draw Subtitle / Key info below
        if (subtitle.isNotBlank()) {
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#94A3B8")
                textSize = 30f
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(subtitle, targetWidth * 0.5f, centerY + 120f, subPaint)
        }

        // Watermark badge
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 22f
            textAlign = Paint.Align.RIGHT
            letterSpacing = 0.15f
        }
        canvas.drawText("PRODUCED WITH VOXFRAME STUDIO", targetWidth - 60f, targetHeight - 40f, badgePaint)

        bitmap
    }

    /**
     * Saves generated cover art to disk and returns the file
     */
    suspend fun saveCoverToDisk(context: Context, bitmap: Bitmap, fileName: String = "cover_${System.currentTimeMillis()}.png"): File = withContext(Dispatchers.IO) {
        val coversDir = File(context.filesDir, "covers")
        if (!coversDir.exists()) coversDir.mkdirs()
        val file = File(coversDir, fileName)
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()
        file
    }
}
