package nx.pingwheel.client

object Core {

    @JvmStatic
    fun doPing() {
        PingWheelClient.LOGGER.info("key.ping-wheel.ping pressed")
    }
}
