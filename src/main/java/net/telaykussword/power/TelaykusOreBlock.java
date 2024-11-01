package net.telaykussword.power;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class TelaykusOreBlock extends Block {
    public TelaykusOreBlock() {
        super(Settings.copy(Blocks.STONE)
                .requiresTool()
                .sounds(BlockSoundGroup.STONE));
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        super.onBreak(world, pos, state, player);

        BlockPos spawnPos = player.getBlockPos();

        if (spawnPos != null && world.isAir(spawnPos)) {
            world.setBlockState(spawnPos, this.getDefaultState(), 3);
            world.emitGameEvent(player, GameEvent.BLOCK_PLACE, spawnPos);
        }
        return state;
    }
}
