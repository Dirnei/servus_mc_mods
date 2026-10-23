package io.github.dirnei.mc.servus.alchemy.athanor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.Placed;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BlockAtPropertiesTest {

    private static final String VAT = "servus_alchemy:principle_vat";

    @Test
    void propertiesMustMatch() {
        BlockAt grown = BlockAt.of(0, -1, 3, VAT, Map.of("crystal", "citrine"));
        assertTrue(grown.matches(Placed.of(VAT, Map.of("crystal", "citrine", "facing", "east"))), "other properties may hold anything");
        assertFalse(grown.matches(Placed.of(VAT, Map.of("crystal", "none"))), "a plain vat is not grown");
        assertFalse(grown.matches(Placed.of(VAT, Map.of())), "a block without the property does not match");
        assertFalse(grown.matches(Placed.of("minecraft:stone", Map.of("crystal", "citrine"))), "the id must match too");
        assertFalse(grown.matches(null), "nothing there");
    }

    @Test
    void idOnlyMatchesAnyState() {
        BlockAt vat = new BlockAt(0, -1, 3, VAT);
        assertTrue(vat.properties().isEmpty());
        assertTrue(vat.matches(Placed.of(VAT, Map.of("crystal", "none"))));
        assertTrue(vat.matches(Placed.of(VAT, Map.of("crystal", "citrine"))));
        assertFalse(vat.matches(Placed.of("minecraft:stone", Map.of())));
    }

    @Test
    void turningKeepsTheProperties() {
        BlockAt grown = BlockAt.of(0, -1, 3, VAT, Map.of("crystal", "citrine"));
        assertEquals(BlockAt.of(-3, -1, 0, VAT, Map.of("crystal", "citrine")), AthanorTiers.turn(grown, Facing.EAST));
    }
}
