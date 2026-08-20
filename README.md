# Boot Animator

Root-powered boot animation switcher for Samsung One UI (Magisk root).

## What it does
1. **Root Access** card - requests Magisk root via libsu.
2. **Auto-Detect** card - scans known partitions (`/system/product/media`, `/system/media`, `/system_ext/media`, `/product/media`, `/vendor/media`, `/odm/media`) for the active `bootanimation.zip`.
3. **Backup** - saves the currently active file to `/sdcard/BootAnimator/backup/bootanimation_original.zip` before you change anything.
4. **Apply New** - pick any properly-formatted `bootanimation.zip` from storage and it gets root-copied to the detected system path with correct permissions.
5. **Restore** - one tap to put the original back from backup.
6. **Reboot** - reboots via root so you can see the change.

## Important - read before flashing
On Samsung One UI, the very first "Samsung" logo screen is a separate proprietary boot video baked into the bootloader/LOGO partition — this app does **not** touch that, and you shouldn't try, since it risks a bootloop. What this app safely replaces is the animation that plays **after** the Samsung logo (the loading dots), which uses the standard AOSP `bootanimation.zip` format.

Always hit **Backup** before **Apply New**, at least the first time — that's your safety net if the detected path is wrong for your firmware build.

## Build via your workflow
Same as your ESP32 project:
```bash
unzip BootAnimator.zip
cd BootAnimator
git init && git add . && git commit -m "init"
git remote add origin https://github.com/namalxhero/<repo-name>.git
git push -u origin main
```
GitHub Actions (`.github/workflows/build.yml`) builds a debug APK automatically and uploads it as an artifact — grab it from the Actions run.

## bootanimation.zip format (for the file you'll pick)
A standard zip containing:
- `desc.txt` (width height fps, then part lines)
- `part0/`, `part1/` folders of numbered PNG frames

If you want, next step can be a simple in-app "convert my video/images into a valid bootanimation.zip" builder — right now you supply an already-packaged zip.
