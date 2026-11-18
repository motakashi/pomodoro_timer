package motakashi.androidapp.pomodorotimer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.SystemClock
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MinuteProgressViewTest {

    private lateinit var context: Context
    private lateinit var bitmap: Bitmap
    private lateinit var canvas: Canvas

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        bitmap = Bitmap.createBitmap(300, 50, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
    }

    @Test
    fun `render呼出後にmeasureとlayoutとdrawを実行してもエラーが発生しない`() {
        val view = MinuteProgressView(context, Robolectric.buildAttributeSet().build())
        view.render(
            remainingMillis = 25 * 60 * 1000L,
            durationMillis = 25 * 60 * 1000L,
            sampledAtMillis = SystemClock.elapsedRealtime(),
            running = true
        )

        view.measure(
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 300, 50)
        view.draw(canvas)
    }

    @Test
    fun `残り時間が半分の場合でも描画が成功する`() {
        val view = MinuteProgressView(context, Robolectric.buildAttributeSet().build())
        view.render(
            remainingMillis = 12 * 60 * 1000L + 30 * 1000L,
            durationMillis = 25 * 60 * 1000L,
            sampledAtMillis = SystemClock.elapsedRealtime(),
            running = false
        )

        view.measure(
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 300, 50)
        view.draw(canvas)
    }

    @Test
    fun `残り時間または全体時間が0の場合でもエラーなく描画がスキップされる`() {
        val view = MinuteProgressView(context, Robolectric.buildAttributeSet().build())

        // durationMillisが0
        view.render(
            remainingMillis = 0L,
            durationMillis = 0L,
            sampledAtMillis = 0L,
            running = false
        )
        view.measure(
            View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 300, 50)
        view.draw(canvas)

        // remainingMillisが0
        view.render(
            remainingMillis = 0L,
            durationMillis = 25 * 60 * 1000L,
            sampledAtMillis = SystemClock.elapsedRealtime(),
            running = false
        )
        view.draw(canvas)
    }

    @Test
    fun `TimerGridDrawableの描画と基本プロパティの動作確認`() {
        val drawable = TimerGridDrawable(20f, Color.WHITE)
        drawable.setBounds(0, 0, 100, 100)
        drawable.draw(canvas)

        drawable.alpha = 128
        drawable.setColorFilter(null)
        @Suppress("DEPRECATION")
        assertEquals(PixelFormat.OPAQUE, drawable.opacity)
    }
}
