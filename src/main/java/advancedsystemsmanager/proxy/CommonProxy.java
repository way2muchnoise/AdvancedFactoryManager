package advancedsystemsmanager.proxy;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;
import thevault.registry.IRenderRegistry;

public class CommonProxy implements IRenderRegistry
{
    public void init()
    {

    }

    public World getClientWorld()
    {
        return null;
    }

    public void initRenderers()
    {
    }

    public void initHandlers()
    {
    }

    @Override
    public void registerItemRenderer(Item item, IItemRenderer renderer)
    {

    }

    @Override
    public void registerSimpleBlockRenderer(int id, ISimpleBlockRenderingHandler renderer)
    {

    }

    @Override
    public void registerTileEntityRenderer(Class<? extends TileEntity> tileEntity, TileEntitySpecialRenderer renderer)
    {

    }

    public boolean isClient()
    {
        return false;
    }
}
