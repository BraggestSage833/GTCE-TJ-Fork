package gregtech.common.items.behaviors;

import gregtech.common.ConfigHolder;
import gregtech.common.sound.GTSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

public class InfiniteSprayCanBehavior extends ColorSprayBehaviour{
    private static final String NBT_COLOR = "SprayColor";


    public InfiniteSprayCanBehavior(ItemStack empty, int totalUses, int color) {
        super(empty, totalUses, color);
    }

    private EnumDyeColor getStackColor(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey(NBT_COLOR)) {
            return EnumDyeColor.byMetadata(tag.getInteger(NBT_COLOR));
        }
        return getColor();
    }


    private void setStackColor(ItemStack stack, EnumDyeColor color) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }

        stack.getTagCompound().setInteger(NBT_COLOR,color.getMetadata());
    }


    public void cycle(ItemStack stack, EntityPlayer player, boolean forward) {
        EnumDyeColor current = getStackColor(stack);
        int length = EnumDyeColor.values().length;
        int next = (current.getMetadata() + (forward ? 1: -1) +length) % length;
        EnumDyeColor newColor = EnumDyeColor.byMetadata(next);
        setStackColor(stack, newColor);

        player.sendMessage(new TextComponentString(I18n.format(
                "behaviour.infinite.paintspray.color_changed", newColor)));
    }

    private int getPaintRange() {
        return ConfigHolder.SprayCanOptions.paintRange;
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!player.canPlayerEdit(pos, side, stack)) {
            return EnumActionResult.FAIL;
        }

        setColor(getStackColor(stack));

        if (player.isSneaking()) {
            paintConnectedBlocks(world, pos,side, getPaintRange(), player);
        }
        else {
            tryPaintBlock(world, pos,side, player);
        }

        world.playSound(player, pos, GTSoundEvents.SPRAY_CAN, SoundCategory.PLAYERS,1F,1F);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public void addInformation(ItemStack itemStack, List<String> lines) {
        EnumDyeColor color = getStackColor(itemStack);
        lines.add(I18n.format("behaviour.infinite.paintspray.tooltip"));
        lines.add(I18n.format(
                "behaviour.infinite.paintspray.current_color", color));
        lines.add("");
        lines.add(I18n.format("behaviour.infinite.paintspray.usage.scroll"));
        lines.add(I18n.format("behaviour.infinite.paintspray.usage.paint_multiple"));
        lines.add(I18n.format("behaviour.infinite.paintspray.usage.paint_single"));
    }

}
