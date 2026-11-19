# Blazer

A simple, elegant IR remote for Android phones with a built-in IR blaster (built for the RedMagic 11 Pro).

## Features

- **Scan** — choose TV & media or Air conditioner, point your phone at the device, tap Scan, and tell Blazer when the device responds. It saves the matching code set as a remote.
- **Clean remote layout** — power, volume, mute, channel, menu, source/input, home, back, and a D-pad with OK.
- Hold volume, channel, or the arrows to repeat.
- Save several remotes, rename or delete them. Blazer reopens the last remote you used.

## Supported devices

| Code set | Protocol | Notes |
|---|---|---|
| Samsung TV | Samsung32, address 0x07 | |
| LG TV | NEC, address 0x04 | |
| Roku TV | NEC extended, address 0xC7EA | TCL, Hisense, Insignia, and other Roku TVs. Roku has no Source button. |
| Roku streaming player | NEC extended, address 0xC7EA | IR-capable Roku boxes and sticks. Many newer Roku sticks use Wi-Fi remotes and won't respond to IR. |
| Sony TV | SIRC 12-bit, address 1 | |
| Vizio TV | NEC, address 0x04 | Shares Power/Volume codes with LG; Scan lets you pick which brand. |
| Sharp Aquos TV | Sharp, address 1 | Power, volume, mute, channel, input. Newer Sharp Roku TVs use the Roku code set. |
| Panasonic TV | Kaseikyo (Panasonic) | |

### Air conditioners and mini-splits (experimental)

AC remotes send the whole state on every press. Blazer's AC remote has power, temperature (°F or °C), mode (Cool, Heat, Dry, Fan, Auto) and fan speed (Auto, Low, Med, High). Changes are sent only while the unit is on.

| Code set | Covers |
|---|---|
| Gree | Gree, Cooper & Hunter, Tosot and other Gree-made units |
| Midea (Coolix) | Midea, Senville, Comfee, Pioneer and other Midea-made units |
| Mitsubishi Electric | Mitsubishi Electric mini-splits |
| LG (type 1 / type 2) | LG air conditioners; two timing variants |

## Install

Every push to `main` builds a new APK. Open **Releases** on this repo from your phone, download `Blazer.apk`, and open it. New versions install over the old one.

## Adding a brand

Codes live in `app/src/main/java/com/z3rotheone/blazer/IrCodes.kt`. Add a function that maps each `Key` to a signal (`Ir.nec`, `Ir.samsung`, or `Ir.sony12`), then add a `RemoteProfile` to `Profiles.all`. Scan picks it up on its own.

## Signing

`app/signing.keystore` is a throwaway debug-style key (password `android`). It's committed on purpose, so every build has the same signature and installs as an update.
