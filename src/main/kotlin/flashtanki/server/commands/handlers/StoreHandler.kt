package flashtanki.server.commands.handlers

import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import flashtanki.server.client.SocketLocale
import flashtanki.server.client.UserSocket
import flashtanki.server.client.send
import flashtanki.server.client.toJson
import flashtanki.server.commands.Command
import flashtanki.server.commands.CommandHandler
import flashtanki.server.commands.CommandName
import flashtanki.server.commands.ICommandHandler
import flashtanki.server.store.*
import flashtanki.server.*

class StoreHandler : ICommandHandler, KoinComponent {
  private val logger = KotlinLogging.logger { }

  private val storeRegistry: IStoreRegistry by inject()
  private val storeItemConverter: IStoreItemConverter by inject()
  private val promoCodeService: IPromoCodeService by inject()

  @CommandHandler(CommandName.OpenStore)
  suspend fun openStore(socket: UserSocket) {
    Command(
      CommandName.ClientOpenStore,
      OpenStoreWrapperData(
        data = OpenStoreData(
          categories = storeRegistry.categories.values.map(storeItemConverter::toClientCategory),
          items = storeRegistry.categories.values
            .map { category -> category.items }
            .flatten()
            .map(storeItemConverter::toClientItem)
        ).toJson()
      ).toJson()
    ).send(socket)
  }

    @CommandHandler(CommandName.TryActivatePromocode)
    suspend fun tryActivatePromocode(socket: UserSocket, promocode: String) {
        val user = socket.user ?: throw Exception("No User")
        if (promoCodeService.checkPromoCode(promocode)) {
		    val promoPrize = 5000
            Command(CommandName.ActivatePromocodeSuccessfully).send(socket)
            user.crystals += promoPrize

          //rofl premium
          //val premium = 2
          //socket.addPremiumAccount(premium)


            socket.updateCrystals()
			Command(
              CommandName.StorePaymentSuccess,
              promoPrize.toString(),
              0.toString(),
              0.toString(),
              when(socket.locale) {
                SocketLocale.Russian -> 124221
                SocketLocale.English -> 123444
                SocketLocale.Portuguese -> 143111
                else -> throw IllegalArgumentException("Unsupported locale: ${socket.locale}")
              }.toString()
            ).send(socket)
            promoCodeService.removePromoCode(promocode)
            logger.debug { "Player ${user.username} activated promocode ${promocode}" }
        } else {
            Command(CommandName.ActivatePromocodeFailed).send(socket)
            logger.debug { "Player ${user.username} can't be activated promocode ${promocode}" }
        }

    }

  @CommandHandler(CommandName.StoreTryBuyItem)
  suspend fun storeTryBuyItem(socket: UserSocket, itemId: String, paymentMethodId: String) {
    val user = socket.user ?: throw Exception("No User")
    val paymentMethod = StorePaymentMethod.get(paymentMethodId) ?: throw IllegalArgumentException("Unknown payment method: $paymentMethodId")

    val item = storeRegistry.categories.values
      .map { category -> category.items }
      .flatten()
      .single { item -> item.id == itemId }

    Command(
      CommandName.StorePaymentSuccess,
      (if(item.crystals != null) item.crystals.base else 0).toString(),
      (if(item.crystals != null) item.crystals.bonus else 0).toString(),
      0.toString(),
      when(socket.locale) {
        SocketLocale.Russian -> 124221
        SocketLocale.English -> 123444
        SocketLocale.Portuguese -> 143111
        else -> throw IllegalArgumentException("Unsupported locale: ${socket.locale}")
      }.toString()
    ).send(socket)

    if(item.crystals != null) {
      user.crystals += item.crystals.base + item.crystals.bonus
      socket.updateCrystals()
    }

    logger.debug { "Player ${user.username} bought ${item.id} (payment method: $paymentMethod)" }
  }
}
