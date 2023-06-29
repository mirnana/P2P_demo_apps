# Gnutella

Ovo je implementacija pojednostavljene verzije Gnutelle. Gnutella je sustav ravnopravnih partnera za dijeljenje datoteka. Naša implementacija služi samo za razmjenu podataka o duljinama rijeka diljem svijeta. Te podatke će svaki čvor čuvati u lokalnom rječniku (ime rijeke : duljina rijeke).

Mreža je nestrukturirana, odnosno pojedini čvor nema informaciju o podacima koje posjeduju ostali čvorovi. 

Tipična Gnutella ima poruke tipa Ping, Pong, Query i QueryHit. Svi podaci koji se dijele u našoj pojednostavljenoj verziji Gnutelle bit će tipa String, stoga sve vrste poruka među čvorovima rade slično. 

Čvor šalje Ping poruku kroz mrežu i smatra se povezanim s onim čvorovima od kojih zaprimi Pong. Ping-Pong poruke šalju se periodično. Ako pojedini čvor detektira da od nekoga nije zaprimio Pong u posljednjih nekoliko ciklusa, briše ga iz svoje liste povezanih. Pong treba sadržavati broj podataka spremljenih na čvoru koji ga šalje.

Query je upit kojeg neki čvor šalje svojim susjedima. Čvor koji zaprimi takav upit treba pogledati ima li potreban podatak -- ako ima, poslati QueryHit poruku kao odgovor, a inače treba proslijediti upit svojim susjedima. Ako zaprimi QueryHit poruku za neki proslijeđeni Query, onda treba taj podatak spremiti i dalje proslijediti onom čvoru koji ga je tražio. Kako Query poruke ne bi vječno kružile po sustavu, ograničavamo vrijeme u sekundama za prosljeđivanje i servisiranje Query-ja, kao i timestamp kad je zadnji put proslijeđen.

Pokretanje: 

```
javac *.java
java Gnutella --port <mojPort> --connect <adresa>:<port>
```