package com.guitartuner.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import kotlin.math.sqrt

/**
 * 麦克风采集引擎：后台线程循环读取 PCM，RMS 静音过滤后交给 YIN 检测，
 * 结果回传到主线程。回调参数为 null 表示当前没有有效声音信号。
 */
class AudioEngine(private val onResult: (PitchResult?) -> Unit) {

    private val sampleRate = 44100
    private val bufferSize = 4096

    private var record: AudioRecord? = null
    private var thread: Thread? = null

    @Volatile private var running = false
    @Volatile var active = false
        private set

    private val detector = PitchDetector(sampleRate, bufferSize)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val readBuffer = ShortArray(bufferSize)
    private val floatBuffer = FloatArray(bufferSize)

    fun start() {
        if (running) return
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, bufferSize * 2)
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return
        }
        record = rec
        running = true
        active = true
        rec.startRecording()
        thread = Thread { readLoop() }.apply {
            name = "AudioEngine"
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        active = false
        try {
            thread?.join(200)
        } catch (_: InterruptedException) {
        }
        thread = null
        record?.run {
            try { stop() } catch (_: Exception) {}
            release()
        }
        record = null
    }

    private fun readLoop() {
        var hadSignal = false
        while (running) {
            val rec = record ?: break
            val n = rec.read(readBuffer, 0, bufferSize, AudioRecord.READ_BLOCKING)
            if (n <= 0) continue

            // 转 float 并归一化，同时算 RMS
            var sumSq = 0.0
            for (i in 0 until n) {
                val v = readBuffer[i].toFloat() / 32768f
                floatBuffer[i] = v
                sumSq += v * v
            }
            val rms = sqrt(sumSq / n)

            val result = if (rms < 0.008) null else detector.detect(floatBuffer)

            // 频率范围覆盖贝斯 B0(31Hz) 到古筝 G6(1568Hz)
            if (result == null || result.frequency < 28f || result.frequency > 1600f) {
                if (hadSignal) {
                    hadSignal = false
                    mainHandler.post { if (active) onResult(null) }
                }
                continue
            }
            hadSignal = true
            mainHandler.post { if (active) onResult(result) }
        }
    }
}
