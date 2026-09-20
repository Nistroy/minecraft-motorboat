# minecraft-motorboat

Une barque à moteur pour serveur **Fabric 1.21.1** : le bateau vanilla, plus un moteur qui brûle du
charbon et qui va deux fois plus vite tant qu'il a de quoi brûler. Avec sa soute pour le carburant,
une grande version à six places, et trois moteurs de plus en plus rapides.

Mod maison écrit pour le serveur entre copains — aucun mod de bateau existant en 1.21.1 ne faisait
l'affaire (voir `MODS.md` §4 et §8 du dépôt `minecraft-server`).

## En jeu

### Les moteurs

Trois moteurs, du plus lent au plus rapide. Le premier va sur les deux coques ; les deux autres sont
trop encombrants pour la barque 2 places et ne se posent que sur la grande.

1. **Moteur** : 4 lingots de fer, 2 lingots de cuivre, 1 four.

   ```
   . I .      I = lingot de fer
   C I C      C = lingot de cuivre
   I F I      F = four
   ```

2. **Gros moteur** : un moteur, 4 lingots de fer, 1 bloc de cuivre.

   ```
   . I .      I = lingot de fer
   I M I      M = moteur
   . C .      C = bloc de cuivre
   ```

3. **Double moteur** : deux gros moteurs accouplés sur un bloc de fer.

   ```
   G B G      G = gros moteur
              B = bloc de fer
   ```

### Les coques

1. **Barque à moteur** : n'importe quel bateau entre 2 lingots de fer. Deux places. Elle sort de la
   table **sans moteur** : c'est un bâti, le moteur se pose dedans.

   ```
   I B I      I = lingot de fer
              B = n'importe quel bateau
   ```
2. **Grande barque à moteur** : une barque à moteur entourée de 7 planches. Six places, trois rangs de
   deux.

   ```
   P P      P = planches (n'importe lesquelles)
   PBP      B = barque à moteur
   PPP
   ```

### La soute

**Accroupi + clic droit à main vide** : la soute s'ouvre.

- Slot en haut à gauche : le **réservoir**. Le moteur y pioche tout seul dès qu'il est à sec, et la
  flamme à côté montre ce qu'il reste. Réservoir plein : 10 minutes de marche, soit 7 charbons et
  demi. Un combustible plus gros que le réservoir (seau de lave) n'est pas gâché à moitié : il remplit
  à ras bord et rend son seau.
- Slot à droite de la flamme : le **moteur**. Vide, la barque est un bateau à rames ordinaire — elle
  ne consomme rien et n'avance qu'à la rame. La barque 2 places refuse le gros et le double moteur.
- Les 27 slots du dessous sont un coffre ordinaire, pratique en expédition.

**Accroupi + clic droit avec un combustible de four en main** (charbon, bûches, blaze rod…) : plein
immédiat sans ouvrir la soute, le message en bas de l'écran donne l'autonomie restante.

### Vitesses

La grande coque est plus lourde : à moteur égal elle va 15 % moins vite.

| Moteur        | Barque 2 places | Grande barque |
| ------------- | --------------- | ------------- |
| aucun         | 8 (à la rame)   | 8 (à la rame) |
| Moteur        | 16              | 13,6          |
| Gros moteur   | ne rentre pas   | 20,4          |
| Double moteur | ne rentre pas   | 27,2          |

En blocs par seconde ; le bateau vanilla à la rame fait 8. Tant qu'il reste du carburant et qu'on
avance, le moteur pousse, fume et fait des bulles. À sec, la barque redevient un bateau à rames.

Le carburant ne se consomme que quand on avance : une barque à l'arrêt ne brûle rien. Casser une
barque rend son contenu — moteur compris — comme un bateau à coffre. La grande barque passe mal dans
les rivières étroites et sous les ponts bas : sa coque fait 2,25 blocs.

## Installation

Mod **requis des deux côtés** : il doit être installé sur le serveur *et* chez chaque joueur (il crée
une entité et des objets ; un client sans le mod ne saurait pas les afficher).

- Serveur : poser le jar dans `mods/`, redémarrer.
- Joueurs : le modpack packwiz du serveur le distribue automatiquement.

Le jar de chaque version est attaché à la [release GitHub](https://github.com/Nistroy/minecraft-motorboat/releases)
correspondante.

## Config

Fichier `config/motorboat.json`, créé au premier lancement :

```json
{
  "basicMotorBlocksPerSecond": 16.0,
  "bigMotorBlocksPerSecond": 24.0,
  "doubleMotorBlocksPerSecond": 32.0,
  "bigHullSpeedFactor": 0.85
}
```

Une vitesse de pointe par moteur, en blocs par seconde, plus le facteur appliqué à la grande coque
(entre 0 et 1). N'importe quelle vitesse au-dessus de 0 est acceptée, il n'y a **pas de plafond** :
au-delà d'une trentaine de blocs/s on traverse les chunks plus vite qu'ils ne chargent, c'est le seul
garde-fou. Une valeur invalide ou un fichier illisible n'empêchent pas le démarrage : le mod garde les
défauts et l'écrit dans le log. Un fichier d'une version précédente (`topSpeedBlocksPerSecond`) est
relu comme la vitesse du moteur de base.

La valeur compte côté client (c'est lui qui simule le bateau du pilote) comme côté serveur, donc
gardez le même fichier partout.

## Développement

Java 21 obligatoire. Fabric Loom ; le wrapper Gradle est fourni, pas besoin de Gradle système.

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew build     # tests + jar
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew runServer # serveur de dev
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew runClient # client de dev
```

Le jar sort dans `build/libs/`. Les textures sont générées par `python3 tools/generate_textures.py`
(pas de dépendance à installer) : si vous changez les boîtes du modèle dans `MotorboatRenderer`,
changez les mêmes nombres dans le script et relancez-le.

## Licence

GPL-3.0-only.
