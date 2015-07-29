package advancedsystemsmanager.flow.menus;


import advancedsystemsmanager.api.ISystemType;
import advancedsystemsmanager.api.gui.IContainerSelection;
import advancedsystemsmanager.api.network.IPacketSync;
import advancedsystemsmanager.flow.FlowComponent;
import advancedsystemsmanager.flow.elements.RadioButtonList;
import advancedsystemsmanager.flow.elements.ScrollController;
import advancedsystemsmanager.flow.elements.Variable;
import advancedsystemsmanager.flow.execution.commands.CommandBase;
import advancedsystemsmanager.gui.GuiBase;
import advancedsystemsmanager.gui.GuiManager;
import advancedsystemsmanager.gui.IAdvancedTooltip;
import advancedsystemsmanager.gui.TextColour;
import advancedsystemsmanager.helpers.CollisionHelper;
import advancedsystemsmanager.helpers.LocalizationHelper;
import advancedsystemsmanager.network.ASMPacket;
import advancedsystemsmanager.reference.Names;
import advancedsystemsmanager.registry.ThemeHandler;
import advancedsystemsmanager.tileentities.TileEntityAENode;
import advancedsystemsmanager.tileentities.manager.TileEntityManager;
import advancedsystemsmanager.util.SystemCoord;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class MenuContainer extends Menu implements IPacketSync
{
    public static final int BACK_SRC_X = 46;
    public static final int BACK_SRC_Y = 52;
    public static final int BACK_SIZE_W = 9;
    public static final int BACK_SIZE_H = 9;
    public static final int BACK_X = 108;
    public static final int BACK_Y = 57;

    public static final int INVENTORY_SIZE = 16;
    public static final int INVENTORY_SRC_X = 88;
    public static final int INVENTORY_SRC_Y = 127;

    public static final int RADIO_BUTTON_MULTI_X = 2;
    public static final int RADIO_BUTTON_MULTI_Y = 27;
    public static final int RADIO_BUTTON_SPACING = 15;

    public static final int MENU_WIDTH = 120;
    public static final int TEXT_MULTI_MARGIN_X = 5;
    public static final int TEXT_MULTI_Y = 10;
    public static final int TEXT_MULTI_ERROR_Y = 30;

    public static final int FILTER_BUTTON_X = 90;
    public static final int FILTER_BUTTON_Y = 0;
    public static final int CHECK_BOX_FILTER_INVERT_Y = 55;
    public static final int FILTER_RESET_BUTTON_X = 70;

    public static final int CHECK_BOX_FILTER_Y = 5;
    public static final int CHECK_BOX_FILTER_SPACING = 12;
    public static final ContainerFilter filter = new ContainerFilter(); //this one is static so all of the menus will share the selection
    public static final String NBT_SELECTION = "InventorySelection";
    public static final String NBT_SELECTION_ID = "InventoryID";
    public static final String NBT_SHARED = "SharedCommand";
    //ugly way to make sure the filter controller isn't updating multiple times
    public static boolean hasUpdated;
    public Page currentPage;
    public List<Long> selectedInventories;
    public List<IContainerSelection<GuiManager>> inventories;
    public RadioButtonList radioButtonsMulti;
    public ScrollController<IContainerSelection<GuiManager>> scrollController;
    public ISystemType type;
    @SideOnly(Side.CLIENT)
    public GuiManager cachedInterface;
    public List<Button> buttons;
    public List<Variable> filterVariables;
    public boolean clientUpdate; //ugly quick way to fix client/server issue
    public int packetId;

    public MenuContainer(FlowComponent parent, ISystemType type)
    {
        super(parent);
        this.type = type;
        parent.registerSyncable(this);
        selectedInventories = new ArrayList<Long>();
        filterVariables = new ArrayList<Variable>();
        radioButtonsMulti = new RadioButtonList(getParent());

        initRadioButtons();
        radioButtonsMulti.setSelectedOption(getDefaultRadioButton());

        scrollController = new ScrollController<IContainerSelection<GuiManager>>(getParent(), getDefaultSearch())
        {
            public boolean locked;
            public int lockedX;
            public int lockedY;
            @SideOnly(Side.CLIENT)
            public ToolTip cachedTooltip;
            public long cachedId;
            public IContainerSelection<GuiManager> cachedContainer;
            public boolean keepCache;

            @Override
            public List<IContainerSelection<GuiManager>> updateSearch(String search, boolean all)
            {
                if (search.equals("") || !clientUpdate || cachedInterface == null)
                {
                    return new ArrayList<IContainerSelection<GuiManager>>();
                }

                if (inventories == null)
                {
                    inventories = getInventories(getParent().getManager());
                }

                if (search.equals(".var"))
                {
                    List<Variable> variables = new ArrayList<Variable>(getParent().getManager().getVariables());
                    Collections.sort(variables);
                    return new ArrayList<IContainerSelection<GuiManager>>(variables);
                }


                boolean noFilter = search.equals(".nofilter");
                boolean selected = search.equals(".selected");

                List<IContainerSelection<GuiManager>> ret = new ArrayList<IContainerSelection<GuiManager>>(inventories);

                Iterator<IContainerSelection<GuiManager>> iterator = ret.iterator();
                while (iterator.hasNext())
                {
                    IContainerSelection<GuiManager> element = iterator.next();

                    if (selected && selectedInventories.contains(element.getId()))
                    {
                        continue;
                    } else if (!element.isVariable())
                    {
                        SystemCoord block = (SystemCoord)element;
                        if (noFilter || ((all || block.containerAdvancedSearch(search) || block.getName(cachedInterface).toLowerCase().contains(search))
                                && filter.matches(getParent().getManager(), selectedInventories, block)))
                        {
                            continue;
                        }
                    }

                    iterator.remove();
                }
                return ret;
            }

            @SideOnly(Side.CLIENT)
            @Override
            public void onClick(IContainerSelection<GuiManager> iContainerSelection, int mX, int mY, int button)
            {
                if (GuiScreen.isShiftKeyDown() && mX != -1 && mY != -1)
                {
                    if (cachedTooltip != null && cachedId == iContainerSelection.getId())
                    {
                        if (!locked)
                        {
                            lockedX = mX;
                            lockedY = mY;
                        }
                        locked = !locked;
                    }
                } else
                {
                    long id = iContainerSelection.getId();
                    TileEntityAENode AENode = null;
                    if (iContainerSelection instanceof SystemCoord)
                        if (((SystemCoord) iContainerSelection).getTileEntity() instanceof TileEntityAENode)
                            AENode = (TileEntityAENode) ((SystemCoord) iContainerSelection).getTileEntity();
                    int index = selectedInventories.indexOf(id);
                    if (index >= 0)
                    {
                        selectedInventories.remove(index);
                        removeInventory(index, AENode);
                    } else
                    {

                        selectedInventories.add(id);
                        addInventory(id, AENode);
                    }
                }
            }

            @SideOnly(Side.CLIENT)
            @Override
            public void draw(GuiManager gui, IContainerSelection<GuiManager> iContainerSelection, int x, int y, boolean hover)
            {
                drawContainer(gui, iContainerSelection, selectedInventories, x, y, hover);
            }

            @SideOnly(Side.CLIENT)
            @Override
            public void drawMouseOver(GuiManager gui, int mX, int mY)
            {
                if (locked && GuiBase.isShiftKeyDown())
                {
                    drawMouseOver(gui, cachedContainer, lockedX, lockedY, mX, mY);
                    cachedTooltip.drawMouseOverMouseOver(gui, lockedX + gui.getAdvancedToolTipContentStartX(cachedTooltip), lockedY + gui.getAdvancedToolTipContentStartY(cachedTooltip), mX, mY);
                } else
                {
                    locked = false;
                    keepCache = false;
                    super.drawMouseOver(gui, mX, mY);
                    if (!keepCache)
                    {
                        cachedTooltip = null;
                        cachedContainer = null;
                    }
                }
            }

            @SideOnly(Side.CLIENT)
            @Override
            public void drawMouseOver(GuiManager gui, IContainerSelection<GuiManager> iContainerSelection, int mX, int mY)
            {
                drawMouseOver(gui, iContainerSelection, mX, mY, mX, mY);
            }

            @SideOnly(Side.CLIENT)
            public void drawMouseOver(GuiManager gui, IContainerSelection<GuiManager> iContainerSelection, int x, int y, int mX, int mY)
            {
                boolean isBlock = !iContainerSelection.isVariable();

                if (GuiScreen.isShiftKeyDown() && isBlock)
                {
                    if (cachedTooltip == null || cachedId != iContainerSelection.getId())
                    {
                        cachedContainer = iContainerSelection;
                        cachedTooltip = new ToolTip(gui, (SystemCoord)iContainerSelection);
                        cachedId = iContainerSelection.getId();
                    }
                    keepCache = true;

                    gui.drawMouseOver(cachedTooltip, x, y, mX, mY);
                } else
                {
                    List<String> lines = getMouseOverForContainer(iContainerSelection, selectedInventories);
                    if (isBlock)
                    {
                        if (lines == null)
                        {
                            lines = new ArrayList<String>();
                        }

                        lines.add("");
                        lines.add(TextColour.GRAY + StatCollector.translateToLocal(Names.TOOLTIP_EXTRA_INFO));
                    }

                    gui.drawMouseOver(lines, mX, mY);
                }
            }

            @SideOnly(Side.CLIENT)
            class ToolTip implements IAdvancedTooltip
            {
                public static final int SRC_X = 30;
                public static final int SRC_Y = 20;
                public ItemStack[] items;
                public List<String>[] itemTexts;
                List<String> prefix;
                List<String> suffix;
                List<String> lockedSuffix;

                @SideOnly(Side.CLIENT)
                public ToolTip(GuiManager gui, SystemCoord block)
                {
                    items = new ItemStack[ForgeDirection.VALID_DIRECTIONS.length];
                    itemTexts = new List[ForgeDirection.VALID_DIRECTIONS.length];

                    World world = block.getTileEntity().getWorldObj();
                    int x = block.getTileEntity().xCoord;
                    int y = block.getTileEntity().yCoord;
                    int z = block.getTileEntity().zCoord;

                    for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS)
                    {
                        int targetX = x + direction.offsetX;
                        int targetY = y + direction.offsetY;
                        int targetZ = z + direction.offsetZ;

                        ItemStack item = gui.getItemStackFromBlock(world, targetX, targetY, targetZ);
                        items[direction.ordinal()] = item;

                        List<String> text = new ArrayList<String>();
                        if (item != null && item.getItem() != null)
                        {
                            text.add(gui.getItemName(item));
                        }
                        String side = LocalizationHelper.getDirectionString(direction.ordinal());
                        text.add(TextColour.YELLOW + StatCollector.translateToLocal(side));

                        TileEntity te = world.getTileEntity(targetX, targetY, targetZ);
                        if (te instanceof TileEntitySign)
                        {
                            TileEntitySign sign = (TileEntitySign)te;
                            for (String txt : sign.signText)
                            {
                                if (!txt.isEmpty())
                                {
                                    text.add(TextColour.GRAY + txt);
                                }
                            }
                        }

                        itemTexts[direction.ordinal()] = text;
                    }

                    prefix = getMouseOverForContainer(block, selectedInventories);
                    prefix.add("");
                    prefix.add(TextColour.LIGHT_BLUE + StatCollector.translateToLocal(Names.TOOLTIP_ADJACENT));

                    suffix = new ArrayList<String>();
                    suffix.add(TextColour.GRAY + StatCollector.translateToLocal(Names.TOOLTIP_LOCK));

                    lockedSuffix = gui.getLinesFromText(StatCollector.translateToLocal(Names.TOOLTIP_UNLOCK), getMinWidth(gui));
                    for (int i = 0; i < lockedSuffix.size(); i++)
                    {
                        lockedSuffix.set(i, TextColour.GRAY + lockedSuffix.get(i));
                    }

                }

                @SideOnly(Side.CLIENT)
                @Override
                public int getMinWidth(GuiBase gui)
                {
                    return 110;
                }

                @SideOnly(Side.CLIENT)
                @Override
                public int getExtraHeight(GuiBase gui)
                {
                    return 70;
                }

                @SideOnly(Side.CLIENT)
                @Override
                public void drawContent(GuiBase gui, int x, int y, int mX, int mY)
                {
                    drawBlock(gui, x + 25, y + 5, mX, mY, ForgeDirection.NORTH);
                    drawBlock(gui, x + 5, y + 25, mX, mY, ForgeDirection.WEST);
                    drawBlock(gui, x + 25, y + 45, mX, mY, ForgeDirection.SOUTH);
                    drawBlock(gui, x + 45, y + 25, mX, mY, ForgeDirection.EAST);

                    drawBlock(gui, x + 80, y + 15, mX, mY, ForgeDirection.UP);
                    drawBlock(gui, x + 80, y + 35, mX, mY, ForgeDirection.DOWN);
                }

                @SideOnly(Side.CLIENT)
                public void drawBlock(GuiBase gui, int x, int y, int mX, int mY, ForgeDirection direction)
                {
                    GL11.glColor4f(1, 1, 1, 1);
                    GuiBase.bindTexture(gui.getComponentResource());
                    gui.drawTexture(x, y, SRC_X, SRC_Y + (CollisionHelper.inBounds(x, y, 16, 16, mX, mY) ? 16 : 0), 16, 16);

                    ItemStack item = items[direction.ordinal()];
                    if (item != null && item.getItem() != null)
                    {
                        gui.drawItemStack(item, x, y);
//                        gui.drawItemAmount(item, x, y);
                    }
                }

                @SideOnly(Side.CLIENT)
                @Override
                public List<String> getPrefix(GuiBase gui)
                {
                    return prefix;
                }

                @SideOnly(Side.CLIENT)
                @Override
                public List<String> getSuffix(GuiBase gui)
                {
                    return locked ? lockedSuffix : suffix;
                }

                @SideOnly(Side.CLIENT)
                public void drawMouseOverMouseOver(GuiBase gui, int x, int y, int mX, int mY)
                {
                    boolean ignored =
                            drawBlockMouseOver(gui, x + 25, y + 5, mX, mY, ForgeDirection.NORTH) ||
                                    drawBlockMouseOver(gui, x + 5, y + 25, mX, mY, ForgeDirection.WEST) ||
                                    drawBlockMouseOver(gui, x + 25, y + 45, mX, mY, ForgeDirection.SOUTH) ||
                                    drawBlockMouseOver(gui, x + 45, y + 25, mX, mY, ForgeDirection.EAST) ||

                                    drawBlockMouseOver(gui, x + 80, y + 15, mX, mY, ForgeDirection.UP) ||
                                    drawBlockMouseOver(gui, x + 80, y + 35, mX, mY, ForgeDirection.DOWN);
                }

                @SideOnly(Side.CLIENT)
                public boolean drawBlockMouseOver(GuiBase gui, int x, int y, int mX, int mY, ForgeDirection direction)
                {
                    if (CollisionHelper.inBounds(x, y, 16, 16, mX, mY))
                    {
                        List<String> itemText = itemTexts[direction.ordinal()];
                        if (itemText != null)
                        {
                            gui.drawMouseOver(itemText, mX, mY);
                        }
                        return true;
                    } else
                    {
                        return false;
                    }
                }
            }
        };

        buttons = new ArrayList<Button>();
        buttons.add(new PageButton(Names.FILTER_SHORT, Page.MAIN, Page.FILTER, false, 102, 21));
        buttons.add(new PageButton(Names.MULTI_SHORT, Page.MAIN, Page.MULTI, false, 111, 21));

        MenuContainer.Page[] subFilterPages = {MenuContainer.Page.POSITION, MenuContainer.Page.DISTANCE, MenuContainer.Page.SELECTION, MenuContainer.Page.VARIABLE};

        for (int i = 0; i < subFilterPages.length; i++)
        {
            buttons.add(new MenuContainer.PageButton(Names.SUB_MENU_SHORT, MenuContainer.Page.FILTER, subFilterPages[i], true, FILTER_BUTTON_X, CHECK_BOX_FILTER_Y + CHECK_BOX_FILTER_SPACING * i + FILTER_BUTTON_Y));
        }
        buttons.add(new MenuContainer.Button(Names.CLEAR_SHORT, MenuContainer.Page.FILTER, true, FILTER_RESET_BUTTON_X, CHECK_BOX_FILTER_INVERT_Y)
        {
            @Override
            void onClick()
            {
                filter.clear();
            }
        });

        buttons.add(new Button(Names.SELECT_ALL_SHORT, Page.MAIN, false, 102, 51)
        {
            @Override
            void onClick()
            {
                for (IContainerSelection iContainerSelection : scrollController.getResult())
                {
                    if (!selectedInventories.contains(iContainerSelection.getId()))
                    {
                        scrollController.onClick(iContainerSelection, -1, -1, 0);
                    }
                }
            }
        });

        buttons.add(new Button(Names.SELECT_NONE_SHORT, Page.MAIN, false, 111, 51)
        {
            @Override
            void onClick()
            {
                for (IContainerSelection iContainerSelection : scrollController.getResult())
                {
                    if (selectedInventories.contains(iContainerSelection.getId()))
                    {
                        scrollController.onClick(iContainerSelection, -1, -1, 0);
                    }
                }
            }
        });

        buttons.add(new Button(Names.SELECT_INVERT_SHORT, Page.MAIN, false, 102, 60)
        {
            @Override
            void onClick()
            {
                for (IContainerSelection iContainerSelection : scrollController.getResult())
                {
                    scrollController.onClick(iContainerSelection, -1, -1, 0);
                }
            }
        });

        buttons.add(new Button(Names.SELECT_VARIABLE_SHORT, Page.MAIN, false, 111, 60)
        {
            @Override
            void onClick()
            {
                if (scrollController.getText().equals(".var"))
                {
                    scrollController.setTextAndCursor(".all");
                } else
                {
                    scrollController.setTextAndCursor(".var");
                }
                scrollController.updateSearch();
            }
        });

        currentPage = Page.MAIN;
    }

    public void initRadioButtons()
    {
        type.initRadioButtons(radioButtonsMulti);
    }

    public int getDefaultRadioButton()
    {
        return type.getDefaultRadioButton();
    }

    public String getDefaultSearch()
    {
        return ".all";
    }

    public List<IContainerSelection<GuiManager>> getInventories(TileEntityManager manager)
    {
        Set<ISystemType> validTypes = getValidTypes();
        List<SystemCoord> tempInventories = manager.getConnectedInventories();
        List<IContainerSelection<GuiManager>> ret = new ArrayList<IContainerSelection<GuiManager>>();
        filterVariables.clear();

        for (Variable variable : manager.getVariables())
        {
            if (isVariableAllowed(validTypes, variable))
            {
                ret.add(variable);
                filterVariables.add(variable);
            }
        }

        for (SystemCoord tempInventory : tempInventories)
        {
            if (tempInventory.isOfAnyType(validTypes))
            {
                ret.add(tempInventory);
            }
        }

        if (getParent().isInventoryListDirty())
        {
            getParent().setInventoryListDirty(false);
            scrollController.updateSearch();
        }
        filter.scrollControllerVariable.updateSearch();


        return ret;
    }

    private void removeInventory(int index, TileEntityAENode te)
    {
        ASMPacket packet = getBasePacket(true);
        packet.writeShort(index);
        packet.writeTileEntity(te);
        packet.sendServerPacket();
    }

    private void addInventory(long inventory, TileEntityAENode te)
    {
        ASMPacket packet = getBasePacket(false);
        packet.writeLong(inventory);
        packet.writeTileEntity(te);
        packet.sendServerPacket();
    }

    @SideOnly(Side.CLIENT)
    public void drawContainer(GuiManager gui, IContainerSelection<GuiManager> iContainerSelection, List<Long> selected, int x, int y, boolean hover)
    {
        gui.drawColouredTexture(x, y, INVENTORY_SRC_X, INVENTORY_SRC_Y, INVENTORY_SIZE, INVENTORY_SIZE, ThemeHandler.theme.menus.checkboxes.getColour(selected.contains(iContainerSelection.getId()), hover));
        iContainerSelection.draw(gui, x, y);
    }

    public List<String> getMouseOverForContainer(IContainerSelection<GuiManager> iContainerSelection, List<Long> selected)
    {
        List<String> ret = new ArrayList<String>();
        if (cachedInterface != null)
        {
            String[] desc = iContainerSelection.getDescription(cachedInterface).split("\n");
            Collections.addAll(ret, desc);
            if (selected.contains(iContainerSelection.getId()))
            {
                ret.add(TextColour.GREEN + StatCollector.translateToLocal(Names.SELECTED));
            }
        }
        return ret;
    }

    public Set<ISystemType> getValidTypes()
    {
        return new HashSet<ISystemType>(Arrays.asList(type));
    }

    public boolean isVariableAllowed(Set<ISystemType> validTypes, Variable variable)
    {
        if (variable.isValid())
        {
            Set<ISystemType> variableValidTypes = ((MenuContainerTypes)variable.getDeclaration().getMenus().get(1)).getValidTypes();
            for (ISystemType type : validTypes)
            {
                if (SystemCoord.isOfType(variableValidTypes, type))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private ASMPacket getBasePacket(boolean remove)
    {
        ASMPacket packet = parent.getSyncPacket();
        packet.writeByte(packetId);
        packet.writeBooleanArray(remove);
        return packet;
    }

    public void writeData(ASMPacket dw, long id, boolean select)
    {
        dw.writeBoolean(false);
        dw.writeLong(id);
        dw.writeBoolean(select);
    }

    public Page getCurrentPage()
    {
        return currentPage;
    }

    public List<Variable> getFilterVariables()
    {
        return filterVariables;
    }

    @Override
    public String getName()
    {
        return type.getName() + "Menu";
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void draw(GuiManager gui, int mX, int mY)
    {
        clientUpdate = true;
        cachedInterface = gui;
        filter.currentMenu = this;
        if (currentPage == Page.MAIN)
        {
            inventories = getInventories(gui.getManager());
            scrollController.draw(gui, mX, mY);

        } else if (currentPage == Page.MULTI)
        {
            gui.drawCenteredString(selectedInventories.size() + " " + StatCollector.translateToLocal(Names.SELECTED_CONTAINERS), TEXT_MULTI_MARGIN_X, TEXT_MULTI_Y, 0.9F, MENU_WIDTH - TEXT_MULTI_MARGIN_X * 2, 0x404040);
            String error = null;

            if (radioButtonsMulti.size() == 0)
            {
                error = Names.NO_MULTI_SETTING;
            } else if (!hasMultipleInventories())
            {
                error = Names.SINGLE_SELECTED;
            }

            if (error != null)
            {
                gui.drawSplitString(error, TEXT_MULTI_MARGIN_X, TEXT_MULTI_ERROR_Y, MENU_WIDTH - TEXT_MULTI_MARGIN_X * 2, 0.7F, 0x404040);
            }
            if (hasMultipleInventories())
            {
                radioButtonsMulti.draw(gui, mX, mY);
            }
        } else if (currentPage == Page.POSITION)
        {
            gui.drawString(Names.RELATIVE_COORDINATES, 5, 60, 0.5F, 0x404040);
        } else if (currentPage == Page.SELECTION)
        {
            filter.radioButtonsSelection.draw(gui, mX, mY);
        } else if (currentPage == Page.VARIABLE)
        {
            filter.radioButtonVariable.draw(gui, mX, mY);
            if (filter.isVariableListVisible())
            {
                inventories = getInventories(gui.getManager());
                filter.scrollControllerVariable.draw(gui, mX, mY);
            }
        }

        filter.textBoxes.draw(gui, mX, mY);
        for (Button button : buttons)
        {
            button.draw(gui, mX, mY);
        }
        filter.checkBoxes.draw(gui, mX, mY);

        if (currentPage.parent != null)
        {
            int srcBackX = inBackBounds(mX, mY) ? 1 : 0;

            gui.drawTexture(BACK_X, BACK_Y, BACK_SRC_X + srcBackX * BACK_SIZE_W, BACK_SRC_Y, BACK_SIZE_W, BACK_SIZE_H);
        }

        hasUpdated = false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void drawMouseOver(GuiManager gui, int mX, int mY)
    {
        filter.currentMenu = this;
        if (currentPage == Page.MAIN)
        {
            scrollController.drawMouseOver(gui, mX, mY);
        } else if (currentPage == Page.VARIABLE && filter.isVariableListVisible())
        {
            filter.scrollControllerVariable.drawMouseOver(gui, mX, mY);
        } else if (currentPage == Page.POSITION)
        {
            if (CollisionHelper.inBounds(5, 60, MENU_WIDTH - 20, 5, mX, mY))
            {
                String str = StatCollector.translateToLocal(Names.ABSOLUTE_RANGES) + ":";

                str += "\n" + StatCollector.translateToLocal(Names.X) + " (" + (filter.lowerRange[0].getNumber() + getParent().getManager().xCoord) + ", " + (filter.higherRange[0].getNumber() + getParent().getManager().xCoord) + ")";
                str += "\n" + StatCollector.translateToLocal(Names.Y) + " (" + (filter.lowerRange[1].getNumber() + getParent().getManager().yCoord) + ", " + (filter.higherRange[1].getNumber() + getParent().getManager().yCoord) + ")";
                str += "\n" + StatCollector.translateToLocal(Names.Z) + " (" + (filter.lowerRange[2].getNumber() + getParent().getManager().zCoord) + ", " + (filter.higherRange[2].getNumber() + getParent().getManager().zCoord) + ")";

                gui.drawMouseOver(str, mX, mY);
            }
        }

        for (Button button : buttons)
        {
            button.drawMouseOver(gui, mX, mY);
        }

        if (currentPage.parent != null && inBackBounds(mX, mY))
        {
            gui.drawMouseOver(StatCollector.translateToLocal(Names.GO_BACK), mX, mY);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void onClick(int mX, int mY, int b)
    {
        filter.currentMenu = this;
        if (currentPage == Page.MAIN)
        {
            scrollController.onClick(mX, mY, b);

        } else if (currentPage == Page.MULTI)
        {
            if (hasMultipleInventories())
            {
                radioButtonsMulti.onClick(mX, mY, b);
            }
        } else if (currentPage == Page.SELECTION)
        {
            filter.radioButtonsSelection.onClick(mX, mY, b);
        } else if (currentPage == Page.VARIABLE)
        {
            filter.radioButtonVariable.onClick(mX, mY, b);
            if (filter.isVariableListVisible())
            {
                filter.scrollControllerVariable.onClick(mX, mY, b);
            }
        }

        for (Button button : buttons)
        {
            if (button.inBounds(mX, mY))
            {
                button.onClick();
                break;
            }
        }
        filter.checkBoxes.onClick(mX, mY);
        filter.textBoxes.onClick(mX, mY, b);
        if (currentPage.parent != null && inBackBounds(mX, mY))
        {
            currentPage = currentPage.parent;
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void onRelease(int mX, int mY, int button, boolean isMenuOpen)
    {
        filter.currentMenu = this;
        scrollController.onRelease(mX, mY); //no need to check we're on the correct menu, this makes sure the holding always stops
        filter.scrollControllerVariable.onRelease(mX, mY);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean onKeyStroke(GuiManager gui, char c, int k)
    {
        filter.currentMenu = this;
        return currentPage == Page.MAIN ? scrollController.onKeyStroke(gui, c, k) : filter.textBoxes.onKeyStroke(gui, c, k);
    }

    @Override
    public void copyFrom(Menu menu)
    {
        setOption(((MenuContainer) menu).getOption());
        selectedInventories.clear();
        for (SystemCoord coord : CommandBase.getContainers(getParent().getManager(), (MenuContainer)menu))
        {
            boolean AEAdd = true;
            if (coord.getTileEntity() instanceof TileEntityAENode)
            {
                TileEntityAENode aeNode = (TileEntityAENode) coord.getTileEntity();
                AEAdd = aeNode.addNode(getParent().getId());
            }
            if (AEAdd)
                selectedInventories.add(coord.getId());
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbtTagCompound, boolean pickup)
    {
        selectedInventories.clear();
        if (!pickup)
        {
            NBTTagList tagList = nbtTagCompound.getTagList(NBT_SELECTION, 10);

            for (int i = 0; i < tagList.tagCount(); i++)
            {
                NBTTagCompound selectionTag = tagList.getCompoundTagAt(i);

                long id = selectionTag.getLong(NBT_SELECTION_ID);

                selectedInventories.add(id);
            }
        }
        setOption(nbtTagCompound.getByte(NBT_SHARED));
    }

    @Override
    public void writeToNBT(NBTTagCompound nbtTagCompound, boolean pickup)
    {
        NBTTagList tagList = new NBTTagList();

        if (!pickup)
        {
            for (long selectedInventory : selectedInventories)
            {
                NBTTagCompound selectionTag = new NBTTagCompound();

                selectionTag.setLong(NBT_SELECTION_ID, selectedInventory);
                tagList.appendTag(selectionTag);
            }
        }

        nbtTagCompound.setTag(NBT_SELECTION, tagList);
        nbtTagCompound.setByte(NBT_SHARED, (byte)getOption());
    }

    @Override
    public void addErrors(List<String> errors)
    {
        type.addErrors(errors, this);
    }

    @Override
    public boolean isVisible()
    {
        return type.isVisible(getParent());
    }

    @Override
    public void update(float partial)
    {
        scrollController.update(partial);
        if (!hasUpdated)
        {
            filter.scrollControllerVariable.update(partial);
            hasUpdated = true;
        }
    }

    @Override
    public void doScroll(int scroll)
    {
        if (currentPage == Page.MAIN)
        {
            scrollController.doScroll(scroll);
        } else if (currentPage == Page.VARIABLE)
        {
            filter.scrollControllerVariable.doScroll(scroll);
        }
    }

    public int getOption()
    {
        return radioButtonsMulti.getSelectedOption();
    }

    public void setOption(int val)
    {
        radioButtonsMulti.setSelectedOption(val);
    }

    public boolean hasMultipleInventories()
    {
        return selectedInventories.size() > 1 || (selectedInventories.size() == 0 && (selectedInventories.get(0) & Variable.NEGATIVE) != 0);
    }

    public boolean inBackBounds(int mX, int mY)
    {
        return CollisionHelper.inBounds(BACK_X, BACK_Y, BACK_SIZE_W, BACK_SIZE_H, mX, mY);
    }

    public List<Long> getSelectedInventories()
    {
        return selectedInventories;
    }

    public void setSelectedInventories(List<Long> selectedInventories)
    {
        this.selectedInventories = selectedInventories;
    }

    @Override
    public void setId(int id)
    {
        packetId = id;
    }

    @Override
    public void onRemove()
    {
        for (SystemCoord coord : CommandBase.getContainers(getParent().getManager(), this))
        {
            if (this.selectedInventories.contains(coord.getId()))
            {
                TileEntity te = coord.getTileEntity();
                if (te != null && te instanceof TileEntityAENode)
                    ((TileEntityAENode) te).removeNode(this.getParent().getId());
            }
        }
    }

    @Override
    public boolean readData(ASMPacket packet)
    {
        if (packet.readBoolean())
        {
            int index = packet.readShort();
            TileEntity te = packet.readTileEntity();
            if (te != null && te instanceof TileEntityAENode)
                ((TileEntityAENode) te).removeNode(this.getParent().getId());
            selectedInventories.remove(index);
        } else
        {
            long id = packet.readLong();
            TileEntity te = packet.readTileEntity();
            boolean AEAdd = true;
            if (te != null && te instanceof TileEntityAENode)
                AEAdd = ((TileEntityAENode) te).addNode(this.getParent().getId());
            if (AEAdd)
            selectedInventories.add(id);
        }
        return false;
    }

    public enum Page
    {
        MAIN(null),
        MULTI(MAIN),
        FILTER(MAIN),
        POSITION(FILTER),
        DISTANCE(FILTER),
        SELECTION(FILTER),
        VARIABLE(FILTER);

        public Page parent;

        Page(Page parent)
        {
            this.parent = parent;
        }
    }

    public abstract class Button
    {
        public final int width;
        public final int height;
        public final int srcX;
        public final int srcY;
        int x, y;
        String label;
        String description;
        Page page;


        public Button(String label, Page page, boolean wide, int x, int y)
        {
            this.x = x;
            this.y = y;
            this.page = page;
            this.label = label;
            this.description = label + "Long";

            if (wide)
            {
                width = 20;
                srcX = 58;
            } else
            {
                width = 8;
                srcX = 50;
            }
            height = 8;
            srcY = 189;
        }

        abstract void onClick();

        @SideOnly(Side.CLIENT)
        void draw(GuiManager gui, int mX, int mY)
        {
            if (isVisible())
            {
                gui.drawTexture(x, y, srcX, srcY + (inBounds(mX, mY) ? height : 0), width, height);
                gui.drawCenteredString(label, x + 1, y + 2, 0.7F, width - 2, 0x404040);
            }
        }

        boolean isVisible()
        {
            return currentPage == page;
        }

        boolean inBounds(int mX, int mY)
        {
            return isVisible() && CollisionHelper.inBounds(x, y, width, height, mX, mY);
        }

        @SideOnly(Side.CLIENT)
        void drawMouseOver(GuiManager gui, int mX, int mY)
        {
            if (inBounds(mX, mY))
            {
                gui.drawMouseOver(description, mX, mY);
            }
        }
    }

    public class PageButton extends Button
    {
        public Page targetPage;

        public PageButton(String label, Page page, Page targetPage, boolean wide, int x, int y)
        {
            super(label, page, wide, x, y);
            this.targetPage = targetPage;
        }

        @Override
        void onClick()
        {
            currentPage = targetPage;
        }
    }


}
