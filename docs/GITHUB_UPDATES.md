# Konfiguracja aktualizacji TuneRide

Aktualizator korzysta z publicznych wydań repozytorium
`https://github.com/aimatthew/TuneRide`.

## 1. Utwórz stały klucz podpisujący

W PowerShellu uruchom (ścieżkę `keytool.exe` można też znaleźć w katalogu JDK Android Studio):

```powershell
keytool -genkeypair -v `
  -keystore "$env:USERPROFILE\TuneRide-release.jks" `
  -alias tuneride `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000
```

Zapisz hasła w menedżerze haseł. Zrób kopię pliku `TuneRide-release.jks` w bezpiecznym
miejscu. Utrata tego pliku uniemożliwi aktualizowanie istniejącej instalacji.

## 2. Dodaj sekrety GitHub Actions

Skopiuj klucz jako Base64:

```powershell
[Convert]::ToBase64String(
  [IO.File]::ReadAllBytes("$env:USERPROFILE\TuneRide-release.jks")
) | Set-Clipboard
```

W repozytorium otwórz **Settings → Secrets and variables → Actions → New repository
secret** i dodaj:

- `ANDROID_KEYSTORE_BASE64` — zawartość schowka;
- `ANDROID_KEYSTORE_PASSWORD` — hasło magazynu;
- `ANDROID_KEY_ALIAS` — `tuneride`;
- `ANDROID_KEY_PASSWORD` — hasło klucza.

Nie zapisuj klucza ani haseł w repozytorium.

## 3. Opublikuj pierwszą wersję

Po wysłaniu kodu na GitHub przejdź do **Actions → Build and publish TuneRide → Run
workflow**. Dla pierwszego wydania podaj:

- `version_name`: `0.1.0`;
- `version_code`: `1`;
- krótki opis zmian.

Workflow zbuduje podpisany APK i doda go do GitHub Releases. Przy każdym następnym
wydaniu zwiększ oba numery, np. `0.2.0` i `2`.

## Ważne przy pierwszej instalacji

Dotychczasowe wydania debug są podpisane innym kluczem. Przed instalacją pierwszego
APK z GitHuba trzeba jednorazowo odinstalować wersję debug. Od tej chwili kolejne APK
podpisane kluczem `TuneRide-release.jks` będą instalowały się jako aktualizacje bez
usuwania historii i ustawień.
