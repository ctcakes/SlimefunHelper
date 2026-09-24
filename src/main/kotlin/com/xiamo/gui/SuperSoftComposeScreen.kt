// Derived from SuperSoftClient-Compose-for-MC-1.21.4 by Xiamo-vip.
// Copyright and license: GPL-3.0-only; see licenses/SuperSoft-renderer-NOTICE.txt.
package com.xiamo.gui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeScenePointer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.xiamo.utils.AWTUtils
import com.xiamo.utils.GlStateUtil
import com.xiamo.utils.glfwToAwtKeyCode
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.FramebufferFormat
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL33C
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent

@OptIn(ExperimentalComposeUiApi::class, androidx.compose.ui.InternalComposeUiApi::class)
abstract class SuperSoftComposeScreen(text: Text) : Screen(text) {
    protected val mc: MinecraftClient = MinecraftClient.getInstance()
    private var skiaContext: DirectContext? = null
    private var surface: Surface? = null
    private var renderTarget: BackendRenderTarget? = null
    private var composeScene: ComposeScene? = null
    private var currentScale = mc.window.scaleFactor
    private var lastScaleFactor = currentScale
    private var disposed = false

    private fun initCompose(width: Int, height: Int) {
        val density = Density(mc.window.scaleFactor.toFloat())
        if (composeScene == null) {
            composeScene = CanvasLayersComposeScene(density = density, invalidate = {}).apply {
                setContent { renderCompose() }
            }
        } else {
            composeScene?.density = density
        }
        composeScene?.size = IntSize(width, height)
    }

    private fun closeSkiaResources() {
        surface?.close()
        renderTarget?.close()
        skiaContext?.close()
        surface = null
        renderTarget = null
        skiaContext = null
    }

    private fun buildCompose() {
        val width = mc.window.framebufferWidth
        val height = mc.window.framebufferHeight
        if (skiaContext != null && surface?.width == width && surface?.height == height) return

        closeSkiaResources()
        skiaContext = DirectContext.makeGL()
        renderTarget = BackendRenderTarget.makeGL(
            width, height, 0, 8, GL33C.glGetInteger(GL33C.GL_FRAMEBUFFER_BINDING), FramebufferFormat.GR_GL_RGBA8
        )
        surface = Surface.makeFromBackendRenderTarget(
            skiaContext!!, renderTarget!!, SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.BGRA_8888, ColorSpace.sRGB
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        if (disposed) return
        val width = mc.window.framebufferWidth
        val height = mc.window.framebufferHeight
        if (composeScene == null) initCompose(width, height)
        if (lastScaleFactor != mc.window.scaleFactor ||
            composeScene?.size?.width != width || composeScene?.size?.height != height) {
            closeSkiaResources()
            initCompose(width, height)
            lastScaleFactor = mc.window.scaleFactor
        }
        currentScale = mc.window.scaleFactor
        buildCompose()

        GlStateUtil.save()
        try {
            glStorePixel()
            skiaContext?.resetAll()
            GL33C.glEnable(GL33C.GL_BLEND)
            surface?.let {
                composeScene?.render(it.canvas.asComposeCanvas(), System.nanoTime())
                it.flush()
            }
        } finally {
            GlStateUtil.restore()
        }
    }

    private fun toComposeOffset(x: Double, y: Double): Offset {
        currentScale = mc.window.scaleFactor
        return Offset((x * currentScale).toFloat(), (y * currentScale).toFloat())
    }

    override fun resize(width: Int, height: Int) {
        closeSkiaResources()
        initCompose(mc.window.framebufferWidth, mc.window.framebufferHeight)
        super.resize(width, height)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        val point = toComposeOffset(mouseX, mouseY)
        val event = AWTUtils.MouseEvent(
            point.x.toInt(), point.y.toInt(),
            AWTUtils.getAwtMods(mc.window.handle), 0, MouseEvent.MOUSE_MOVED
        )
        composeScene?.sendPointerEvent(
            PointerEventType.Move, position = point, type = PointerType.Mouse,
            button = PointerButton(0), nativeEvent = event
        )
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        val point = toComposeOffset(click.x(), click.y())
        val event = AWTUtils.MouseEvent(
            point.x.toInt(), point.y.toInt(),
            AWTUtils.getAwtMods(mc.window.handle), click.button(), MouseEvent.MOUSE_PRESSED
        )
        composeScene?.sendPointerEvent(PointerEventType.Press, position = point, nativeEvent = event)
        return true
    }

    override fun mouseDragged(click: Click, deltaX: Double, deltaY: Double): Boolean {
        val point = toComposeOffset(click.x(), click.y())
        val event = AWTUtils.MouseEvent(
            point.x.toInt(), point.y.toInt(),
            AWTUtils.getAwtMods(mc.window.handle), click.button(), MouseEvent.MOUSE_DRAGGED
        )
        val pointer = ComposeScenePointer(PointerId(0), point, true, PointerType.Mouse)
        composeScene?.sendPointerEvent(PointerEventType.Move, pointers = listOf(pointer), nativeEvent = event)
        return true
    }

    override fun mouseReleased(click: Click): Boolean {
        val point = toComposeOffset(click.x(), click.y())
        val event = AWTUtils.MouseEvent(
            point.x.toInt(), point.y.toInt(),
            AWTUtils.getAwtMods(mc.window.handle), click.button(), MouseEvent.MOUSE_RELEASED
        )
        composeScene?.sendPointerEvent(PointerEventType.Release, position = point, nativeEvent = event)
        return true
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val point = toComposeOffset(mouseX, mouseY)
        val event = AWTUtils.MouseWheelEvent(
            point.x.toInt(), point.y.toInt(), verticalAmount,
            AWTUtils.getAwtMods(mc.window.handle), MouseEvent.MOUSE_WHEEL
        )
        composeScene?.sendPointerEvent(
            position = point, eventType = PointerEventType.Scroll,
            scrollDelta = toComposeOffset(horizontalAmount, -verticalAmount), nativeEvent = event
        )
        return true
    }

    override fun charTyped(input: CharInput): Boolean {
        composeScene?.sendKeyEvent(
            AWTUtils.KeyEvent(
                KeyEvent.KEY_TYPED, System.nanoTime() / 1_000_000,
                AWTUtils.getAwtMods(mc.window.handle), KeyEvent.VK_UNDEFINED,
                input.codepoint().toChar(), KeyEvent.KEY_LOCATION_UNKNOWN
            )
        )
        return true
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            close()
            return true
        }
        composeScene?.sendKeyEvent(
            AWTUtils.KeyEvent(
                KeyEvent.KEY_PRESSED, System.nanoTime() / 1_000_000,
                AWTUtils.getAwtMods(mc.window.handle), glfwToAwtKeyCode(input.key()),
                KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_STANDARD
            )
        )
        return true
    }

