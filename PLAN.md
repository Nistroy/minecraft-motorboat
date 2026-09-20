# PLAN — v0.2 (état de l'implémentation)

Fichier de reprise : une session qui repart lit **ça** en premier, puis `CLAUDE.md`.
Branche `feat/fuel-container-big-boat`. Mettre à jour les cases à chaque étape verte.

## Demande (validée nistroy 2026-09-20)
Acquis, on n'y touche pas : item moteur + sprite, barque 2 places + rendu (coque vanilla + bloc moteur).
1. **Interface carburant** sur les **deux** barques : coffre 27 slots **+** slot carburant dédié,
   menu custom (texture de GUI à générer). Le moteur pioche tout seul dans le slot carburant.
   Le plein à la main actuel (accroupi + clic droit avec combustible) **reste**.
2. **Grande barque à moteur**, **6 places** (3 rangs de 2), **entité + item en plus** (l'actuelle reste
   craftable). Coque custom ~2 blocs de large × ~2,5 de long, même moteur à la poupe.
3. Textures : `tools/generate_textures.py` amélioré (aucun asset Mojang copié — dépôt public GPL-3.0).

Ouverture du menu : accroupi + clic droit **à main vide** (accroupi + combustible = plein direct, inchangé).

## Décisions techniques (vérifiées au javap, 1.21.1)
- `MenuType.<init>` est **privé** et l'AW de `fabric-screen-handler-api-v1` n'est **pas** transitive
  (lignes `accessible`, pas `transitive-accessible`) → menu enregistré via
  `net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType<T, D>(factory, StreamCodec)`,
  données = id de l'entité (`ByteBufCodecs.VAR_INT`). Côté serveur : `ExtendedScreenHandlerFactory<Integer>`.
- Réserve de carburant déjà synchronisée par `SynchedEntityData` → **pas** besoin de `ContainerData`
  dans le menu, le client lit `MotorboatEntity.fuel()` via l'entité (id transmis à l'ouverture).
- `ChestBoat` implémente `HasCustomInventoryScreen` + `ContainerEntity` ; les `default` de
  `ContainerEntity` (`addChestVehicleSaveData`, `chestVehicleDestroyed`, `getChestVehicleSlot`…)
  gèrent NBT, drops et `SlotAccess` — s'en servir plutôt que réécrire.
- Combustible > réserve max (seau de lave = 20 000 ticks > 12 000) : `Motor.load` **refuse**.
  Le remplissage auto doit donc écrêter (`Motor.autoLoad`) sinon un seau de lave bloque le slot.
  Rendu du contenant (seau vide) à gérer comme un four.
- Grande barque : `getMaxPassengers()`, `getPassengerAttachmentPoint(Entity, EntityDimensions, float)`,
  `sized()` à revoir. Lire l'implémentation vanilla au `javap -c -p` avant d'écrire.

## Étapes (chacune = build vert + commit)
- [x] 0. Branche + PLAN.md.
- [ ] 1. `Motor.autoLoad` (TDD : test rouge d'abord) + conso auto depuis le slot carburant.
- [ ] 2. Conteneur sur `MotorboatEntity` (28 slots : 0 = carburant, 1-27 = coffre), NBT, drops.
- [ ] 3. `MotorboatMenu` + enregistrement `ExtendedScreenHandlerType`, ouverture au clic droit accroupi
      main vide, `quickMoveStack`.
- [ ] 4. `MotorboatScreen` + texture de GUI générée (176×190, jauge de flamme), lang FR/EN.
- [ ] 5. Grande barque : entité 6 places, hitbox, points d'attache, item, recette, lang.
- [ ] 6. Modèle + rendu de la grande coque (mesh custom + texture générée) + moteur à la poupe.
- [ ] 7. Docs (`README.md`, `CLAUDE.md` §Suite), version `0.2.0`, PR, merge, tag, pack packwiz du serveur.

## Vérifications
- `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew build`
- Rendu et menus : `./gradlew runClient` (obligatoire pour les étapes 4 et 6).
