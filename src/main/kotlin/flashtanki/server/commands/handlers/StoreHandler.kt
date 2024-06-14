package flashtanki.server.commands.handlers

import com.squareup.moshi.Moshi
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import flashtanki.server.commands.Command
import flashtanki.server.commands.CommandHandler
import flashtanki.server.commands.CommandName
import flashtanki.server.commands.ICommandHandler
import flashtanki.server.store.*
import flashtanki.server.*
import flashtanki.server.client.*
import java.io.File

class StoreHandler : ICommandHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val storeRegistry: IStoreRegistry by inject()
  private val storeItemConverter: IStoreItemConverter by inject()
  private val promoCodeService: IPromoCodeService by inject()
  private val userRepository: IUserRepository by inject()

  private fun checkUser(socket: UserSocket): User {
    return socket.user ?: throw Exception("No User")
  }

  @CommandHandler(CommandName.OpenStore)
  suspend fun openStore(socket: UserSocket) {
    val categories = storeRegistry.categories.values
    val clientCategories = categories.map(storeItemConverter::toClientCategory)
    val clientItems = categories.flatMap { it.items }.map(storeItemConverter::toClientItem)

    Command(
      CommandName.ClientOpenStore,
      OpenStoreWrapperData(
        data = OpenStoreData(
          categories = clientCategories,
          items = clientItems
        ).toJson()
      ).toJson()
    ).send(socket)
  }

  @CommandHandler(CommandName.TryActivatePromocode)
  suspend fun tryActivatePromocode(socket: UserSocket, promocode: String) {
    val user = checkUser(socket)
    if (promoCodeService.checkPromoCode(promocode)) {
      val promoPrize = 5000
      Command(CommandName.ActivatePromocodeSuccessfully).send(socket)
      user.crystals += promoPrize

      socket.updateCrystals()
      Command(
        CommandName.StorePaymentSuccess,
        promoPrize.toString(),
        0.toString(),
        0.toString(),
        getLocaleValue(socket.locale)
      ).send(socket)
      promoCodeService.removePromoCode(promocode)
      logger.debug { "Player ${user.username} activated promocode $promocode" }
    } else {
      Command(CommandName.ActivatePromocodeFailed).send(socket)
      logger.debug { "Player ${user.username} failed to activate promocode $promocode" }
    }
  }

  @CommandHandler(CommandName.StoreTryBuyItem)
  suspend fun storeTryBuyItem(socket: UserSocket, itemId: String, paymentMethodId: String) {
    val user = checkUser(socket)
    val paymentMethod = StorePaymentMethod.get(paymentMethodId)
      ?: throw IllegalArgumentException("Unknown payment method: $paymentMethodId")

    val item = storeRegistry.categories.values
      .flatMap { it.items }
      .single { it.id == itemId }

    Command(
      CommandName.StorePaymentSuccess,
      (item.crystals?.base ?: 0).toString(),
      (item.crystals?.bonus ?: 0).toString(),
      0.toString(),
      getLocaleValue(socket.locale)
    ).send(socket)

    item.crystals?.let {
      user.crystals += it.base + it.bonus
      socket.updateCrystals()
      logger.debug { "Player ${user.username} added crystals (${it.base} + ${it.bonus})" }
    }

    item.premium?.let {
      socket.addPremiumAccount(it.base)
      userRepository.updateUser(user)
      logger.debug { "Player ${user.username} added premium days (${it.base})" }
    }

    if (itemId == "clan_license") {
      Command(CommandName.AddClanLicense).send(socket)
      logger.debug { "Player ${user.username} added clan_license" }
    }

    logger.debug { "Player ${user.username} bought ${item.id} with payment method: $paymentMethod" }
  }

  private fun getLocaleValue(locale: SocketLocale?): String {
    return when (locale) {
      SocketLocale.Russian -> "124221"
      SocketLocale.English -> "123444"
      SocketLocale.Portuguese -> "143111"
      else -> throw IllegalArgumentException("Unsupported or null locale: $locale")
    }
  }
}
