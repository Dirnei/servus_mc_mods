package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.GreyOut;
import io.github.dirnei.mc.servus.foundation.client.InlineStepper;
import io.github.dirnei.mc.servus.foundation.client.NumberStepper;
import io.github.dirnei.mc.servus.foundation.ui.AmountSteps;
import io.github.dirnei.mc.servus.shuttles.terminal.CraftPreviewPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.CraftPreviewRequestPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.ItemPopupTabs;
import io.github.dirnei.mc.servus.shuttles.terminal.ItemPopupTabs.Tab;
import io.github.dirnei.mc.servus.shuttles.terminal.RequestItemPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.RequestSplit;
import io.github.dirnei.mc.servus.shuttles.terminal.RuleInput;
import io.github.dirnei.mc.servus.shuttles.terminal.RuleRow;
import io.github.dirnei.mc.servus.shuttles.terminal.SetStockRulePayload;
import io.github.dirnei.mc.servus.shuttles.terminal.StockEntry;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Rect;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Storage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The Storage Terminal's item popup: Request, Craft and Rule tabs over one
 * panel for one item. The Request and Rule pages live here; the Craft page is
 * the screen's amount panel, which the screen shows when the host hears the
 * Craft tab. Tabs that cannot act for the item are greyed out with the reason.
 */
final class ItemPopup {

    /** What the popup asks of the screen that shows it. */
    interface Host {
        /** The tab now shown; the screen shows its amount panel for {@link Tab#CRAFT}. */
        void tabShown(Tab tab);

        /** Gives the field the screen's focus, so typed characters reach it. */
        void focus(EditBox field);

        void closePopup();
    }

    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_DIM = 0xFF707070;
    private static final int COLOUR_ALERT = 0xFFB03030;
    private static final int COLOUR_GOOD = 0xFF2E7D32;
    private static final int COLOUR_TAB_OFF = 0xFF8A8A8A;
    private static final int PREVIEW_ROWS = 2;
    private static final String KEY = "gui.servus_shuttles.terminal.";

    private final Host host;
    private ItemStack item = ItemStack.EMPTY;
    private long requestable;
    private boolean craftable;
    private ItemPopupTabs tabs = ItemPopupTabs.of(0, false, false);
    private Tab tab = Tab.REQUEST;
    private int left;
    private int top;

    private NumberStepper amount;
    private Button requestCancel;
    private Button requestButton;
    private String requestReason;
    private int askedAmount = -1;

    private InlineStepper orderBelow;
    private InlineStepper fillUpTo;
    private InlineStepper refuseAbove;
    private InlineStepper exportAbove;
    private Button removeButton;
    private Button ruleCancel;
    private Button saveButton;
    private String saveReason;

    ItemPopup(Host host) {
        this.host = host;
    }

