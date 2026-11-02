package gregtech.common.pipelike.item;

import gregtech.api.pipenet.block.material.ItemBlockMaterialPipe;
import gregtech.common.pipelike.fluidpipe.BlockFluidPipe;
import gregtech.common.pipelike.fluidpipe.FluidPipeProperties;
import gregtech.common.pipelike.fluidpipe.FluidPipeType;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBlockItemPipe extends ItemBlockMaterialPipe<ItemPipeType, ItemPipeProperties> {

    public ItemBlockItemPipe(BlockItemPipe block) {
        super(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        ItemPipeProperties pipeProperties = blockPipe.createItemProperties(stack);

        int itemsPerSecond = pipeProperties.getItemsPerSecond();
        if (itemsPerSecond >= ItemPipeProperties.STACK_SIZE && itemsPerSecond % ItemPipeProperties.STACK_SIZE == 0) {
            tooltip.add(I18n.format("gregtech.item_pipe.item_rate_stack", itemsPerSecond / ItemPipeProperties.STACK_SIZE));
        } else {
            tooltip.add(I18n.format("gregtech.item_pipe.item_rate", itemsPerSecond));
        }
        tooltip.add(I18n.format("gregtech.item_pipe.priority", pipeProperties.getPriority()));
    }
}
