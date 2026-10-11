package io.github.dirnei.mc.servus.foundation.datagen.model;

import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/** Writes item models and their item model definitions for one mod. Subclasses fill it in {@link #registerModels()}. */
public abstract class ItemModelProvider extends ModelProvider.Items implements DataProvider {

    private final PackOutput output;

    protected ItemModelProvider(PackOutput output, String modid) {
        super(modid);
        this.output = output;
    }

    protected abstract void registerModels();

    @Override
    public ItemModelBuilder basicItem(Item item) {
        return super.basicItem(item);
    }

    @Override
    public ItemModelBuilder basicItem(Identifier item) {
        return super.basicItem(item);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        registerModels();
        return write(cache, output);
    }

    @Override
    public String getName() {
        return "Item models: " + modid;
    }
}
