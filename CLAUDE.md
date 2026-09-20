# minecraft-motorboat — instructions agents

Mod Fabric 1.21.1 (client + serveur, `required` des deux côtés) : bateau vanilla + moteur à
combustible de four, 2× la vitesse vanilla, soute (réservoir + coffre 27), et une grande barque
6 places à coque maison. Public, GPL-3.0. Pour le serveur `minecraft-server`
(spec d'origine : `MODS.md` §8 de ce dépôt-là). Docs `.md` = notes denses pour agents, sauf
`README.md` (humains).

## Carte
- `src/main/java/io/github/nistroy/motorboat/` — `Motorboat` registres + config · `MotorboatEntity`
  entité 2 places (hérite `Boat`, porte le conteneur) · `BigMotorboatEntity` 6 places (hérite
  `MotorboatEntity`) · `MotorboatItem` pose sur l'eau, une fabrique de coque par item ·
  `MotorboatMenu` réservoir + coffre · `Motor` réserve de carburant (pur) · `Thrust` maths de poussée
  (pur) · `MotorboatConfig` JSON.
- `src/client/java/.../client/` — `MotorboatClient` enregistrement · `MotorboatRenderer` coque
  vanilla (`BoatRenderer`) + bloc moteur, et repère commun `applyBoatPose` ·
  `BigMotorboatModel`/`BigMotorboatRenderer` grande coque maison · `MotorboatScreen` écran de la soute.
- `src/test/java/` — JUnit sur les classes sans Minecraft (`Motor`, `Thrust`, `MotorboatConfig`).
- `tools/generate_textures.py` — génère les PNG (stdlib seule), textures d'entité, sprites d'items et
  texture de GUI. Boîtes des modèles et coordonnées des slots dupliquées ici : changer le modèle ou la
  disposition du menu = changer le script.

## Répartition client/serveur (ne pas se tromper)
- Vanilla : le **client du pilote** simule le bateau et envoie sa position ; seul `LocalPlayer`
  appelle `Boat.setInput`. → poussée appliquée côté client, dans `tick()`, avant `super.tick()`.
- Le serveur ne voit pas `setInput` mais a la saisie du pilote (`LivingEntity.zza`, via
  `ServerboundPlayerInputPacket`) → c'est lui qui consomme le carburant et fait autorité dessus
  (`SynchedEntityData`).
- Contrôle serveur `moved too quickly` : refus si distance² du paquet − vitesse² > 100
  (`ServerGamePacketListenerImpl.handleMoveVehicle`, vérifié au javap 1.21.1). 16 blocs/s = 0,8
  bloc/tick : très en dessous.

## Conteneur et menu (relevés au javap, 1.21.1)
- `MenuType.<init>` est privé, l'AW de `fabric-screen-handler-api-v1` le rouvre (Loom l'applique) mais
  le client a besoin de l'id de l'entité → `ExtendedScreenHandlerType<MotorboatMenu, Integer>` +
  `ByteBufCodecs.VAR_INT`, entité implémentant `ExtendedScreenHandlerFactory<Integer>`.
- 28 slots : 0 = réservoir (`AbstractFurnaceBlockEntity.isFuel`), 1-27 = coffre. NBT, drops et
  `SlotAccess` viennent des `default` de `ContainerEntity` (comme `ChestBoat`), pas réécrits.
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
Vérifié en v0.2 par RCON sur `runServer` (voir `PLAN.md`) : enregistrement des entités, NBT
`Fuel`/`Items`, slot réservoir, conso auto (8 charbons → 7, réserve 1600), écrêtage du seau de lave.
**Pas** vérifiable sans joueur humain : rendu des coques et du GUI, position des six sièges, ouverture
du menu au clic droit. `/ride ... mount` force le montage (court-circuite `canAddPassenger`) et la
position des passagers n'est pas observable en NBT sans client — la barque vanilla donne le même
relevé plat, c'est la mesure qui ne voit rien, pas le code.
