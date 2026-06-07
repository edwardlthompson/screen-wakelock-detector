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
    suspend fun execute(allowlist: RootCommandAllowlist): RootCommandResult {
        val start = System.currentTimeMillis()
        val result = rootShellService.runCommand(allowlist.command)
        val duration = System.currentTimeMillis() - start
        return result.fold(
            onSuccess = { output ->
                RootCommandResult(
                    command = allowlist.command,
                    success = true,
                    output = output,
                    error = null,
                    durationMs = duration,
                )
            },
            onFailure = { error ->
                RootCommandResult(
                    command = allowlist.command,
                    success = false,
                    output = null,
                    error = error.message,
                    durationMs = duration,
                )
            },
        )
    }

    suspend fun executeUnsafe(input: String): RootCommandResult {
        val allowlist = RootCommandAllowlist.fromUserInput(input)
            ?: return RootCommandResult(
                command = input,
                success = false,
                output = null,
                error = "Command not in allowlist",
                durationMs = 0,
            )
        return execute(allowlist)
    }
}
