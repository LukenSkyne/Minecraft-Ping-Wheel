package nx.pingwheel.common.resource;

import net.minecraft.client.texture.MissingSprite;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.profiler.Profiler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static nx.pingwheel.common.ClientGlobal.Game;
import static nx.pingwheel.common.ClientGlobal.PING_TEXTURE_ID;

public class ResourceReloadListener implements ResourceReloader {

	@Override
	public CompletableFuture<Void> reload(Synchronizer helper, ResourceManager resourceManager, Profiler loadProfiler, Profiler applyProfiler, Executor loadExecutor, Executor applyExecutor) {
		return reloadTextures(helper, resourceManager, loadExecutor, applyExecutor);
	}

	public static CompletableFuture<Void> reloadTextures(Synchronizer helper, ResourceManager resourceManager, Executor loadExecutor, Executor applyExecutor) {
		return CompletableFuture
			.supplyAsync(() -> {
				final var canLoadTexture = resourceManager.getResource(PING_TEXTURE_ID).isPresent();

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
