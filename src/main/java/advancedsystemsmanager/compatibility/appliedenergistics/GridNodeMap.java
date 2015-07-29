package advancedsystemsmanager.compatibility.appliedenergistics;

import appeng.api.AEApi;
import appeng.api.exceptions.FailedConnection;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import net.minecraft.tileentity.TileEntity;

import java.util.Map;
import java.util.TreeMap;

public class GridNodeMap<T>
{
    private Map<T, GridBlock> gridBlocks;
    private Map<T, IGridNode> gridNodes;
    private IGridNode proxy;

    public GridNodeMap(IGridNode proxy)
    {
        this.proxy = proxy;
        this.gridBlocks = new TreeMap<T, GridBlock>();
        this.gridNodes = new TreeMap<T, IGridNode>();
    }

    @SuppressWarnings("unchecked")
    public boolean addNode(T id, TileEntity proxyHost)
    {
        GridBlock gridBlock = new GridBlock().addFlag(GridFlags.REQUIRE_CHANNEL).setProxy(proxyHost);
        gridBlocks.put(id, gridBlock);
        IGridNode gridNode = AEApi.instance().createGridNode(gridBlock);
        gridNode.updateState();
        try
        {
            AEApi.instance().createGridConnection(this.proxy, gridNode);
            this.gridNodes.put(id, gridNode);
        } catch (FailedConnection failedConnection)
        {
            failedConnection.printStackTrace();
            gridNode.destroy();
            gridBlocks.remove(id);
            return false;
        }
        return true;
    }

    public void removeNode(T id)
    {
        gridBlocks.remove(id);
        IGridNode gridNode = gridNodes.get(id);
        if (gridNode != null)
            gridNode.destroy();
        gridNodes.remove(id);
    }

    public void destroy()
    {
        for (IGridNode gridNode : this.gridNodes.values())
            gridNode.destroy();
        this.gridNodes.clear();
        this.proxy = null;
    }

    public void reconstruct(IGridNode proxyNode)
    {
        this.proxy = proxyNode;
        for (Map.Entry<T, GridBlock> entry : this.gridBlocks.entrySet())
        {
            IGridNode gridNode = AEApi.instance().createGridNode(entry.getValue());
            gridNode.updateState();
            try
            {
                AEApi.instance().createGridConnection(this.proxy, gridNode);
                this.gridNodes.put(entry.getKey(), gridNode);
            } catch (FailedConnection failedConnection)
            {
                failedConnection.printStackTrace();
                gridNode.destroy();
                gridBlocks.remove(entry.getKey());
            }
        }
    }
}
