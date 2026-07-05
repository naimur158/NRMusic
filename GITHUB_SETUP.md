# Publishing NR Music on GitHub (with automatic updates)

Once this is set up, **every time you push a change**, GitHub builds a signed APK,
publishes it as a Release, and any installed copy of the app auto-updates to it.

## One-time setup

### 1. Create the repo and push this project
```bash
cd "C:\1.Naimur Rahman\NRMusic"
git init
git add .
git commit -m "NR Music"
git branch -M main
# create an empty repo named NRMusic on github.com first, then:
git remote add origin https://github.com/<your-username>/NRMusic.git
git push -u origin main
```
> `nrmusic-release.jks` and `local.properties` are gitignored on purpose — never commit them.

### 2. Add the signing key as repository secrets
The build server needs your keystore to sign updates with the **same key** every time
(required so updates install over the old version).

First, turn the keystore into text you can paste (run in the project folder):
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("nrmusic-release.jks")) | Set-Clipboard
```
That copies the base64 keystore to your clipboard.

Then on GitHub: **Settings → Secrets and variables → Actions → New repository secret**, add:

| Secret name        | Value                                             |
|--------------------|---------------------------------------------------|
| `KEYSTORE_BASE64`  | *(paste the base64 text from your clipboard)*     |
| `KEYSTORE_PASSWORD`| `nrmusic123`                                      |
| `KEY_ALIAS`        | `nrmusic`                                          |
| `KEY_PASSWORD`     | `nrmusic123`                                       |

*(The passwords are the ones the keystore was created with. Change them in
`app/build.gradle.kts` and regenerate the keystore if you want your own.)*

### 3. Done — push to release
Any push to `main` (that touches code) triggers the **Build & Release APK** workflow
(`.github/workflows/release.yml`). Watch it under the repo's **Actions** tab. When it
finishes, a new **Release** appears with the APK attached.

## Installing so updates work
Install the APK **from a GitHub Release** (not a local build) once:
- Open the latest release on your phone → download `NRMusic-vN.apk` → install.

From then on, the app checks GitHub on launch and, when you've pushed a newer build,
shows an **"Update available"** dialog. Tap **Update now** → it downloads and installs.
(First time, Android asks you to allow NR Music to "install unknown apps" — allow it.)

## How versioning works
The workflow sets `versionCode` = the CI run number and tags the release `vN`.
The app compares that number to its own `versionCode`, so newer builds are always detected.
No manual version bumping needed.

## Sharing with friends
Send them the GitHub Release link (or the APK). If they install from a Release, they get
auto-updates too — every time you push, their app offers the update.
