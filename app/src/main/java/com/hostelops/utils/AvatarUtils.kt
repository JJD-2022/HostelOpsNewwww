package com.hostelops.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlin.math.abs

object AvatarUtils {
    private val COLORS = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7", 
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", 
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39", 
        "#FFC107", "#FF9800", "#FF5722"
    )

    fun getLetterAvatar(context: Context, name: String): Drawable {
        val size = 200
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint()
        paint.isAntiAlias = true
        
        // Background color based on name hash
        val color = Color.parseColor(COLORS[abs(name.hashCode()) % COLORS.size])
        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        
        // Text
        paint.color = Color.WHITE
        paint.textSize = size / 2f
        paint.textAlign = Paint.Align.CENTER
        
        val letter = if (name.isNotEmpty()) name[0].uppercase() else "?"
        val bounds = Rect()
        paint.getTextBounds(letter, 0, 1, bounds)
        val y = (size / 2f) - (bounds.centerY())
        
        canvas.drawText(letter, size / 2f, y, paint)
        
        return BitmapDrawable(context.resources, bitmap)
    }
}