    /** Builds the widgets for a screen at {@code left}, {@code top}, keeping the values shown; add them with {@code addRenderableWidget}. */
    List<AbstractWidget> build(Font font, int left, int top) {
        this.left = left;
        this.top = top;
        int keptAmount = amount == null ? AmountSteps.MIN : amount.value();
        amount = new NumberStepper(font, Component.translatable(KEY + "amount"), AmountSteps.MIN, AmountSteps.MAX, this::stackSize,
                value -> amountChanged(), 1, 10, AmountSteps.STACK);
        amount.place(left + Storage.CRAFT_STEPPER.x(), top + Storage.CRAFT_STEPPER.y());
        amount.setValue(keptAmount);
        Rect row = Storage.CRAFT_BUTTONS;
        requestCancel = Button.builder(Component.translatable(KEY + "cancel"), b -> host.closePopup())
                .bounds(left + row.x(), top + row.y(), Storage.BUTTON, row.h()).build();
        requestButton = Button.builder(Component.translatable(KEY + "request"), b -> confirm())
                .bounds(left + row.right() - Storage.WIDE_BUTTON, top + row.y(), Storage.WIDE_BUTTON, row.h()).build();
        requestReason = null;

        int keptMin = orderBelow == null ? 0 : orderBelow.value();
        int keptMax = fillUpTo == null ? AmountSteps.MIN : fillUpTo.value();
        int keptCap = refuseAbove == null ? 0 : refuseAbove.value();
        int keptExport = exportAbove == null ? -1 : exportAbove.value();
        orderBelow = ruleField(font, "rule.order_below", Storage.RULE_MAKE, 0, 0, Integer.MIN_VALUE, keptMin);
        fillUpTo = ruleField(font, "rule.fill_up_to", Storage.RULE_MAKE, 1, 0, 0, keptMax);
        exportAbove = ruleField(font, "rule.export_above", Storage.RULE_LIMIT, 0, -1, -1, keptExport);
        refuseAbove = ruleField(font, "rule.refuse_above", Storage.RULE_LIMIT, 1, 0, 0, keptCap);
        Rect buttons = Storage.RULE_BUTTONS;
        removeButton = Button.builder(Component.translatable(KEY + "rule_remove"), b -> removeRule())
                .bounds(left + buttons.x(), top + buttons.y(), Storage.BUTTON, buttons.h()).build();
        int saveX = left + buttons.right() - Storage.WIDE_BUTTON;
        saveButton = Button.builder(Component.translatable(KEY + "rule_save"), b -> confirm())
                .bounds(saveX, top + buttons.y(), Storage.WIDE_BUTTON, buttons.h()).build();
        ruleCancel = Button.builder(Component.translatable(KEY + "cancel"), b -> host.closePopup())
                .bounds(saveX - 2 - Storage.BUTTON, top + buttons.y(), Storage.BUTTON, buttons.h()).build();
        saveReason = null;
        return widgets();
    }

    /**
     * A field of a Make or Limit row from {@code lowest} up, showing
     * {@code off} as "off"; {@link Integer#MIN_VALUE} for a field that is never off.
     */
    private InlineStepper ruleField(Font font, String key, Rect group, int column, int lowest, int off, int value) {
        InlineStepper stepper = new InlineStepper(font, Component.translatable(KEY + key), lowest, AmountSteps.MAX, this::stackSize, changed -> { });
        if (off != Integer.MIN_VALUE) {
            stepper.offAt(off, Component.translatable(KEY + "rule.off"));
        }
        stepper.place(left + Storage.ruleFieldX(group, column), top + group.y() + Storage.RULE_FIELD_LABEL, Storage.RULE_FIELD);
        stepper.setValue(value);
        return stepper;
    }

    private List<InlineStepper> ruleFields() {
        return List.of(orderBelow, fillUpTo, exportAbove, refuseAbove);
    }

    private List<AbstractWidget> requestWidgets() {
        List<AbstractWidget> widgets = new ArrayList<>(amount.widgets());
        widgets.add(requestCancel);
        widgets.add(requestButton);
        return widgets;
    }

    private List<AbstractWidget> ruleWidgets() {
        List<AbstractWidget> widgets = new ArrayList<>();
        ruleFields().forEach(field -> widgets.addAll(field.widgets()));
        widgets.addAll(List.of(removeButton, ruleCancel, saveButton));
        return widgets;
    }

    List<AbstractWidget> widgets() {
        List<AbstractWidget> widgets = requestWidgets();
        widgets.addAll(ruleWidgets());
        return widgets;
    }

    /** The widgets of the page shown, for clicks and drawing over the panel. */
    List<AbstractWidget> pageWidgets() {
        return switch (tab) {
            case REQUEST -> requestWidgets();
            case RULE -> ruleWidgets();
            case CRAFT -> List.of();
        };
    }

    ItemStack item() {
        return item;
    }

    Tab tab() {
        return tab;
    }

    private int stackSize() {
        return item.isEmpty() ? 64 : item.getMaxStackSize();
    }

