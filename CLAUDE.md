# minecraft-motorboat — instructions agents

Mod Fabric 1.21.1 (client + serveur, `required` des deux côtés) : bateau vanilla + moteur à
combustible de four, 2× la vitesse vanilla. Public, GPL-3.0. Pour le serveur `minecraft-server`
(spec d'origine : `MODS.md` §8 de ce dépôt-là). Docs `.md` = notes denses pour agents, sauf
`README.md` (humains).

## Carte
- `src/main/java/io/github/nistroy/motorboat/` — `Motorboat` registres + config · `MotorboatEntity`
  entité (hérite `Boat`) · `MotorboatItem` pose sur l'eau · `Motor` réserve de carburant (pur) ·
  `Thrust` maths de poussée (pur) · `MotorboatConfig` JSON.
- `src/client/java/.../client/` — `MotorboatClient` enregistrement · `MotorboatRenderer` coque
  vanilla (`BoatRenderer`) + bloc moteur.
- `src/test/java/` — JUnit sur les classes sans Minecraft (`Motor`, `Thrust`, `MotorboatConfig`).
- `tools/generate_textures.py` — génère les PNG (stdlib seule). Boîtes du modèle dupliquées ici :
  changer le modèle = changer le script.

## Répartition client/serveur (ne pas se tromper)
- Vanilla : le **client du pilote** simule le bateau et envoie sa position ; seul `LocalPlayer`
  appelle `Boat.setInput`. → poussée appliquée côté client, dans `tick()`, avant `super.tick()`.
- Le serveur ne voit pas `setInput` mais a la saisie du pilote (`LivingEntity.zza`, via
  `ServerboundPlayerInputPacket`) → c'est lui qui consomme le carburant et fait autorité dessus
  (`SynchedEntityData`).
- Contrôle serveur `moved too quickly` : refus si distance² du paquet − vitesse² > 100
  (`ServerGamePacketListenerImpl.handleMoveVehicle`, vérifié au javap 1.21.1). 16 blocs/s = 0,8
  bloc/tick : très en dessous.

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

## Suite (v0.2, décidé 2026-09-20)
- Barque plus grosse, plusieurs places (nistroy : « plusieurs personnes puissent monter dessus ») :
  coque custom allongée, `getMaxPassengers` + `getPassengerAttachmentPoint` à réécrire, hitbox à
  élargir. Hors périmètre v0.1.
- Garder le bois du bateau utilisé au craft (aujourd'hui : coque chêne quel que soit le bateau).
- **Jamais** : pont praticable en mouvement (écarté par nistroy — demanderait mixins client + physique).
