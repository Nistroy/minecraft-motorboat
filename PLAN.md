# PLAN — v0.4 : coque 3D et sprites faits main (état de reprise)

Fichier de reprise : session qui repart lit **ça** puis `CLAUDE.md`. Mettre à jour à chaque étape verte.

## État au 2026-09-20
- **Déploiement `0.3.1` → `0.6.0`** demandé par nistroy 2026-09-20 : coque + moteurs + houle en une
  seule mise à jour pour les joueurs, comme voulu. Gestes en §Étapes 8.
- `v0.4.0` (release seule) : coque modélisée, six places, sprites d'items faits main.
- `v0.5.0` : hors-bord accrochés au tableau sur les deux barques (un modèle, un accrochage par coque),
  gros moteur avec son propre modèle, coque allongée 42 → 48 px pour que les six places gardent
  l'écart de vanilla, hitbox laissée à 2,25.
- Contenu v0.3.x (ce qui tourne en ligne) : soute (réservoir + slot moteur + coffre 27), grande barque
  6 places, 3 moteurs (16 / 24 / 32 blocs/s, grande coque ×0,85), recettes à forme fixe, config sans
  plafond de vitesse.
- **Au déploiement, ne pas oublier** : `MODS.md` du dépôt serveur (version du mod) et prévenir les
  joueurs — client et serveur doivent avoir la même version, sinon les objets se mélangent (§Pièges).
- **`v0.6.0` : houle** (demande nistroy 2026-09-20), PR #12 mergée, release + jar publiés. Déployée
  serveur + pack avec la 0.4/0.5 : le saut du live est `0.3.1` → `0.6.0`. Détails §Houle ; amplitudes
  encore à valider à l'œil en jeu.

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
- Hitbox `sized(2.25F, 0.5625F)` (36 px) — **ne pas l'agrandir pour allonger le bateau**. Une hitbox
  d'entité est **toujours carrée en X/Z** : `EntityDimensions` n'a que `width` et `height` (javap
  1.21.1), et l'AABB ne tourne pas avec le lacet. Monter à 2,5 pour la longueur élargit donc aussi,
  et c'est la largeur qui coince dans les rivières → essayé puis annulé (nistroy 2026-09-20).
  Le modèle déborde à la place : 48 px pour 36 = +33 %, vanilla +27 % (coque de 28 dans 22).
  Conséquence acceptée : la barre est à 1,375 du centre, soit 0,25 hors de la boîte.
