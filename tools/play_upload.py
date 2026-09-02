#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.11"
# dependencies = ["google-auth>=2.30", "requests>=2.32"]
# ///
"""Upload an AAB to a Google Play track and create a release.

    tools/play_upload.py <path.aab> <track> <releaseName> <status> [userFraction]

    track:  internal | alpha (closed) | beta (open) | production
    status: completed | inProgress | draft | halted
            userFraction is required for inProgress (e.g. 0.2), rejected otherwise.

The app itself must already exist in the Play Console: the Android Publisher API can
publish to a package but cannot create one, so a brand new app is a console-only step.

Credentials come from the same service account the Prava app uses. Its contents are
never read or printed by this script beyond handing the file to google-auth.
"""
import os
import sys

import requests
from google.auth.transport.requests import Request
from google.oauth2 import service_account

KEY = os.path.expanduser(os.environ.get("PLAY_KEY", "~/.config/play/trongio-key.json"))
PKG = os.environ.get("PLAY_PACKAGE", "ge.hackerman.gree")
NOTES = os.path.join(os.path.dirname(__file__), "..", "play", "release_notes_en.txt")

API = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{PKG}"
UPLOAD = f"https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/{PKG}"
SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]


def headers():
    creds = service_account.Credentials.from_service_account_file(KEY, scopes=SCOPES)
    creds.refresh(Request())
    return {"Authorization": f"Bearer {creds.token}"}


def check(response, what):
    if not response.ok:
        sys.exit(f"{what} failed: {response.status_code} {response.text[:400]}")
    return response


def main():
    if len(sys.argv) < 5:
        sys.exit(__doc__)

    aab, track, release_name, status = sys.argv[1:5]
    fraction = float(sys.argv[5]) if len(sys.argv) > 5 else None
    if status == "inProgress" and fraction is None:
        sys.exit("inProgress needs a userFraction, e.g. 0.2")
    if not os.path.exists(aab):
        sys.exit(f"no such bundle: {aab}")

    h = headers()

    edit = check(requests.post(f"{API}/edits", headers=h, json={}), "open edit").json()["id"]
    print(f"edit {edit}")

    with open(aab, "rb") as f:
        up = check(
            requests.post(
                f"{UPLOAD}/edits/{edit}/bundles?uploadType=media",
                headers={**h, "Content-Type": "application/octet-stream"},
                data=f,
            ),
            "upload bundle",
        ).json()
    version = up["versionCode"]
    print(f"uploaded versionCode {version}")

    release = {
        "name": release_name,
        "versionCodes": [str(version)],
        "status": status,
    }
    if fraction is not None:
        release["userFraction"] = fraction
    if os.path.exists(NOTES):
        with open(NOTES) as f:
            release["releaseNotes"] = [{"language": "en-US", "text": f.read().strip()}]

    check(
        requests.put(
            f"{API}/edits/{edit}/tracks/{track}",
            headers=h,
            json={"track": track, "releases": [release]},
        ),
        "set track",
    )
    print(f"track {track} <- {release_name} ({status})")

    check(requests.post(f"{API}/edits/{edit}:commit", headers=h, json={}), "commit")
    print("committed")
    print("\nWith managed publishing on, this sits in 'changes ready to publish' until")
    print("you publish it in the console. It does not reach users yet.")


if __name__ == "__main__":
    main()