    /**
     * Opens the popup for one of {@code stack} on {@code wanted}, or on the
     * first tab that can act when that one cannot.
     *
     * @param count how many of the item the warehouse can ship
     */
    void open(ItemStack stack, long count, boolean canCraft, boolean warehouse, Tab wanted) {
        item = stack.copyWithCount(1);
        requestable = count;
        craftable = canCraft;
        tabs = ItemPopupTabs.of(count, canCraft, warehouse);
        amount.setValue(RequestSplit.prefill(stackSize(), requestable, craftable));
        RuleRow rule = ClientStock.ruleFor(item);
        orderBelow.setValue(rule == null || !rule.hasMake() ? 0 : rule.min());
        fillUpTo.setValue(rule == null ? stackSize() : rule.hasMake() ? rule.max() : 0);
        refuseAbove.setValue(rule == null ? 0 : rule.cap());
        exportAbove.setValue(rule == null ? -1 : rule.export());
        tab = wanted;
        if (!tabs.enabled(wanted)) {
            for (Tab other : Tab.values()) {
                if (tabs.enabled(other)) {
                    tab = other;
                    break;
                }
            }
        }
        show();
    }

    /** Shows the tab when it can act for the item. */
    void select(Tab wanted) {
        if (wanted != tab && tabs.enabled(wanted)) {
            tab = wanted;
            show();
        }
    }

    private void show() {
        ClientStock.clearPreview();
        askedAmount = -1;
        setVisible(true);
        host.tabShown(tab);
        if (tab == Tab.REQUEST) {
            focus(amount.field());
            askPreview();
        } else if (tab == Tab.RULE) {
            focus(fillUpTo.field());
        }
    }

    private void focus(EditBox field) {
        for (AbstractWidget widget : widgets()) {
            if (widget != field) {
                widget.setFocused(false);
            }
        }
        host.focus(field);
    }

    /** Shows the widgets of the shown page while the popup is open, none while it is closed. */
    void setVisible(boolean open) {
        requestWidgets().forEach(widget -> widget.visible = open && tab == Tab.REQUEST);
        ruleWidgets().forEach(widget -> widget.visible = open && tab == Tab.RULE);
        if (!open) {
            widgets().forEach(widget -> widget.setFocused(false));
        }
    }

    void tick() {
        amount.tick();
        ruleFields().forEach(InlineStepper::tick);
    }

    /** The tab under the mouse on the strip, null elsewhere. */
    private Tab tabAt(Font font, double mouseX, double mouseY) {
        List<Rect> rects = tabRects(font);
        for (int i = 0; i < rects.size(); i++) {
            if (rects.get(i).contains((int) mouseX - left, (int) mouseY - top)) {
                return Tab.values()[i];
            }
        }
        return null;
    }

    private List<Rect> tabRects(Font font) {
        Tab[] all = Tab.values();
        int[] widths = new int[all.length];
        for (int i = 0; i < all.length; i++) {
            widths[i] = font.width(tabLabel(all[i]));
        }
        return TerminalLayout.compactTabs(Storage.POPUP_TABS, widths, Storage.TAB_PADDING);
    }

    private static Component tabLabel(Tab tab) {
        return Component.translatable(KEY + "popup." + tab.name().toLowerCase(Locale.ROOT));
    }

    /** A click on the tab strip: shows an enabled tab; true when the click was on a tab. */
    boolean clickTab(Font font, double mouseX, double mouseY) {
        Tab clicked = tabAt(font, mouseX, mouseY);
        if (clicked == null) {
            return false;
        }
        select(clicked);
        return true;
    }

