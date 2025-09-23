package fansirsqi.xposed.sesame.newui

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Paint
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withRotation
import fansirsqi.xposed.sesame.data.ViewAppInfo.verifyId
import fansirsqi.xposed.sesame.BuildConfig

class WatermarkView(context: android.content.Context) : android.view.View(context) {

    private val paint = Paint().apply {
        color = "#66FF0000".toColorInt()
        textSize = 38f
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }

    private var textLines: List<String> = emptyList()
    private val colorCache = mutableListOf<Int>()

    var watermarkText: String? = null
        set(value) {
            val prefixLines = listOf(
                "免费模块仅供学习",
                "勿在国内平台传播,倒卖必死全家!",
                "UID: $verifyId",
                "阿亮亮哥编译版本👑",
                "版本号: ${BuildConfig.VERSION}.${BuildConfig.BUILD_TYPE} 📦",
                "打包时间: ${BuildConfig.BUILD_DATE} ${BuildConfig.BUILD_TIME} ⏰")
            val combinedLines = if (value.isNullOrBlank()) {
                prefixLines
            } else {
                prefixLines + value.split("\n")
            }
            field = combinedLines.joinToString("\n")
            rebuildColorCache()
            textLines = combinedLines
            invalidate()
        }


    var horizontalSpacingScale: Float = 1.2f
    var verticalSpacingScale: Float = 1.2f
    var rotationAngle: Float = -30f
    var maxDrawCount = 400


    init {
        watermarkText = watermarkText // 初始化文本
    }

    fun setInfo(tags: List<String>) {
        watermarkText = buildString {
            appendLine("免费模块仅供学习\n勿在国内平台传播,倒卖必死全家!!")
            appendLine(tags.joinToString(" | "))
        }
    }

    fun setWatermarkStyle(color: Int, size: Float) {
        paint.color = color
        paint.textSize = size
        invalidate()
    }

    fun setSpacingScale(horizontal: Float = 1.2f, vertical: Float = 1.2f) {
        horizontalSpacingScale = horizontal
        verticalSpacingScale = vertical
        invalidate()
    }

    private fun rebuildColorCache() {
        colorCache.clear()
        repeat(maxDrawCount) {
            val alpha = 0xc6
            val r = (180..255).random()
            val g = (180..255).random()
            val b = (180..255).random()
            colorCache += (alpha shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0 || textLines.isEmpty()) return

        val maxLineWidth = textLines.maxOfOrNull { paint.measureText(it) } ?: 0f
        val lineHeight = paint.fontSpacing
        val totalTextHeight = lineHeight * textLines.size

        val horizontalSpacing = (maxLineWidth * horizontalSpacingScale).toInt()
        val verticalSpacing = (totalTextHeight * verticalSpacingScale).toInt()

        var count = 0
        var yIndex = 0

        canvas.withRotation(rotationAngle) {
            var y = -height.toFloat()
            while (y < height * 2 && count < maxDrawCount) {
                var x = -width.toFloat()

                // 偶数行交错位移
                if (yIndex % 2 == 1) {
                    x += horizontalSpacing / 2
                }

                while (x < width * 2 && count < maxDrawCount) {
                    paint.color = colorCache.getOrElse(count) { colorCache.lastOrNull() ?: 0x66FFFFFF.toInt() }

                    val centerX = x
                    val baseY = y - totalTextHeight / 2

                    for ((i, line) in textLines.withIndex()) {
                        drawText(
                            line,
                            centerX,
                            baseY + i * lineHeight,
                            paint
                        )
                    }

                    count++
                    x += horizontalSpacing
                }
                y += verticalSpacing
                yIndex++
            }
        }
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun install(
            activity: Activity,
            text: String = "",
            color: Int = "#87FF0000".toColorInt(),
            fontSize: Float = 28f,
            spacingX: Float = 2.5f,
            spacingY: Float = 3.7f
        ): WatermarkView {
            val watermarkView = WatermarkView(activity).apply {
                watermarkText = text
                setWatermarkStyle(color, fontSize)
                setSpacingScale(spacingX, spacingY)
            }

            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            rootView.addView(
                watermarkView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            return watermarkView
        }
    }
}
