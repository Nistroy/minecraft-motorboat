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
- Coque : **maquette de départ fournie par l'agent** à la demande de nistroy (2026-09-20),
  `tools/art/big_hull.bbmodel` sur la branche `art/coque` — barque vanilla agrandie, à retoucher
  dans Blockbench (pas d'art fait par l'agent : formes seulement, aucune texture).
- Reste attendu de nistroy : le `.bbmodel` retouché (projet, pas un export) + PNG de texture de coque
  (`256 × 128` aujourd'hui). Transfert : branche `art/coque` (upload web GitHub) ou Discord.
- Pas de tag tant que la coque n'est pas là : sprites + coque sortent ensemble en `0.4.0`
  (choix nistroy 2026-09-20).

## Contraintes données à nistroy (ne pas les contredire)
- 1 unité Blockbench = 1 px Minecraft = 1/16 bloc ; modéliser **Y vers le haut**, l'agent gère l'inversion.
- Hitbox `sized(2.25F, 0.5625F)` (36 px, **carrée en X/Z**), **inchangée** : le modèle a le droit de
  déborder, vanilla le fait déjà (coque de 28 px dans une hitbox de 22, relevé au javap 1.21.1).
  Débord actuel 42 px = +17 %, vanilla +27 %. Largeur : rester ≤ 28 px, sinon on cogne les berges.
- Coque v0.3 (celle qu'on remplace) : 36 long × 28 large × 7 haut, fond 1 d'épaisseur ; texture `256 × 128`.
- Plan d'eau (masque) et six sièges : **en code**, nistroy ne les modélise pas. Bancs éventuels → aligner
  les sièges dessus.
- Bloc moteur refaisable aussi : 4×7×5 + échappement 2×4×2, texture `32 × 32`.
- Repère de la maquette : quille posée sur `y = 0`, **+X = proue**, Z en travers. Ne pas déplacer la
  coque en bloc, les chiffres de recâblage en dépendent.

## Maquette `tools/art/big_hull.bbmodel` (agent, 2026-09-20)
23 cubes, `box_uv` dans `256 × 128` (empreintes packées sans recouvrement, vérifié), **texture
embarquée en base64** → le fichier s'ouvre déjà habillé, rien à importer ; `relative_path` pointe
`src/main/resources/assets/motorboat/textures/entity/big_motorboat.png` pour le réenregistrement.
Encombrement **42 × 28 × 10** (x ±21, z ±14, y 0→10), rotations comprises. Allongée de 36 à 42 à la
demande de nistroy (2026-09-20) pour loger le poste de barre.
- Groupe `coque` : fond (+ 5 marches d'étrave), bordés 2 d'épaisseur × 6 de haut, tableau arrière,
  listons (débord 0,5 vers l'intérieur), `banquette_poupe`, pont avant, étrave, 3 bancs.
- `banquette_poupe` (x −21→−17, y 7→9, z ±11,5) : liston de poupe élargi et épaissi, **le pilote
  s'assoit dessus** (nistroy 2026-09-20, « comme quelqu'un qui se pose dessus pour manœuvrer »).
  Dépasse d'1 px les listons de bord, d'où une banquette qui se lit.
- Groupes `proue_bd`/`proue_td` : bordé + liston inclinés, pivot `(11, 1, ±13)`, rotation Y `±51,953°`,
  longueur 14,6 → pointe à `(20, ±1,5)`. **Une rotation par groupe** (format `modded_entity` : les cubes
  ne tournent pas seuls) → conversion directe en `PartPose.offsetAndRotation`.
- Pièces de fond/pont de l'étrave taillées à la largeur de leur **bord arrière** : le débord (≤ 3,0)
  reste noyé dans l'épaisseur du bordé incliné (2 / cos 51,953° = 3,25 mesurés en Z) donc invisible ;
  les tailler au bord avant laisserait un trou.
- Texture : `tools/generate_textures.py` → `big_hull_texture()` **lit le `.bbmodel`** (boîtes + `uv_offset`),
  une teinte par famille de nom (`fond`, `pont`, `borde`, `tableau`, `etrave`, `liston`, `banquette`,
  `banc`). Retoucher la forme dans Blockbench puis relancer le script = texture à jour. Peinte à la
  main un jour → supprimer `big_hull_texture()`, sinon elle écrase (piège déjà vu sur les sprites).
  Nom de pièce inconnu du tableau `HULL_TONES` = `KeyError` : ajouter la famille avant de relancer.

## Plan de sièges de la maquette (à recâbler, `BigMotorboatEntity`)
Six places, **pilote assis sur la banquette de poupe** (nistroy 2026-09-20).
`Boat.getControllingPassenger` = **premier passager** (javap 1.21.1) → le siège 0 doit être la barre,
le premier monté pilote.
| siège | appui | x maquette (px) | travers z (px) | dessus (y_bb) | entité : `along` / `across` / hauteur (blocs) |
|---|---|---|---|---|---|
| 0 (barre) | `banquette_poupe` | −19 | 0 | 9 | −1,1875 / 0 / **0,6875** |
| 1-2 | `banc_milieu_ar` | −4 | ±6,4 | 3 | −0,25 / ∓0,4 / 0,3125 |
| 3-4 | `banc_milieu_av` | +5 | ±6,4 | 3 | +0,3125 / ∓0,4 / 0,3125 |
| 5 | `banc_etrave` | +13,5 | 0 | 3 | +0,84375 / 0 / 0,3125 |
- `ROW_OFFSETS`/`SEAT_OFFSET` ne suffisent plus (rangs inégaux, places centrales, **hauteurs
  différentes**) → table de 6 triplets `(along, across, hauteur)`.
- `along` = x maquette / 16, `across` = z maquette / 16 ; hauteur = `0,375 + (dessus − 1) / 16 − 0,1875`
  (0,375 = translation du rendu, 0,1875 = enfoncement de l'assise, celui de vanilla).
- Attache = `new Vec3(across, hauteur, along).yRot(-yRot)` : **Z = proue côté entité**, donc
  `along` vient du **x** de la maquette.
- **Moteur reculé pour la grande coque seulement** : `translate(-2/16, 0, 0)` dans
  `BigMotorboatRenderer` avant `renderEngine` (boîte `x −15..−11` → `−17..−13`), calé pile devant la
  banquette. Ne pas toucher `MotorboatRenderer`, la barque 2 places garde son moteur où il est.
- Plan d'eau : à redimensionner en code (intérieur ≈ 30 × 24) ; il est rendu en `RenderType.waterMask`,
  donc ses UV ne sont pas échantillonnées — pas de zone à réserver dans l'atlas.

## Conversion maquette → modèle du mod (à faire au recâblage)
- `x_mod = x_bb`, `z_mod = z_bb`, **`y_mod = 1 − y_bb`** (le rendu monte en −Y).
- `scale(-1, -1, 1)` inverse le sens des rotations autour de Y → **`yRot_mod = −rot_Y_bb`**.
  À confirmer dans `runClient` : si l'étrave part du mauvais bord, c'est ce signe.
- `addBox(x, y, z, w, h, d)` prend le coin **minimal** : `y_mod` du coin = `1 − y_bb_max`.

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
- [x] 3a. Texture de coque générée depuis le `.bbmodel` (`big_hull_texture()` réécrit, table
      `BIG_HULL_BOXES` supprimée), `256 × 128` inchangé. À refaire à la main plus tard si nistroy veut.
- [ ] 5. Sièges : table de 6 places ci-dessus (§Plan de sièges), décalage du moteur, assise remontée.
      `sized()` inchangé.
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

## Backlog (sorti du périmètre 0.4.0)
- **Choisir sa place** (question nistroy 2026-09-20) : faisable mais pas gratuit — vanilla attribue le
  siège par ordre de montée (`getPassengers().indexOf`). Il faut un plan de sièges stocké et
  **synchronisé** (le client du pilote calcule les attaches), le choix du siège libre le plus proche
  dans `interact`, et redéfinir `getControllingPassenger` pour que ce soit l'occupant de la barre qui
  pilote. À part, après la coque.
- **Trois vraies formes de moteur** : aujourd'hui un seul modèle 4×7×5 pour les trois paliers (gros =
  ×1,35, double = deux copies). Demanderait de l'art en plus.

## Reste en attente (hors art)
- Supprimer la branche `test/motorboat-0-3-0` (dépôt serveur) et la pré-version `test-0.3.0-rc1` quand
  nistroy confirme que son hook Prism est revenu sur `main`.
- Garder le bois du bateau utilisé au craft (aujourd'hui : coque chêne quel que soit le bateau).
- **Jamais** : pont praticable en mouvement (écarté par nistroy).

## Vérifications
- `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew build`
- Rendu : `./gradlew runClient` — obligatoire pour toute étape qui touche au modèle ou aux textures.
