package nx.pingwheel.common.resource;

import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.profiler.Profiler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static nx.pingwheel.common.ClientGlobal.PING_TEXTURE_ID;

public class ResourceReloadListener implements ResourceReloader {

	@Override
	public CompletableFuture<Void> reload(Synchronizer helper, ResourceManager resourceManager, Profiler loadProfiler, Profiler applyProfiler, Executor loadExecutor, Executor applyExecutor) {
		return reloadTextures(helper, resourceManager, loadExecutor, applyExecutor);
	}

	private static int numCustomTextures;

	public static boolean hasCustomTexture() {
		return numCustomTextures > 1;
	}

	public static CompletableFuture<Void> reloadTextures(Synchronizer helper, ResourceManager resourceManager, Executor loadExecutor, Executor applyExecutor) {
		return CompletableFuture
			.supplyAsync(() -> {
				numCustomTextures = resourceManager.getAllResources(PING_TEXTURE_ID).size();

				return true;
			}, loadExecutor)
			.thenCompose(helper::whenPrepared)
			.thenAcceptAsync((ignored) -> {}, applyExecutor);
	}
}
