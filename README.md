# MANISHA Android AI — starter project

A native Android starter app for the Redmi K50i. It includes:
- A futuristic female-assistant-inspired interface (a custom vector-style female AI avatar illustration with a pulsing holographic frame).
- Manual microphone input using the Android speech recognizer.
- Groq chat-completions API connection.
- Hindi, English, Marathi and Gujarati prompt handling.
- Android Text-to-Speech output using the selected locale when the installed TTS engine supports it.
- Settings for Groq API key, model ID and preferred language.
- GitHub Actions workflow to build a debug APK in the cloud.

## Important limitations in this first build

1. This is source code, not a prebuilt or tested APK. It must be built and installed.
2. Speech recognition and female TTS voice availability depend on the speech/TTS services and language packs installed on the phone.
3. The app uses the phone's Android speech-recognition activity for manual input. It does not continuously listen for "Hey Manisha".
4. Wake-word detection, background foreground-service behavior, phone actions, reminders and a custom realistic avatar are later development stages.
5. The Groq API key is stored in app-private SharedPreferences in this starter and is not encrypted in this prototype. It is not baked into the APK, but do not commit the key to GitHub or share it in screenshots. A production release should use encrypted local storage or, preferably, a backend proxy so provider credentials are not exposed in a distributed app.
6. Groq usage and model availability are subject to current account limits and provider policies. The default model is `openai/gpt-oss-20b`, currently listed in Groq's supported-model documentation; you can change it in Settings if availability changes.

## Build APK using GitHub from your phone (no PC)

1. On your Android phone, open GitHub in Chrome and sign in or create an account.
2. Create a new repository named `manisha-android-ai`.
3. Upload the contents of this ZIP to the repository root. Extract the ZIP first using your phone's file manager.
4. Open the repository's **Actions** tab. The workflow named **Build MANISHA Android APK** should run automatically after the files are uploaded to the default branch. If not, open Actions and run it manually.
5. Wait for the workflow to finish successfully.
6. Open that workflow run, find **Artifacts**, and download `MANISHA-debug-apk`.
7. Extract the downloaded artifact ZIP, then tap `app-debug.apk` to install. Android may ask you to allow installation from Chrome or Files.

## First launch

1. Open MANISHA.
2. Tap the Settings gear.
3. Create a Groq API key in the Groq Console, then paste it into the app.
4. Keep model ID as `openai/gpt-oss-20b` unless your Groq account's model list indicates otherwise.
5. Select a preferred language and save.
6. Tap the microphone button and allow microphone access. Speak, then tap Send.
7. If Android asks you to install language data for text-to-speech, follow the prompt or install a female voice in your phone's Text-to-speech settings.

## Phone-specific notes

On Xiaomi/MIUI/HyperOS, battery optimization can stop background services. This first version does not need background microphone access. Wake-word mode will require a dedicated, user-enabled foreground service and battery/permission testing on the Redmi K50i.

## Cloud build details

The included GitHub Actions workflow uses a hosted Ubuntu runner and Gradle 8.9 / JDK 17. No PC is needed, but uploading files and downloading the APK must be done on the phone.
