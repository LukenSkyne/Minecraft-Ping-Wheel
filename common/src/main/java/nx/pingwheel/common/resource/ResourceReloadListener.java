package nx.pingwheel.common.resource;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import nx.pingwheel.common.Global;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static nx.pingwheel.common.ClientGlobal.PING_TEXTURE_ID;

public class ResourceReloadListener implements PreparableReloadListener {

	@Override
	public CompletableFuture<Void> reload(PreparationBarrier helper, ResourceManager resourceManager, ProfilerFiller loadProfiler, ProfilerFiller applyProfiler, Executor loadExecutor, Executor applyExecutor) {
		return reloadTextures(helper, resourceManager, loadExecutor, applyExecutor);
	}

	private static int numCustomTextures;

	public static boolean hasCustomTexture() {
		return numCustomTextures > 1;
	}

	public static CompletableFuture<Void> reloadTextures(PreparationBarrier helper, ResourceManager resourceManager, Executor loadExecutor, Executor applyExecutor) {
		return CompletableFuture
			.supplyAsync(() -> {
				try {
					numCustomTextures = resourceManager.getResources(PING_TEXTURE_ID).size();
				} catch (IOException e) {
					Global.LOGGER.error("failed to gather resources: " + e.getMessage());
				}

				return true;
			}, loadExecutor)
			.thenCompose(helper::wait)
			.thenAcceptAsync((ignored) -> {}, applyExecutor);
	}
}
