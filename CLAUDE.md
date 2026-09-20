# minecraft-motorboat — instructions agents

Mod Fabric 1.21.1 (client + serveur, `required` des deux côtés) : bateau vanilla + moteur à
combustible de four, soute (réservoir + moteur + coffre 27), grande barque 6 places à coque maison,
3 paliers de moteur. Public, GPL-3.0. Pour le serveur `minecraft-server`
(spec d'origine : `MODS.md` §8 de ce dépôt-là). Docs `.md` = notes denses pour agents, sauf
`README.md` (humains).

## Carte
- `src/main/java/io/github/nistroy/motorboat/` — `Motorboat` registres + config + `motorTier(ItemStack)` ·
  `MotorboatEntity` entité 2 places (hérite `Boat`, porte le conteneur) · `BigMotorboatEntity` 6 places
  (hérite `MotorboatEntity`, `isBigHull()` vrai) · `MotorboatItem` pose sur l'eau, une fabrique de coque
  par item · `TooltipItem` item à infobulle d'une ligne (moteurs, coques) · `MotorboatMenu` réservoir +
  moteur + coffre · `Motor` réserve de carburant (pur) · `MotorTier` palier de moteur (pur) · `Thrust`
  maths de poussée (pur) · `SeatPlan` les six places de la grande barque (pur) · `MotorboatConfig` JSON.
- `src/client/java/.../client/` — `MotorboatClient` enregistrement · `MotorboatRenderer` coque
  vanilla (`BoatRenderer`) + bloc moteur, et repère commun `applyBoatPose` ·
  `BigMotorboatModel`/`BigMotorboatRenderer` grande coque, boîtes **traduites** de
  `tools/art/big_hull.bbmodel` (ne pas les retoucher à la main) · `MotorboatScreen` écran de la soute.
- `src/test/java/` — JUnit sur les classes sans Minecraft (`Motor`, `Thrust`, `MotorboatConfig`, `SeatPlan`).
- `tools/generate_textures.py` — génère les PNG (stdlib seule), textures d'entité et texture de GUI.
  Les modèles d'entité sont **lus** dans `tools/art/*.bbmodel` (boîtes + `uv_offset`) : retoucher une
  maquette puis relancer le script suffit, la texture recolle. Seules les coordonnées des slots du menu
  sont dupliquées ici. Ne touche **pas** aux sprites d'items (faits main).
- `tools/art/*.bbmodel` — maquettes Blockbench, **source des formes et des textures** :
  `big_hull` (grande coque), `motor` / `big_motor` (les deux hors-bord). Texture embarquée dans le
  fichier, donc il s'ouvre habillé. Le Java en est traduit (scripts de session, voir `PLAN.md`) :
  ne pas retoucher les boîtes dans le Java, retoucher la maquette.
- `tools/art/motorboat_items.png` — planche 80 × 16 de nistroy, source des 5 sprites d'items (ordre :
  `motor`, `big_motor`, `double_motor`, `motorboat`, `big_motorboat`). Hors `assets/` : tout PNG de
  `textures/item/` est cousu dans l'atlas des items. `tools/split_item_sheet.py` la redécoupe vers
  `assets/motorboat/textures/item/` — relancer après chaque retouche de la planche.

## Répartition client/serveur (ne pas se tromper)
- Vanilla : le **client du pilote** simule le bateau et envoie sa position ; seul `LocalPlayer`
  appelle `Boat.setInput`. → poussée appliquée côté client, dans `tick()`, avant `super.tick()`.
- Le serveur ne voit pas `setInput` mais a la saisie du pilote (`LivingEntity.zza`, via
  `ServerboundPlayerInputPacket`) → c'est lui qui consomme le carburant et fait autorité dessus
  (`SynchedEntityData`).
- Contrôle serveur `moved too quickly` : refus si distance² du paquet − vitesse² > 100
  (`ServerGamePacketListenerImpl.handleMoveVehicle`, vérifié au javap 1.21.1). 32 blocs/s = 1,6
  bloc/tick : toujours très en dessous.

## Moteurs (v0.3)
- 3 items : `motor` (BASIC), `big_motor` (BIG), `double_motor` (DOUBLE) ; `MotorTier.NONE` = slot vide
  → bateau à rames, rien consommé, pas de bloc moteur dessiné.
- `MotorTier.fitsHull(bigHull)` : la coque 2 places ne prend que BASIC (place sur le pont), la grande
  prend les trois. Appliqué par `MotorboatEntity.acceptsMotor` → `MotorSlot.mayPlace`.
- Vitesses (`MotorboatConfig.topSpeed(tier, bigHull)`) : 16 / 24 / 32 blocs/s × 0,85 sur la grande
  coque. Aucun plafond de config (choix nistroy 2026-09-20), seulement > 0 et facteur dans ]0, 1].
  Ancienne clé `topSpeedBlocksPerSecond` relue comme vitesse du moteur de base.
- Le palier vit dans le slot 28 du conteneur (pas de NBT à part) mais le contenu n'est envoyé au
  client que menu ouvert → il est aussi publié en `SynchedEntityData` (`DATA_MOTOR`) pour la poussée
  (simulée par le client du pilote) et le rendu.
- Coque craftée **sans** moteur (`fer bateau fer`, forme fixe demandée par nistroy 2026-09-20) :
  sinon casser la barque rendrait coque + moteur, soit un moteur gratuit par cycle.
