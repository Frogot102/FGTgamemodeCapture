package com.zov.zovcapture.item;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class SiegeBreakerItem extends PickaxeItem {
    public SiegeBreakerItem(Properties properties) {
        super(Tiers.IRON, properties);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, net.minecraft.core.BlockPos pos, Player player) {
        return isSiegeMineable(state);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (isSiegeMineable(state)) {
            return Tiers.IRON.getSpeed();
        }
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return isSiegeMineable(state);
    }

    private static boolean isSiegeMineable(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                || state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.zovcapture.siege_breaker.tooltip"));
        tooltip.add(Component.translatable("item.zovcapture.siege_breaker.tools"));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
