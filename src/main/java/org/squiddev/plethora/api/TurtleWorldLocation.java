package org.squiddev.plethora.api;

import com.google.common.base.Preconditions;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.turtle.ITurtleAccess;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.squiddev.plethora.api.reference.ConstantReference;

import javax.annotation.Nonnull;

/**
 * A world location for turtles
 */
public class TurtleWorldLocation extends ConstantReference<IWorldLocation> implements IWorldLocation {
	private final ITurtleAccess turtle;

	public TurtleWorldLocation(@Nonnull ITurtleAccess turtle) {
		Preconditions.checkNotNull(turtle, "entity cannot be null");
		this.turtle = turtle;
	}

	@Nonnull
	@Override
	public World getWorld() {
		return turtle.getWorld();
	}

	@Nonnull
	@Override
	public BlockPos getPos() {
		return turtle.getPosition();
	}

	@Nonnull
	@Override
	public Vec3d getLoc() {
		BlockPos pos = turtle.getPosition();
		return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
	}

	@Nonnull
	@Override
	public IWorldLocation get() throws LuaException {
		return this;
	}

	@Nonnull
	@Override
	public IWorldLocation safeGet() throws LuaException {
		return new WorldLocation(getWorld(), getLoc());
	}
}
