package org.squiddev.plethora.core;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import org.objectweb.asm.Type;
import org.squiddev.plethora.api.meta.IMetaProvider;
import org.squiddev.plethora.api.meta.IMetaRegistry;
import org.squiddev.plethora.api.meta.NamespacedMetaProvider;
import org.squiddev.plethora.api.method.ContextKeys;
import org.squiddev.plethora.api.method.IPartialContext;
import org.squiddev.plethora.core.collections.ClassIteratorIterable;
import org.squiddev.plethora.core.collections.SortedMultimap;
import org.squiddev.plethora.utils.DebugLogger;
import org.squiddev.plethora.utils.Helpers;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MetaRegistry implements IMetaRegistry {
	public static final MetaRegistry instance = new MetaRegistry();

	private final SortedMultimap<Class<?>, IMetaProvider<?>> providers = SortedMultimap.create((o1, o2) -> {
		int p1 = o1.getPriority();
		int p2 = o2.getPriority();
		return (p1 < p2) ? -1 : ((p1 == p2) ? 0 : 1);
	});

	@Override
	public <T> void registerMetaProvider(@Nonnull Class<T> target, @Nonnull IMetaProvider<T> provider) {
		Preconditions.checkNotNull(target, "target cannot be null");
		Preconditions.checkNotNull(provider, "provider cannot be null");

		providers.put(target, provider);

		// TODO: Can we walk .getGenericSubclass/.getGenericInterface to check that target type is correct?
	}

	@Override
	public <T> void registerMetaProvider(@Nonnull Class<T> target, @Nonnull String namespace, @Nonnull IMetaProvider<T> provider) {
		registerMetaProvider(target, new NamespacedMetaProvider<>(namespace, provider));
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public Map<Object, Object> getMeta(@Nonnull IPartialContext<?> context) {
		Preconditions.checkNotNull(context, "context cannot be null");
		if (!(context instanceof PartialContext)) throw new IllegalStateException("Unknown context class");

		PartialContext partial = (PartialContext<?>) context;
		String[] keys = partial.keys;
		Object[] values = partial.values;

		// TODO: Handle priority across each conversion correctly

		HashMap<Object, Object> out = Maps.newHashMap();
		for (int i = values.length - 1; i >= 0; i--) {
			if (!ContextKeys.TARGET.equals(keys[i])) continue;

			Object child = values[i];
			IPartialContext<?> childContext = partial.withIndex(i);

			for (IMetaProvider provider : getMetaProviders(child.getClass())) {
				out.putAll(provider.getMeta(childContext));
			}
		}

		return out;
	}

	@Nonnull
	@Override
	public List<IMetaProvider<?>> getMetaProviders(@Nonnull Class<?> target) {
		Preconditions.checkNotNull(target, "target cannot be null");

		List<IMetaProvider<?>> result = Lists.newArrayList();

		for (Class<?> klass : new ClassIteratorIterable(target)) {
			result.addAll(providers.get(klass));
		}

		return Collections.unmodifiableList(result);
	}

	@SuppressWarnings("unchecked")
	public void loadAsm(ASMDataTable asmDataTable) {
		for (ASMDataTable.ASMData asmData : asmDataTable.getAll(IMetaProvider.Inject.class.getName())) {
			String name = asmData.getClassName();
			try {
				if (Helpers.classBlacklisted(ConfigCore.Blacklist.blacklistProviders, name)) {
					DebugLogger.debug("Ignoring " + name + " as it has been blacklisted");
					continue;
				}

				Map<String, Object> info = asmData.getAnnotationInfo();
				String modId = (String) info.get("modId");
				if (!Strings.isNullOrEmpty(modId) && !Helpers.modLoaded(modId)) {
					DebugLogger.debug("Skipping " + name + " as " + modId + " is not loaded or is blacklisted");
					continue;
				}

				DebugLogger.debug("Registering " + name);

				Class<?> asmClass = Class.forName(name);
				IMetaProvider instance = asmClass.asSubclass(IMetaProvider.class).newInstance();

				Class<?> target = Class.forName(((Type) info.get("value")).getClassName());
				Helpers.assertTarget(asmClass, target, IMetaProvider.class);

				String namespace = (String) info.get("namespace");
				if (Strings.isNullOrEmpty(namespace)) {
					registerMetaProvider(target, instance);
				} else {
					registerMetaProvider(target, namespace, instance);
				}
			} catch (Throwable e) {
				if (ConfigCore.Testing.strict) {
					throw new IllegalStateException("Failed to load: " + name, e);
				} else {
					DebugLogger.error("Failed to load: " + name, e);
				}
			}
		}
	}
}
