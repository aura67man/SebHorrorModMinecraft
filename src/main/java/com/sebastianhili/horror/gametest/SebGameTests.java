package com.sebastianhili.horror.gametest;

import com.sebastianhili.horror.SebHorrorMod;
import com.sebastianhili.horror.entity.SebState;
import com.sebastianhili.horror.entity.SebastianEntity;
import com.sebastianhili.horror.logic.ChatAnswer;
import com.sebastianhili.horror.logic.PennyLogic;
import com.sebastianhili.horror.registry.ModBlocks;
import com.sebastianhili.horror.registry.ModEntities;
import com.sebastianhili.horror.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Forge GameTests. Run with: ./gradlew runGameTestServer
 * Tests that spawn Sebastian each get their own batch so they never see each other's players.
 */
@GameTestHolder(SebHorrorMod.MODID)
@PrefixGameTestTemplate(false)
public class SebGameTests {
    private static final String EMPTY = "empty";

    // ---------------------------------------------------------------- recipes

    private static ItemStack craft(GameTestHelper h, ItemStack... grid) {
        AbstractContainerMenu menu = new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player p, int i) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player p) {
                return true;
            }
        };
        TransientCraftingContainer inv = new TransientCraftingContainer(menu, 3, 3);
        for (int i = 0; i < grid.length; i++) inv.setItem(i, grid[i]);
        ServerLevel level = h.getLevel();
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, inv, level)
                .map(r -> r.assemble(inv, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    private static ItemStack cook(GameTestHelper h, RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> type, ItemStack in) {
        ServerLevel level = h.getLevel();
        SimpleContainer c = new SimpleContainer(in);
        return level.getRecipeManager().getRecipeFor(type, c, level).map(r -> r.assemble(c, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    private static void expect(GameTestHelper h, ItemStack actual, net.minecraft.world.item.Item item, int count, String what) {
        if (!actual.is(item) || actual.getCount() != count) {
            h.fail(what + ": expected " + count + "x " + item + " but got " + actual);
        }
    }

    @GameTest(template = EMPTY)
    public static void pennyRecipe(GameTestHelper h) {
        ItemStack z = new ItemStack(ModItems.ZINC_INGOT.get());
        ItemStack cu = new ItemStack(Items.COPPER_INGOT);
        expect(h, craft(h, z.copy(), z.copy(), z.copy(), cu.copy()), ModItems.PENNY.get(), 4, "3 zinc + copper");
        // shapeless: any arrangement works
        expect(h, craft(h, ItemStack.EMPTY, cu.copy(), ItemStack.EMPTY, z.copy(), ItemStack.EMPTY, z.copy(), z.copy()),
                ModItems.PENNY.get(), 4, "shuffled penny recipe");
        // wrong amounts don't
        if (!craft(h, z.copy(), z.copy(), cu.copy()).isEmpty()) h.fail("2 zinc + copper should not craft");
        if (!craft(h, z.copy(), z.copy(), z.copy()).isEmpty()) h.fail("zinc without copper should not craft");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void storageBlockRecipes(GameTestHelper h) {
        ItemStack[] nineIngots = new ItemStack[9];
        ItemStack[] nineRaw = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            nineIngots[i] = new ItemStack(ModItems.ZINC_INGOT.get());
            nineRaw[i] = new ItemStack(ModItems.RAW_ZINC.get());
        }
        expect(h, craft(h, nineIngots), ModItems.ZINC_BLOCK.get(), 1, "9 ingots -> block");
        expect(h, craft(h, new ItemStack(ModItems.ZINC_BLOCK.get())), ModItems.ZINC_INGOT.get(), 9, "block -> 9 ingots");
        expect(h, craft(h, nineRaw), ModItems.RAW_ZINC_BLOCK.get(), 1, "9 raw -> raw block");
        expect(h, craft(h, new ItemStack(ModItems.RAW_ZINC_BLOCK.get())), ModItems.RAW_ZINC.get(), 9, "raw block -> 9 raw");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void smeltingRecipes(GameTestHelper h) {
        expect(h, cook(h, RecipeType.SMELTING, new ItemStack(ModItems.RAW_ZINC.get())), ModItems.ZINC_INGOT.get(), 1, "furnace raw zinc");
        expect(h, cook(h, RecipeType.BLASTING, new ItemStack(ModItems.RAW_ZINC.get())), ModItems.ZINC_INGOT.get(), 1, "blast furnace raw zinc");
        expect(h, cook(h, RecipeType.SMELTING, new ItemStack(ModItems.ZINC_ORE.get())), ModItems.ZINC_INGOT.get(), 1, "furnace ore");
        expect(h, cook(h, RecipeType.BLASTING, new ItemStack(ModItems.DEEPSLATE_ZINC_ORE.get())), ModItems.ZINC_INGOT.get(), 1, "blast deepslate ore");
        h.succeed();
    }

    // ---------------------------------------------------------------- ores

    private static List<ItemStack> drops(GameTestHelper h, BlockState state, ItemStack tool) {
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        return Block.getDrops(state, h.getLevel(), pos, null, null, tool);
    }

    @GameTest(template = EMPTY)
    public static void oreHarvestLevels(GameTestHelper h) {
        for (Block ore : new Block[]{ModBlocks.ZINC_ORE.get(), ModBlocks.DEEPSLATE_ZINC_ORE.get()}) {
            BlockState s = ore.defaultBlockState();
            if (!s.requiresCorrectToolForDrops()) h.fail(ore + " should need the right tool");
            if (new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(s)) h.fail("wooden pickaxe must not mine " + ore);
            if (new ItemStack(Items.GOLDEN_PICKAXE).isCorrectToolForDrops(s)) h.fail("gold pickaxe (wood tier) must not mine " + ore);
            for (var pick : new net.minecraft.world.item.Item[]{Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE}) {
                if (!new ItemStack(pick).isCorrectToolForDrops(s)) h.fail(pick + " should mine " + ore);
            }
        }
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void oreDropsRawZincWithFortune(GameTestHelper h) {
        for (Block ore : new Block[]{ModBlocks.ZINC_ORE.get(), ModBlocks.DEEPSLATE_ZINC_ORE.get()}) {
            BlockState s = ore.defaultBlockState();
            int plain = 0, fortune = 0, trials = 300;
            ItemStack pick = new ItemStack(Items.STONE_PICKAXE);
            ItemStack fortunePick = new ItemStack(Items.DIAMOND_PICKAXE);
            fortunePick.enchant(Enchantments.BLOCK_FORTUNE, 3);
            for (int i = 0; i < trials; i++) {
                for (ItemStack d : drops(h, s, pick)) {
                    if (!d.is(ModItems.RAW_ZINC.get())) h.fail(ore + " dropped " + d + " instead of raw zinc");
                    if (d.getCount() < 1 || d.getCount() > 3) h.fail("plain drop count out of range: " + d.getCount());
                    plain += d.getCount();
                }
                for (ItemStack d : drops(h, s, fortunePick)) {
                    fortune += d.getCount();
                }
            }
            if (fortune < plain * 1.5) h.fail("Fortune III should give clearly more raw zinc (" + fortune + " vs " + plain + ")");
            ItemStack silk = new ItemStack(Items.IRON_PICKAXE);
            silk.enchant(Enchantments.SILK_TOUCH, 1);
            List<ItemStack> silkDrops = drops(h, s, silk);
            if (silkDrops.size() != 1 || !silkDrops.get(0).is(ore.asItem())) h.fail("silk touch should drop the ore block, got " + silkDrops);
        }
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void storageBlocksDropThemselves(GameTestHelper h) {
        for (Block b : new Block[]{ModBlocks.ZINC_BLOCK.get(), ModBlocks.RAW_ZINC_BLOCK.get()}) {
            List<ItemStack> d = drops(h, b.defaultBlockState(), new ItemStack(Items.STONE_PICKAXE));
            if (d.size() != 1 || !d.get(0).is(b.asItem())) h.fail(b + " should drop itself, got " + d);
        }
        h.succeed();
    }

    // ---------------------------------------------------------------- helpers for Sebastian tests

    /** Solid floor for the test (relative y = 0) and clear air above it. */
    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 15; z++) {
                h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                for (int y = 1; y < 7; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
            }
        }
    }

    /** Removes mock players left over from earlier batches so Sebastian only sees this test's players. */
    private static void clearOldPlayers(GameTestHelper h) {
        for (ServerPlayer old : List.copyOf(h.getLevel().getServer().getPlayerList().getPlayers())) {
            if (old.getGameProfile().getName().startsWith("seb-test")) {
                h.getLevel().getServer().getPlayerList().remove(old);
            }
        }
    }

    private static SebastianEntity spawnSeb(GameTestHelper h, int x, int z) {
        floor(h);
        clearOldPlayers(h);
        SebastianEntity seb = h.spawn(ModEntities.SEBASTIAN_HILI.get(), new BlockPos(x, 1, z));
        seb.setAutonomous(false);
        return seb;
    }

    private static ServerPlayer survivalPlayer(GameTestHelper h, int x, int z) {
        // Like GameTestHelper#makeMockServerPlayerInLevel, but with a real (embedded) network channel,
        // because Sebastian sends the player chat lines, sounds and titles.
        ServerLevel level = h.getLevel();
        ServerPlayer p = new ServerPlayer(level.getServer(), level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "seb-test-" + (h.getTick() % 1000)));
        net.minecraft.network.Connection connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, p);
        p.setGameMode(GameType.SURVIVAL);
        Vec3 v = h.absoluteVec(new Vec3(x + 0.5, 1, z + 0.5));
        p.teleportTo(v.x, v.y, v.z);
        p.getInventory().clearContent();
        return p;
    }

    // ---------------------------------------------------------------- lie detection (chat search)

    @GameTest(template = EMPTY, batch = "seb_lie_no", timeoutTicks = 100)
    public static void lyingNoGetsLightning(GameTestHelper h) {
        SebastianEntity seb = spawnSeb(h, 2, 2);
        ServerPlayer p = survivalPlayer(h, 4, 2);
        p.getInventory().add(new ItemStack(ModItems.PENNY.get(), 3));
        seb.askNow(p);
        if (!seb.isWaitingForAnswerFrom(p)) h.fail("he should be waiting for an answer");
        if (!SebastianEntity.handleChatAnswer(p, ChatAnswer.parse("No."))) h.fail("chat answer was not routed to him");
        if (PennyLogic.hasPenny(p)) h.fail("he should have taken the liar's pennies");
        AABB around = new AABB(p.blockPosition()).inflate(4);
        if (h.getLevel().getEntitiesOfClass(LightningBolt.class, around).isEmpty()) h.fail("no lightning bolt was summoned");
        h.succeedWhen(() -> {
            if (p.isAlive()) h.fail("the liar should be dead");
            if (seb.getSebState() != SebState.WATCHING) h.fail("he should go back to watching");
        });
    }

    @GameTest(template = EMPTY, batch = "seb_truth_no", timeoutTicks = 100)
    public static void truthfulNoIsSafe(GameTestHelper h) {
        SebastianEntity seb = spawnSeb(h, 2, 2);
        ServerPlayer p = survivalPlayer(h, 4, 2);
        p.getInventory().add(new ItemStack(Items.DIAMOND, 5));
        seb.askNow(p);
        SebastianEntity.handleChatAnswer(p, ChatAnswer.parse("NO"));
        h.runAfterDelay(30, () -> {
            if (!p.isAlive()) h.fail("an honest 'no' must not be punished");
            if (p.getInventory().countItem(Items.DIAMOND) != 5) h.fail("he must never take anything but pennies");
            if (seb.getSebState() != SebState.WATCHING) h.fail("he should go back to watching");
            if (!h.getLevel().getEntitiesOfClass(LightningBolt.class, new AABB(p.blockPosition()).inflate(6)).isEmpty())
                h.fail("no lightning for honest players");
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = "seb_yes", timeoutTicks = 100)
    public static void honestYesTakesExactlyOnePenny(GameTestHelper h) {
        SebastianEntity seb = spawnSeb(h, 2, 2);
        ServerPlayer p = survivalPlayer(h, 4, 2);
        p.getInventory().add(new ItemStack(ModItems.PENNY.get(), 5));
        seb.askNow(p);
        SebastianEntity.handleChatAnswer(p, ChatAnswer.parse("yes"));
        h.runAfterDelay(20, () -> {
            if (!p.isAlive()) h.fail("an honest 'yes' must not be punished");
            if (PennyLogic.countPennies(p) != 4) h.fail("expected 4 pennies left, got " + PennyLogic.countPennies(p));
            if (seb.getPennies() != 1) h.fail("he should have 1 penny");
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = "seb_lie_shulker", timeoutTicks = 100)
    public static void lieDetectorSeesPenniesInShulkerBoxes(GameTestHelper h) {
        SebastianEntity seb = spawnSeb(h, 2, 2);
        ServerPlayer p = survivalPlayer(h, 4, 2);
        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        net.minecraft.nbt.CompoundTag be = new net.minecraft.nbt.CompoundTag();
        net.minecraft.nbt.ListTag items = new net.minecraft.nbt.ListTag();
        net.minecraft.nbt.CompoundTag entry = new ItemStack(ModItems.PENNY.get(), 2).save(new net.minecraft.nbt.CompoundTag());
        entry.putByte("Slot", (byte) 0);
        items.add(entry);
        be.put("Items", items);
        box.getOrCreateTag().put("BlockEntityTag", be);
        p.getInventory().add(box);
        if (PennyLogic.countPennies(p) != 2) h.fail("should count 2 pennies hidden in the shulker box");
        seb.askNow(p);
        SebastianEntity.handleChatAnswer(p, ChatAnswer.NO);
        h.succeedWhen(() -> {
            if (p.isAlive()) h.fail("hiding pennies in a shulker box is still lying");
        });
    }

    // ---------------------------------------------------------------- penny burning

    @GameTest(template = EMPTY, batch = "seb_burn_lava", timeoutTicks = 200)
    public static void burningPennyInLavaCausesRage(GameTestHelper h) {
        SebastianEntity seb = spawnSeb(h, 2, 2);
        h.setBlock(new BlockPos(9, 0, 9), Blocks.LAVA);
        h.setBlock(new BlockPos(9, 1, 9), Blocks.AIR);
        ItemEntity penny = h.spawnItem(ModItems.PENNY.get(), 9.5F, 1.2F, 9.5F);
        penny.setDeltaMovement(Vec3.ZERO);
        h.succeedWhen(() -> {
            if (penny.isAlive()) h.fail("penny not burned yet");
            if (seb.getSebState() != SebState.RAGE) h.fail("he saw the penny burn and should be in RAGE");
        });
    }

    @GameTest(template = EMPTY, batch = "seb_burn_fire", timeoutTicks = 200)
    public static void burningPennyInFireCausesRage(GameTestHelper h) {
        SebastianEntity seb = spawnSeb(h, 2, 2);
        h.setBlock(new BlockPos(8, 1, 3), Blocks.FIRE);
        ItemEntity penny = h.spawnItem(ModItems.PENNY.get(), 8.5F, 1.1F, 3.5F);
        penny.setDeltaMovement(Vec3.ZERO);
        h.succeedWhen(() -> {
            if (penny.isAlive()) h.fail("penny not burned yet");
            if (seb.getSebState() != SebState.RAGE) h.fail("fire should enrage him too");
        });
    }

    @GameTest(template = EMPTY, batch = "seb_burn_hidden", timeoutTicks = 200)
    public static void burningPennyBehindAWallDoesNothing(GameTestHelper h) {
        SebastianEntity seb = spawnSeb(h, 2, 7);
        // solid wall between him and the lava
        for (int z = 0; z < 15; z++) {
            for (int y = 1; y < 6; y++) h.setBlock(new BlockPos(5, y, z), Blocks.STONE);
        }
        h.setBlock(new BlockPos(10, 0, 7), Blocks.LAVA);
        ItemEntity penny = h.spawnItem(ModItems.PENNY.get(), 10.5F, 1.2F, 7.5F);
        penny.setDeltaMovement(Vec3.ZERO);
        h.succeedWhen(() -> {
            if (penny.isAlive()) h.fail("penny not burned yet");
            if (seb.getSebState() == SebState.RAGE) h.fail("he couldn't see it, so no rage");
        });
    }

    @GameTest(template = EMPTY, batch = "seb_cactus", timeoutTicks = 200)
    public static void pennyDestroyedByCactusIsNotBurning(GameTestHelper h) {
        SebastianEntity seb = spawnSeb(h, 2, 2);
        h.setBlock(new BlockPos(8, 1, 8), Blocks.SAND);
        h.setBlock(new BlockPos(8, 2, 8), Blocks.CACTUS);
        ItemEntity penny = h.spawnItem(ModItems.PENNY.get(), 8.5F, 3.2F, 8.5F);
        penny.setDeltaMovement(Vec3.ZERO);
        h.succeedWhen(() -> {
            if (penny.isAlive()) h.fail("penny not destroyed yet");
            if (seb.getSebState() == SebState.RAGE) h.fail("cactus is not fire: no rage");
        });
    }

    // ---------------------------------------------------------------- rummaging

    @GameTest(template = EMPTY, batch = "seb_rummage", timeoutTicks = 400)
    public static void rummagingTakesOnlyPennies(GameTestHelper h) {
        SebastianEntity seb = spawnSeb(h, 2, 2);
        BlockPos chestPos = new BlockPos(9, 1, 9);
        h.setBlock(chestPos, Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) h.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(ModItems.PENNY.get(), 5));
        chest.setItem(1, new ItemStack(Items.DIAMOND, 3));
        chest.setItem(2, new ItemStack(Items.DIRT, 64));
        chest.setItem(9, new ItemStack(ModItems.PENNY.get(), 2));
        chest.setItem(10, new ItemStack(ModItems.ZINC_INGOT.get(), 7));
        if (!seb.forceState(SebState.RUMMAGING, null)) h.fail("he should find the chest");
        h.succeedWhen(() -> {
            if (seb.getSebState() != SebState.WATCHING) h.fail("still rummaging");
            int pennies = 0;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                if (PennyLogic.isPenny(chest.getItem(i))) pennies++;
            }
            if (pennies != 0) h.fail("pennies left in the chest");
            if (!chest.getItem(1).is(Items.DIAMOND) || chest.getItem(1).getCount() != 3) h.fail("diamonds were touched");
            if (!chest.getItem(2).is(Items.DIRT) || chest.getItem(2).getCount() != 64) h.fail("dirt was touched");
            if (!chest.getItem(10).is(ModItems.ZINC_INGOT.get()) || chest.getItem(10).getCount() != 7) h.fail("zinc was touched");
            if (seb.getPennies() != 7) h.fail("he should be carrying 7 pennies, has " + seb.getPennies());
        });
    }

    @GameTest(template = EMPTY)
    public static void barrelPennyRemovalLeavesOtherItems(GameTestHelper h) {
        floor(h);
        BlockPos pos = new BlockPos(3, 1, 3);
        h.setBlock(pos, Blocks.BARREL);
        BarrelBlockEntity barrel = (BarrelBlockEntity) h.getBlockEntity(pos);
        barrel.setItem(4, new ItemStack(ModItems.PENNY.get(), 12));
        barrel.setItem(5, new ItemStack(Items.GOLD_INGOT, 2));
        int taken = SebastianEntity.takePenniesFrom(barrel);
        if (taken != 12) h.fail("expected 12 pennies taken, got " + taken);
        if (!barrel.getItem(4).isEmpty()) h.fail("penny slot should be empty");
        if (barrel.getItem(5).getCount() != 2) h.fail("gold must stay");
        h.succeed();
    }

    // ---------------------------------------------------------------- begging / charging

    @GameTest(template = EMPTY, batch = "seb_give", timeoutTicks = 100)
    public static void givingAPennyWhileBegging(GameTestHelper h) {
        SebastianEntity seb = spawnSeb(h, 2, 2);
        ServerPlayer p = survivalPlayer(h, 4, 2);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PENNY.get(), 3));
        seb.forceState(SebState.BEGGING, p);
        if (seb.getSebState() != SebState.BEGGING) h.fail("should be begging");
        p.interactOn(seb, InteractionHand.MAIN_HAND);
        if (p.getMainHandItem().getCount() != 2) h.fail("he should take exactly one penny");
        if (seb.getSebState() != SebState.WATCHING) h.fail("he should go back to watching");
        if (!p.isAlive()) h.fail("generous players live");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "seb_beg_timeout", timeoutTicks = 400)
    public static void refusingToGiveLeadsToCharge(GameTestHelper h) {
        SebastianEntity seb = spawnSeb(h, 2, 2);
        ServerPlayer p = survivalPlayer(h, 10, 10);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PENNY.get(), 2));
        seb.forceState(SebState.BEGGING, p);
        // the player puts the penny away
        ItemStack put = p.getMainHandItem().copy();
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.getInventory().setItem(20, put); // into the backpack, out of his sight
        h.succeedWhen(() -> {
            if (p.isAlive()) h.fail("he should charge and kill the player");
            if (seb.getPennies() != 1) h.fail("he should have taken exactly one penny, has " + seb.getPennies());
        });
    }

    // ---------------------------------------------------------------- the boss fight

    @GameTest(template = EMPTY, batch = "boss_map", timeoutTicks = 60)
    public static void fifthRefusalGivesExhibitA(GameTestHelper h) {
        SebastianEntity seb = spawnSeb(h, 2, 2);
        ServerPlayer p = survivalPlayer(h, 4, 2);
        com.sebastianhili.horror.boss.BurnedPennyMap.resetRefusals(p);
        for (int i = 1; i <= 4; i++) {
            if (com.sebastianhili.horror.boss.BurnedPennyMap.onRefusal(seb, p)) h.fail("map given too early, at refusal " + i);
        }
        if (com.sebastianhili.horror.boss.BurnedPennyMap.getRefusals(p) != 4) h.fail("should count 4 refusals");
        if (!com.sebastianhili.horror.boss.BurnedPennyMap.onRefusal(seb, p)) h.fail("the 5th refusal should give the map");
        ItemStack map = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (com.sebastianhili.horror.boss.BurnedPennyMap.isMap(p.getInventory().getItem(i))) map = p.getInventory().getItem(i);
        }
        if (map.isEmpty()) h.fail("Exhibit A is not in the player's inventory");
        if (com.sebastianhili.horror.boss.BurnedPennyMap.getRefusals(p) != 0) h.fail("the count should reset after the map");
        Integer id = net.minecraft.world.item.MapItem.getMapId(map);
        var data = id == null ? null : h.getLevel().getServer().overworld().getMapData(net.minecraft.world.item.MapItem.makeKey(id));
        if (data == null) h.fail("map has no picture data");
        int painted = 0;
        for (byte b : data.colors) if (b != 0) painted++;
        if (painted < 128 * 128 / 2) h.fail("the burned penny picture was not painted onto the map (" + painted + " pixels)");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "boss_reset", timeoutTicks = 60)
    public static void givingAPennyResetsTheRefusalStreak(GameTestHelper h) {
        SebastianEntity seb = spawnSeb(h, 2, 2);
        ServerPlayer p = survivalPlayer(h, 4, 2);
        com.sebastianhili.horror.boss.BurnedPennyMap.setRefusals(p, 3);
        seb.acceptPenny(p, new ItemStack(ModItems.PENNY.get(), 1));
        if (com.sebastianhili.horror.boss.BurnedPennyMap.getRefusals(p) != 0) h.fail("a gift should reset the streak");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "boss_fight", timeoutTicks = 600)
    public static void showingTheMapStartsTheFightAndSurvivingSendsYouHome(GameTestHelper h) {
        var bossData = com.sebastianhili.horror.boss.BossData.get(h.getLevel().getServer());
        bossData.gone = false;
        SebastianEntity seb = spawnSeb(h, 2, 2);
        ServerPlayer p = survivalPlayer(h, 4, 2);
        Vec3 home = p.position();
        p.setItemInHand(InteractionHand.MAIN_HAND, com.sebastianhili.horror.boss.BurnedPennyMap.create(h.getLevel().getServer()));
        p.interactOn(seb, InteractionHand.MAIN_HAND);
        var fight = com.sebastianhili.horror.boss.BossFight.get();
        if (fight == null) h.fail("showing him the map should start the fight");
        if (p.level().dimension() != com.sebastianhili.horror.boss.BossFight.THE_RED) h.fail("the player should be in The Red");
        if (!p.getMainHandItem().isEmpty()) h.fail("he should take the map");
        if (!seb.isRemoved()) h.fail("the overworld Sebastian should go with you");
        if (p.level().getBlockState(p.blockPosition().below()).isAir()) h.fail("no arena floor under the player");
        if (!fight.boss().isBoss() || !fight.boss().isInvulnerable()) h.fail("the boss should be invulnerable");
        fight.skipTo(com.sebastianhili.horror.boss.BossFight.T_END - 3);
        h.succeedWhen(() -> {
            if (com.sebastianhili.horror.boss.BossFight.isActive()) h.fail("fight still running");
            if (p.level() != h.getLevel() || p.position().distanceTo(home) > 1.0) h.fail("not sent home");
            if (!bossData.gone) h.fail("surviving should make him gone for good");
            bossData.gone = false; // leave the world as we found it
        });
    }

    @GameTest(template = EMPTY, batch = "boss_death", timeoutTicks = 200)
    public static void dyingInTheRedKeepsYourItems(GameTestHelper h) {
        SebastianEntity seb = spawnSeb(h, 2, 2);
        ServerPlayer p = survivalPlayer(h, 4, 2);
        p.getInventory().add(new ItemStack(Items.DIAMOND, 5));
        if (!com.sebastianhili.horror.boss.BossFight.start(seb, p)) h.fail("fight did not start");
        ServerLevel red = p.serverLevel();
        p.kill();
        if (!red.getEntitiesOfClass(ItemEntity.class, new AABB(p.blockPosition()).inflate(5)).isEmpty()) h.fail("items were dropped in The Red");
        ServerPlayer back = h.getLevel().getServer().getPlayerList().respawn(p, false);
        if (back.getInventory().countItem(Items.DIAMOND) != 5) h.fail("the diamonds should come back on respawn");
        if (back.level().dimension() == com.sebastianhili.horror.boss.BossFight.THE_RED) h.fail("respawn should be outside The Red");
        h.succeedWhen(() -> {
            if (com.sebastianhili.horror.boss.BossFight.isActive()) h.fail("with nobody left the fight should end");
        });
    }

    @GameTest(template = EMPTY, batch = "seb_spawn", timeoutTicks = 60)
    public static void sebastianSpawnsWithConfiguredStats(GameTestHelper h) {
        SebastianEntity seb = spawnSeb(h, 5, 5);
        h.runAfterDelay(25, () -> {
            if (seb.getMaxHealth() < 100) h.fail("he should be tough, max health " + seb.getMaxHealth());
            if (!seb.hasCustomName() || !"Sebastian Hili".equals(seb.getCustomName().getString())) h.fail("nameplate should say Sebastian Hili");
            if (!seb.fireImmune()) h.fail("fire immune");
            if (seb.getSebState() != SebState.WATCHING) h.fail("default state is WATCHING");
            h.succeed();
        });
    }

}