    override fun keyReleased(input: KeyInput): Boolean {
        composeScene?.sendKeyEvent(
            AWTUtils.KeyEvent(
                KeyEvent.KEY_RELEASED, System.nanoTime() / 1_000_000,
                AWTUtils.getAwtMods(mc.window.handle), glfwToAwtKeyCode(input.key()),
                KeyEvent.CHAR_UNDEFINED, KeyEvent.KEY_LOCATION_STANDARD
            )
        )
        return true
    }

    override fun shouldPause(): Boolean = false
    override fun shouldCloseOnEsc(): Boolean = true

    protected fun disposeCompose() {
        if (disposed) return
        disposed = true
        closeSkiaResources()
        composeScene?.close()
        composeScene = null
    }

    override fun removed() {
        disposeCompose()
        super.removed()
    }

    override fun close() {
        disposeCompose()
        super.close()
    }

    @Composable
    protected abstract fun renderCompose()

    private fun glStorePixel() {
        GL33C.glBindBuffer(GL33C.GL_PIXEL_UNPACK_BUFFER, 0)
        GL33C.glPixelStorei(GL33C.GL_UNPACK_SWAP_BYTES, GL33C.GL_FALSE)
        GL33C.glPixelStorei(GL33C.GL_UNPACK_LSB_FIRST, GL33C.GL_FALSE)
        GL33C.glPixelStorei(GL33C.GL_UNPACK_ROW_LENGTH, 0)
        GL33C.glPixelStorei(GL33C.GL_UNPACK_SKIP_ROWS, 0)
        GL33C.glPixelStorei(GL33C.GL_UNPACK_SKIP_PIXELS, 0)
        GL33C.glPixelStorei(GL33C.GL_UNPACK_ALIGNMENT, 4)
    }
}
