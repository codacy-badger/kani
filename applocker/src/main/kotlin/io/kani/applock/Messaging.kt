package io.kani.applock

import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Type alias for message handler, which is used by [MessageServer] to
 * process incoming requests
 */
typealias MessageHandler = (message: String) -> String

/**
 * Response received by client
 */
sealed class Response {
    /** Contains response to query */
    data class Answer(val answer: String) : Response()

    /** Indicates about issues during execution */
    data class Failure(val description: String) : Response()
}

/**
 * Internal server for communication between [AppLocker] instances.
 *
 * Server reads client message once and discards connection
 *
 * Make sure to call [run] (via thread or executors), otherwise [selector] and [channel] won't be closed
 *
 * @param messageHandler callback responsible for processing client messages
 * @param portNumber server portNumber number, 0 for random; use [portNumber] to access real portNumber number
 */
internal class MessageServer(private val messageHandler: MessageHandler, portNumber: Int = 0) : Runnable {
    private val selector: Selector = Selector.open()
    private val channel: ServerSocketChannel

    /** Port number of the server */
    val portNumber: Int

    init {
        // create channel and bind to random portNumber
        channel = ServerSocketChannel.open().apply {
            bind(InetSocketAddress(InetAddress.getLocalHost(), portNumber))
            configureBlocking(false)
            register(selector, SelectionKey.OP_ACCEPT)
        }
        // store port number in public property
        this.portNumber = channel.socket().localPort
    }

    override fun run() {
        // 1. Run selector
        // 2. In infinite loop call select (blocking) and retrieve results
        // 3. Process results according to type
        selector.use { selector ->
            channel.use { socket ->
                while (selector.isOpen && channel.isOpen) {
                    if (Thread.currentThread().isInterrupted) return

                    selector.select()
                    val selectedKeys = selector.selectedKeys()
                    val iter = selectedKeys.iterator()

                    while (iter.hasNext()) {
                        if (Thread.currentThread().isInterrupted) return

                        val key = iter.next()

                        // establish connection
                        if (key.isAcceptable) {
                            val client = socket.accept()
                            client.configureBlocking(false)
                            client.register(selector, SelectionKey.OP_READ)
                        }

                        // process client message
                        if (key.isReadable) {
                            (key.channel() as SocketChannel).use { client ->
                                val message = readChannel(client)
                                val answer = messageHandler(message)
                                client.write(ByteBuffer.wrap(answer.toByteArray()))
                            }
                        }
                        iter.remove()
                    }
                }
            }
        }
    }
}

/**
 * Server client, support sending/receiving messages
 *
 * Opens new connection on each [sendMessage] call
 */
internal class MessageClient(val id: String) {
    private val messageExecutor = Executors.newSingleThreadExecutor { task ->
        Thread(task).apply {
            name = "AppLocker `${this@MessageClient.id}` message client thread"
            isDaemon = true
        }
    }

    /**
     * Send a message to the running instance
     *
     * @param msg the message to send, size is limited by 1024 bytes
     * @return result of the operation
     */
    fun sendMessage(msg: String, portNumber: Int): Response {
        val response = messageExecutor.submit(Callable {
            SocketChannel.open(InetSocketAddress(InetAddress.getLocalHost(), portNumber)).use { server ->
                // send message to server
                server.write(ByteBuffer.wrap(msg.toByteArray()))
                // wait for response
                val message = readChannel(server)
                Response.Answer(message)
            }
        })
        return response.get()
    }
}

/**
 * Reads data from channel to string
 */
private fun readChannel(client: ReadableByteChannel, bufferSize: Int = 1024): String {
    val sb = StringBuilder()
    val buffer = ByteBuffer.allocate(bufferSize)
    while (true) {
        if (client.read(buffer) <= 0) break

        buffer.flip()
        val bytes = ByteArray(buffer.limit())
        buffer.get(bytes)
        sb.append(String(bytes))
        buffer.clear()
    }
    return sb.toString()
}