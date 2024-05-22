    package flashtanki.server.battles.weapons

    import flashtanki.server.battles.*
    import flashtanki.server.battles.mode.DeathmatchModeHandler
    import flashtanki.server.client.ChangeTankSpecificationData
    import flashtanki.server.client.send
    import flashtanki.server.client.toJson
    import flashtanki.server.client.weapons.freeze.FireTarget
    import flashtanki.server.client.weapons.freeze.StartFire
    import flashtanki.server.client.weapons.freeze.StopFire
    import flashtanki.server.commands.Command
    import flashtanki.server.commands.CommandName
    import flashtanki.server.garage.ServerGarageUserItemWeapon
    import kotlinx.coroutines.delay

    class FreezeWeaponHandler(
        player: BattlePlayer,
        weapon: ServerGarageUserItemWeapon
    ) : WeaponHandler(player, weapon) {
        private var fireStarted = false
        private var temperature = 0.0
        private var originalSpeed: MutableMap<String, Double?> = mutableMapOf()
        private var originalTurnSpeed: MutableMap<String, Double?> = mutableMapOf()
        private var originalTurretRotationSpeed: MutableMap<String, Double?> = mutableMapOf()
        private var speedReduced = mutableMapOf<String, Boolean>()

        suspend fun fireStart(startFire: StartFire) {
            if (fireStarted) return
            val tank = player.tank ?: throw Exception("No Tank")
            val battle = player.battle
            fireStarted = true
            Command(CommandName.ClientStartFire, tank.id).send(battle.players.exclude(player).ready())
        }

        suspend fun fireTarget(target: FireTarget) {
            if (!fireStarted) return
            val sourceTank = player.tank ?: throw Exception("No Tank")
            val battle = player.battle
            val targetTanks = battle.players
                .mapNotNull { player -> player.tank }
                .filter { tank -> target.targets.contains(tank.id) }
                .filter { tank -> tank.state == TankState.Active }

            targetTanks.forEach { targetTank ->
                if (sourceTank != targetTank && (battle.properties[BattleProperty.FriendlyFireEnabled] || (battle.modeHandler is DeathmatchModeHandler || player.team != targetTank.player.team))) {

                    val damage = damageCalculator.calculate(sourceTank, targetTank)
                    battle.damageProcessor.dealDamage(
                        sourceTank,
                        targetTank,
                        damage.damage,
                        isCritical = damage.isCritical
                    )

                    if (speedReduced[targetTank.id] != true) {
                        val targetSpecification = ChangeTankSpecificationData.fromPhysics(
                            targetTank.hull.modification.physics,
                            targetTank.weapon.item.modification.physics
                        )
                        originalSpeed[targetTank.id] = targetSpecification.speed
                        originalTurnSpeed[targetTank.id] = targetSpecification.turnSpeed
                        originalTurretRotationSpeed[targetTank.id] = targetSpecification.turretRotationSpeed
                        speedReduced[targetTank.id] = true
                        targetSpecification.speed *= 0.3
                        targetSpecification.turnSpeed *= 0.3
                        targetSpecification.turretRotationSpeed *= 0.3
                        Command(
                            CommandName.ChangeTankSpecification,
                            targetTank.id,
                            targetSpecification.toJson()
                        ).sendTo(
                            battle
                        )
                        temperature = -0.8
                        Command(CommandName.Temperature, targetTank.id, temperature.toString()).sendTo(battle)
                        delay(5000)
                    }
                }
            }
        }

        suspend fun fireStop(stopFire: StopFire) {
            if (!fireStarted) return
            val tank = player.tank ?: throw Exception("No Tank")
            val battle = player.battle
            val targetTanks = battle.players.mapNotNull { it.tank }.filter { it.state == TankState.Active }
            fireStarted = false
            temperature = 0.0

            Command(CommandName.ClientStopFire, tank.id).send(battle.players.exclude(player).ready())
            delay(3500)
            targetTanks.forEach { targetTank ->
                if (tank != targetTank && (battle.properties[BattleProperty.FriendlyFireEnabled] || (battle.modeHandler is DeathmatchModeHandler || player.team != targetTank.player.team))) {
                    if (speedReduced[targetTank.id] == true) {
                        val targetSpecification = ChangeTankSpecificationData.fromPhysics(
                            targetTank.hull.modification.physics,
                            targetTank.weapon.item.modification.physics
                        )
                        delay(1600)
                        targetSpecification.speed = originalSpeed[targetTank.id] ?: targetSpecification.speed
                        targetSpecification.turnSpeed =
                            originalTurnSpeed[targetTank.id] ?: targetSpecification.turnSpeed
                        targetSpecification.turretRotationSpeed =
                            originalTurretRotationSpeed[targetTank.id] ?: targetSpecification.turretRotationSpeed
                        Command(
                            CommandName.ChangeTankSpecification,
                            targetTank.id,
                            targetSpecification.toJson()
                        ).sendTo(
                            battle
                        )
                        speedReduced[targetTank.id] = false
                    }
                    delay(500)
                }
                Command(CommandName.Temperature, targetTank.id, "-0.6").sendTo(battle)
                delay(1000)
                Command(CommandName.Temperature, targetTank.id, "-0.3").sendTo(battle)
                delay(1500)
                Command(CommandName.Temperature, targetTank.id, temperature.toString()).sendTo(battle)
            }
        }
    }