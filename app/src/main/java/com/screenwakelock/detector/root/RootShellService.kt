package com.screenwakelock.detector.root

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class RootShellService {
    private val mutex = Mutex()
    private var sessionReady = false

    suspend fun preheat(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val result = withTimeoutOrNull(10_000L) {
                    Shell.getShell().isRoot
                } ?: false
                sessionReady = result
                result
            }.getOrDefault(false)
        }
    }

    suspend fun isRootAvailable(): Boolean {
        if (!sessionReady) preheat()
        return sessionReady && Shell.getShell().isRoot
    }

    suspend fun runCommand(command: String, timeoutMs: Long = 15_000L): Result<String> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (!RootCommandAllowlist.isAllowed(command)) {
                    return@withContext Result.failure(
                        SecurityException("Command not in allowlist: $command"),
                    )
                }
                if (!isRootAvailable()) {
                    return@withContext Result.failure(IllegalStateException("Root not available"))
                }
                runCatching {
                    val job = withTimeoutOrNull(timeoutMs) {
                        Shell.cmd(command).exec()
                    } ?: return@withContext Result.failure(
                        java.util.concurrent.TimeoutException("Root command timed out"),
                    )
                    if (!job.isSuccess) {
                        return@withContext Result.failure(
                            RuntimeException(job.err.joinToString("\n").ifEmpty { "Root command failed" }),
                        )
                    }
                    val output = job.out.joinToString("\n")
                    if (output.length > MAX_OUTPUT_BYTES) {
                        output.take(MAX_OUTPUT_BYTES)
                    } else {
                        output
                    }
                }
            }
        }

    fun dropSession() {
        sessionReady = false
        runCatching { Shell.getShell().close() }
    }

    companion object {
        const val MAX_OUTPUT_BYTES = 512_000
    }
}
