package com.nipuna.bootanimator

import com.topjohnwu.superuser.Shell

/**
 * Thin wrapper around libsu for root shell access.
 * Magisk grants root via the su binary; libsu talks to it directly.
 */
object RootUtils {

    init {
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(15)
        )
    }

    /** Returns true if root was granted (triggers the Magisk prompt on first call). */
    fun hasRoot(): Boolean {
        return try {
            val shell = Shell.getShell()
            shell.isRoot
        } catch (e: Exception) {
            false
        }
    }

    /** Runs a single root command, returns (success, output lines). */
    fun run(cmd: String): Pair<Boolean, List<String>> {
        val result = Shell.cmd(cmd).exec()
        return Pair(result.isSuccess, result.out)
    }

    /** Runs multiple root commands in one session, returns (success, output lines). */
    fun run(vararg cmds: String): Pair<Boolean, List<String>> {
        val result = Shell.cmd(*cmds).exec()
        return Pair(result.isSuccess, result.out)
    }

    /** Checks if a path exists (file or dir) via root shell. */
    fun exists(path: String): Boolean {
        val (ok, out) = run("[ -e \"$path\" ] && echo YES || echo NO")
        return ok && out.any { it.trim() == "YES" }
    }
}
