package advancedsystemsmanager.flow.menus;


import advancedsystemsmanager.helpers.Localization;
import advancedsystemsmanager.gui.ContainerManager;
import advancedsystemsmanager.gui.GuiManager;
import advancedsystemsmanager.flow.elements.CheckBox;
import advancedsystemsmanager.flow.elements.CheckBoxList;
import advancedsystemsmanager.flow.FlowComponent;
import advancedsystemsmanager.network.DataBitHelper;
import advancedsystemsmanager.network.DataReader;
import advancedsystemsmanager.network.DataWriter;
import advancedsystemsmanager.network.PacketHandler;
import advancedsystemsmanager.util.ConnectionBlockType;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class MenuContainerTypes extends Menu
{
    public MenuContainerTypes(FlowComponent parent)
    {
        super(parent);

        types = new ArrayList<ConnectionBlockType>();
        for (ConnectionBlockType connectionBlockType : ConnectionBlockType.values())
        {
            if (!connectionBlockType.isGroup())
            {
                types.add(connectionBlockType);
            }
        }

        checked = new boolean[types.size()];
        for (int i = 0; i < checked.length; i++)
        {
            checked[i] = true;
        }
        checkBoxes = new CheckBoxList();
        for (int i = 0; i < types.size(); i++)
        {
            final int id = i;
            int x = i % 2;
            int y = i / 2;
            checkBoxes.addCheckBox(new CheckBox(types.get(i).getName(), CHECK_BOX_X + CHECK_BOX_SPACING_X * x, CHECK_BOX_Y + CHECK_BOX_SPACING_Y * y)
            {
                @Override
                public void setValue(boolean val)
                {
                    checked[id] = val;
                }

                @Override
                public boolean getValue()
                {
                    return checked[id];
                }

                @Override
                public void onUpdate()
                {
                    DataWriter dw = getWriterForServerComponentPacket();
                    dw.writeData(id, DataBitHelper.CONTAINER_TYPE);
                    dw.writeBoolean(checked[id]);
                    PacketHandler.sendDataToServer(dw);
                }
            });
        }

    }

    @Override
    public String getName()
    {
        return Localization.CONTAINER_TYPE_MENU.toString();
    }

    public static final int CHECK_BOX_X = 5;
    public static final int CHECK_BOX_Y = 5;
    public static final int CHECK_BOX_SPACING_X = 55;
    public static final int CHECK_BOX_SPACING_Y = 12;


    public List<ConnectionBlockType> types;
    public boolean[] checked;
    public CheckBoxList checkBoxes;


    @SideOnly(Side.CLIENT)
    @Override
    public void draw(GuiManager gui, int mX, int mY)
    {
        checkBoxes.draw(gui, mX, mY);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void drawMouseOver(GuiManager gui, int mX, int mY)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onClick(int mX, int mY, int button)
    {
        checkBoxes.onClick(mX, mY);
    }

    @Override
    public void onDrag(int mX, int mY, boolean isMenuOpen)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onRelease(int mX, int mY, boolean isMenuOpen)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void writeData(DataWriter dw)
    {
        for (boolean b : checked)
        {
            dw.writeBoolean(b);
        }
    }

    @Override
    public void readData(DataReader dr)
    {
        for (int i = 0; i < checked.length; i++)
        {
            checked[i] = dr.readBoolean();
        }
    }

    @Override
    public void copyFrom(Menu menu)
    {
        MenuContainerTypes menuTypes = (MenuContainerTypes)menu;

        System.arraycopy(menuTypes.checked, 0, checked, 0, checked.length);
    }

    @Override
    public void refreshData(ContainerManager container, Menu newData)
    {
        MenuContainerTypes newDataTypes = (MenuContainerTypes)newData;

        for (int i = 0; i < checked.length; i++)
        {
            if (newDataTypes.checked[i] != checked[i])
            {
                checked[i] = newDataTypes.checked[i];
                DataWriter dw = getWriterForClientComponentPacket(container);
                dw.writeData(i, DataBitHelper.CONTAINER_TYPE);
                dw.writeBoolean(checked[i]);
                PacketHandler.sendDataToListeningClients(container, dw);
            }
        }
    }

    public static final String NBT_CHECKED = "Checked";

    @Override
    public void readFromNBT(NBTTagCompound nbtTagCompound, int version, boolean pickup)
    {
        short data = nbtTagCompound.getShort(NBT_CHECKED);
        for (int i = 0; i < checked.length; i++)
        {
            checked[i] = ((data >> i) & 1) != 0;
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbtTagCompound, boolean pickup)
    {
        short data = 0;
        for (int i = 0; i < checked.length; i++)
        {
            if (checked[i])
            {
                data |= 1 << i;
            }
        }
        nbtTagCompound.setShort(NBT_CHECKED, data);
    }

    @Override
    public void readNetworkComponent(DataReader dr)
    {
        int id = dr.readData(DataBitHelper.CONTAINER_TYPE);
        checked[id] = dr.readBoolean();
    }

    public boolean[] getChecked()
    {
        return checked;
    }

    public List<ConnectionBlockType> getTypes()
    {
        return types;
    }

    public EnumSet<ConnectionBlockType> getValidTypes()
    {
        EnumSet<ConnectionBlockType> types = EnumSet.noneOf(ConnectionBlockType.class);
        for (int i = 0; i < getTypes().size(); i++)
        {
            if (getChecked()[i])
            {
                types.add(getTypes().get(i));
            }
        }
        return types;
    }
}
