# PLAN — v0.3 (état de l'implémentation)

Fichier de reprise : une session qui repart lit **ça** en premier, puis `CLAUDE.md`.
Branche `feat/motor-tiers`. Mettre à jour les cases à chaque étape verte.

## Demande (validée nistroy 2026-09-20)
Paliers de moteur, par ordre de vitesse. Réponses aux questions posées :
1. Montage : **slot moteur dans la soute** (pas d'échange au clic droit, pas d'item de barque par moteur).
2. Vitesses : 16 / 24 / 32 blocs/s, grande coque −15 % — **mais la barque 2 places n'accepte que le
   moteur basique**.
3. Plafond de vitesse de la config : **supprimé**.

Conséquence assumée : coque craftée sans moteur (bateau + 2 fer), sinon casser la barque rendrait
coque + moteur = moteur gratuit à chaque cycle.

## Fait
- [x] `MotorTier` (pur, TDD) : NONE/BASIC/BIG/DOUBLE, `fitsHull`, `byId`.
- [x] `MotorboatConfig` : 4 clés, pas de plafond, reprise de l'ancienne `topSpeedBlocksPerSecond`.
- [x] Slot moteur (conteneur 29, slot 28), `DATA_MOTOR` synchronisé, poussée et conso par palier.
- [x] `MotorSlot` dans le menu (refuse ce qui ne tient pas sur la coque, 1 objet max), `quickMoveStack`.
- [x] Items `big_motor` / `double_motor` + `TooltipItem`, sprites, modèles, lang FR/EN, recettes.
- [x] Recette de coque sans moteur (bateau + 2 lingots de fer).
- [x] Rendu par palier : rien / bloc / bloc × 1,35 / deux blocs.
- [x] Docs (`README.md`, `CLAUDE.md`), version `0.3.0`.

## Reste
- [ ] **Test en jeu par nistroy** (v0.2 jamais taguée : son test reste à faire aussi) : rendu des trois
      moteurs, refus du gros moteur sur la petite coque, vitesse de chaque palier, six places, soute.
- [ ] Après feu vert : tag `v0.3.0` → release → `pack/mods/motorboat.pw.toml` du dépôt
      `minecraft-server` (touche les joueurs : demander avant).

## Vérifié le 2026-09-20 (RCON sur `runServer`)
Démarrage propre (`Done (`), 1295 recettes chargées, `summon` des 2 entités, items `big_motor` /
`double_motor` existants, `data merge` du slot 28, conso auto **seulement** moteur posé (2 charbons → 1,
réserve 0 → 1600 ; sans moteur : réserve 0, charbons intacts), config par défaut réécrite aux 4 clés.
Piège : sans joueur les entités ne tiquent pas → `forceload add 0 0` avant toute mesure.

## Vérifications
- `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew build`
- Rendu et menus : `./gradlew runClient` (obligatoire pour le slot moteur et les trois blocs moteur).
