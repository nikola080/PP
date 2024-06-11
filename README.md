Ovaj projekat je ralizovan kroz 4 faze:
Leksicka analiza - u ovoj fazi po po pravilima napisanim u .flex fajlu lakser/skener prolazi kroz source code i razgradjuje ga i pravi tokene.
Sintaksna analiza - u ovoj fazi po poravili napisanim u .cup fajlo parser prolazi kroz source code ali ovog puta citajuci tokene umesto simbola iz prosle faze i od njih gradi stablo izvodjenja na osnovu napisane gramatike.
  Stablo izvodjenja je izgradjeno od terminalnih (simboli) i neterminalsnih (skup simbola u nekom kontekstu) cvorova.
Semasticka analiza - u ovoj fazi kompajler obilazi citavo stablo top-down metodom po postorderu, i posecuje svaki cvor vrseci poslednje provere da mozda ne postoji semantickih gresaka u kodu.
Generator koda - u ovoj fazi kompajler ponovo obilazi citavo stablo izvodjenja i ovog puta se ne vrsi provera vec formira bajt kod koji se na kraju i izvrsava u virtuelnoj masini.

