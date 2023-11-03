package nx.pingwheel.forge.client;

import net.minecraft.client.texture.MissingSprite;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.profiler.Profiler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static nx.pingwheel.forge.client.PingWheelClient.Game;
import static nx.pingwheel.forge.client.PingWheelClient.PING_TEXTURE_ID;

public class ResourceReloadListener implements ResourceReloader {
	@Override
	public CompletableFuture<Void> reload(ResourceReloader.Synchronizer helper, ResourceManager resourceManager, Profiler loadProfiler, Profiler applyProfiler, Executor loadExecutor, Executor applyExecutor) {
		return reloadTextures(helper, resourceManager, loadExecutor, applyExecutor);
	}

	private CompletableFuture<Void> reloadTextures(ResourceReloader.Synchronizer helper, ResourceManager resourceManager, Executor loadExecutor, Executor applyExecutor) {
		return CompletableFuture
			.supplyAsync(() -> {
				final var canLoadTexture = resourceManager.containsResource(PING_TEXTURE_ID);

				if (!canLoadTexture) {
					// force texture manager to remove the entry from its index
					Game.getTextureManager().registerTexture(PING_TEXTURE_ID, MissingSprite.getMissingSpriteTexture());
				}

				return canLoadTexture;
			}, loadExecutor)
			.thenCompose(helper::whenPrepared)
			.thenAcceptAsync(canLoadTexture -> {
				if (canLoadTexture) {
					Game.getTextureManager().bindTexture(PING_TEXTURE_ID);
				}
			}, applyExecutor);
	}
}
