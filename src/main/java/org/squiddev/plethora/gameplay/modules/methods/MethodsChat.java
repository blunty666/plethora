package org.squiddev.plethora.gameplay.modules.methods;

import com.mojang.authlib.GameProfile;
import dan200.computercraft.api.lua.LuaException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import org.squiddev.plethora.api.IPlayerOwnable;
import org.squiddev.plethora.api.IWorldLocation;
import org.squiddev.plethora.api.method.ContextKeys;
import org.squiddev.plethora.api.method.IContext;
import org.squiddev.plethora.api.method.IUnbakedContext;
import org.squiddev.plethora.api.method.MethodResult;
import org.squiddev.plethora.api.module.IModuleContainer;
import org.squiddev.plethora.api.module.ModuleContainerMethod;
import org.squiddev.plethora.api.module.SubtargetedModuleMethod;
import org.squiddev.plethora.api.module.SubtargetedModuleObjectMethod;
import org.squiddev.plethora.gameplay.ConfigGameplay;
import org.squiddev.plethora.gameplay.PlethoraFakePlayer;
import org.squiddev.plethora.gameplay.modules.PlethoraModules;
import org.squiddev.plethora.integration.vanilla.FakePlayerProviderEntity;
import org.squiddev.plethora.integration.vanilla.FakePlayerProviderLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static dan200.computercraft.core.apis.ArgumentHelper.getString;
import static org.squiddev.plethora.gameplay.modules.ChatListener.Listener;
import static org.squiddev.plethora.utils.ContextHelpers.getOriginOr;

public final class MethodsChat {
	@ModuleContainerMethod.Inject(
		value = PlethoraModules.CHAT_S,
		doc = "function(message:string) -- Send a message to everyone"
	)
	@Nonnull
	public static MethodResult say(@Nonnull final IUnbakedContext<IModuleContainer> unbaked, @Nonnull Object[] args) throws LuaException {
		final String message = getString(args, 0);
		validateMessage(message);

		return MethodResult.nextTick(() -> {
			IContext<IModuleContainer> context = unbaked.bake();

			// If we've got an entity, just use that
			@Nullable
			Entity entity = getOriginOr(context, PlethoraModules.CHAT_S, Entity.class);
			@Nullable
			IWorldLocation location = context.getContext(ContextKeys.ORIGIN, IWorldLocation.class);

			IPlayerOwnable moduleOwner = context.getContext(PlethoraModules.CHAT_S, IPlayerOwnable.class);
			GameProfile moduleProfile = moduleOwner == null ? null : moduleOwner.getOwningProfile();

			EntityPlayerMP player;
			ITextComponent name;

			// Attempt to guess who is posting it and their position.
			if (entity instanceof EntityPlayerMP) {
				// If we've got some player, go ahead as normal
				name = entity.getDisplayName();
				player = (EntityPlayerMP) entity;
			} else if (ConfigGameplay.Chat.allowMobs && entity != null && entity.getEntityWorld() instanceof WorldServer) {
				IPlayerOwnable ownable = context.getContext(ContextKeys.ORIGIN, IPlayerOwnable.class);
				GameProfile owner = ownable == null ? null : ownable.getOwningProfile();
				if (owner == null) owner = PlethoraFakePlayer.PROFILE;// We include the position of the entity

				name = entity.getDisplayName().createCopy();

				PlethoraFakePlayer fakePlayer = new PlethoraFakePlayer((WorldServer) entity.getEntityWorld(), entity, owner);
				FakePlayerProviderEntity.load(fakePlayer, entity);
				player = fakePlayer;
			} else if (ConfigGameplay.Chat.allowOffline && moduleProfile != null && location != null && location.getWorld() instanceof WorldServer) {
				// If we've got a location and a game profile _associated with this module_ then we use that
				PlethoraFakePlayer fakePlayer = new PlethoraFakePlayer((WorldServer) location.getWorld(), null, moduleProfile);
				fakePlayer.setCustomNameTag(moduleProfile.getName());
				FakePlayerProviderLocation.load(fakePlayer, location);
				player = fakePlayer;

				name = fakePlayer.getDisplayName();
			} else {
				throw new LuaException("Cannot post to chat");
			}

			// Create the chat event and post to chat
			TextComponentTranslation translateChat = new TextComponentTranslation("chat.type.text", name, ForgeHooks.newChatWithLinks(message));
			ServerChatEvent event = new ServerChatEvent(player, message, translateChat);
			if (MinecraftForge.EVENT_BUS.post(event) || event.getComponent() == null) return MethodResult.empty();

			player.mcServer.getPlayerList().sendMessage(event.getComponent(), false);
			return MethodResult.empty();
		});
	}

	@SubtargetedModuleMethod.Inject(
		module = PlethoraModules.CHAT_S, target = ICommandSender.class,
		doc = "function(message:string) -- Send a message to yourself"
	)
	@Nonnull
	public static MethodResult tell(@Nonnull final IUnbakedContext<IModuleContainer> unbaked, @Nonnull Object[] args) throws LuaException {
		final String message = getString(args, 0);
		validateMessage(message);

		return MethodResult.nextTick(() -> {
			IContext<IModuleContainer> context = unbaked.bake();
			ICommandSender sender = getOriginOr(context, PlethoraModules.CHAT_S, ICommandSender.class);
			sender.sendMessage(ForgeHooks.newChatWithLinks(message));
			return MethodResult.empty();
		});
	}

	public static void validateMessage(String message) throws LuaException {
		if (ConfigGameplay.Chat.maxLength > 0 && message.length() > ConfigGameplay.Chat.maxLength) {
			throw new LuaException(String.format("Message is too long (was %d, maximum is %d)", message.length(), ConfigGameplay.Chat.maxLength));
		}

		for (int i = 0; i < message.length(); ++i) {
			char character = message.charAt(i);
			if (character < 32 || character == 127 || (character == 167 && !ConfigGameplay.Chat.allowFormatting)) {
				throw new LuaException("Illegal character '" + message.charAt(i) + "'");
			}
		}
	}

	@SubtargetedModuleObjectMethod.Inject(
		module = PlethoraModules.CHAT_S, target = Listener.class, worldThread = false,
		doc = "function(pattern:string) -- Capture all chat messages matching a Lua pattern, preventing them from being said."
	)
	@Nullable
	public static Object[] capture(Listener listener, @Nonnull IContext<IModuleContainer> context, @Nonnull Object[] args) throws LuaException {
		String pattern = getString(args, 0);
		listener.addPattern(pattern);
		return null;
	}

	@SubtargetedModuleObjectMethod.Inject(
		module = PlethoraModules.CHAT_S, target = Listener.class, worldThread = false,
		doc = "function(pattern:string):boolean -- Remove a capture added by capture(pattern)."
	)
	@Nonnull
	public static Object[] uncapture(Listener listener, @Nonnull IContext<IModuleContainer> context, @Nonnull Object[] args) throws LuaException {
		String pattern = getString(args, 0);
		boolean removed = listener.removePattern(pattern);
		return new Object[]{removed};
	}

	@SubtargetedModuleObjectMethod.Inject(
		module = PlethoraModules.CHAT_S, target = Listener.class, worldThread = false,
		doc = "function() -- Remove all listeners added by capture()."
	)
	@Nullable
	public static Object[] clearCaptures(Listener listener, @Nonnull final IContext<IModuleContainer> unbaked, @Nonnull Object[] args) throws LuaException {
		listener.clearPatterns();
		return null;
	}
}