- Rendu : BASIC = hors-bord de base, BIG = gros hors-bord (**son propre modèle**, pas une mise à
  l'échelle), DOUBLE = deux hors-bord de base à ±4 unités en travers (`MotorboatRenderer.renderEngine`).
  Le moteur pend **dehors**, accroché au tableau arrière : le modèle est dessiné autour de son point
  d'accrochage et chaque coque passe le sien (`Mount`) — barque 2 places `(-16, -3)`, grande coque
  `(-24, -8)`. Un seul modèle pour les deux barques.

## Conteneur et menu (relevés au javap, 1.21.1)
- `MenuType.<init>` est privé, l'AW de `fabric-screen-handler-api-v1` le rouvre (Loom l'applique) mais
  le client a besoin de l'id de l'entité → `ExtendedScreenHandlerType<MotorboatMenu, Integer>` +
  `ByteBufCodecs.VAR_INT`, entité implémentant `ExtendedScreenHandlerFactory<Integer>`.
- 29 slots : 0 = réservoir (`AbstractFurnaceBlockEntity.isFuel`), 1-27 = coffre, 28 = moteur (rangé
  en dernier pour ne pas décaler les barques déjà posées). Index de menu ≠ index de conteneur : le
  slot moteur est le 2ᵉ du menu, `quickMoveStack` utilise les constantes `MENU_*`. NBT, drops et
  `SlotAccess` viennent des `default` de `ContainerEntity` (comme `ChestBoat`), pas réécrits.
- `AbstractContainerMenu.moveItemStackTo` vérifie `mayPlace` et `Slot.getMaxStackSize(stack)` sur la
  branche « slot vide » (javap 1.21.1) → un shift-clic ne peut pas forcer un gros moteur sur la
  petite coque, et le slot moteur reste à 1 objet.
- Un combustible plus gros que la réserve (seau de lave, 20 000 ticks > 12 000) : `Motor.load` refuse
  (plein à la main), `Motor.autoLoad` écrête (soute) — sinon le slot se bloquerait. Contenant rendu
  comme dans un four.
- Attaches des passagers : `new Vec3(travers, hauteur, avant).yRot(-yRot)` — **Z vers la proue**,
  X en travers (vérifié au `javap -c` sur `Boat.getPassengerAttachmentPoint`).
- Repère du modèle après le rendu de bateau : +X = proue, **-Y = haut** (`scale(-1, -1, 1)`).

## Physique vanilla (relevée au javap, 1.21.1)
Ordre d'un tick de `Boat` : `floatBoat` (vitesse × `invFriction`) → `controlBoat` (+0,04 bloc/tick²
vers l'avant) → `move`. `invFriction` = 0.9 dans l'eau, 0.45 sous l'eau, 0.05 en l'air/sur terre.
Équilibre vanilla : 0,04 / (1 − 0,9) = 0,4 bloc/tick = 8 blocs/s. Poussée ajoutée avant la friction
→ v = (poussée × 0,9 + 0,04) / 0,1 (voir `Thrust`).

## Tests — TDD obligatoire
- Red-Green-Refactor : pas de comportement sans test rouge d'abord ; bug → test de régression d'abord.
- `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew build`
- Fini = build vert **et** démarrage propre de `./gradlew runServer` (`Done (` dans le log, aucune
  erreur de mod). Ce qui touche au rendu se vérifie dans `./gradlew runClient`.
- Jamais affaiblir/supprimer/skip un test pour passer au vert : le dire, demander.
- CI `.github/workflows/ci.yml` : `./gradlew build` sur chaque PR ; PR rouge jamais mergée.

## Règles
- Langue : identifiants EN ; docs, commentaires, textes joueurs FR ; commits/branches Conventional
  Commits EN.
- Git : jamais commit sur `main` ; branche `<type>/<sujet>` ; PR + merge via `gh`.
- Anti-invention : API Minecraft vérifiée au `javap` sur les jars Loom
  (`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-{common,clientonly}-1.21.1-*.jar`),
  jamais de mémoire. Modrinth/GitHub → `curl` (Python `urllib` SSL cassé sur le Mac de nistroy).
- Release : tag `vX.Y.Z` (= `version` de `gradle.properties`) → workflow release → jar attaché ; le
  pack packwiz du serveur pointe l'URL de release.
- Ajout au serveur = mod `required` des deux côtés → passe par le pack packwiz + le vote Discord,
  comme tout ajout de mod (`CLAUDE.md` de `minecraft-server`).

## Suite
- Garder le bois du bateau utilisé au craft (aujourd'hui : coque chêne quel que soit le bateau, et
  coque unique pour la grande barque).
- Rames visibles sur la grande barque : pas de modèle de rame (elle est à moteur), la rame marche
  quand même.
- **Jamais** : pont praticable en mouvement (écarté par nistroy — demanderait mixins client + physique).

## Ce que le test automatisé ne couvre pas
Vérifié par RCON sur `runServer` (voir `PLAN.md`) : enregistrement des entités et des items, NBT
`Fuel`/`Items`, slot réservoir, slot moteur (28), conso auto seulement moteur posé, écrêtage du seau
de lave. **Piège RCON** : sans joueur les entités ne tiquent pas dans les chunks de spawn →
`forceload add 0 0` avant toute mesure, sinon `Fuel` reste à 0 et on croit à un bug.
**Pas** vérifiable sans joueur humain : rendu des coques, des trois moteurs et du GUI, position des six
sièges, ouverture du menu au clic droit, refus du gros moteur sur la petite coque (passe par `mayPlace`),
vitesse réelle de chaque palier. `/ride ... mount` force le montage (court-circuite `canAddPassenger`) et la
position des passagers n'est pas observable en NBT sans client — la barque vanilla donne le même
relevé plat, c'est la mesure qui ne voit rien, pas le code.
