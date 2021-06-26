package fr.atesab.xray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public class XrayBlockMenu extends Screen {
    private static final Text ADD = new LiteralText("+").formatted(Formatting.GREEN);
    private static final Text REPLACE = new TranslatableText("x13.mod.menu.replace").formatted(Formatting.YELLOW);
    private static final Text DELETE = new TranslatableText("x13.mod.menu.delete").formatted(Formatting.RED);
    private Screen parent;
    private XrayMode mode;
    private List<Block> config;
    private List<Block> visible = new ArrayList<>();
    private TextFieldWidget searchBar;
    private ButtonWidget nextPage;
    private ButtonWidget lastPage;
    private int elementByPage = 1;
    private int elementsX = 1;
    private int elementsY = 1;
    private int page = 0;

    public XrayBlockMenu(Screen parent, XrayMode mode) {
        super(new LiteralText(mode.getNameTranslate()));
        this.mode = mode;
        this.parent = parent;
        this.config = new ArrayList<>();
        this.config.addAll(mode.getBlocks());
    }

    @Override
    protected void init() {
        int sizeX = Math.min(width, 400);
        int sizeY = Math.min(height - 48, 400);

        elementsX = sizeX / 18;
        elementsY = sizeY / 18;
        elementByPage = elementsX * elementsY;

        int pageTop = height / 2 - sizeY / 2 - 24;
        int pageBottom = height / 2 + sizeY / 2 + 2;

        searchBar = new TextFieldWidget(textRenderer, width / 2 - sizeX / 2, pageTop + 2, sizeX, 16,
                new LiteralText("")) {
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 1 && mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y
                        && mouseY <= this.y + this.height) {
                    setText("");
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public void setText(String text) {
                super.setText(text);
                updateSearch();
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (super.keyPressed(keyCode, scanCode, modifiers)) {
                    updateSearch();
                    return true;
                }
                return false;
            }

            @Override
            public boolean charTyped(char chr, int modifiers) {
                if (super.charTyped(chr, modifiers)) {
                    updateSearch();
                    return true;
                }
                return false;
            }
        };

        lastPage = new ButtonWidget(width / 2 - 200, pageBottom, 20, 20, new LiteralText("<-"), b -> {
            page--;
            updateArrows();
        });

        ButtonWidget doneBtn = new ButtonWidget(width / 2 - 4 - 114 - 116 / 2, pageBottom, 114, 20,
                new TranslatableText("gui.done"), b -> {
                    mode.setBlocks(config);
                    client.openScreen(parent);
                });

        ButtonWidget exportBtn = new ButtonWidget(width / 2 - 116 / 2, pageBottom, 116, 20,
                new TranslatableText("x13.mod.menu.extract"), b -> {
                    client.openScreen(new ExtractionScreen(this, mode));
                });

        ButtonWidget cancelBtn = new ButtonWidget(width / 2 + 4 + 116 / 2, pageBottom, 114, 20,
                new TranslatableText("gui.cancel"), b -> {
                    client.openScreen(parent);
                });
        nextPage = new ButtonWidget(width / 2 + 200 - 20, pageBottom, 20, 20, new LiteralText("->"), b -> {
            page++;
            updateArrows();
        });

        addSelectableChild(searchBar);
        addDrawableChild(lastPage);
        addDrawableChild(doneBtn);
        addDrawableChild(exportBtn);
        addDrawableChild(cancelBtn);
        addDrawableChild(nextPage);

        updateArrows();
        updateSearch();

        focusOn(searchBar);
    }

    public void updateArrows() {
        nextPage.active = (page + 1) * elementByPage < visible.size() + 1; // have last page (+1 = next button)
        lastPage.active = page * elementByPage > 0; // have next page
    }

    public void updateSearch() {
        String query = searchBar.getText().toString().toLowerCase();
        visible.clear();
        config.stream().filter(block -> I18n.translate(block.getTranslationKey()).toLowerCase().contains(query))
                .forEach(visible::add);
        page = Math.min(visible.size(), page);
        updateArrows();
    }

    public List<Block> getView() {
        return visible.subList(page * elementByPage, Math.min((page + 1) * elementByPage, visible.size()));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(matrixStack);
        searchBar.render(matrixStack, mouseX, mouseY, partialTick);

        int left = width / 2 - elementsX * 18 / 2;
        int top = height / 2 - elementsY * 18 / 2;

        List<Block> view = getView();
        ItemStack hovered = null;
        Block hoveredBlock = null;
        int i;
        for (i = 0; i < view.size(); i++) {
            Block b = view.get(i);
            int x = left + (i % elementsX) * 18;
            int y = top + (i / elementsX) * 18;

            int color;
            ItemStack stack = new ItemStack(b);

            if (hovered == null && mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18) {
                color = 0x446666ff;
                hovered = stack;
                hoveredBlock = b;
            } else {
                color = 0x44666699;
            }

            fill(matrixStack, x, y, x + 18, y + 18, color);
            client.getItemRenderer().renderInGui(stack, x + 1, y + 1);
        }
        // add [+] button
        int x = left + (i % elementsX) * 18;
        int y = top + (i / elementsX) * 18;

        int color;

        if (hovered == null && mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18) {
            color = 0x4466ff66;
        } else {
            color = 0x44669966;
        }

        fill(matrixStack, x, y, x + 18, y + 18, color);
        textRenderer.draw(matrixStack, ADD, x + 18 / 2 - textRenderer.getWidth(ADD) / 2,
                y + 18 / 2 - textRenderer.fontHeight / 2, color);

        super.render(matrixStack, mouseX, mouseY, partialTick);

        if (hovered != null) {
            renderTooltip(matrixStack,
                    Arrays.asList(new TranslatableText(hoveredBlock.getTranslationKey()), REPLACE, DELETE), mouseX,
                    mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button))
            return true;

        int left = width / 2 - elementsX * 18 / 2;
        int top = height / 2 - elementsY * 18 / 2;

        List<Block> view = getView();
        int i;
        for (i = 0; i < view.size(); i++) {
            Block b = view.get(i);
            int x = left + (i % elementsX) * 18;
            int y = top + (i / elementsX) * 18;
            if (mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18) {
                if (button == 0) { // left click: replace
                    client.openScreen(new BlockSelector(this) {
                        @Override
                        protected void save(Block selection) {
                            int index = config.indexOf(b);
                            if (index == -1) // wtf?
                                config.add(selection);
                            else
                                config.set(index, selection);
                            updateSearch();
                        }
                    });
                    return true;
                }
                if (button == 1) { // right click: delete
                    config.remove(b);
                    updateSearch();
                    return true;
                }
                return false;
            }
        }
        int x = left + (i % elementsX) * 18;
        int y = top + (i / elementsX) * 18;
        if (button == 0 && mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18) {
            // add
            client.openScreen(new BlockSelector(this) {
                @Override
                protected void save(Block selection) {
                    config.add(selection);
                    updateSearch();
                }
            });
            return true;
        }
        return false;
    }
}