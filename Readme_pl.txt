Carnobyl64k 1.0
m4tx

http://m4tx.pl/

--------------------------------------------------------------------------------

Gra wymaga środowiska Java Runtime Environment w wersji 6 (1.6) lub nowszej.
Gra powinna działać bezproblemowo na systemach Windows, Linux i Mac.

--------------------------------------------------------------------------------

Carnobyl64k to gra z widokiem z góry, w której sterujemy samochodem, a naszym
głównym celem jest rozjechanie wszystkich przechodniów. W lewym górnym rogu
podana jest liczba przechodniów na mapie oraz ich łączna ilość. Cała grafika
wykorzystywana w grze jest generowana z poziomu kodu i można podać seed
(ziarno), które będzie wykorzystywane do generowania grafiki, za pomocą
parametru --seed, np.:
 java -jar Carnobyl.jar --seed 16711935
Uruchomienie gry w ten sposób da nam różowy (magenta) samochód. Ziarno musi być
liczbą, nie może to być np. tekst.

Gra ma otwarty kod źródłowy i jest dostępna na licencji GNU GPL v3.

--------------------------------------------------------------------------------

Sterowanie:
- W - przyspieszanie
- S - hamowanie
- A - skręcanie w lewo
- D - skręcanie w prawo

--------------------------------------------------------------------------------

Jeżeli gra nie działa płynnie, spróbuj uruchomić wirtualną maszynę Javy
z parametrem:
 -Dsun.java2d.opengl=true
Na przykład:
 java -Dsun.java2d.opengl=true -jar Carnobyl.jar

--------------------------------------------------------------------------------

Autorzy:
- Główny pomysł, wykonanie: m4tx
Podziękowania również dla:
- rhino

--------------------------------------------------------------------------------

Dzięki za pobranie gry!

--------------------------------------------------------------------------------

m4tx
www.m4tx.pl

