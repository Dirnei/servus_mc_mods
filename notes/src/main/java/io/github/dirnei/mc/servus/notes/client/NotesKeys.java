package io.github.dirnei.mc.servus.notes.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** The key bindings of Servus: Notes, in their own "Servus: Notes" category. */
@EventBusSubscriber(modid = ServusNotesIds.MOD_ID, value = Dist.CLIENT)
public final class NotesKeys {

    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "main"));

    /** Opens the notebook from anywhere in game; closes it again while no text field has focus. */
    public static final KeyMapping OPEN_NOTEBOOK = new KeyMapping("key.servus_notes.open_notebook",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY);

    /** Hides the HUD pin, or shows it again. */
    public static final KeyMapping TOGGLE_HUD_PIN = new KeyMapping("key.servus_notes.toggle_hud_pin",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY);

    private NotesKeys() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_NOTEBOOK);
        event.register(TOGGLE_HUD_PIN);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_NOTEBOOK.consumeClick()) {
            if (minecraft.screen == null && minecraft.player != null) {
                minecraft.setScreen(new NotebookScreen());
            }
        }
        while (TOGGLE_HUD_PIN.consumeClick()) {
            HudPin.toggle();
        }
    }
}
