package me.matl114.gui.nl;

import java.util.ArrayList;
import java.util.List;
import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.api.ModuleGroup;
import me.matl114.hacks.modules.HackModules;
import me.matl114.hacks.modules.task.ClickGui;
import me.matl114.managers.config.ConfigEnum;
import me.matl114.managers.config.EnumRef;
import me.matl114.managers.config.FlagRef;
import me.matl114.managers.config.IntRef;
import me.matl114.managers.config.FloatRef;
import me.matl114.managers.config.DoubleRef;
import me.matl114.managers.config.LongRef;
import me.matl114.managers.config.StringRef;
import me.matl114.managers.config.KeyBindRef;
import me.matl114.managers.config.NBTRef;
import me.matl114.hacks.utils.config.WrapColor;
import me.matl114.hacks.utils.config.Vec2;
import me.matl114.hacks.utils.config.WidgetPos;
import me.matl114.managers.config.Ref;
import me.matl114.managers.input.MultiKeyBind;
import me.matl114.managers.input.KeyCode;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

/** Minecraft input surface for the native Dear ImGui / Neverlose menu. */
public final class NlMainScreen extends Screen {
    private int groupIndex;
    private int moduleIndex = -1;
    private float wheel;
    private KeyBindRef pendingBind;

    public NlMainScreen() {
        super(Text.literal("SlimefunHelper / Neverlose"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // The menu itself is drawn by Dear ImGui after Minecraft blits to the window.
        context.fill(0, 0, width, height, 0x99000000);
        if (NlNative.failure() != null) {
            context.drawCenteredTextWithShadow(textRenderer, "Neverlose native renderer unavailable", width / 2, height / 2 - 8, 0xFFFF7777);
            context.drawCenteredTextWithShadow(textRenderer, NlNative.failure(), width / 2, height / 2 + 8, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return true; // GLFW state is forwarded to Dear ImGui by NlNative.
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        wheel += (float) verticalAmount;
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (pendingBind != null) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                pendingBind.set(new MultiKeyBind());
                pendingBind = null;
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_LEFT_CONTROL || input.key() == GLFW.GLFW_KEY_RIGHT_CONTROL
                    || input.key() == GLFW.GLFW_KEY_LEFT_SHIFT || input.key() == GLFW.GLFW_KEY_RIGHT_SHIFT
                    || input.key() == GLFW.GLFW_KEY_LEFT_ALT || input.key() == GLFW.GLFW_KEY_RIGHT_ALT
                    || input.key() == GLFW.GLFW_KEY_LEFT_SUPER || input.key() == GLFW.GLFW_KEY_RIGHT_SUPER) {
                return true;
            } else {
                java.util.ArrayList<Integer> keys = new java.util.ArrayList<>();
                int mods = input.modifiers();
                if ((mods & GLFW.GLFW_MOD_CONTROL) != 0) keys.add(GLFW.GLFW_KEY_LEFT_CONTROL);
                if ((mods & GLFW.GLFW_MOD_SHIFT) != 0) keys.add(GLFW.GLFW_KEY_LEFT_SHIFT);
                if ((mods & GLFW.GLFW_MOD_ALT) != 0) keys.add(GLFW.GLFW_KEY_LEFT_ALT);
                if ((mods & GLFW.GLFW_MOD_SUPER) != 0) keys.add(GLFW.GLFW_KEY_LEFT_SUPER);
                if (input.key() != GLFW.GLFW_KEY_LEFT_CONTROL && input.key() != GLFW.GLFW_KEY_RIGHT_CONTROL
                        && input.key() != GLFW.GLFW_KEY_LEFT_SHIFT && input.key() != GLFW.GLFW_KEY_RIGHT_SHIFT
                        && input.key() != GLFW.GLFW_KEY_LEFT_ALT && input.key() != GLFW.GLFW_KEY_RIGHT_ALT
                        && input.key() != GLFW.GLFW_KEY_LEFT_SUPER && input.key() != GLFW.GLFW_KEY_RIGHT_SUPER)
                    keys.add(input.key());
                int[] keyArray = keys.stream().mapToInt(Integer::intValue).toArray();
                try { pendingBind.set(new MultiKeyBind(keyArray)); }
                catch (RuntimeException ignored) { }
            }
            pendingBind = null;
            return true;
        }
        NlNative.inputKey(input.key(), input.modifiers(), true);
        super.keyPressed(input);
        return true;
    }

    @Override
    public boolean keyReleased(KeyInput input) {
        NlNative.inputKey(input.key(), input.modifiers(), false);
        return true;
    }

    @Override
    public boolean charTyped(CharInput input) {
        NlNative.inputChar(input.codepoint());
        return true;
    }

    float consumeWheel() {
        float value = wheel;
        wheel = 0;
        return value;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    void renderNative(int framebufferWidth, int framebufferHeight, double mouseX, double mouseY, boolean mouseDown) {
        if (!NlNative.ensureReady()) return;

        List<ModuleGroup> groups = new ArrayList<>(HackModules.getModuleGroups());
        if (groups.isEmpty()) return;
        groupIndex = Math.clamp(groupIndex, 0, groups.size() - 1);
        List<BaseModule> modules = groups.get(groupIndex).getModules().stream()
                .filter(BaseModule::shouldShowInGui).toList();
        if (moduleIndex >= modules.size()) moduleIndex = modules.isEmpty() ? -1 : 0;
        BaseModule selected = moduleIndex >= 0 ? modules.get(moduleIndex) : null;
        List<BaseModule.WrapperConfigRef<?>> refs = selected == null ? List.of() : selected.getEditableConfig().stream()
                .filter(BaseModule.WrapperConfigRef::shouldShow).toList();

        String[] groupNames = groups.stream()
                .map(g -> Text.translatableWithFallback("widget.click-gui.module-group-name." + g.getName(), g.getName()).getString())
                .toArray(String[]::new);
        String[] moduleNames = modules.stream()
                .map(m -> ClickGui.INSTANCE == null ? m.getName() : ClickGui.INSTANCE.getModuleName(m).getString())
                .toArray(String[]::new);
        int[] enabled = new int[modules.size()];
        for (int i = 0; i < modules.size(); i++) {
            FlagRef flag = modules.get(i).getBindFlag();
            enabled[i] = flag == null ? -1 : flag.get() ? 1 : 0;
        }
        String[] keys = refs.stream()
                .map(wrapper -> settingLabel(Text.translatableWithFallback(wrapper.keyName(), wrapper.keyName()).getString()))
                .toArray(String[]::new);
        String[] values = new String[refs.size()];
        int[] types = new int[refs.size()];
        for (int i = 0; i < refs.size(); i++) {
            Ref<?> ref = refs.get(i).ref();
            Object value = ref.getValue();
            values[i] = value instanceof ConfigEnum configEnum ? configEnum.getDisplay().getString()
                    : value instanceof Vec2 vec ? vec.x() + "," + vec.y()
                    : value instanceof WidgetPos pos ? pos.getType() + "|" +
                            (pos.getType() == 0 ? pos.getPercentageX() : pos.getLengthX()) + "|" +
                            (pos.getType() == 0 ? pos.getPercentageY() : pos.getLengthY())
                    : String.valueOf(ref.getAsPrimitive());
            types[i] = ref instanceof FlagRef ? 1 : ref instanceof EnumRef ? 2
                    : ref instanceof KeyBindRef ? 4
                    : ref instanceof NBTRef<?> && value instanceof WrapColor ? 5
                    : ref instanceof NBTRef<?> && value instanceof Vec2 ? 6
                    : ref instanceof NBTRef<?> && value instanceof WidgetPos ? 7
                    : ref instanceof IntRef || ref instanceof FloatRef || ref instanceof DoubleRef
                    || ref instanceof LongRef || ref instanceof StringRef || ref instanceof NBTRef ? 3 : 0;
        }

        String action = NlNative.frame(framebufferWidth, framebufferHeight, mouseX, mouseY, mouseDown,
                consumeWheel(), groupNames, groupIndex, moduleNames, enabled, moduleIndex, keys, values, types);
        if (action == null || action.isEmpty()) return;
        int separator = action.indexOf(':');
        if (separator < 0) return;
        String payload = action.substring(separator + 1);
        String textValue = null;
        if (action.startsWith("set:")) {
            int second = payload.indexOf(':');
            if (second < 0) return;
            textValue = payload.substring(second + 1);
            payload = payload.substring(0, second);
        }
        int index;
        try {
            index = Integer.parseInt(payload);
        } catch (NumberFormatException ignored) {
            return;
        }
        switch (action.substring(0, separator)) {
            case "group" -> {
                if (index >= 0 && index < groups.size()) {
                    groupIndex = index;
                    moduleIndex = -1;
                }
            }
            case "module" -> {
                if (index >= 0 && index < modules.size()) moduleIndex = index;
            }
            case "toggle" -> {
                if (index >= 0 && index < modules.size() && modules.get(index).getBindFlag() != null)
                    modules.get(index).getBindFlag().toggle();
            }
            case "config" -> {
                if (index >= 0 && index < refs.size()) changeConfig(refs.get(index).ref());
            }
            case "set" -> {
                if (index >= 0 && index < refs.size()) setConfig(refs.get(index).ref(), textValue);
            }
            case "bind" -> {
                if (index >= 0 && index < refs.size() && refs.get(index).ref() instanceof KeyBindRef bind) {
                    pendingBind = bind;
                }
            }
            default -> { }
        }
    }

    /** The selected module already identifies the owner of each setting. */
    private static String settingLabel(String translated) {
        int separator = translated.indexOf('：');
        if (separator < 0) separator = translated.indexOf(':');
        if (separator >= 0 && separator + 1 < translated.length()) {
            String name = translated.substring(separator + 1).trim();
            if (!name.isEmpty()) return name;
        }
        return translated;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void changeConfig(Ref<?> ref) {
        if (ref instanceof FlagRef flag) flag.toggle();
        else if (ref instanceof EnumRef enumRef && enumRef.getValue() instanceof ConfigEnum current) {
            Object[] choices = current.getClass().getEnumConstants();
            if (choices != null && choices.length > 0)
                enumRef.setValue((ConfigEnum) choices[(current.cast().ordinal() + 1) % choices.length]);
        }
    }

    public static void setConfig(Ref<?> ref, String text) {
        try {
            if (ref instanceof IntRef value) value.set(Integer.parseInt(text));
            else if (ref instanceof LongRef value) value.set(Long.parseLong(text));
            else if (ref instanceof FloatRef value) value.set(Float.parseFloat(text));
            else if (ref instanceof DoubleRef value) value.set(Double.parseDouble(text));
            else if (ref instanceof StringRef value) value.set(text);
            else if (ref instanceof KeyBindRef value) value.set(new MultiKeyBind(text));
            else if (ref instanceof NBTRef<?> value) {
                if (value.getValue() instanceof WrapColor) {
                    WrapColor color = new WrapColor(text);
                    if (!value.copyValueFrom(new NBTRef<>(color)))
                        throw new IllegalArgumentException("Invalid color");
                    return;
                }
                if (value.getValue() instanceof Vec2 current) {
                    String[] pair = text.split(",", 2);
                    if (pair.length != 2) throw new IllegalArgumentException("Expected x,y");
                    Vec2 next = new Vec2(Double.parseDouble(pair[0]), Double.parseDouble(pair[1]));
                    if (!value.copyValueFrom(new NBTRef<>(next))) throw new IllegalArgumentException("Invalid vector");
                    return;
                }
                if (value.getValue() instanceof WidgetPos current) {
                    String[] fields = text.split("\\|", 3);
                    if (fields.length != 3) throw new IllegalArgumentException("Expected type|x|y");
                    int type = Integer.parseInt(fields[0]);
                    double x = Double.parseDouble(fields[1]), y = Double.parseDouble(fields[2]);
                    WidgetPos next = new WidgetPos(type, type == 0 ? x : current.getPercentageX(),
                            type == 0 ? y : current.getPercentageY(), type == 1 ? (int) x : current.getLengthX(),
                            type == 1 ? (int) y : current.getLengthY());
                    if (!value.copyValueFrom(new NBTRef<>(next))) throw new IllegalArgumentException("Invalid widget position");
                    return;
                }
                NBTRef<?> parsed = NBTRef.fromString(text);
                if (parsed == null || !value.copyValueFrom(parsed))
                    throw new IllegalArgumentException("Invalid NBT value or incompatible type");
            }
        } catch (RuntimeException error) {
            System.err.println("[SlimefunHelper/NL] Config value rejected: " + text + " (" + error.getMessage() + ")");
        }
    }
}
