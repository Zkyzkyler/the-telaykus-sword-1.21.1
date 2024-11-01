package power;

import net.minecraft.world.Item.Item;
import net.fabricmc.fabric.api.object.builder.v1.item.FabricItemBuilder;
import net.minecraft.world.item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import org.intellij.lang.annotations.Identifier;

public class TelaykusSwordRegistry {
    public static final Item TELAYKUS_SWORD = FabricItemBuilder.create(ItemGroup.COMBAT).build();

    public static void init() {
        Registry.register(RegistryKeys.ITEM, Identifier.of("telaykus", "telaykus_sword"), TELAYKUS_SWORD);
    }
}