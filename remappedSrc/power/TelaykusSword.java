package power;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;

public class TelaykusSword extends SwordItem {

    public TelaykusSword(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
        super(material, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, PlayerEntity target, PlayerEntity attacker) {
        stack.damage(1, attacker, (e) -> e.sendToolBreakStatus(Hand.MAIN_HAND));
        return true;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.RARE; // Set item rarity to rare
    }

    @Environment(EnvType.CLIENT)
    public static void registerKeyBinding(PlayerEntity player, World world) {
        // Listen for key events on the client
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.player.isAlive()) {
                if (client.options.keyAttack.wasPressed()) { // Assuming keyAttack is bound to 'R'
                    if (!player.isSneaking() && !player.hasVehicle()) { // Prevent when sneaking or riding
                        summonLightning(player, world);
                    }
                }
            }
        });
    }

    private static void summonLightning(PlayerEntity player, World world) {
        for (int i = 0; i < 5; i++) {
            LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
            lightning.setPos(player.getX(), player.getY(), player.getZ()); // Set lightning position to player position
            world.spawnEntity(lightning);
        }
    }
}
