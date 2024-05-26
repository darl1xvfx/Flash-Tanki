package flashtanki.server

import java.io.ByteArrayOutputStream
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.reflect.KClass
import java.io.File
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.CancellationException
import mu.KotlinLogging
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.logger.SLF4JLogger
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import flashtanki.server.api.IApiServer
import flashtanki.server.api.WebApiServer
import flashtanki.server.battles.BattleProcessor
import flashtanki.server.battles.DamageCalculator
import flashtanki.server.battles.IBattleProcessor
import flashtanki.server.battles.IDamageCalculator
import flashtanki.server.battles.map.IMapRegistry
import flashtanki.server.battles.map.MapRegistry
import flashtanki.server.chat.ChatCommandRegistry
import flashtanki.server.chat.IChatCommandRegistry
import flashtanki.server.client.*
import flashtanki.server.commands.CommandRegistry
import flashtanki.server.commands.ICommandRegistry
import flashtanki.server.extensions.cast
import flashtanki.server.extensions.gitVersion
import flashtanki.server.garage.GarageItemConverter
import flashtanki.server.garage.GarageMarketRegistry
import flashtanki.server.garage.IGarageItemConverter
import flashtanki.server.garage.IGarageMarketRegistry
import flashtanki.server.invite.IInviteRepository
import flashtanki.server.invite.IInviteService
import flashtanki.server.invite.InviteRepository
import flashtanki.server.invite.InviteService
import flashtanki.server.ipc.IProcessNetworking
import flashtanki.server.ipc.NullNetworking
import flashtanki.server.ipc.ProcessMessage
import flashtanki.server.ipc.WebSocketNetworking
import flashtanki.server.lobby.chat.ILobbyChatManager
import flashtanki.server.lobby.chat.LobbyChatManager
import flashtanki.server.quests.*
import flashtanki.server.resources.IResourceServer
import flashtanki.server.resources.ResourceServer
import flashtanki.server.serialization.*
import flashtanki.server.store.IStoreItemConverter
import flashtanki.server.store.IStoreRegistry
import flashtanki.server.store.StoreItemConverter
import flashtanki.server.store.StoreRegistry

suspend fun ByteReadChannel.readAvailable(): ByteArray {
  val data = ByteArrayOutputStream()
  val temp = ByteArray(1024)
  // while(!isClosedForRead) {
  val read = readAvailable(temp)
  if(read > 0) {
    data.write(temp, 0, read)
  }
  // }

  return data.toByteArray()
}

interface IPromoCodeService {
  val promocodes: MutableList<String>
  val blackList: MutableList<String>

  suspend fun initPromoCodes()
  suspend fun checkPromoCode(promo: String) : Boolean
  suspend fun removePromoCode(promo: String)
}

class PromoCodeService : IPromoCodeService {
  override var promocodes: MutableList<String> = mutableListOf()
  override var blackList: MutableList<String> = mutableListOf()
  private val logger = KotlinLogging.logger { }

  override suspend fun initPromoCodes() {
    logger.debug { "Initing promocodes..." }
    val items = File(ResourceManager().get("promocodes/promocodes.json").toString()).bufferedReader().use { it.readText() }.replace("[", "").replace("]", "").split(",")
    val blackListFile = File(ResourceManager().get("promocodes/blacklist.json").toString())
    if (blackListFile.exists()) {
      blackListFile.forEachLine { line ->
        blackList.add(line.trim())
      }
    }
    for (item in items) {
        val trItem = item.trim()
        if (!blackList.contains(trItem)) {
          promocodes.add(trItem)
          logger.debug { "Inited promocode! Promocode: ${trItem}" }
        }
    }
    logger.debug { "All promocodes inited!" }
  }

  override suspend fun checkPromoCode(promo: String) : Boolean {
    return promocodes.contains(promo)
  }

  override suspend fun removePromoCode(promo: String) {
    promocodes.remove(promo)
    File(ResourceManager().get("promocodes/blacklist.json").toString()).bufferedWriter().use { out ->
      out.write("$promo\n")
    }
  }
}

interface ISocketServer {
  val players: MutableList<UserSocket>

  suspend fun run(scope: CoroutineScope)
  suspend fun stop()
}

class SocketServer : ISocketServer {
  private val logger = KotlinLogging.logger { }

  override val players: MutableList<UserSocket> = mutableListOf()

  private lateinit var server: ServerSocket

  private var acceptJob: Job? = null

  override suspend fun run(scope: CoroutineScope) {
    server = aSocket(ActorSelectorManager(Dispatchers.IO))
      .tcp()
      .bind(InetSocketAddress("0.0.0.0", 2351))

    logger.info { "Started TCP server on ${server.localAddress}" }

    acceptJob = scope.launch {
      try {
        val coroutineScope = CoroutineScope(scope.coroutineContext + SupervisorJob())

        while(true) {
          val tcpSocket = server.accept()
          val socket = UserSocket(coroutineContext, tcpSocket)
          players.add(socket)

          println("Socket accepted: ${socket.remoteAddress}")

          coroutineScope.launch { socket.handle() }
        }
      } catch(exception: CancellationException) {
        logger.debug { "Client accept job cancelled" }
      } catch(exception: Exception) {
        logger.error(exception) { "Exception in client accept loop" }
      }
    }
  }

