package com.sebastianhili.horror.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds the boss arena in The Red: a crimson island floating on a lava sea, with pillars for cover,
 * a few small lava pools, shroomlights for light and lavafalls pouring off the far rocks.
 * Rebuilt fresh at the start of every fight (anything players built last time is cleared).
 */
public final class RedArena {
    public static final int PLATFORM_RADIUS = 24;
    private static final int CLEAR_RADIUS = 34;
    private static final int SEA_RADIUS = 52;
    private static final int FLAGS = 2; // update clients, no neighbour updates

    public static void build(ServerLevel level, BlockPos c, long seed) {
        RandomSource rng = RandomSource.create(seed);
        int cy = c.getY();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();

        // clear the space above and around the island
        for (int x = -CLEAR_RADIUS; x <= CLEAR_RADIUS; x++) {
            for (int z = -CLEAR_RADIUS; z <= CLEAR_RADIUS; z++) {
                if (x * x + z * z > CLEAR_RADIUS * CLEAR_RADIUS) continue;
                for (int y = cy - 7; y <= cy + 26; y++) {
                    m.set(c.getX() + x, y, c.getZ() + z);
                    if (!level.getBlockState(m).isAir()) level.setBlock(m, air, FLAGS);
                }
            }
        }

        // the lava sea: a netherrack basin with a rim so nothing flows into the void
        for (int x = -SEA_RADIUS - 2; x <= SEA_RADIUS + 2; x++) {
            for (int z = -SEA_RADIUS - 2; z <= SEA_RADIUS + 2; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r > SEA_RADIUS + 2) continue;
                m.set(c.getX() + x, cy - 9, c.getZ() + z);
                level.setBlock(m, Blocks.NETHERRACK.defaultBlockState(), FLAGS);
                m.setY(cy - 8);
                level.setBlock(m, r < SEA_RADIUS ? Blocks.LAVA.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState(), FLAGS);
            }
        }

        // the island: ragged edge, tapering underneath into the lava
        double[] wobble = new double[8];
        for (int i = 0; i < wobble.length; i++) wobble[i] = rng.nextDouble() * Math.PI * 2;
        for (int x = -PLATFORM_RADIUS - 4; x <= PLATFORM_RADIUS + 4; x++) {
            for (int z = -PLATFORM_RADIUS - 4; z <= PLATFORM_RADIUS + 4; z++) {
                double r = Math.sqrt(x * x + z * z);
                double a = Math.atan2(z, x);
                double edge = PLATFORM_RADIUS + 2.2 * Math.sin(3 * a + wobble[0]) + 1.4 * Math.sin(7 * a + wobble[1]);
                if (r > edge) continue;
                for (int d = 0; d <= 8; d++) {
                    if (r > edge - d * 2.6) break;
                    m.set(c.getX() + x, cy - d, c.getZ() + z);
                    BlockState s;
                    if (d == 0) {
                        int roll = rng.nextInt(100);
                        s = roll < 58 ? Blocks.CRIMSON_NYLIUM.defaultBlockState()
                                : roll < 82 ? Blocks.NETHERRACK.defaultBlockState()
                                : roll < 91 ? Blocks.NETHER_WART_BLOCK.defaultBlockState()
                                : roll < 97 ? Blocks.BLACKSTONE.defaultBlockState()
                                : Blocks.SHROOMLIGHT.defaultBlockState();
                    } else {
                        s = rng.nextInt(12) == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState();
                    }
                    level.setBlock(m, s, FLAGS);
                }
                // crimson roots and fungi on the nylium
                m.set(c.getX() + x, cy, c.getZ() + z);
                if (level.getBlockState(m).is(Blocks.CRIMSON_NYLIUM) && r > 9 && rng.nextInt(9) == 0) {
                    m.setY(cy + 1);
                    level.setBlock(m, (rng.nextInt(4) == 0 ? Blocks.CRIMSON_FUNGUS : Blocks.CRIMSON_ROOTS).defaultBlockState(), FLAGS);
                }
            }
        }

        // four small lava pools ringed with magma: lava around, but not everywhere
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 4 + i * Math.PI / 2 + rng.nextDouble() * 0.3;
            int px = (int) Math.round(Math.cos(a) * 14), pz = (int) Math.round(Math.sin(a) * 14);
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    double r = Math.sqrt(x * x + z * z);
                    m.set(c.getX() + px + x, cy, c.getZ() + pz + z);
                    if (r <= 2.0) {
                        level.setBlock(m, Blocks.LAVA.defaultBlockState(), FLAGS);
                        level.setBlock(m.move(Direction.UP), air, FLAGS);
                    } else if (r <= 2.9) {
                        level.setBlock(m, Blocks.MAGMA_BLOCK.defaultBlockState(), FLAGS);
                        level.setBlock(m.move(Direction.UP), air, FLAGS);
                    }
                }
            }
        }

        // six pillars at the edge of the fight: cover from pennies and dashes
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3 + Math.PI / 6;
            int px = (int) Math.round(Math.cos(a) * 18), pz = (int) Math.round(Math.sin(a) * 18);
            int h = 4 + rng.nextInt(5);
            boolean stem = i % 2 == 0;
            for (int y = 1; y <= h; y++) {
                for (int dx = 0; dx <= 1; dx++) {
                    for (int dz = 0; dz <= 1; dz++) {
                        m.set(c.getX() + px + dx, cy + y, c.getZ() + pz + dz);
                        BlockState s = stem ? Blocks.CRIMSON_STEM.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y)
                                : Blocks.RED_NETHER_BRICKS.defaultBlockState();
                        if (y == h && rng.nextBoolean()) s = Blocks.SHROOMLIGHT.defaultBlockState();
                        level.setBlock(m, s, FLAGS);
                    }
                }
            }
            m.set(c.getX() + px, cy + h + 1, c.getZ() + pz);
            level.setBlock(m, Blocks.NETHER_WART_BLOCK.defaultBlockState(), FLAGS);
        }

        // distant rocks with lava pouring off them into the sea
        for (int i = 0; i < 5; i++) {
            double a = rng.nextDouble() * Math.PI * 2;
            int px = (int) Math.round(Math.cos(a) * 33), pz = (int) Math.round(Math.sin(a) * 33);
            int top = cy + 8 + rng.nextInt(8);
            for (int y = cy - 7; y <= top; y++) {
                int rad = y > top - 3 ? 2 : 1;
                for (int dx = -rad; dx <= rad; dx++) {
                    for (int dz = -rad; dz <= rad; dz++) {
                        m.set(c.getX() + px + dx, y, c.getZ() + pz + dz);
                        level.setBlock(m, (rng.nextInt(5) == 0 ? Blocks.BLACKSTONE : Blocks.NETHERRACK).defaultBlockState(), FLAGS);
                    }
                }
            }
            // lava source on the inner face of the rock, falling into the sea
            int ix = (int) Math.round(Math.cos(a) * 30), iz = (int) Math.round(Math.sin(a) * 30);
            m.set(c.getX() + ix, top - 1, c.getZ() + iz);
            level.setBlock(m, Blocks.LAVA.defaultBlockState(), 3);
        }
    }

    private RedArena() {
    }
}
