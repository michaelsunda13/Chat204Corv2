package ru.smak.chat

import java.io.PrintWriter
import java.net.Socket
import java.nio.channels.AsynchronousSocketChannel
import java.util.*
import kotlin.concurrent.thread

class Communicator(
    private val socket: AsynchronousSocketChannel
) {
    var isRunning = false
        private set
    private var parse: ((String)->Unit)? = null

    private fun startMessageAccepting(){
        thread {
            while (isRunning){
                try {
                    val message = reader.nextLine()
                    parse?.invoke(message)
                } catch (_: Throwable){
                    break
                }
            }
        }
    }

    fun sendMessage(message: String){


        writer.println(message)
        writer.flush()
    }

    fun start(parser: (String)->Unit){
        if (socket.isClosed) throw Exception("Connection closed")
        parse = parser
        if (!isRunning){
            isRunning = true
            startMessageAccepting()
        }
    }

    fun stop(){
        isRunning = false
        socket.close()
    }

}