    /** The tab strip over the dimmed title row, joined to the popup panel; a greyed tab says why in its tooltip. */
    void renderTabs(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        List<Rect> rects = tabRects(font);
        Tab[] all = Tab.values();
        for (int i = 0; i < rects.size(); i++) {
            Rect rect = rects.get(i);
            boolean active = all[i] == tab;
            PanelPainter.tab(graphics, left + rect.x(), top + rect.y(), rect.w(), rect.h(), active, 2);
            int colour = !tabs.enabled(all[i]) ? COLOUR_TAB_OFF : COLOUR_TEXT;
            Component label = tabLabel(all[i]);
            int textY = top + rect.y() + (active ? 4 : 5);
            graphics.text(font, label, left + rect.x() + (rect.w() - font.width(label)) / 2, textY, colour, false);
        }
        Tab hovered = tabAt(font, mouseX, mouseY);
        if (hovered != null && !tabs.enabled(hovered)) {
            graphics.setTooltipForNextFrame(font, Component.translatable(KEY + "tab_reason." + tabs.reason(hovered)), mouseX, mouseY);
        }
    }

    /** Draws the Request or Rule page over the popup panel. */
    void renderPage(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        if (tab == Tab.REQUEST) {
            renderRequest(graphics, font, mouseX, mouseY);
        } else if (tab == Tab.RULE) {
            renderRule(graphics, font, mouseX, mouseY);
        }
        for (AbstractWidget widget : pageWidgets()) {
            if (widget.visible) {
                widget.extractRenderState(graphics, mouseX, mouseY, 0);
            }
        }
    }

    private void text(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int width, int colour) {
        graphics.text(font, font.plainSubstrByWidth(text.getString(), width), left + x, top + y, colour, false);
    }

    private void amountChanged() {
        int cap = RequestSplit.cap(requestable, craftable);
        if (amount.value() > cap) {
            amount.setValue(cap);
        }
        askPreview();
    }

    /** Asks the server how the rest of an amount above the requestable count would be crafted. */
    private void askPreview() {
        int wanted = amount.value();
        if (tab == Tab.REQUEST && !item.isEmpty() && wanted > requestable && wanted != askedAmount) {
            askedAmount = wanted;
            ClientPacketDistributor.sendToServer(new CraftPreviewRequestPayload(item, wanted, CraftPreviewRequestPayload.Mode.REQUEST));
        }
    }

    private CraftPreviewPayload currentPreview() {
        CraftPreviewPayload preview = ClientStock.preview();
        if (preview == null || preview.amount() != amount.value() || !ItemStack.isSameItemSameComponents(preview.item(), item)) {
            return null;
        }
        return preview;
    }

    /** Why Request cannot be pressed, as a lang key, or null when it can. */
    private String requestRefusal() {
        if (amount.value() <= requestable) {
            return null;
        }
        CraftPreviewPayload preview = currentPreview();
        if (preview == null) {
            return KEY + "craft_wait";
        }
        if (preview.requestable()) {
            return null;
        }
        if (!preview.refusal().isEmpty()) {
            return KEY + "message." + preview.refusal();
        }
        return KEY + "message.missing";
    }

    private void renderRequest(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        Rect title = Storage.CRAFT_TITLE;
        text(graphics, font, Component.translatable(KEY + "request_title", item.getHoverName()), title.x(), title.y(), title.w(), COLOUR_TEXT);
        requestReason = GreyOut.apply(requestButton, requestReason, requestRefusal());
        Rect at = Storage.CRAFT_ITEM;
        ItemStack shown = item.copyWithCount(amount.value());
        graphics.item(shown, left + at.x(), top + at.y());
        graphics.itemDecorations(font, shown, left + at.x(), top + at.y());
        if (mouseX >= left + at.x() && mouseX < left + at.x() + 16 && mouseY >= top + at.y() && mouseY < top + at.y() + 16) {
            graphics.setTooltipForNextFrame(font, item, mouseX, mouseY);
        }
        Rect lists = Storage.CRAFT_LISTS;
        int wanted = amount.value();
        if (wanted <= requestable) {
            text(graphics, font, Component.translatable(KEY + "request_lands"), lists.x(), lists.y(), lists.w(), COLOUR_DIM);
            return;
        }
        CraftPreviewPayload preview = currentPreview();
        if (preview == null) {
            text(graphics, font, Component.translatable(KEY + "craft_wait"), lists.x(), lists.y(), lists.w(), COLOUR_DIM);
            return;
        }
        Component split = Component.translatable(KEY + "request_split", preview.inStock(), wanted - preview.inStock());
        text(graphics, font, split, lists.x(), lists.y(), lists.w(), COLOUR_TEXT);
        int columnY = lists.y() + 10;
        previewColumn(graphics, font, Storage.craftListX(0), columnY, KEY + "craft_from_stock", preview.fromStock(), COLOUR_TEXT);
        previewColumn(graphics, font, Storage.craftListX(1), columnY, KEY + "craft_to_craft", preview.toCraft(), COLOUR_GOOD);
        previewColumn(graphics, font, Storage.craftListX(2), columnY, KEY + "craft_missing", preview.missing(), COLOUR_ALERT);
    }

