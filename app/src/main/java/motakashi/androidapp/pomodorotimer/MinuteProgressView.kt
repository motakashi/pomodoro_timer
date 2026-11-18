package motakashi.androidapp.pomodorotimer

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View

/**
 * 残り時間を1分単位のカラーブロックで表示するカスタムビューです。
 */
class MinuteProgressView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density
    private val verticalInset = 2f * density
    private var colorGradient: Shader? = null
    private var glossGradient: Shader? = null
    private var durationMillis = 0L
    private var remainingMillis = 0L
    private var sampledAtMillis = 0L
    private var running = false

    /**
     * タイマーの状態を更新して再描画します。
     *
     * @param remainingMillis フェーズの残り時間（ミリ秒）
     * @param durationMillis フェーズ全体の時間（ミリ秒）
     * @param sampledAtMillis 残り時間を取得した時点の経過時間（ミリ秒）
     * @param running タイマーが進行中かどうか
     */
    fun render(remainingMillis: Long, durationMillis: Long, sampledAtMillis: Long, running: Boolean) {
        this.durationMillis = durationMillis
        this.remainingMillis = remainingMillis
        this.sampledAtMillis = sampledAtMillis
        this.running = running
        invalidate()
    }

    /**
     * ビューのサイズ変更時に、表示用グラデーションを作成します。
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        colorGradient = LinearGradient(
            0f, 0f, w.toFloat(), 0f,
            intArrayOf(
                0xFFAD243F.toInt(), 0xFFF16B32.toInt(), 0xFFF2BE30.toInt(),
                0xFF68B650.toInt(), 0xFF159C86.toInt()
            ),
            null, Shader.TileMode.CLAMP
        )
        glossGradient = LinearGradient(
            0f, verticalInset, 0f, h - verticalInset,
            intArrayOf(0x66FFFFFF, 0x00FFFFFF, 0x220C3134),
            null, Shader.TileMode.CLAMP
        )
    }

    /**
     * 残り時間に応じた分単位のブロックと装飾を描画します。
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (durationMillis <= 0L || width == 0 || height == 0) return

        val elapsed = if (running) SystemClock.elapsedRealtime() - sampledAtMillis else 0L
        val remaining = (remainingMillis - elapsed.coerceAtLeast(0L)).coerceIn(0L, durationMillis)
        val blocks = ((durationMillis + MILLIS_PER_MINUTE - 1) / MILLIS_PER_MINUTE).toInt()
        val gap = minOf(5 * density, width.toFloat() / blocks / 4)
        val blockWidth = (width - (blocks - 1) * gap) / blocks
        val top = verticalInset
        val bottom = height - verticalInset
        if (bottom <= top) return
        val radius = minOf(5 * density, blockWidth / 2, (bottom - top) / 2)
        val glow = minOf(density, gap / 2)

        repeat(blocks) { index ->
            val left = index * (blockWidth + gap)
            val right = left + blockWidth
            paint.shader = null
            paint.color = 0xFFDCE1E4.toInt()
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)

            val blockStart = index * MILLIS_PER_MINUTE
            val blockDuration = minOf(MILLIS_PER_MINUTE, durationMillis - blockStart)
            val fraction = ((remaining - blockStart).toFloat() / blockDuration).coerceIn(0f, 1f)
            if (fraction > 0f) {
                val activeRight = left + blockWidth * fraction
                val activeRadius = minOf(radius, (activeRight - left) / 2)
                paint.shader = colorGradient
                paint.alpha = 55
                canvas.drawRoundRect(
                    left - glow, top - glow, activeRight + glow, bottom + glow,
                    activeRadius + glow, activeRadius + glow, paint
                )
                paint.alpha = 255
                canvas.drawRoundRect(left, top, activeRight, bottom, activeRadius, activeRadius, paint)
                paint.shader = glossGradient
                canvas.drawRoundRect(left, top, activeRight, bottom, activeRadius, activeRadius, paint)
            }
        }

        if (running && remaining > 0L) postInvalidateOnAnimation()
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}

/**
 * タイマー画面の背景に方眼を描画する Drawable です。
 */
internal class TimerGridDrawable(
    private val spacing: Float,
    private val backgroundColor: Int
) : Drawable() {
    private val paint = Paint().apply { color = 0xFFB8C8E0.toInt() }

    /**
     * 背景色と方眼線をキャンバスへ描画します。
     */
    override fun draw(canvas: Canvas) {
        canvas.drawColor(backgroundColor)
        var x = bounds.exactCenterX() % spacing
        while (x < bounds.right) {
            canvas.drawLine(x, bounds.top.toFloat(), x, bounds.bottom.toFloat(), paint)
            x += spacing
        }
        var y = bounds.exactCenterY() % spacing
        while (y < bounds.bottom) {
            canvas.drawLine(bounds.left.toFloat(), y, bounds.right.toFloat(), y, paint)
            y += spacing
        }
    }

    /**
     * 描画に使用する透明度を設定します。
     */
    override fun setAlpha(alpha: Int) { paint.alpha = alpha }

    /**
     * 描画に使用するカラーフィルターを設定します。
     */
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }

    @Suppress("DEPRECATION")
    /**
     * Drawable の不透明度を返します。
     */
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}
