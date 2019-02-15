package io.kani.gui.lock

import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

internal class SimpleMessageServer(val id: String) : Runnable {

    override fun run() {
    }

    fun stop() {
    }

}

internal class SimpleMessageClient(val id: String) {


}

//private class LockerServer(val id: String) {
//    private val connections: MutableList<Any> = emptyList<Any>().toMutableList()
//
//    fun start(portFile: File, messageHandler: MessageHandler, backlog: Int = 20) {
//        ServerSocket(0, backlog, InetAddress.getLocalHost()).use {
//            while (true) {
//                val client = it.accept()
//                println("Client connected: ${client.inetAddress.hostAddress}")
//
//                // Run client in it's own thread.
//                thread { ClientHandler(client).run() }
//            }
//        }
//    }
//
//    fun sendMessage(msg: String) {
//
//    }
//
//    fun stop() {
//
//    }
//}
