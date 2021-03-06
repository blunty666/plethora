package org.squiddev.plethora.integration.computercraft;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.squiddev.plethora.api.transfer.ITransferProvider;
import org.squiddev.plethora.utils.DebugLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Transfer providers for CC: Tweaked's (and hopefully CC's) wired networks.
 */
@ITransferProvider.Inject(value = IComputerAccess.class, modId = ComputerCraft.MOD_ID)
public class TransferComputerAccess implements ITransferProvider<IComputerAccess> {
	private boolean fetched;
	private Method getAvailablePeripheral;
	private Method getAvailablePeripherals;

	private void fetchReflection() {
		if (fetched) return;

		try {
			getAvailablePeripherals = IComputerAccess.class.getMethod("getAvailablePeripherals");
			getAvailablePeripheral = IComputerAccess.class.getMethod("getAvailablePeripheral", String.class);
		} catch (NoSuchMethodException ignored) {
		}

		fetched = true;
	}

	@Nullable
	@Override
	public Object getTransferLocation(@Nonnull IComputerAccess object, @Nonnull String key) {
		fetchReflection();
		if (getAvailablePeripheral != null) {
			try {
				return getAvailablePeripheral.invoke(object, key);
			} catch (ReflectiveOperationException e) {
				DebugLogger.error("Failed to call IComputerAccess.getAvailablePeripheral", e);
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	@Nonnull
	@Override
	public Set<String> getTransferLocations(@Nonnull IComputerAccess object) {
		fetchReflection();
		if (getAvailablePeripherals != null) {
			try {
				return ((Map<String, IPeripheral>) getAvailablePeripherals.invoke(object)).keySet();
			} catch (ReflectiveOperationException e) {
				DebugLogger.error("Failed to call IComputerAccess.getAvailablePeripherals", e);
			}
		}

		return Collections.emptySet();
	}
}
