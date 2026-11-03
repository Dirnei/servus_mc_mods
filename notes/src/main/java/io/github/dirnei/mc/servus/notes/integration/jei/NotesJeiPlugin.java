package io.github.dirnei.mc.servus.notes.integration.jei;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.client.NotebookScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Shows JEI's item list beside the notebook while its item picker is open, and lets an item dragged from
 * it onto the picker be picked. JEI hides its overlay while the notebook gives no gui properties, which
 * is the rest of the time. Only loaded when JEI is present.
 */
@JeiPlugin
public final class NotesJeiPlugin implements IModPlugin {

    private static final Identifier UID = Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "jei");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(NotebookScreen.class, NotesJeiPlugin::properties);
        registration.addGhostIngredientHandler(NotebookScreen.class, new NotebookGhostHandler());
    }

    private static @Nullable IGuiProperties properties(NotebookScreen screen) {
        if (screen.width <= 0 || screen.height <= 0 || !screen.picking()) {
            return null;
        }
        int[] area = screen.bookArea();
        return new IGuiProperties() {
            @Override
            public Class<? extends Screen> screenClass() {
                return NotebookScreen.class;
            }

            @Override
            public int guiLeft() {
                return area[0];
            }

            @Override
            public int guiTop() {
                return area[1];
            }

            @Override
            public int guiXSize() {
                return area[2];
            }

            @Override
            public int guiYSize() {
                return area[3];
            }

            @Override
            public int screenWidth() {
                return screen.width;
            }

            @Override
            public int screenHeight() {
                return screen.height;
            }
        };
    }
}
