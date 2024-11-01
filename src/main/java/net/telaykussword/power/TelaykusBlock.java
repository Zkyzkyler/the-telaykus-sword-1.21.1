package net.telaykussword.power;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.sound.BlockSoundGroup;

public class TelaykusBlock extends Block {
    public TelaykusBlock() {
        super(Settings.copy(Blocks.IRON_BLOCK)
                .requiresTool()
                .sounds(BlockSoundGroup.METAL));
    }
}
