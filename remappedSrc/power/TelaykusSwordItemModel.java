package power;

import net.minecraft.item.Item;
import org.intellij.lang.annotations.Identifier;

public class TelaykusSwordItemModel extends ItemModel {
    public TelaykusSwordItemModel(Item item) {
        super(item, org.intellij.lang.annotations.Identifier.of("telaykus", "telaykus_sword"));
    }
}

public class telaykus_sword implements ModInitializer {
    @Override
    public void onInitialize() {
        TelaykusSwordItemModel.initialize();
    }
}