- Largeur : rester ≤ 28 px, sinon on cogne les berges.
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
Encombrement **48 × 28 × 10** (x ±24, z ±14, y 0→10), rotations comprises. Allongée deux fois à la demande de nistroy
(2026-09-20) : 36 → 42 pour loger le poste de barre, puis 42 → 48 pour que les six places retrouvent
l'écart de vanilla.
- Groupe `coque` : fond (+ 5 marches d'étrave), bordés 2 d'épaisseur × 6 de haut, tableau arrière,
  listons (débord 0,5 vers l'intérieur), `banquette_poupe`, pont avant, étrave, 3 bancs.
- `banquette_poupe` (x −24→−20, y 7→9, z ±11,5) : liston de poupe élargi et épaissi, **le pilote
  s'assoit dessus** (nistroy 2026-09-20, « comme quelqu'un qui se pose dessus pour manœuvrer »).
  Dépasse d'1 px les listons de bord, d'où une banquette qui se lit.
- Groupes `proue_bd`/`proue_td` : bordé + liston inclinés, pivot `(14, 1, ±13)`, rotation Y `±51,953°`,
  longueur 14,6 → pointe à `(23, ±1,5)`. **Une rotation par groupe** (format `modded_entity` : les cubes
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
| 0 (barre) | `banquette_poupe` | −22 | 0 | 9 | −1,375 / 0 / **0,6875** |
| 1-2 | `banc_milieu_ar` | −10 | ±6,4 | 3 | −0,625 / ∓0,4 / 0,3125 |
| 3-4 | `banc_milieu_av` | +2 | ±6,4 | 3 | +0,125 / ∓0,4 / 0,3125 |
| 5 | `banc_etrave` | +13 | 0 | 3 | +0,8125 / 0 / 0,3125 |
- `ROW_OFFSETS`/`SEAT_OFFSET` ne suffisent plus (rangs inégaux, places centrales, **hauteurs
  différentes**) → table de 6 triplets `(along, across, hauteur)`.
- `along` = x maquette / 16, `across` = z maquette / 16 ; hauteur = `0,375 + (dessus − 1) / 16 − 0,1875`
  (0,375 = translation du rendu, 0,1875 = enfoncement de l'assise, celui de vanilla).
- Attache = `new Vec3(across, hauteur, along).yRot(-yRot)` : **Z = proue côté entité**, donc
  `along` vient du **x** de la maquette.
- **Moteur reculé pour la grande coque seulement** : `translate(-2/16, 0, 0)` dans
  `BigMotorboatRenderer` avant `renderEngine` (boîte `x −15..−11` → `−17..−13`), calé pile devant la
  banquette. Ne pas toucher `MotorboatRenderer`, la barque 2 places garde son moteur où il est.
- **Écart des rangs : 0,75 bloc** (0,69 entre les deux derniers). Référence : vanilla espace ses deux
  places de **0,8** (offsets `0.2` et `-0.6`, javap 1.21.1). En dessous de ça, les jambes de chacun
  traversent le dos du précédent — c'est ce qui a fait rallonger la coque (six places à 0,56-0,69
  dans 42 px, jugé trop serré sur capture 2026-09-20).
- Plan d'eau : 36 × 24 (intérieur x −22..14, z ±12) ; il est rendu en `RenderType.waterMask`,
  donc ses UV ne sont pas échantillonnées — pas de zone à réserver dans l'atlas.

## Conversion maquette → modèle du mod (à faire au recâblage)
- `x_mod = x_bb`, `z_mod = z_bb`, **`y_mod = 1 − y_bb`** (le rendu monte en −Y).
- **`yRot_mod = +rot_Y_bb`**, même signe (vérifié en jeu 2026-09-20). `scale(-1, -1, 1)` a un
  déterminant de +1 : c'est une rotation de 180° autour de Z, **pas un miroir**, elle ne retourne
  pas le sens des rotations. L'avoir inversé ouvrait les deux panneaux d'étrave en ailes au lieu de
  les fermer en pointe.
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
- [x] 2. `.bbmodel` → `BigMotorboatModel.createBodyModel()` : 21 pièces traduites par script
      (scratchpad, `bbmodel_to_java.py`), plan d'eau ramené à 30 × 24. Piège rencontré : les cubes
      hors groupe ont `PartPose.ZERO` alors que la quille est en `y_mod = 1` → poser `py = 1` dans la
      conversion, sinon tout descend d'1 px.
- [x] 3a. Texture de coque générée depuis le `.bbmodel` (`big_hull_texture()` réécrit, table
      `BIG_HULL_BOXES` supprimée), `256 × 128` inchangé. À refaire à la main plus tard si nistroy veut.
- [x] 5. Sièges : classe pure `SeatPlan` (6 places, testée : symétrie, barre la plus à l'arrière et
      la plus haute, index écrêté), `BigMotorboatEntity` y délègue ; moteur reculé de 2 px dans
      `BigMotorboatRenderer` (`ENGINE_OFFSET`), ombre portée 1,25. `sized()` inchangé.
- [ ] 6. `./gradlew build` vert (fait, tests compris) **et** `./gradlew runClient`.
      1ère passe 2026-09-20 : physique OK, texture OK, aucune texture manquante au log ; **étrave en
      ailes** → signe de rotation corrigé (§Conversion), à revérifier. Reste à regarder : assise du
      pilote sur la banquette, moteur devant lui (il faut **poser un moteur dans la soute**, sans
      moteur rien n'est dessiné, c'est voulu), pas de trou à l'étrave.
- [x] 7. `0.4.0` : PR #8 mergée, tag `v0.4.0`, release + jar publiés 2026-09-20.
- [ ] 8. Déploiement — **reporté à la 0.5.0, avec les moteurs** (nistroy 2026-09-20). Rappel des gestes,
      **les deux ensemble** : `server/mods/` du dépôt serveur + `pack/mods/motorboat.pw.toml`
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

## Hors-bord (`tools/art/motor.bbmodel`, `big_motor.bbmodel`)
Repère local : **origine = point d'accrochage** (arête haute du tableau arrière, face extérieure),
+X vers la proue, Y vers le haut. Conversion : `y_mod = -y_local`, x et z inchangés ; la coque
translate ensuite jusqu'à son accrochage, d'où un seul modèle pour les deux barques.
- Accrochages (`MotorboatRenderer.Mount`) : barque 2 places `(-16, -3)`, grande coque `(-24, -8)`.
  Le `(-16, -3)` vient du modèle vanilla relevé au javap 1.21.1 : tableau arrière `x -16..-14`,
  parois `y -3..3`, plancher `y 3..6`.
- Pièces : chape, capot, échappement(s), arbre, embase, hélice (deux pales croisées), barre franche.
  Arbre jusqu'à `y_local -11` → 2 px sous le fond de coque sur les deux barques, embase et hélice
  sous la flottaison.
- Tailles entières partout : le gros moteur a **son propre modèle** (capot 7×8×7, deux échappements)
  au lieu de l'ancien ×1,35 qui sortait de la grille de pixels.
- Double = deux hors-bord de base à ±4 en Z (capot de 6 de large → 2 px d'écart).
- Textures `64 × 64` générées par `engine_textures()` depuis les maquettes, teintes par famille de nom
  (`ENGINE_TONES`) ; bandeau de cuivre sur les flancs du capot, seule touche de couleur.

## Houle (`Wave` + `MotorboatRenderer.applyWave`, 2026-09-21)
Étrave qui se lève de temps en temps puis se repose, nez qui se lève en vitesse. **Purement visuel** :
rien côté entité, rien à synchroniser, serveur pas concerné.
- **La houle vient par groupes** (correction 2026-09-21, `v0.6.1`). 1re version rejetée par nistroy en
  jeu : « ça ne fait que trembler ». Cause : deux trains de 47 et 29 ticks **plus** une phase qui
  avance avec la position → une crête toutes les ~5 ticks à pleine vitesse, et une oscillation
  permanente de 1,5 s à l'arrêt. Une houle permanente se lit comme une vibration, pas comme la mer.
- Modèle actuel : **une** vague longue (130 ticks, 80 blocs) × une **enveloppe de groupes** =
  produit de deux battements lents (430 et 670 ticks, directions croisées) passé au smoothstep
  `[0,30 ; 0,85]`. Mesuré sur la classe compilée : **eau plate ~72 % du temps**, séries de 5-10 s,
  pointe 1,6° à l'arrêt / 4,0° à pleine vitesse. Plus on va vite, plus on croise de vagues.
- Amplitude montante avec la vitesse (1,6° → 4,0° à 12 blocs/s) ; **assiette de déjaugeage** en plus,
  0 → 3,0° en `f(2−f)` sur 0 → 20 blocs/s. Pilonnement ±0,07 bloc. Tangage max 7°.
- Petites amplitudes **obligatoires** : les attaches de `SeatPlan` ignorent le tangage, les passagers
  restent debout → 0,08 bloc d'écart au pire au banc d'étrave. Hitbox jamais inclinée (AABB).
- **Fondu à l'eau, pas un interrupteur** (`afloat()`) : `isInWater()` suit un drapeau qui bascule d'un
  tick à l'autre sur une coque qui flotte au ras de la surface ; allumer/éteindre la houle 10 fois
  par seconde se verrait comme un tremblement. On fond sur `getFluidHeight(WATER)` / 0,125 bloc.
- Pose : `applyWave` se pose dans le repère **du monde**, à l'origine de l'entité (pivot à la
  flottaison) ; elle entre dans le repère coque par le lacet et en ressort. Tangage `Axis.XP` positif
  = proue qui lève, roulis `Axis.ZP` — vérifié hors jeu sous JOML : proue +0,078 / poupe −0,079 bloc
  à +3°, identique à tous les lacets.
- Appelée **deux fois par rendu** pour la barque 2 places : sa coque est dessinée par `BoatRenderer`
  (donc `super.render` enveloppé), le moteur passe par `applyBoatPose`. La grande coque n'a rien de
  spécial. Effet de bord accepté : la plaque de nom de la barque 2 places tangue avec la coque.
- Vitesse mesurée sur `getX() − xOld`, pas `getDeltaMovement()` : nul sur les barques des autres
  joueurs, que le client interpole. Position lue interpolée (`getX(partialTicks)`).
- **Si un scintillement subsiste** : suspect suivant = le masque d'eau (`waterPatch`), qui s'incline
  avec la coque et peut battre contre la surface plate. Correctif possible : le dessiner hors houle.

## Backlog
- **Choisir sa place** (question nistroy 2026-09-20) : faisable mais pas gratuit — vanilla attribue le
  siège par ordre de montée (`getPassengers().indexOf`). Il faut un plan de sièges stocké et
  **synchronisé** (le client du pilote calcule les attaches), le choix du siège libre le plus proche
  dans `interact`, et redéfinir `getControllingPassenger` pour que ce soit l'occupant de la barre qui
  pilote. À part, après la coque.
- (vidé : les modèles de moteur sont passés en chantier courant, voir §Hors-bord)

## Reste en attente (hors art)
- Supprimer la branche `test/motorboat-0-3-0` (dépôt serveur) et la pré-version `test-0.3.0-rc1` quand
  nistroy confirme que son hook Prism est revenu sur `main`.
- Garder le bois du bateau utilisé au craft (aujourd'hui : coque chêne quel que soit le bateau).
- **Jamais** : pont praticable en mouvement (écarté par nistroy).

## Vérifications
- `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew build`
- Rendu : `./gradlew runClient` — obligatoire pour toute étape qui touche au modèle ou aux textures.
