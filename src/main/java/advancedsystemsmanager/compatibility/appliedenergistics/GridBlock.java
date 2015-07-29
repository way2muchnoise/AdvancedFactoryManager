package advancedsystemsmanager.compatibility.appliedenergistics;

import advancedsystemsmanager.registry.BlockRegistry;
import appeng.api.networking.*;
import appeng.api.util.AEColor;
import appeng.api.util.DimensionalCoord;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.EnumSet;

public class GridBlock <T extends TileEntity & IGridHost> implements IGridBlock
{
    private T host;
    private EnumSet<GridFlags> flags;
    private T proxy;
    private int powerUsage;

    public GridBlock()
    {
        this(null);
    }

    public GridBlock(T host)
    {
        this.host = host;
        this.flags = EnumSet.noneOf(GridFlags.class);
        this.proxy = host;
        this.powerUsage = 10;
    }

    public GridBlock<T> setProxy(T proxy)
    {
        this.proxy = proxy;
        return this;
    }

    public GridBlock<T> setFlags(EnumSet<GridFlags> flags)
    {
        this.flags = flags;
        return this;
    }

    public GridBlock<T> addFlag(GridFlags flag)
    {
        this.flags.add(flag);
        return this;
    }

    public GridBlock<T> removeFlag(GridFlags flag)
    {
        this.flags.remove(flag);
        return this;
    }

    public GridBlock<T> setPowerUsage(int powerUsage)
    {
        this.powerUsage = powerUsage;
        return this;
    }

    @Override
    public double getIdlePowerUsage()
    {
        return this.powerUsage;
    }

    @Override
    public EnumSet<GridFlags> getFlags()
    {
        return this.flags;
    }

    @Override
    public boolean isWorldAccessible()
    {
        return this.host != null;
    }

    @Override
    public DimensionalCoord getLocation()
    {
        return new DimensionalCoord(this.proxy);
    }

    @Override
    public AEColor getGridColor()
    {
        return AEColor.Transparent;
    }

    @Override
    public void onGridNotification(GridNotification gridNotification)
    {

    }

    @Override
    public void setNetworkStatus(IGrid iGrid, int i)
    {

    }

    @Override
    public EnumSet<ForgeDirection> getConnectableSides()
    {
        return EnumSet.allOf(ForgeDirection.class);
    }

    @Override
    public IGridHost getMachine()
    {
        return this.proxy;
    }

    @Override
    public void gridChanged()
    {

    }

    @Override
    public ItemStack getMachineRepresentation()
    {
        return this.host != null ? new ItemStack(BlockRegistry.cableAENode) : null;
    }
}