# Więzi ze Zwierzętami (Animal Bonds)

Mod do Minecraft **26.2** na **NeoForge**. Opiekuj się zwierzętami i potworami, buduj z nimi więź (poziomy 1–10), a one odwdzięczą się podarunkami, wierzchowcem, jukami i lojalnością aż po omdlenie.

## Jak grać

| Akcja | Sterowanie |
|---|---|
| Nawiązanie więzi / karmienie z ręki | **kucnij + PPM** ulubionym jedzeniem stwora |
| Głaskanie | **PPM pustą ręką** (albo pędzlem) |
| Ujeżdżanie (od poz. 5) | **kucnij + PPM pustą ręką**; zsiadanie: kucnięcie |
| Juki (od poz. 4) | **PPM jukami** na stworze; otwierasz je w Kronice albo klawiszem ekwipunku podczas jazdy |
| Kronika Zagrody | postaw **piedestał z księgą** w zagrodzie lub tuż przy jej płocie i kliknij PPM |
| Koryto | PPM paszą (pszenica, nasiona, warzywa, mięso, siano = pełne) |

Oswojone w zwykły sposób zwierzęta (wilk, kot, koń…) dostają więź automatycznie.

### Poziomy (wszystko da się zmienić w `config/animalbonds-common.toml`)
- **1**: można nadać imię w Kronice; potwór przestaje atakować graczy
- **2**: stwór zaczyna przynosić podarunki (materiały) zamiast trzeba go zabijać
- **4**: można założyć juki
- **5**: można ujeżdżać
- **7**: zamiast umrzeć, stwór mdleje i wraca do swojej zagrody, gdzie się regeneruje
- **8+**: hojniejsze podarunki, krótsza regeneracja

### Potrzeby
Każdy gatunek ma swoje potrzeby, np. krowa: koryto, trawa, woda, miejsce, towarzystwo, dach, przysmak i głaskanie.
Zombie: koryto, dach, ciemność, towarzystwo… Spełnione potrzeby podnoszą zadowolenie, a ono przyspiesza więź i podarunki.

### Zagroda
Zamknięty płotem (lub murem, ścianą) teren na jednym poziomie, maks. 1600 bloków. Otwarta furtka = brak zagrody.

## Budowanie
`./gradlew build` → `build/libs/animalbonds-*.jar`. Każdy push uruchamia GitHub Actions:
kompilację, automatyczny test serwerowy (`SelfTest`) i test wizualny klienta ze zrzutami ekranu.
Wyniki, logi i gotowy `.jar` lądują na gałęzi `ci-results`.
