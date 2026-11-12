# SklepGUI

Plugin (Spigot / Paper **1.21+**, Java 21) zastepujacy Twoje 3 skrypty:
`/sklep` (menu glowne), `/sklepzaserca`, `/sklepzawalute`, `/sklepzaczas`, rynek -> `/ah`.

## Budowanie (1 komenda)
Wymagane: JDK 21 + Maven.
```
mvn clean package
```
Gotowy plik: `target/SklepGUI-1.0.0.jar` -> wrzuc do `plugins/`.
(IntelliJ: File > Open > pom.xml > Maven > Lifecycle > package)

## Wymagania na serwerze
- **Vault + plugin ekonomii** (np. EssentialsX) - tylko dla sklepu za walute (`/sklepzawalute`)
- Opcjonalnie: LuckPerms i plugin kluczy (komendy z `czas.yml`)

## WAZNE przy przejsciu ze Skripta
Usun/wylacz stare skrypty (`Sklep-Anarchia.sk`, `Sklepczas-Anarchia.sk`, `XSLODZIAK-SklepZaSerca.sk`),
bo ich komendy kolidowalyby z pluginem.
W `XSLODZIAK-SklepZaSerca.sk` zostaw tylko czesc od serc (`/wyplacserce`, uzywanie serc, smierc) -
usun z niego komende `/sklepzaserca` i obsluge klikniecia w GUI.

## Pliki konfiguracyjne (plugins/SklepGUI/)
| plik | co edytujesz |
|---|---|
| `config.yml` | menu `/sklep` (sloty, nazwy, komendy), wiadomosci |
| `serca.yml` | sklep za serca - itemy, koszty, nagrody, losowania |
| `waluta.yml` | sklep za walute - ceny kupna/sprzedazy, GUI ilosci |
| `czas.yml` | sklep za czas - itemy, koszty, komendy (klucze, rangi) |

Po edycji: `/sklepadmin reload`

## Komendy
| komenda | opis |
|---|---|
| `/sklep` `/sklepzaserca` `/sklepzawalute` `/sklepzaczas` | dla graczy (`sklep.use`, domyslnie wszyscy) |
| `/sklepadmin reload` | przeladowanie configow |
| `/sklepadmin czas <get/set/add/remove> <gracz> [czas]` | zarzadzanie czasem, np. `4h`, `2d`, `1d12h` |
| `/sklepadmin pool add <nazwa>` | dodaje przedmiot z reki do puli losowej (zastepuje `/xslodziak-ustaw-ksiege`) |
| `/sklepadmin pool clear <nazwa>` | czysci pule (zastepuje `/xslodziak-usun-ksiegi`) |
| `/sklepadmin dajserce [ilosc]` | daje przedmiot-serce (do testow) |

Uprawnienie administracyjne: `sklep.admin` (domyslnie OP).

## Dodawanie przedmiotow
**Sklep za serca / czas** - skopiuj dowolny blok w `items:` i zmien: `slot`, `cost`, wyglad
(`material`, `name`, `lore`, `enchants`...) oraz nagrode (`rewards`, `pool`, `commands`).
**Sklep za walute** - dopisz blok w `buy.items:` lub `sell.items:` (`slot`, `material`, `price`).

## Uwagi o zmianach wzgledem skryptow
- Czas gry jest teraz liczony przez plugin (zapis w `data.yml`). Stare wartosci ze Skripta
  nie przenosza sie same - ustaw je `/sklepadmin czas set`, albo wlacz `time.seed-from-playtime`
  w `config.yml`, by nowi gracze startowali ze swoim czasem gry z statystyk MC.
- Sprzedaz: surowa miedz miala w skrypcie 0.40$ za sztuke, a 0.08$ przy SHIFT+PPM (blad).
  Teraz zawsze 0.40$ (zmienisz w `waluta.yml`).
- Sprzedawane sa tylko "zwykle" przedmioty (bez nazwy/zaklec) - opcja `sell.only-plain-items`.
- Zakup sprawdza miejsce w ekwipunku, a losowanie ksiegi z pustej puli nie zabiera serc.
