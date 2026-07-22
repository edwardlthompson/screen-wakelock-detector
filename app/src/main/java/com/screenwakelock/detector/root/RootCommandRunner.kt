package com.screenwakelock.detector.root

data class RootCommandResult(
    val command: String,
    val success: Boolean,
    val output: String?,
    val error: String?,
    val durationMs: Long,
)

class RootCommandRunner(
    private val rootShellService: RootShellService,
) {
    suspend fun execute(allowlist: RootCommandAllowlist): RootCommandResult =
        executeCommand(allowlist.command)

    /**
     * Runs a command only if [RootCommandAllowlist.isAllowed] accepts it
     * (fixed enum entries or validated templates).
     */
    suspend fun executeCommand(command: String): RootCommandResult {
        if (!RootCommandAllowlist.isAllowed(command)) {
            return RootCommandResult(
                command = command,
                success = false,
                output = null,
                error = "Command not in allowlist",
                durationMs = 0,
            )
        }
        val start = System.currentTimeMillis()
        val result = rootShellService.runCommand(command)
        val duration = System.currentTimeMillis() - start
        return result.fold(
            onSuccess = { output ->
                RootCommandResult(
                    command = command,
                    success = true,
                    output = output,
                    error = null,
                    durationMs = duration,
                )
            },
            onFailure = { error ->
                RootCommandResult(
                    command = command,
                    success = false,
                    output = null,
                    error = error.message,
                    durationMs = duration,
                )
            },
        )
    }

    suspend fun executeUnsafe(input: String): RootCommandResult = executeCommand(input)
}
