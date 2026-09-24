package me.matl114.gui.nl;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

/** JNI bridge into the copied Neverlose Last Dear ImGui core and its OpenGL3 backend. */
public final class NlNative {
    private static boolean loaded;
    private static boolean ready;
    private static String failure;

    private NlNative() {}

    static String failure() {
        return failure;
    }

    static void inputKey(int key, int modifiers, boolean down) {
        if (ready) keyEvent(key, modifiers, down);
    }

    static void inputChar(int codepoint) {
        if (ready) charEvent(codepoint);
    }

    static synchronized boolean ensureReady() {
        if (ready) return true;
        if (failure != null) return false;
        try {
            if (!loaded) {
                Path library = extract("neverlose_bridge.dll");
                System.load(library.toAbsolutePath().toString());
                loaded = true;
            }
            Path regular = extract("SSTMedium.TTF");
            Path chinese = extract("PingFangSC-Regular.ttf");
            Path icons = extract("fa-solid-900.ttf");
            Path bold = extract("SSTBold.TTF");
            ready = init(regular.toString(), chinese.toString(), icons.toString(), bold.toString());
            if (!ready) failure = "ImGui OpenGL3 initialization failed";
        } catch (Throwable error) {
            failure = error.getClass().getSimpleName() + ": " + error.getMessage();
        }
        return ready;
    }

    private static Path extract(String fileName) throws Exception {
        String resource = "/assets/slimefunhelper/native/" + fileName;
        try (InputStream input = NlNative.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Missing native resource " + fileName);
            Path file = Files.createTempFile("slimefunhelper-nl-", "-" + fileName);
            Files.copy(input, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            file.toFile().deleteOnExit();
            return file;
        }
    }

    public static void renderIfOpen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof NlMainScreen screen)) return;
        var window = client.getWindow();
        long handle = window.getHandle();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var x = stack.mallocDouble(1);
            var y = stack.mallocDouble(1);
            GLFW.glfwGetCursorPos(handle, x, y);
            double scaleX = (double) window.getFramebufferWidth() / window.getWidth();
            double scaleY = (double) window.getFramebufferHeight() / window.getHeight();
            int priorFramebuffer = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            try {
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
                screen.renderNative(window.getFramebufferWidth(), window.getFramebufferHeight(),
                        x.get(0) * scaleX, y.get(0) * scaleY,
                        GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS);
            } finally {
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, priorFramebuffer);
            }
        }
    }

    private static native boolean init(String regularFont, String chineseFont, String iconFont, String boldFont);

    static native String frame(int width, int height, double mouseX, double mouseY, boolean mouseDown, float wheel,
            String[] groups, int selectedGroup, String[] modules, int[] enabled, int selectedModule,
            String[] configKeys, String[] configValues, int[] configTypes);

    private static native void keyEvent(int key, int modifiers, boolean down);

    private static native void charEvent(int codepoint);

    public static native void shutdown();
}
