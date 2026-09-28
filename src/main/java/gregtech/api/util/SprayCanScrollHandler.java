package gregtech.api.util;

import gregtech.api.items.metaitem.MetaItem;
import gregtech.api.items.metaitem.stats.IItemBehaviour;
import gregtech.api.net.NetworkHandler;
import gregtech.api.net.PacketSprayCanScroll;
import gregtech.common.items.behaviors.InfiniteSprayCanBehavior;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SprayCanScrollHandler {

    @SubscribeEvent
    public void onMouseScroll(MouseEvent event) {
        if (event.getDwheel() == 0) {
            return;
        }

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null || !player.isSneaking()) {
            return;
        }

        EnumHand hand = findSprayHand(player);

        if (hand == null) {
            return;
        }
        event.setCanceled(true);
        boolean forward = event.getDwheel() > 0;

        PacketSprayCanScroll packet = new PacketSprayCanScroll(forward,hand);
        NetworkHandler.channel.sendToServer(packet.toFMLPacket());
    }

    private static EnumHand findSprayHand(EntityPlayer player) {
        if (isSprayCan(player.getHeldItemMainhand())) {
            return EnumHand.MAIN_HAND;
        }
        else if (isSprayCan(player.getHeldItemOffhand())) {
            return EnumHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isSprayCan(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (!(stack.getItem() instanceof MetaItem<?> metaItem)) {
           return false;
        }

        MetaItem<?>.MetaValueItem valueItem = metaItem.getItem(stack);
        if (valueItem == null) {
            return false;
        }

        for (IItemBehaviour behaviour : valueItem.getBehaviours()) {
            if (behaviour instanceof InfiniteSprayCanBehavior) {
                return true;
            }
        }

        return false;
    }
}
