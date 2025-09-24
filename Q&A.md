### Questions:

- zmena je teda vždy iba typu "add", "update" alebo "delete" jedného alebo viac parametrov, ale nikdy nie kombinácia napr. "add" a "update"?
- "zmena parametra" ten parameter mám chápať ako leaf parameter v tom JSON, alebo ako root, alebo čokoľvek?
- pokúšam sa zisťiť viac o tých zmenách, hlavne či je to niečo uniformné, alebo tam môže byť čokoľvek

Mne tam totiž chýba "source of truth" tých konfigurácií a spôsob akým sa menia, keby som vedel kde a ako sa ukladajú možno by sa dal aj "tracking of config changes" urobiť priamočiarejšie ako volaním API. Ak to má prebublať do ďalších systémov a volanie API sa z akéhokoľvek dôvodu nepodarí (network, throttling, API down, atď.) tak prichádzame o notifikáciu tej zmeny. 

Napr. ak by boli konfigurácie uložené v DynamoDB tak cez DynamoDB Streams by som vedel pekne chytať zmeny v záznamoch.  Stream vieme nastaviť tak aby tam prišiel "OldImage" aj "NewImage" a teda vieme si urobiť DIFF tej zmeny, nemuseli by sme na to mať vôbec API... teda pokiaľ na to nie sú iné dôvody, o ktorých neviem.

### Answers:
- Add, update, delete sa teda týkajú jedno alebo viacerých parametrov
- Zmena môže byť aj kombináciou operácii, ale keď si to zjednodušíš na práve jeden typ nebude to vyslovene zle.
- Zmena môže byť aj lead parameter v JSON alebo root alebo čokoľvek - znova je na tebe čo si vyberieš
- Zadanie je tak spravené aby si ho senior vedel predstaviť aj prakticky zložité aj primerane zjednodušiť a junior spravil len nevyhnutne minimum.
- K tomu source od true a návrhu kľudne to prispôsobiť tak, aby tvoje riešenie (a riešená situácia) bola ideálna. Napríkad tak ako píšeš. Jediná podmienka je, aby sa to nevylučovalo so zadaním v ľubovoľnej creativnej interpretácii. 