    /** One preview list: its heading, up to two half size rows, the last saying how many more there are when they do not fit. */
    private void previewColumn(GuiGraphicsExtractor graphics, Font font, int x, int y, String heading, List<StockEntry> rows, int colour) {
        int width = Storage.CRAFT_LIST_WIDTH;
        text(graphics, font, Component.translatable(heading), x, y, width, colour);
        int shown = rows.size() > PREVIEW_ROWS ? PREVIEW_ROWS - 1 : rows.size();
        for (int i = 0; i < shown; i++) {
            StockEntry row = rows.get(i);
            int rowY = top + y + 10 + i * 9;
            graphics.pose().pushMatrix();
            graphics.pose().translate(left + x, rowY);
            graphics.pose().scale(0.5F, 0.5F);
            graphics.item(row.stack(), 0, 0);
            graphics.pose().popMatrix();
            String line = row.count() + " " + row.stack().getHoverName().getString();
            graphics.text(font, font.plainSubstrByWidth(line, width - 10), left + x + 10, rowY, COLOUR_TEXT, false);
        }
        if (shown < rows.size()) {
            PanelPainter.smallText(graphics, font, Component.translatable(KEY + "craft_more", rows.size() - shown),
                    left + x + width, top + y + 10 + shown * 9 + 2, COLOUR_DIM);
        }
    }

    private RuleRow rule() {
        return ClientStock.ruleFor(item);
    }

    private void renderRule(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        Rect title = Storage.RULE_TITLE;
        graphics.item(item, left + title.x(), top + title.y());
        RuleRow rule = rule();
        Component state = rule == null ? Component.translatable(KEY + "rule.none") : stateText(rule);
        Component stock = Component.translatable(KEY + "rule.in_stock", requestable);
        int stateWidth = font.width(state);
        int stockWidth = font.width(stock);
        int textY = title.y() + 4;
        int right = title.right();
        graphics.text(font, state, left + right - stateWidth, top + textY, rule == null ? COLOUR_DIM : stateColour(rule.state()), false);
        graphics.text(font, stock, left + right - stateWidth - 8 - stockWidth, top + textY, COLOUR_DIM, false);
        int nameWidth = title.w() - 20 - stateWidth - stockWidth - 16;
        text(graphics, font, item.getHoverName(), title.x() + 20, textY, nameWidth, COLOUR_TEXT);

        renderGroup(graphics, font, Storage.RULE_MAKE, KEY + "rule.make", KEY + "rule.order_below", KEY + "rule.fill_up_to");
        renderGroup(graphics, font, Storage.RULE_LIMIT, KEY + "rule.limit", KEY + "rule.export_above", KEY + "rule.refuse_above");
        String refusal = input().refusal();
        Rect note = Storage.RULE_NOTE;
        if (refusal != null) {
            text(graphics, font, Component.translatable(KEY + refusal), note.x(), note.y(), note.w(), COLOUR_ALERT);
        }
        if (exportAbove.value() >= 0) {
            boolean held = rule != null && rule.hasExport() && !rule.exportState().equals("no_station");
            Component line = held ? Component.translatable(KEY + "rule.export_goes_to", rule.holder())
                    : Component.translatable(KEY + "rule.export_no_station");
            text(graphics, font, line, note.x(), note.y() + 10, note.w(), held ? COLOUR_TEXT : COLOUR_ALERT);
        }
        saveReason = GreyOut.apply(saveButton, saveReason, refusal == null ? null : KEY + refusal);
        removeButton.active = rule != null;
    }

