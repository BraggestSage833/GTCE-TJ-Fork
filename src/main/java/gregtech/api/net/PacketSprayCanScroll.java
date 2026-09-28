package gregtech.api.net;

import gregtech.api.items.metaitem.MetaItem;
import gregtech.api.items.metaitem.stats.IItemBehaviour;
import gregtech.common.items.behaviors.CoverPlaceBehavior;
import gregtech.common.items.behaviors.CrowbarBehaviour;
import gregtech.common.items.behaviors.InfiniteSprayCanBehavior;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

public class PacketSprayCanScroll implements NetworkHandler.Packet {
    private boolean forward;
    private EnumHand hand;

    public PacketSprayCanScroll() {}

    public PacketSprayCanScroll(boolean forward, EnumHand hand) {
        this.forward = forward;
        this.hand = hand;
    }

     public static void encode(PacketSprayCanScroll packet, PacketBuffer buffer) {
        buffer.writeBoolean(packet.forward);
        buffer.writeBoolean(packet.hand == EnumHand.OFF_HAND);
    }


    public static PacketSprayCanScroll decode(PacketBuffer buffer) {
        boolean forward = buffer.readBoolean();
        EnumHand hand = buffer.readBoolean() ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        return new PacketSprayCanScroll(forward, hand);
    }


    public static void handle(PacketSprayCanScroll packet, net.minecraft.network.NetHandlerPlayServer handler) {
        EntityPlayerMP player = handler.player;
        ItemStack stack = player.getHeldItem(packet.hand);
        if (stack.isEmpty()) {
            return;
        }

        InfiniteSprayCanBehavior behavior = getSprayBehaviour(stack);
        if (behavior == null) {
            return;
        }
        behavior.cycle(stack, player, packet.forward);
    }

    private static InfiniteSprayCanBehavior getSprayBehaviour(ItemStack stack) {
        if (stack.getItem() instanceof MetaItem<?> metaItem) {
            MetaItem<?>.MetaValueItem valueItem = metaItem.getItem(stack);
            if (valueItem == null) {
                return null;
            }

            for (IItemBehaviour behaviour : valueItem.getBehaviours()) {
                if (behaviour instanceof InfiniteSprayCanBehavior sprayBehaviour) {
                    return sprayBehaviour;
                }
            }
        }
        return null;
    }

}
