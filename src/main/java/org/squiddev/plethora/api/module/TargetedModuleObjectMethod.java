package org.squiddev.plethora.api.module;

import dan200.computercraft.api.lua.LuaException;
import net.minecraft.util.ResourceLocation;
import org.squiddev.plethora.api.method.IContext;
import org.squiddev.plethora.api.method.IPartialContext;
import org.squiddev.plethora.api.method.ISubTargetedMethod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A top-level module method which requires a particular context object to execute.
 */
public abstract class TargetedModuleObjectMethod<T> extends ModuleObjectMethod<IModuleContainer> implements ISubTargetedMethod<IModuleContainer, T> {
	private final Class<T> klass;

	public TargetedModuleObjectMethod(String name, ResourceLocation module, Class<T> klass, boolean worldThread) {
		this(name, module, klass, worldThread, 0, null);
	}

	public TargetedModuleObjectMethod(String name, ResourceLocation module, Class<T> klass, boolean worldThread, int priority) {
		this(name, module, klass, worldThread, priority, null);
	}

	public TargetedModuleObjectMethod(String name, ResourceLocation module, Class<T> klass, boolean worldThread, String docs) {
		this(name, module, klass, worldThread, 0, docs);
	}

	public TargetedModuleObjectMethod(String name, ResourceLocation module, Class<T> klass, boolean worldThread, int priority, String docs) {
		super(name, module, worldThread, priority, docs);
		this.klass = klass;
	}

	@Override
	public boolean canApply(@Nonnull IPartialContext<IModuleContainer> context) {
		return super.canApply(context) && context.hasContext(klass);
	}

	@Nullable
	@Override
	public final Object[] apply(@Nonnull IContext<IModuleContainer> context, @Nonnull Object[] args) throws LuaException {
		return apply(context.getContext(klass), context, args);
	}

	@Nullable
	public abstract Object[] apply(@Nonnull T target, @Nonnull IContext<IModuleContainer> context, @Nonnull Object[] args) throws LuaException;

	@Nonnull
	@Override
	public Class<T> getSubTarget() {
		return klass;
	}
}
