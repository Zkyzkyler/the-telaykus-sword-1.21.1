package net.telaykussword.power;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class TelaykusSwordMod implements ModInitializer {
    public static final String MODID = "ts";
    private static final Logger LOGGER = LoggerFactory.getLogger(TelaykusSwordMod.class);

    private static KeyBinding dashKey;

    @Override
    public void onInitialize() {
        // Register key binding for dashing with "Q"
        dashKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.telaykussword.dash", // ID
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Q, // "Q" key
                "category.telaykussword" // Category in controls
        ));

        // Listen for dash key press
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (dashKey.wasPressed()) {
                PlayerEntity player = client.player;
                if (player != null) {
                    ItemStack heldItem = player.getMainHandStack();
                    if (heldItem.getItem() instanceof TelaykusSword) {
                        ((TelaykusSword) heldItem.getItem()).tryDash(client.world, player);
                    }
                }
            }
        });
    }


    public static final Item TELAYKUS_SWORD = new TelaykusSword(new Item.Settings().maxDamage(5000));
    public static final Item TELAYKUS_INGOT = new Item(new Item.Settings());
    public static final Block TELAYKUS_ORE = new TelaykusOreBlock();
    public static final Block TELAYKUS_BLOCK = new TelaykusBlock(); // Ensure this class is implemented

    @Override
    public void onInitialize() {
        // Register items and blocks
        Registry.register(Registries.ITEM, id("telaykus_sword"), TELAYKUS_SWORD);
        Registry.register(Registries.ITEM, id("telaykus_ingot"), TELAYKUS_INGOT);
        Registry.register(Registries.BLOCK, id("telaykus_ore"), TELAYKUS_ORE);
        Registry.register(Registries.BLOCK, id("telaykus_block"), TELAYKUS_BLOCK);
        Registry.register(Registries.ITEM, id("telaykus_ore"), new BlockItem(TELAYKUS_ORE, new Item.Settings()));
        Registry.register(Registries.ITEM, id("telaykus_block"), new BlockItem(TELAYKUS_BLOCK, new Item.Settings())); // Register block item for TELAYKUS_BLOCK

        // Modify item groups
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(itemGroup -> {
            itemGroup.add(TELAYKUS_SWORD);
            itemGroup.add(TELAYKUS_INGOT);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(itemGroup -> {
            itemGroup.add(TELAYKUS_ORE);
            itemGroup.add(TELAYKUS_BLOCK);
        });
    }

    public static Identifier id(String path) {
        return Identifier.of(MODID, path);
    }
}
