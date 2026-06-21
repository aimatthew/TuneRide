# TuneRide

Prywatna aplikacja Android do pobierania ścieżki audio i zapisywania jej jako MP3.

## Funkcje

- wklejanie linku lub użycie opcji **Udostępnij** w aplikacji YouTube;
- konwersja do MP3: 128, 192, 256 albo 320 kb/s;
- wybór dowolnego folderu przez systemowy selektor Androida;
- domyślny zapis do `Music/TuneRide`;
- trwała historia tytułów, wykonawców, dat, jakości i statusu;
- miniatury, postęp pobierania i otwieranie gotowego pliku;
- praca w tle z powiadomieniem.

## Otwieranie projektu

1. Otwórz katalog projektu w Android Studio.
2. Użyj JDK 17 i pozwól Android Studio wykonać synchronizację Gradle.
3. Jeżeli projekt nie ma jeszcze pliku `gradle-wrapper.jar`, wybierz lokalną instalację
   Gradle 8.9 albo wykonaj `gradle wrapper --gradle-version 8.9`.
4. Zbuduj wariant `debug` i zainstaluj APK ręcznie na telefonie.

Projekt jest przeznaczony dla Androida 10 lub nowszego. Pierwsze uruchomienie silnika
może potrwać dłużej, ponieważ rozpakowuje on do pamięci aplikacji środowisko yt-dlp
i FFmpeg.

## Gdzie trafiają pliki

Bez zmiany ustawienia pliki trafiają do `Music/TuneRide`. Po dotknięciu pola **Folder
zapisu** można wskazać dowolny katalog, również na karcie SD. Aplikacja zachowuje
uprawnienie do tego katalogu po ponownym uruchomieniu.

## Ważne technicznie

Silnik yt-dlp jest zależny od zmian po stronie obsługiwanych serwisów. Jeśli pobieranie
przestanie działać mimo prawidłowego linku, zwykle trzeba zaktualizować bibliotekę
`youtubedl-android` albo sam pakiet yt-dlp. Pobieraj wyłącznie treści, do których masz
prawa lub zgodę autora.
