package gregtech.integration.jei.multiblock;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.util.BlockInfo;
import gregtech.common.blocks.MetaBlocks;
import gregtech.integration.jei.multiblock.channel.PlaceholderType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiblockShapeInfo {

    private final BlockInfo[][][] blocks; //[z][y][x]
    private final boolean isTiered;

    public MultiblockShapeInfo(BlockInfo[][][] blocks, boolean isTiered) {
        this.blocks = blocks;
        this.isTiered = isTiered;
    }

    public BlockInfo[][][] getBlocks() {
        return blocks;
    }

    public boolean isTiered() {
        return isTiered;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private List<String[]> shape = new ArrayList<>();
        private Map<Character, BlockInfo> symbolMap = new HashMap<>();
        boolean isTiered = false;

        public Builder aisle(String... data) {
            this.shape.add(data);
            return this;
        }

        public Builder where(char symbol, BlockInfo value) {
            this.symbolMap.put(symbol, value);
            return this;
        }

        public Builder where(char symbol, PlaceholderType type) {
            this.symbolMap.put(symbol, BlockInfo.placeholder(type));
            this.isTiered = true;
            return this;
        }

        public Builder where(char symbol, IBlockState blockState) {
            return where(symbol, new BlockInfo(blockState));
        }

        public Builder where(char symbol, PlaceholderType type, IBlockState blockState) {
            return where(symbol, new BlockInfo(blockState, type));
        }

        public Builder where(char symbol, MetaTileEntity tileEntity, EnumFacing frontSide) {
            MetaTileEntityHolder holder = new MetaTileEntityHolder();
            holder.setMetaTileEntity(tileEntity);
            holder.getMetaTileEntity().setFrontFacing(frontSide);
            return where(symbol, new BlockInfo(MetaBlocks.MACHINE.getDefaultState(), holder,null)); // TODO: Tää tarvii olla muuta kuin null
        }

        public Builder where(char symbol, PlaceholderType type, MetaTileEntity tileEntity, EnumFacing frontSide) {
            this.isTiered = true;
            MetaTileEntityHolder holder = new MetaTileEntityHolder();
            holder.setMetaTileEntity(tileEntity);
            holder.getMetaTileEntity().setFrontFacing(frontSide);
            return where(symbol, new BlockInfo(MetaBlocks.MACHINE.getDefaultState(), holder,type)); // TODO: Tää tarvii olla muuta kuin null
        }


        private BlockInfo[][][] bakeArray() {
            BlockInfo[][][] blockInfos = new BlockInfo[shape.size()][][];
            for (int i = 0; i < blockInfos.length; i++) {
                String[] aisleEntry = shape.get(i);
                BlockInfo[][] aisleData = new BlockInfo[aisleEntry.length][];
                for (int j = 0; j < aisleData.length; j++) {
                    String columnEntry = aisleEntry[j];
                    BlockInfo[] columnData = new BlockInfo[columnEntry.length()];
                    for (int k = 0; k < columnData.length; k++) {
                        columnData[k] = symbolMap.getOrDefault(columnEntry.charAt(k), BlockInfo.EMPTY);
                        TileEntity tileEntity = columnData[k].getTileEntity();
                        if (tileEntity != null) {
                            MetaTileEntityHolder holder = (MetaTileEntityHolder) tileEntity;
                            final MetaTileEntity mte = holder.getMetaTileEntity();
                            holder = new MetaTileEntityHolder();
                            holder.setMetaTileEntity(mte);
                            holder.getMetaTileEntity().setFrontFacing(mte.getFrontFacing());
                            columnData[k] = new BlockInfo(columnData[k].getBlockState(), holder, columnData[k].getPlaceHolderType()); // TODO: SAMOIN TÄÄ
                        }
                    }
                    aisleData[j] = columnData;
                }
                blockInfos[i] = aisleData;
            }
            return blockInfos;
        }

        public MultiblockShapeInfo build() {
            return new MultiblockShapeInfo(bakeArray(),this.isTiered);
        }

    }

}
