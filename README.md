# minecraft-motorboat

Une barque à moteur pour serveur **Fabric 1.21.1** : le bateau vanilla, plus un moteur qui brûle du
charbon et qui va deux fois plus vite tant qu'il a de quoi brûler. Avec sa soute pour le carburant,
et une grande version à six places.

Mod maison écrit pour le serveur entre copains — aucun mod de bateau existant en 1.21.1 ne faisait
l'affaire (voir `MODS.md` §4 et §8 du dépôt `minecraft-server`).

## En jeu

1. **Craft du moteur** : 4 lingots de fer, 2 lingots de cuivre, 1 four.

   ```
   . I .      I = lingot de fer
   C I C      C = lingot de cuivre
   I F I      F = four
   ```

2. **Craft de la barque** : le moteur + n'importe quel bateau (recette sans forme).
3. **Craft de la grande barque** : une barque à moteur entourée de 7 planches. Six places, trois rangs
   de deux, même moteur.

   ```
   P P      P = planches (n'importe lesquelles)
   PBP      B = barque à moteur
   PPP
   ```

4. **Le plein**, au choix :
   - **accroupi + clic droit** avec un combustible de four en main (charbon, bûches, blaze rod…) :
     plein immédiat, le message en bas de l'écran donne l'autonomie restante ;
   - **accroupi + clic droit à main vide** : la soute s'ouvre. Le slot en haut à gauche est le
     réservoir — le moteur y pioche tout seul dès qu'il est à sec, et la flamme à côté montre ce qu'il
     reste. Les 27 slots du dessous sont un coffre ordinaire, pratique en expédition.

   Réservoir plein : 10 minutes de marche, soit 7 charbons et demi. Un combustible plus gros que le
   réservoir (seau de lave) n'est pas gâché à moitié : il remplit à ras bord et rend son seau.

5. **Conduite** : comme un bateau. Tant qu'il reste du carburant et qu'on avance, le moteur pousse
   (16 blocs/s au lieu de 8), fume et fait des bulles. À sec, la barque redevient un bateau à rames.
   La grande barque va aussi vite que la petite, mais sa coque de 2,25 blocs passe mal dans les
   rivières étroites et sous les ponts bas.

Le carburant ne se consomme que quand on avance : une barque à l'arrêt ne brûle rien. Casser une
barque rend son contenu, comme un bateau à coffre.

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
  "topSpeedBlocksPerSecond": 16.0
}
```

Vitesse de pointe visée, en blocs par seconde, entre 8 (la rame vanilla) et 40. Une valeur hors
bornes ou un fichier illisible n'empêchent pas le démarrage : le mod garde les défauts et l'écrit
dans le log. La valeur compte côté client (c'est lui qui simule le bateau du pilote) comme côté
serveur, donc gardez le même fichier partout.

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
