package com.stunmap.service

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.stunmap.classifier.IpClassifier
import com.stunmap.parser.IpPacketParser
import com.stunmap.parser.StunParser
import com.stunmap.session.SessionManager
import com.stunmap.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class StunCaptureService : VpnService() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var ipClassifier: IpClassifier
    @Inject lateinit var stunParser: StunParser
    @Inject lateinit var ipPacketParser: IpPacketParser

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var captureJob: Job? = null

    companion object {
        const val ACTION_START = "com.stunmap.START_CAPTURE"
        const val ACTION_STOP = "com.stunmap.STOP_CAPTURE"

        fun start(context: Context) =
            context.startForegroundService(
                Intent(context, StunCaptureService::class.java).apply {
                    action = ACTION_START
                }
            )

        fun stop(context: Context) =
            context.startService(
                Intent(context, StunCaptureService::class.java).apply {
                    action = ACTION_STOP
                }
            )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCapture()
            ACTION_STOP -> stopCapture()
        }
        return START_STICKY
    }

    private fun startCapture() {
        ServiceNotification.createChannel(this)
        startForeground(Constants.NOTIFICATION_ID, ServiceNotification.build(this))

        try {
            val builder = Builder()
                .setSession("STUNMAP Capture")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("8.8.8.8")
                .addDnsServer("8.8.4.4")
                .setMtu(1500)
                .setBlocking(true)

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                Timber.e("VPN establish() returned null — permission not granted")
                stopSelf()
                return
            }

            captureJob = serviceScope.launch {
                try {
                    sessionManager.startSession()
                    readPacketLoop()
                } catch (e: Exception) {
                    Timber.e(e, "Capture session error")
                    sessionManager.markSessionInterrupted()
                }
            }
            Timber.d("StunCaptureService started")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start VPN")
            stopSelf()
        }
    }

    private suspend fun readPacketLoop() {
        val fd = vpnInterface!!.fileDescriptor
        val inputStream = FileInputStream(fd)
        val outputStream = FileOutputStream(fd)
        val buffer = ByteArray(32767)

        Timber.d("Packet read loop started")
        while (serviceScope.isActive) {
            try {
                val length = withContext(Dispatchers.IO) {
                    inputStream.read(buffer)
                }
                if (length <= 0) continue

                val packet = buffer.copyOf(length)

                // Forward immediately — must not block internet
                withContext(Dispatchers.IO) {
                    try {
                        outputStream.write(packet)
                    } catch (e: Exception) {
                        Timber.w(e, "Forward write failed")
                    }
                }

                withContext(Dispatchers.Default) {
                    processPacket(packet)
                }
            } catch (e: Exception) {
                if (serviceScope.isActive) {
                    Timber.w(e, "Packet read error — continuing")
                }
            }
        }
    }

    private suspend fun processPacket(packet: ByteArray) {
        try {
            val ipPacket = ipPacketParser.parse(packet) ?: return
            if (!ipPacket.isUdp) return

            val udpPayload = ipPacket.payload ?: return
            val stunPayload = if (udpPayload.size >= 8) {
                udpPayload.copyOfRange(8, udpPayload.size)
            } else return

            if (!stunParser.isStunPacket(stunPayload)) return

            val stunMessage = stunParser.parse(stunPayload, ipPacket.srcIp, ipPacket.dstIp)
                ?: return

            sessionManager.recordStunHit(stunMessage)
        } catch (e: Exception) {
            // Never crash the packet loop
            Timber.w(e, "processPacket error — skipping")
        }
    }

    private fun stopCapture() {
        Timber.d("StunCaptureService stopping")
        captureJob?.cancel()
        vpnInterface?.close()
        vpnInterface = null

        serviceScope.launch {
            try {
                sessionManager.endSession()
            } catch (e: Exception) {
                Timber.w(e, "endSession error")
            }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        Timber.d("VPN permission revoked by user")
        stopCapture()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        serviceScope.launch {
            try {
                sessionManager.markSessionInterrupted()
            } catch (e: Exception) {
                Timber.w(e, "markSessionInterrupted error on task removed")
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        captureJob?.cancel()
        vpnInterface?.close()
        super.onDestroy()
    }
}
