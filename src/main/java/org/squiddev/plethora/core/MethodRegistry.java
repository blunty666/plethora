package org.squiddev.plethora.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import net.minecraft.util.Tuple;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import org.objectweb.asm.Type;
import org.squiddev.plethora.api.method.*;
import org.squiddev.plethora.api.reference.IReference;
import org.squiddev.plethora.api.reference.IdentityReference;
import org.squiddev.plethora.core.collections.SortedMultimap;
import org.squiddev.plethora.utils.DebugLogger;
import org.squiddev.plethora.utils.Helpers;

import javax.annotation.Nonnull;
import java.util.*;

public final class MethodRegistry implements IMethodRegistry {
	public static final MethodRegistry instance = new MethodRegistry();

	private final SortedMultimap<Class<?>, IMethod<?>> providers = SortedMultimap.create(new Comparator<IMethod<?>>() {
		@Override
		public int compare(IMethod<?> o1, IMethod<?> o2) {
			int p1 = o1.getPriority();
			int p2 = o2.getPriority();
			return (p1 < p2) ? -1 : ((p1 == p2) ? 0 : 1);
		}
	});

	@Override
	public <T> void registerMethod(@Nonnull Class<T> target, @Nonnull IMethod<T> method) {
		Preconditions.checkNotNull(target, "target cannot be null");
		Preconditions.checkNotNull(method, "method cannot be null");

		providers.put(target, method);
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <T> List<IMethod<T>> getMethods(@Nonnull IContext<T> context) {
		Preconditions.checkNotNull(context, "context cannot be null");

		List<IMethod<T>> methods = Lists.newArrayList();

		for (IMethod<?> genMethod : getMethods(context.getTarget().getClass())) {
			final IMethod<T> method = (IMethod<T>) genMethod;
			if (method.canApply(context)) methods.add(method);
		}

		return Collections.unmodifiableList(methods);
	}

	@Nonnull
	@Override
	public List<IMethod<?>> getMethods(@Nonnull Class<?> target) {
		Preconditions.checkNotNull(target, "target cannot be null");

		List<IMethod<?>> result = Lists.newArrayList();

		HashSet<Class<?>> visited = Sets.newHashSet();
		Queue<Class<?>> toVisit = Queues.newArrayDeque();

		visited.add(target);
		toVisit.add(target);

		while (toVisit.size() > 0) {
			Class<?> klass = toVisit.poll();
			result.addAll(providers.get(klass));

			Class<?> parent = klass.getSuperclass();
			if (parent != null && visited.add(parent)) {
				toVisit.add(parent);
			}

			for (Class<?> iface : klass.getInterfaces()) {
				if (iface != null && visited.add(iface)) {
					toVisit.add(iface);
				}
			}
		}

		return Collections.unmodifiableList(result);
	}

	@Nonnull
	@Override
	public <T> IUnbakedContext<T> makeContext(@Nonnull IReference<T> target, @Nonnull ICostHandler handler, @Nonnull IReference<?>... context) {
		Preconditions.checkNotNull(target, "target cannot be null");
		Preconditions.checkNotNull(handler, "handler cannot be null");
		Preconditions.checkNotNull(context, "context cannot be null");
		return new UnbakedContext<T>(target, handler, context);
	}

	@Nonnull
	@Override
	public ICostHandler getCostHandler(@Nonnull ICapabilityProvider object) {
		Preconditions.checkNotNull(object, "object cannot be null");
		ICostHandler handler = object.getCapability(CostHandler.COST_HANDLER_CAPABILITY, null);
		return handler != null ? handler : CostHandler.get(object);
	}

	public Tuple<List<IMethod<?>>, List<IUnbakedContext<?>>> getMethodsPaired(IUnbakedContext<?> initialContext, IContext<?> initialBaked) {
		// TODO: Handle priority correctly.

		ArrayList<IMethod<?>> methods = Lists.newArrayList();
		ArrayList<IUnbakedContext<?>> contexts = Lists.newArrayList();

		Object initialTarget = initialBaked.getTarget();
		for (Object obj : ConverterRegistry.instance.convertAll(initialTarget)) {
			IUnbakedContext<?> ctx;
			IContext<?> ctxBaked;

			if (obj == initialTarget) {
				ctx = initialContext;
				ctxBaked = initialBaked;
			} else {
				ctx = initialContext.makeChild(new IdentityReference<Object>(obj));
				ctxBaked = initialBaked.makeBakedChild(obj);
			}

			for (IMethod method : getMethods(ctxBaked)) {
				methods.add(method);
				contexts.add(ctx);
			}
		}

		return new Tuple<List<IMethod<?>>, List<IUnbakedContext<?>>>(methods, contexts);
	}

	@SuppressWarnings("unchecked")
	public void loadAsm(ASMDataTable asmDataTable) {
		for (ASMDataTable.ASMData asmData : asmDataTable.getAll(Method.class.getCanonicalName())) {
			String name = asmData.getClassName();
			try {
				if (Helpers.classBlacklisted(ConfigCore.Blacklist.blacklistMethods, name)) {
					DebugLogger.debug("Ignoring " + name);
					continue;
				}

				DebugLogger.debug("Registering " + name);

				Class<?> asmClass = Class.forName(asmData.getClassName());
				Map<String, Object> info = asmData.getAnnotationInfo();

				IMethod instance = asmClass.asSubclass(IMethod.class).newInstance();

				Class<?> target = Class.forName(((Type) info.get("value")).getClassName());
				registerMethod(target, instance);
			} catch (ClassNotFoundException e) {
				DebugLogger.error("Failed to load: %s", name, e);
			} catch (IllegalAccessException e) {
				DebugLogger.error("Failed to load: %s", name, e);
			} catch (InstantiationException e) {
				DebugLogger.error("Failed to load: %s", name, e);
			}
		}
	}
}
