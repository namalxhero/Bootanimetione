package com.nipuna.bootanimator

/**
 * Handles locating, backing up, applying and restoring the boot animation
 * on rooted Samsung One UI devices.
 *
 * Note on Samsung One UI boot sequence:
 * The initial "Samsung" logo screen is a separate, proprietary boot video
 * baked into the bootloader/LOGO partition and is NOT safely replaceable
 * without real risk of a bootloop. What this app targets is the standard
 * AOSP-style `bootanimation.zip` — the spinning-dots/loading animation
 * that plays right after the Samsung logo, before the UI loads. That file
 * follows the normal Android bootanimation.zip format and lives on the
 * /system (or /product / /system_ext) partition, which is what's safe to
 * swap on a Magisk-rooted device.
 */
object BootAnimManager {

    // Known locations across Samsung One UI / AOSP variants, checked in order.
    val CANDIDATE_PATHS = listOf(
        "/system/product/media/bootanimation.zip",
        "/system/media/bootanimation.zip",
        "/system_ext/media/bootanimation.zip",
        "/product/media/bootanimation.zip",
        "/vendor/media/bootanimation.zip",
        "/odm/media/bootanimation.zip"
    )

    const val BACKUP_DIR = "/sdcard/BootAnimator/backup"
    const val BACKUP_FILE = "$BACKUP_DIR/bootanimation_original.zip"

    data class DetectResult(val found: Boolean, val path: String?, val log: List<String>)

    /** Scans all known partitions/paths for an existing bootanimation.zip. */
    fun autoDetect(): DetectResult {
        val log = mutableListOf<String>()
        for (path in CANDIDATE_PATHS) {
            log.add("Checking $path ...")
            if (RootUtils.exists(path)) {
                log.add("Found -> $path")
                return DetectResult(true, path, log)
            }
        }
        log.add("No bootanimation.zip found in known locations.")
        return DetectResult(false, null, log)
    }

    /** Backs up the currently active boot animation to shared storage. */
    fun backupCurrent(activePath: String): Boolean {
        val (ok, _) = RootUtils.run(
            "mkdir -p $BACKUP_DIR",
            "cp -f \"$activePath\" \"$BACKUP_FILE\"",
            "chmod 644 \"$BACKUP_FILE\""
        )
        return ok
    }

    fun hasBackup(): Boolean = RootUtils.exists(BACKUP_FILE)

    /**
     * Applies a new boot animation zip (already copied to app-accessible
     * [stagedPath], e.g. app cache dir) to the detected system [targetPath].
     */
    fun apply(stagedPath: String, targetPath: String): Boolean {
        val (ok, _) = RootUtils.run(
            "mount -o rw,remount /system 2>/dev/null; " +
                "mount -o rw,remount / 2>/dev/null; " +
                "cp -f \"$stagedPath\" \"$targetPath\" && " +
                "chmod 644 \"$targetPath\" && " +
                "chown root:root \"$targetPath\""
        )
        return ok
    }

    /** Restores the original boot animation from the backup file. */
    fun restore(targetPath: String): Boolean {
        if (!hasBackup()) return false
        return apply(BACKUP_FILE, targetPath)
    }

    fun reboot() {
        RootUtils.run("reboot")
    }
}
