package ru.smak.chat

import kotlinx.coroutines.*
import java.nio.channels.AsynchronousSocketChannel

class ConnectedClient(socket: AsynchronousSocketChannel) {

    private val communicator = Communicator(socket)
    private var userName: String? = null
    private val clientScope = CoroutineScope(Dispatchers.IO)

    init{
        connectedClients.add(this)
        communicator.start { message -> parse(message) }
        clientScope.launch{communicator.sendMessage("Введите ник:")}
        communicator.onClose = {
            connectedClients.remove(this)
            userName?.let{ sendToAll("$it покинул чат.", echo = false)}
        }
    }

    private fun parse(message: String){
        userName?.let{
            if(message.startsWith("@")){
                directMessage(message)
            } else {
                sendToAll("$userName: $message", echo = true)
            }
        } ?: run {
            val usernameAttempt = message.replace(" ", "") //удаление пробелов вообще.
            if  (!usernameAttempt[0].isLetter() || usernameAttempt.isEmpty()) {
                clientScope.launch { communicator.sendMessage("[!] должен начинаться с буквы. Попробуйте снова:") }
                return@run
            }

            var unavailable = false
            connectedClients.forEach {
                if(it !== this && it.userName?.equals(usernameAttempt) == true) unavailable = true
            }
            if (unavailable) {
                clientScope.launch {communicator.sendMessage("[!] Ник '$usernameAttempt' занят. Попробуйте снова:")}
                return@run
            }

            userName = usernameAttempt
            clientScope.launch { communicator.sendMessage("[!] Установлен ник '$userName'.") }
            sendToAll("$userName присоединился к чату.", echo = false)

        }
    }

    private fun directMessage(message: String) {
        val parts = message.split(" ", limit = 2)
        if (parts.size < 2) {
            clientScope.launch { communicator.sendMessage("[!] Какая-то ошибка, вот и думай...")}
            return
        }
        val target = parts[0].substring(1) //всё что после "@"
        val newMessage = parts[1]
        connectedClients.forEach {
            if ( it.userName == target) clientScope.launch {
                it.communicator.sendMessage("[От $userName]: $newMessage")
                communicator.sendMessage("[К $target]: $newMessage")
            }
        }
    }

    fun stop(){
        communicator.stop()
    }

    private fun sendToAll(message: String, echo: Boolean = true){
        connectedClients.forEach {
            if (echo || it != this) clientScope.launch { it.communicator.sendMessage(message) }
        }
    }

    companion object {
        private val connectedClients = mutableListOf<ConnectedClient>()
    }
}