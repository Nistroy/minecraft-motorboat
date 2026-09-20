# PLAN — v0.4 : coque 3D et sprites faits main (état de reprise)

Fichier de reprise : session qui repart lit **ça** puis `CLAUDE.md`. Mettre à jour à chaque étape verte.

## État au 2026-09-20
- `v0.3.1` publiée, **déployée partout** : `server/mods/` du dépôt `minecraft-server` + pack packwiz `main`
  (`pack/mods/motorboat.pw.toml`). Test en jeu par nistroy : OK (« tout marche super bien »).
- Contenu v0.3.x : soute (réservoir + slot moteur + coffre 27), grande barque 6 places, 3 moteurs
  (`motor` 16 / `big_motor` 24 / `double_motor` 32 blocs/s, grande coque ×0,85), recettes à forme fixe,
  config sans plafond de vitesse.

## Demande (nistroy 2026-09-20)
Coque de la grande barque jugée trop « radeau » (caisse : fond plat + 4 parois droites). **nistroy
modélise dans Blockbench et refait les sprites d'items ; l'agent recâble, ne crée pas l'art.**

- Sprites d'items : **livrés et en place** 2026-09-20 (branche `feat/item-sprites`), planche
  `tools/art/motorboat_items.png`.
- Reste attendu de nistroy : `.bbmodel` (projet, pas un export) + PNG de texture de coque
  (`256 × 128` aujourd'hui). Transfert : branche `art/coque` (upload web GitHub) ou Discord.
- Pas de tag tant que la coque n'est pas là : sprites + coque sortent ensemble en `0.4.0`
  (choix nistroy 2026-09-20).

## Contraintes données à nistroy (ne pas les contredire)
- 1 unité Blockbench = 1 px Minecraft = 1/16 bloc ; modéliser **Y vers le haut**, l'agent gère l'inversion.
- Encombrement ≤ 36 × 36 unités au sol (hitbox `sized(2.25F, 0.5625F)`, **carrée en X/Z**) ; hauteur libre.
- Coque actuelle : 36 long × 28 large × 7 haut, fond 1 d'épaisseur ; texture `256 × 128`.
- Plan d'eau (masque) et six sièges : **en code**, nistroy ne les modélise pas. Bancs éventuels → aligner
  les sièges dessus.
- Bloc moteur refaisable aussi : 4×7×5 + échappement 2×4×2, texture `32 × 32`.

## Repères techniques (relevés au javap 1.21.1, ne pas re-deviner)
- Repère après `MotorboatRenderer.applyBoatPose` : **+X = proue**, **−Y = haut** (`scale(-1, -1, 1)`),
  d'où les parois actuelles de `y = -6` à `y = 0`.
- Sièges : `BigMotorboatEntity.ROW_OFFSETS = {0.7, 0.0, -0.7}` (Z, proue = +Z dans
  `getPassengerAttachmentPoint`), `SEAT_OFFSET = 0.4` (X en travers). Attache =
  `new Vec3(travers, hauteur, avant).yRot(-yRot)`.
- Rendu du moteur par palier (`MotorboatRenderer.renderEngine`) : BASIC = bloc, BIG = ×1,35 autour de
  `(-11, 1)`, DOUBLE = deux blocs à `±3` en Z. Recalculer ces nombres si le bloc moteur change.

## Étapes
- [x] 1. Sprites d'items reçus (planche 80 × 16, 5 × 16 × 16, palette de `tools/motorboat_sprites.lua`).
- [x] 3b. Sprites découpés vers `assets/motorboat/textures/item/` par `tools/split_item_sheet.py`
      (pixels identiques à la planche, vérifié) ; planche gardée hors de `assets/`.
- [x] 4. Fonctions de sprites d'items retirées de `tools/generate_textures.py` (il les écrasait),
      en-tête + carte de `CLAUDE.md` à jour.
- [ ] 2. `.bbmodel` → `BigMotorboatModel.createBodyModel()` : cubes `from`/`to`/`origin`/`rotation`/`uv`,
      attention `inflate` et pivots ; convertir Y-up Blockbench vers le repère du mod.
- [ ] 3a. Texture de coque en place (`assets/motorboat/textures/entity/`) ; ajuster
      `TEXTURE_WIDTH`/`TEXTURE_HEIGHT` si nistroy a changé la taille, et retirer `big_hull_texture()`
      de `tools/generate_textures.py`.
- [ ] 5. Sièges et `sized()` réajustés si les proportions changent.
- [ ] 6. `./gradlew build` vert **et** `./gradlew runClient` (le rendu ne se vérifie pas autrement).
- [ ] 7. Version `0.4.0`, PR, merge, tag `v0.4.0` → release (workflow : tag = `version` de
      `gradle.properties`, sinon il échoue).
- [ ] 8. Déploiement, **les deux ensemble** : `server/mods/` du dépôt serveur + `pack/mods/motorboat.pw.toml`
      (url + sha256 de la release, puis `~/go/bin/packwiz refresh`). Avant : sauvegarde hors rotation
      (`./mc cmd save-off` → `save-all flush` → `./mc backup` → `save-on`, puis renommer `pre-<change>_…`).
      Serveur : `./mc stop`/`start` sans demander si 0 joueur.

## Pièges vérifiés en vrai
- **Client et serveur doivent avoir la même version** : client 0.3.0 sur serveur 0.1.1 → Fabric remappe les
  registres, la connexion passe mais les items pris en créatif deviennent ceux d'autres mods
  (`item.universal_graves.icon`), crafts des nouveaux items KO, menus custom muets (2026-09-20).
- RCON sur `runServer` : sans joueur, les entités ne tiquent pas dans les chunks de spawn →
  `forceload add 0 0` avant toute mesure, sinon `Fuel` reste à 0 et on croit à un bug.
- Dépôt `minecraft-server` : checkout live, **jamais** changer sa branche → worktree + `merge --ff-only`.

## Reste en attente (hors art)
- Supprimer la branche `test/motorboat-0-3-0` (dépôt serveur) et la pré-version `test-0.3.0-rc1` quand
  nistroy confirme que son hook Prism est revenu sur `main`.
- Garder le bois du bateau utilisé au craft (aujourd'hui : coque chêne quel que soit le bateau).
- **Jamais** : pont praticable en mouvement (écarté par nistroy).

## Vérifications
- `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew build`
- Rendu : `./gradlew runClient` — obligatoire pour toute étape qui touche au modèle ou aux textures.
