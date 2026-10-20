#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running emulator.
# The sample shows bundled demo content only (made-up example.com addresses, blog topics and skills), so nothing is
# loaded from the network. It opens each scene from the `scene` extra, drives the tag inputs through their public API
# (typing text, focusing, moving the suggestion highlight) and logs SCENE_READY once the screen has settled. A scene
# that logs SCENE_FAILED (for example when the suggestion list did not open) or never becomes ready fails the run.
# Every capture is also checked for the expected text and for a blank image.
#
#   bash scripts/screenshots.sh tablet   # pixel_tablet in landscape: all three forms side by side
#   bash scripts/screenshots.sh phone    # pixel_7: recipients, open suggestions, validation error
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

device="${1:-phone}"

# The text each scene must show; the capture fails without it.
expected_text() {
  case "$1" in
    recipients) echo "New message" ;;
    suggestions) echo "Write a post" ;;
    validation) echo "Not a valid email address" ;;
    tablet-form) echo "You can list up to 5 skills" ;;
  esac
}

install_sample
set_night_mode light
if [ "$device" = tablet ]; then
  ensure_landscape
  fresh_launch --es scene tablet-form
  wait_for_scene tablet-form
  capture "tablet-form" "$(expected_text tablet-form)"
else
  for scene in recipients suggestions validation; do
    fresh_launch --es scene "$scene"
    wait_for_scene "$scene"
    if [ "$scene" = suggestions ]; then
      # The scene focuses the field and hides the soft keyboard itself; make sure it is gone before the capture.
      if adb shell dumpsys input_method | grep -q "mInputShown=true"; then
        # Back closes the keyboard (the IME takes it); Escape would reach the field and close the list.
        adb shell input keyevent KEYCODE_BACK > /dev/null 2>&1 || true
        sleep 1
      fi
    fi
    capture "phone-$scene" "$(expected_text "$scene")"
  done
fi