  override suspend fun stop() {
    // TODO(Assasans): Hack to prevent ConcurrentModificationException
    players.toList().forEach { player -> player.deactivate() }
    acceptJob?.cancel()
    withContext(Dispatchers.IO) { server.close() }

    logger.info { "Stopped game server" }
  }
}

fun main(args: Array<String>) = object : CliktCommand() {
  val ipcUrl by option("--ipc-url", help = "IPC server URL")

  override fun run() = runBlocking {
    val logger = KotlinLogging.logger { }

    logger.info { "Hello!" }
    logger.info { "Version: ${BuildConfig.gitVersion}" }
    logger.info { "Root path: ${Paths.get("").absolute()}" }

    val module = module {
      single<IProcessNetworking> {
        when(val url = ipcUrl) {
          null -> NullNetworking()
          else -> WebSocketNetworking(url)
        }
      }
      single<ISocketServer> { SocketServer() }
      single<IPromoCodeService> { PromoCodeService() }
      single<IResourceServer> { ResourceServer() }
      single<IApiServer> { WebApiServer() }
      single<ICommandRegistry> { CommandRegistry() }
      single<IBattleProcessor> { BattleProcessor() }
      single<IResourceManager> { ResourceManager() }
      single<IGarageItemConverter> { GarageItemConverter() }
      single<IResourceConverter> { ResourceConverter() }
      single<IGarageMarketRegistry> { GarageMarketRegistry() }
      single<IMapRegistry> { MapRegistry() }
      single<IStoreRegistry> { StoreRegistry() }
      single<IStoreItemConverter> { StoreItemConverter() }
      single<ILobbyChatManager> { LobbyChatManager() }
      single<IChatCommandRegistry> { ChatCommandRegistry() }
      single<IDamageCalculator> { DamageCalculator() }
      single<IQuestConverter> { QuestConverter() }
      single<IRandomQuestService> { RandomQuestService() }
      single<IUserRepository> { UserRepository() }
      single<IUserSubscriptionManager> { UserSubscriptionManager() }
      single<IInviteService> { InviteService(enabled = false) }
      single<IInviteRepository> { InviteRepository() }
      single {
        Moshi.Builder()
          .add(
            PolymorphicJsonAdapterFactory.of(ProcessMessage::class.java, "_").let {
              var factory = it
              val reflections = Reflections("flashtanki.server")

              reflections.get(Scanners.SubTypes.of(ProcessMessage::class.java).asClass<ProcessMessage>()).forEach { type ->
                val messageType = type.kotlin.cast<KClass<ProcessMessage>>()
                val name = messageType.simpleName ?: throw IllegalStateException("$messageType has no simple name")

                factory = factory.withSubtype(messageType.java, name.removeSuffix("Message"))
                logger.debug { "Registered IPC message: $name" }
              }

              factory
            }
          )
          .add(
            PolymorphicJsonAdapterFactory.of(WeaponVisual::class.java, "\$type")
              .withSubtype(SmokyVisual::class.java, "smoky")
              .withSubtype(RailgunVisual::class.java, "railgun")
              .withSubtype(Railgun_XTVisual::class.java, "railgun_xt")
              .withSubtype(ThunderVisual::class.java, "thunder")
              .withSubtype(FlamethrowerVisual::class.java, "flamethrower")
              .withSubtype(FreezeVisual::class.java, "freeze")
              .withSubtype(IsidaVisual::class.java, "isida")
              .withSubtype(TwinsVisual::class.java, "twins")
              .withSubtype(ShaftVisual::class.java, "shaft")
              .withSubtype(RicochetVisual::class.java, "ricochet")
          )
          .add(BattleDataJsonAdapterFactory())
          .add(LocalizedStringAdapterFactory())
          .add(ClientLocalizedStringAdapterFactory())
          .add(KotlinJsonAdapterFactory())
          .add(GarageItemTypeAdapter())
          .add(ResourceTypeAdapter())
          .add(ServerMapThemeAdapter())
          .add(BattleTeamAdapter())
          .add(BattleModeAdapter())
          .add(IsidaFireModeAdapter())
          .add(BonusTypeMapAdapter())
          .add(SkyboxSideAdapter())
          .add(EquipmentConstraintsModeAdapter())
          .add(ChatModeratorLevelAdapter())
          .add(SocketLocaleAdapter())
          .add(StoreCurrencyAdapter())
          .add(ScreenAdapter())
          .add(SerializeNull.JSON_ADAPTER_FACTORY)
          .build()
      }
    }

    startKoin {
      logger(SLF4JLogger(Level.ERROR))

      modules(module)
    }

    val server = Server()

    server.run()
  }
}.main(args)
