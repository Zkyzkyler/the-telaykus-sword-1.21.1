package net.telaykussword.power;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class TelaykusSword extends Item {
	private static final int DAMAGE_AMOUNT = 38;
	private static final int COOLDOWN_TICKS = 50; // 2.5 seconds
	private static final int DASH_COOLDOWN_TICKS = 700; // 35 seconds
	private static final double DASH_SPEED = 25.0;

	public TelaykusSword(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (world.isClient) {
			return TypedActionResult.pass(user.getStackInHand(hand));
		}

		// Apply cooldown for the sword
		user.getItemCooldownManager().set(this, COOLDOWN_TICKS);

		// Apply lightning and damage to target
		LivingEntity target = user.getAttacking();
		if (target != null) {
			target.damage(DAMAGE_AMOUNT);
			addLightningAndCobweb(world, target);
		}

		return TypedActionResult.success(user.getStackInHand(hand));
	}

	public void tryDash(World world, PlayerEntity player) {
		// Check if cooldown is active
		if (player.getItemCooldownManager().isCoolingDown(this)) {
			return;
		}

		// Dash in the direction player is facing
		Vec3d dashDirection = player.getRotationVec(1.0F).multiply(DASH_SPEED);
		player.addVelocity(dashDirection.x, dashDirection.y, dashDirection.z);
		player.velocityModified = true;

		// Simulate Riptide dash effect
		if (!world.isClient) {
			TridentEntity trident = new TridentEntity(world, player, new ItemStack(this));
			trident.setRiptide(1);
			world.spawnEntity(trident);
		}

		// Set dash cooldown
		player.getItemCooldownManager().set(this, DASH_COOLDOWN_TICKS);
	}

	private void addLightningAndCobweb(World world, LivingEntity target) {
		// Spawn lightning at target location
		LightningEntity lightningBolt = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
		lightningBolt.refreshPositionAfterTeleport(target.getX(), target.getY(), target.getZ());
		world.spawnEntity(lightningBolt);

		// Add cobwebs above target location
		addCobwebs(world, target.getBlockPos());
	}

	private void addCobwebs(WorldAccess world, BlockPos pos) {
		// Place cobwebs in a column at target position
		for (int y = pos.getY(); y <= pos.getY() + 3; y++) {
			BlockPos cobwebPos = new BlockPos(pos.getX(), y, pos.getZ());
			world.setBlockState(cobwebPos, Blocks.COBWEB.getDefaultState(), 3);
		}
	}

	@Override
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		// Damage the sword when it hits an entity
		stack.damage(1, attacker, EquipmentSlot.MAINHAND);
		return true;
	}
}
