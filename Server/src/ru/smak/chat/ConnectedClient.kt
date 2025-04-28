package ru.smak.chat

import java.nio.channels.AsynchronousSocketChannel

class ConnectedClient(socket: AsynchronousSocketChannel) {

    private val communicator = Communicator(socket)
    private var userName: String? = null

    init{
        connectedClients.add(this)
        communicator.start { message -> parse(message) }
    }

    private fun parse(message: String){
        sendToAll(message, echo = false)
    }

    fun stop(){
        communicator.stop()
    }

    private fun sendToAll(message: String, echo: Boolean = true){
        connectedClients.forEach {
            if (echo || it != this) it.communicator.sendMessage(message)
        }
    }

    companion object {
        private val connectedClients = mutableListOf<ConnectedClient>()
    }
}