    /** A group row: its label in the left column and a label over each of its fields; a null field label leaves that column empty. */
    private void renderGroup(GuiGraphicsExtractor graphics, Font font, Rect group, String label, String first, String second) {
        int labelWidth = Storage.RULE_GROUP_LABEL - 2;
        text(graphics, font, Component.translatable(label), group.x(), group.y() + Storage.RULE_FIELD_LABEL + 3, labelWidth, COLOUR_TEXT);
        String[] fields = {first, second};
        for (int column = 0; column < fields.length; column++) {
            if (fields[column] != null) {
                text(graphics, font, Component.translatable(fields[column]), Storage.ruleFieldX(group, column), group.y(), Storage.RULE_FIELD,
                        COLOUR_TEXT);
            }
        }
    }

    /** A rule's state as players read it: "refused at Inbound #1" names the station. */
    static Component stateText(RuleRow rule) {
        return rule.state().equals("refusing") ? Component.translatable(KEY + "rule_state.refusing", rule.detail())
                : Component.translatable(KEY + "rule_state." + rule.state());
    }

    private RuleInput input() {
        return RuleInput.of(orderBelow.value(), fillUpTo.value(), refuseAbove.value(), exportAbove.value());
    }

    /** The colour of a rule state in the popup and the Rules tab. */
    static int stateColour(String state) {
        return switch (state) {
            case "ok" -> COLOUR_GOOD;
            case "ordering" -> 0xFF8A5A00;
            default -> COLOUR_ALERT;
        };
    }

    private void saveRule() {
        RuleInput input = input();
        if (input.refusal() != null) {
            return;
        }
        ClientPacketDistributor.sendToServer(new SetStockRulePayload(item, input.min(), input.max(), input.cap(), input.export(), false));
        host.closePopup();
    }

    private void removeRule() {
        if (rule() != null) {
            ClientPacketDistributor.sendToServer(SetStockRulePayload.removal(item));
        }
        host.closePopup();
    }

    /** Request or Save, as Enter does; the Craft page is the screen's. */
    void confirm() {
        if (tab == Tab.REQUEST) {
            amount.commit();
            if (requestRefusal() != null) {
                return;
            }
            ClientPacketDistributor.sendToServer(new RequestItemPayload(item, amount.value()));
            host.closePopup();
        } else if (tab == Tab.RULE) {
            ruleFields().forEach(InlineStepper::commit);
            saveRule();
        }
    }

    /** A key while the popup shows the Request or Rule page: Enter confirms, everything else goes to the focused field. */
    boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
            return true;
        }
        EditBox field = focusedField();
        if (field != null) {
            field.keyPressed(event);
        }
        return true;
    }

    private EditBox focusedField() {
        if (tab == Tab.REQUEST) {
            return amount.field();
        }
        if (tab == Tab.RULE) {
            return ruleFields().stream().map(InlineStepper::field).filter(EditBox::isFocused).findFirst().orElse(fillUpTo.field());
        }
        return null;
    }

    /** The wheel over a field of the shown page; true when it stepped one. */
    boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        return switch (tab) {
            case REQUEST -> amount.mouseScrolled(mouseX, mouseY, scrollY);
            case RULE -> ruleFields().stream().anyMatch(field -> field.mouseScrolled(mouseX, mouseY, scrollY));
            case CRAFT -> false;
        };
    }
}
