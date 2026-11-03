package io.github.dirnei.mc.servus.notes.client;

import com.mojang.logging.LogUtils;
import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.model.DoneRecord;
import io.github.dirnei.mc.servus.notes.model.Guest;
import io.github.dirnei.mc.servus.notes.model.ListView;
import io.github.dirnei.mc.servus.notes.model.NoteList;
import io.github.dirnei.mc.servus.notes.model.NoteEdit;
import io.github.dirnei.mc.servus.notes.model.Person;
import io.github.dirnei.mc.servus.notes.model.Spot;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * Dev only: creates or opens the demo world, fills it with example projects and saves one screenshot per notebook
 * view to {@code screenshots/}. Idle unless the system property {@code servus.notes.screenshots} is set.
 */
@EventBusSubscriber(modid = ServusNotesIds.MOD_ID, value = Dist.CLIENT)
public final class NotesScreenshots {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ACTIVE = Boolean.getBoolean("servus.notes.screenshots");
    private static final String WORLD = "notes_demo";
    private static final long SEED = Long.getLong("servus.notes.seed", 1234567L);
    private static final String OVERWORLD = "minecraft:overworld";

    private static final UUID CASTLE = id("castle");
    private static final UUID FARM = id("farm");
    private static final UUID NETHER = id("nether");
    private static final UUID SHOPPING = id("shopping");
    private static final UUID STONE = id("stone");
    private static final UUID TOWER = id("tower");
    private static final Person ANNA = new Person(id("anna"), "Anna");
    private static final Person BEN = new Person(id("ben"), "Ben");

    private static final Deque<Step> steps = new ArrayDeque<>();
    private static boolean worldRequested;
    private static boolean scripted;
    private static int wait;

    private NotesScreenshots() {
    }

