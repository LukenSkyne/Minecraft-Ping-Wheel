package nx.pingwheel.client

object Core {

    @JvmStatic
    fun doPing() {
        PingWheelClient.LOGGER.info("key.ping-wheel.ping pressed")

	@JvmStatic
	fun onRenderWorld(stack: MatrixStack, projectionMatrix: Matrix4f, tickDelta: Float) {

	}

	@JvmStatic
	fun onRenderGUI(stack: MatrixStack, ci: CallbackInfo) {

	}
}