    private interface Step {
        void run(Minecraft minecraft);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ACTIVE) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!worldRequested && minecraft.screen instanceof TitleScreen) {
            worldRequested = true;
            openWorld(minecraft);
            return;
        }
        if (minecraft.player == null || minecraft.getConnection() == null) {
            return;
        }
        if (!scripted) {
            scripted = true;
            script();
        }
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = steps.poll();
        if (step != null) {
            step.run(minecraft);
        }
    }

    private static void openWorld(Minecraft minecraft) {
        if (minecraft.getLevelSource().levelExists(WORLD)) {
            minecraft.createWorldOpenFlows().openWorld(WORLD, () -> minecraft.setScreen(new TitleScreen()));
            return;
        }
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
        minecraft.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(SEED, true, false),
                WorldPresets::createNormalWorldDimensions, new TitleScreen());
    }

    // ---------- the script ----------

    private static void script() {
        pause(100);
        command("gamerule advance_time false");
        command("gamerule advance_weather false");
        command("time set 4000");
        command("weather clear");
        command("clear @s");
        command("give @s minecraft:stone_bricks 64");
        command("give @s minecraft:stone_bricks 64");
        command("give @s minecraft:oak_log 40");
        command("give @s minecraft:glass 30");
        command("give @s minecraft:torch 40");
        command("give @s minecraft:bread 10");
        command("tp @s ~ ~" + Integer.getInteger("servus.notes.rise", 18) + " ~ " + System.getProperty("servus.notes.yaw", "~")
                + " " + System.getProperty("servus.notes.pitch", "12"));
        do_(NotesScreenshots::fly);
        pause(20);
        do_(m -> {
            if (ClientNotes.get(CASTLE).isEmpty()) {
                demoProjects(m);
            }
        });
        pause(60);
        do_(m -> {
            m.player.getInventory().setSelectedSlot(8);
            m.gui.getChat().clearMessages(false);
        });
        pause(5);

        do_(m -> {
            ClientNotes.select(null);
            m.setScreen(new NotebookScreen());
        });
        pause(40);
        shot("01_projects");

        do_(m -> ClientNotes.select(CASTLE));
        pause(40);
        shot("02_task_list");

        do_(m -> openTask(m, STONE));
        pause(40);
        shot("03_task_page");

        do_(m -> {
            if (m.screen instanceof NotebookScreen screen) {
                showGuests();
                screen.taskPage().openShare(CASTLE);
            }
        });
        pause(30);
        shot("04_share");

        do_(m -> {
            m.setScreen(null);
            ClientNotes.pin(Optional.of(SHOPPING));
            beamAhead(m);
        });
        pause(60);
        shot("05_hud_pin_and_beam");

        do_(NotesScreenshots::toast);
        pause(25);
        shot("06_toast");
        do_(m -> LOGGER.info("Notes screenshots done"));
    }

    /** Shows Anna and Ben as guests in this client only; the server knows no such players. */
    private static void showGuests() {
        ClientNotes.get(CASTLE).ifPresent(view -> ClientNotes.update(new ListView(
                view.list().withGuests(java.util.List.of(new Guest(ANNA, Guest.Right.EDIT), new Guest(BEN, Guest.Right.READ))),
                view.rights())));
    }

    private static void fly(Minecraft minecraft) {
        minecraft.player.getAbilities().flying = true;
        minecraft.player.onUpdateAbilities();
    }

    /** Marks a spot on the ground in front of the camera as the Quarry and turns its beam on. */
    private static void beamAhead(Minecraft minecraft) {
        Vec3 ahead = minecraft.player.position().add(minecraft.player.getLookAngle().multiply(1, 0, 1).normalize().yRot(-0.35F).scale(16));
        int x = Mth.floor(ahead.x);
        int z = Mth.floor(ahead.z);
        int y = minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        Spot spot = new Spot(x, y, z, OVERWORLD, "Quarry");
        send(new NoteEdit.SetSpot(CASTLE, STONE, Optional.of(spot)));
        if (!Beams.isOn(spot)) {
            Beams.toggle(spot);
        }
    }

    /** Shows the toast a player gets when Anna ticks a task on a shared project. */
    private static void toast(Minecraft minecraft) {
        ClientNotes.get(CASTLE).map(ListView::list).ifPresent(before -> {
            NoteList after = before.withTasks(before.tasks().stream()
                    .map(task -> task.id().equals(TOWER)
                            ? task.withDone(Optional.of(new DoneRecord(ANNA, 12, System.currentTimeMillis())))
                            : task)
                    .toList());
            TickToasts.show(before, after);
        });
    }

    private static void demoProjects(Minecraft minecraft) {
        var pos = minecraft.player.blockPosition();
        Person self = new Person(ClientNotes.self(), minecraft.player.getGameProfile().name());

        send(new NoteEdit.CreateList(CASTLE, "Castle on the Hill"));
        UUID foundation = task(CASTLE, "Lay the foundation", null);
        send(new NoteEdit.Tick(CASTLE, foundation, true));
        task(CASTLE, "Gather building stone", foundation, STONE);
        send(new NoteEdit.SetAssignee(CASTLE, STONE, Optional.of(self)));
        goal(CASTLE, STONE, "minecraft:stone_bricks", 512, 300);
        goal(CASTLE, STONE, "minecraft:oak_log", 64, 24);
        goal(CASTLE, STONE, "minecraft:glass", 96, 0);
        send(new NoteEdit.SetSpot(CASTLE, STONE, Optional.of(
                new Spot(pos.getX() + 40, pos.getY(), pos.getZ() - 25, OVERWORLD, "Quarry"))));
        send(new NoteEdit.EditNotes(CASTLE, STONE,
                "Quarry east of spawn. Keep the cobble for the walls, smelt the rest into bricks."));
        UUID hall = task(CASTLE, "Great hall", STONE);
        UUID banners = sub(CASTLE, "Banners for the walls", hall);
        goal(CASTLE, banners, "minecraft:white_banner", 8, 2);
        UUID chandelier = sub(CASTLE, "Chandelier", banners);
        send(new NoteEdit.Tick(CASTLE, chandelier, true));
        sub(CASTLE, "Long table", chandelier);
        UUID tower = task(CASTLE, "Watchtower", hall, TOWER);
        send(new NoteEdit.SetAssignee(CASTLE, tower, Optional.of(ANNA)));
        UUID moat = task(CASTLE, "Moat with drawbridge", tower);
        send(new NoteEdit.SetAssignee(CASTLE, moat, Optional.of(BEN)));
        task(CASTLE, "Plant the courtyard", moat);

        send(new NoteEdit.CreateList(FARM, "Wheat and cows"));
        UUID fields = task(FARM, "Till the fields", null);
        send(new NoteEdit.Tick(FARM, fields, true));
        UUID barn = task(FARM, "Barn", fields);
        goal(FARM, barn, "minecraft:spruce_planks", 192, 140);
        task(FARM, "Breed cows", barn);

        send(new NoteEdit.CreateList(NETHER, "Nether highway"));
        UUID portal = task(NETHER, "Portal at spawn", null);
        goal(NETHER, portal, "minecraft:obsidian", 10, 10);
        send(new NoteEdit.Tick(NETHER, portal, true));
        UUID ice = task(NETHER, "Blue ice road", portal);
        goal(NETHER, ice, "minecraft:blue_ice", 256, 64);

        send(new NoteEdit.CreateList(SHOPPING, "Supplies for the trip"));
        UUID torches = task(SHOPPING, "Torches", null);
        goal(SHOPPING, torches, "minecraft:torch", 128, 64);
        UUID bread = task(SHOPPING, "Bread", torches);
        goal(SHOPPING, bread, "minecraft:bread", 32, 12);
    }

    private static void openTask(Minecraft minecraft, UUID taskId) {
        if (!(minecraft.screen instanceof NotebookScreen screen)) {
            return;
        }
        ClientNotes.get(CASTLE).map(ListView::list)
                .flatMap(list -> list.tasks().stream().filter(task -> task.id().equals(taskId)).findFirst())
                .ifPresent(task -> screen.taskPage().open(task));
    }

    // ---------- helpers ----------

    private static UUID task(UUID listId, String text, UUID after) {
        return task(listId, text, after, id(listId + text));
    }

    private static UUID task(UUID listId, String text, UUID after, UUID taskId) {
        send(new NoteEdit.AddTask(listId, taskId, text, Optional.ofNullable(after)));
        return taskId;
    }

    private static UUID sub(UUID listId, String text, UUID after) {
        UUID taskId = task(listId, text, after);
        send(new NoteEdit.PlaceTask(listId, taskId, Optional.of(after), true));
        return taskId;
    }

    private static void goal(UUID listId, UUID taskId, String item, int target, int count) {
        UUID goalId = id(taskId + item);
        send(new NoteEdit.AddGoal(listId, taskId, goalId, item));
        send(new NoteEdit.SetGoalTarget(listId, taskId, goalId, target));
        if (count > 0) {
            send(new NoteEdit.SetGoalCount(listId, taskId, goalId, count));
        }
    }

    private static void send(NoteEdit edit) {
        ClientNotes.send(edit);
    }

    private static void command(String command) {
        do_(m -> m.getConnection().sendCommand(command));
        pause(2);
    }

    private static void shot(String name) {
        do_(m -> Screenshot.grab(m.gameDirectory, name + ".png", m.getMainRenderTarget(), 1,
                message -> LOGGER.info("Notes screenshot {}", name)));
        pause(10);
    }

    private static void pause(int ticks) {
        steps.add(m -> wait = ticks);
    }

    private static void do_(Step step) {
        steps.add(step);
    }

    private static UUID id(String name) {
        return UUID.nameUUIDFromBytes(("servus_notes_demo:" + name).getBytes(StandardCharsets.UTF_8));
    }
}
