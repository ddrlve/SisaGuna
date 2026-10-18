package com.sisaguna.android.data.remote

import com.sisaguna.android.data.model.Address
import com.sisaguna.android.data.model.AdminComplaint
import com.sisaguna.android.data.model.AdminMerchant
import com.sisaguna.android.data.model.AdminVoucher
import com.sisaguna.android.data.remote.dto.AdminComplaintDto
import com.sisaguna.android.data.remote.dto.AdminMerchantDto
import com.sisaguna.android.data.remote.dto.AdminVoucherDto
import com.sisaguna.android.data.model.ChatMessage
import com.sisaguna.android.data.model.ChatSide
import com.sisaguna.android.data.model.Complaint
import com.sisaguna.android.data.model.Conversation
import com.sisaguna.android.data.remote.dto.ConversationDto
import com.sisaguna.android.data.remote.dto.MessageDto
import com.sisaguna.android.data.model.ComplaintReason
import com.sisaguna.android.data.model.ComplaintStatus
import com.sisaguna.android.data.model.Courier
import com.sisaguna.android.data.model.DeliveryQuote
import com.sisaguna.android.data.model.DeliverySpeed
import com.sisaguna.android.data.model.Fulfillment
import com.sisaguna.android.data.model.Order
import com.sisaguna.android.data.model.OrderLine
import com.sisaguna.android.data.model.OrderRating
import com.sisaguna.android.data.model.OrderStatus
import com.sisaguna.android.data.model.PaymentKind
import com.sisaguna.android.data.model.PaymentMethod
import com.sisaguna.android.data.model.Review
import com.sisaguna.android.data.remote.dto.OrderComplaintDto
import com.sisaguna.android.data.remote.dto.OrderDeliveryDto
import com.sisaguna.android.data.remote.dto.OrderDto
import com.sisaguna.android.data.remote.dto.PaymentMethodDto
import com.sisaguna.android.data.remote.dto.ReviewDto
import com.sisaguna.android.data.remote.dto.VoucherDto
import com.sisaguna.android.data.repository.IncomingOrder
import com.sisaguna.android.data.repository.IncomingStatus
import com.sisaguna.android.data.repository.Voucher
import com.sisaguna.android.data.model.Allergen
import com.sisaguna.android.data.model.AppNotification
import com.sisaguna.android.data.model.NotificationType
import com.sisaguna.android.data.remote.dto.NotificationDto
import com.sisaguna.android.data.model.HalalStatus
import com.sisaguna.android.data.model.Listing
import com.sisaguna.android.data.model.ListingTier
import com.sisaguna.android.data.model.Merchant
import com.sisaguna.android.data.model.MerchantStatus
import com.sisaguna.android.data.model.QuantityUnit
import com.sisaguna.android.data.model.SafetyCheck
import com.sisaguna.android.data.model.StorageMethod
import com.sisaguna.android.data.model.UserProfile
import com.sisaguna.android.data.remote.dto.AddressDto
import com.sisaguna.android.data.remote.dto.ListingCardDto
import com.sisaguna.android.data.remote.dto.ListingDetailDto
import com.sisaguna.android.data.remote.dto.MyListingDto
import com.sisaguna.android.data.remote.dto.MerchantDto
import com.sisaguna.android.data.remote.dto.UserDto
import java.time.Instant
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonPrimitive
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneOffset

// ---- Primitives. The API sends lowercase snake_case enum values and RFC 3339 timestamps.

internal inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    this?.let { s -> enumValues<E>().firstOrNull { it.name.equals(s, ignoreCase = true) } } ?: default

/** Unknown values are dropped rather than failing the whole response. */
internal inline fun <reified E : Enum<E>> Iterable<String>.toEnumSet(): Set<E> =
    mapNotNull { s -> enumValues<E>().firstOrNull { it.name.equals(s, ignoreCase = true) } }.toSet()

/** Accepts `Z` and `+07:00` offsets with any fractional precision. */
internal fun String.toInstant(): Instant = OffsetDateTime.parse(this).toInstant()

internal fun String?.toInstantOrNull(): Instant? = this?.let { runCatching { it.toInstant() }.getOrNull() }

internal fun String.toQuantityUnit(): QuantityUnit =
    if (equals("kg", ignoreCase = true)) QuantityUnit.KILOGRAM else QuantityUnit.PORTION

// ---- Catalog

fun MerchantDto.toMerchant(): Merchant = Merchant(
    id = id,
    name = name,
    isVerified = isVerified,
    status = status.toEnum(MerchantStatus.APPROVED),
    location = locationLabel,
    rating = ratingAvg,
    ratingCount = ratingCount,
    bannerUrl = bannerUrl.orEmpty(),
    photos = photos.map { it.url },
    about = about,
    address = address,
    openHours = openHours,
    halal = halal.toEnum(HalalStatus.UNVERIFIED),
    allergens = allergens.toEnumSet<Allergen>(),
    todaysOffer = todaysOffer,
    deliveryAvailable = deliveryAvailable,
    prepMinutes = prepMinutes,
    distanceKm = distanceKm,
    totalRescued = totalRescued,
    availableCount = availableCount,
    nextPickupEnd = nextPickupEnd.toInstantOrNull(),
    ratingBreakdown = ratingBreakdown.mapNotNull { (star, n) -> star.toIntOrNull()?.let { it to n } }.toMap(),
)

fun ListingCardDto.toListing(): Listing = Listing(
    id = id,
    merchantId = merchant.id,
    title = title,
    tier = tier.toEnum(ListingTier.HUMAN),
    priceOriginal = priceOriginal,
    priceDiscounted = price,
    isFree = isFree,
    pickupEnd = pickupEnd.toInstant(),
    imageUrl = coverUrl,
    distanceKm = distanceKm,
    stock = stock,
    pickupStart = pickupStart.toInstantOrNull(),
    unit = unit.toQuantityUnit(),
    halal = halal.toEnum(HalalStatus.UNVERIFIED),
    allergens = allergens.toEnumSet<Allergen>(),
    soldCount = soldCount,
)

fun ListingDetailDto.toListing(): Listing {
    val urls = photos.map { it.url }
    return Listing(
        id = id,
        merchantId = merchant.id,
        title = title,
        tier = tier.toEnum(ListingTier.HUMAN),
        priceOriginal = priceOriginal,
        priceDiscounted = price,
        isFree = isFree,
        pickupEnd = pickupEnd.toInstant(),
        imageUrl = urls.firstOrNull().orEmpty(),
        distanceKm = distanceKm,
        description = description,
        stock = stock,
        pickupStart = pickupStart.toInstantOrNull(),
        gallery = urls.drop(1),
        unit = unit.toQuantityUnit(),
        halal = halal.toEnum(HalalStatus.UNVERIFIED),
        allergens = allergens.toEnumSet<Allergen>(),
        madeAt = madeAt.toInstantOrNull(),
        storage = storage.toEnum(StorageMethod.ROOM_TEMP),
        safetyChecks = safetyChecks.toEnumSet<SafetyCheck>(),
        videoUrl = videoUrl,
        soldCount = soldCount,
    )
}

fun MyListingDto.toListing(merchantId: String): Listing = Listing(
    id = id,
    merchantId = merchantId,
    title = title,
    tier = tier.toEnum(ListingTier.HUMAN),
    priceOriginal = priceOriginal,
    priceDiscounted = price,
    isFree = isFree,
    pickupEnd = pickupEnd.toInstant(),
    imageUrl = coverUrl,
    distanceKm = null,
    stock = stock,
    pickupStart = pickupStart.toInstantOrNull(),
    unit = unit.toQuantityUnit(),
    soldCount = soldCount,
)

// ---- Account

fun UserDto.toProfile(): UserProfile = UserProfile(
    name = name,
    email = email,
    phone = phone,
    location = locationLabel.orEmpty(),
    memberSince = createdAt.toInstantOrNull()?.let { YearMonth.from(it.atOffset(ZoneOffset.UTC)) } ?: YearMonth.now(),
    photoUri = photoUrl,
    avatar = avatarKey,
    isAdmin = isAdmin,
)

fun AddressDto.toAddress(): Address = Address(id, label, fullAddress, lat, lng, note)

// ---- Orders

private fun OrderDeliveryDto.toQuote() = DeliveryQuote(
    courier = courier.toEnum(Courier.GOSEND),
    speed = speed.toEnum(DeliverySpeed.STANDARD),
    fee = fee,
    discount = discount,
    etaMinMinutes = etaMin,
    etaMaxMinutes = etaMax,
    promoLabel = promoLabel,
    promoLabelEn = promoLabelEn,
)

private fun OrderComplaintDto.toComplaint() = Complaint(
    reason = reason.toEnum(ComplaintReason.OTHER),
    detail = detail,
    photos = photoUrls,
    createdAt = createdAt.toInstantOrNull() ?: Instant.EPOCH,
    status = status.toEnum(ComplaintStatus.SUBMITTED),
)

/** [merchantLookup] lets an order show the full store (photo, hours) when it is already cached. */
fun OrderDto.toOrder(merchantLookup: (String) -> Merchant? = { null }): Order = Order(
    id = id,
    pickupCode = code.orEmpty(),
    merchant = merchantLookup(merchant.id)
        ?: Merchant(merchant.id, merchant.name, isVerified = false, status = MerchantStatus.APPROVED, location = merchant.locationLabel.orEmpty()),
    lines = items.map {
        OrderLine(
            listingId = it.listingId,
            title = it.title,
            tier = it.tier.toEnum(ListingTier.HUMAN),
            imageUrl = it.imageUrl.orEmpty(),
            quantity = it.quantity,
            unitPrice = it.unitPrice,
            unitOriginalPrice = it.unitOriginalPrice,
            unit = it.unit.toQuantityUnit(),
        )
    },
    payment = paymentKind.toEnum(PaymentKind.CASH),
    note = note,
    status = status.toEnum(OrderStatus.READY),
    createdAt = createdAt.toInstant(),
    pickupEnd = pickupEnd.toInstant(),
    completedAt = completedAt.toInstantOrNull(),
    rating = rating?.let { OrderRating(it.stars, it.tags, it.comment) },
    voucherCode = voucherCode,
    voucherDiscount = voucherDiscount,
    fulfillment = fulfillment.toEnum(Fulfillment.PICKUP),
    delivery = delivery?.toQuote(),
    deliveryAddress = deliveryAddress?.let { Address(it.id, it.label, it.fullAddress, it.lat, it.lng, it.note) },
    pickupBy = pickupBy.toInstantOrNull(),
    complaint = complaint?.toComplaint(),
    serviceFee = serviceFee,
    paymentPaid = payment?.status == "paid",
    paymentSimulated = payment?.provider == "dummy",
    canCancel = canCancel,
    canRate = canRate,
    canComplain = canComplain,
)

/** The merchant's view of an order. The pickup code is never sent to the merchant (the buyer tells it). */
fun OrderDto.toIncoming(): IncomingOrder = IncomingOrder(
    id = id,
    buyerName = buyerName?.takeIf { it.isNotBlank() } ?: "Pembeli",
    itemTitle = items.firstOrNull()?.title.orEmpty() + if (items.size > 1) " +${items.size - 1}" else "",
    quantity = items.sumOf { it.quantity },
    unit = items.firstOrNull()?.unit?.toQuantityUnit() ?: QuantityUnit.PORTION,
    total = subtotal,
    fulfillment = fulfillment.toEnum(Fulfillment.PICKUP),
    courierLabel = delivery?.let { "${it.courierLabel} · ${it.speedLabel}" },
    createdAt = createdAt.toInstant(),
    pickupBy = (pickupBy ?: pickupEnd).toInstant(),
    status = when (status) {
        "completed" -> IncomingStatus.HANDED_OVER
        "ready" -> IncomingStatus.WAITING
        else -> IncomingStatus.NO_SHOW
    },
)

// ---- Payment methods, vouchers, reviews

fun PaymentMethodDto.toMethod() = PaymentMethod("p-$kind", kind.toEnum(PaymentKind.CASH), label, detail)

private const val DEFAULT_ACCENT = 0xFF55B931

fun VoucherDto.toVoucher() = Voucher(
    code = code,
    title = title,
    description = description,
    percentOff = percentOff,
    flatOff = flatOff,
    maxDiscount = maxDiscount,
    minSpend = minSpend,
    expiresAt = expiresAt.toInstant(),
    imageUrl = imageUrl.orEmpty(),
    terms = terms,
    merchantName = merchantName,
    appliesTo = appliesTo.ifBlank { "Semua makanan siap santap" },
    accent = accentHex.removePrefix("#").toLongOrNull(16)?.let { 0xFF000000L or it } ?: DEFAULT_ACCENT,
)

fun ReviewDto.toReview(merchantId: String) = Review(
    id = id,
    merchantId = merchantId,
    author = buyerName.ifBlank { "Pembeli" },
    stars = stars,
    comment = comment,
    createdAt = createdAt.toInstant(),
    photos = photos.map { it.url },
    tags = tags,
)

// ---- Notifications

fun NotificationDto.toNotification() = AppNotification(
    id = id,
    type = type.toEnum(NotificationType.PROMO),
    title = title,
    body = body,
    createdAt = createdAt.toInstant(),
    isRead = readAt != null,
    orderId = (data?.get("order_id") as? JsonPrimitive)?.contentOrNull,
    merchantId = (data?.get("merchant_id") as? JsonPrimitive)?.contentOrNull,
)

// ---- Chat

fun ConversationDto.toConversation(side: ChatSide) = Conversation(
    id = id,
    counterpartId = counterpart.id,
    counterpartName = counterpart.name,
    side = side,
    unread = unread.toInt(),
    lastText = lastMessage?.body.orEmpty(),
    lastAt = lastMessage?.at.toInstantOrNull(),
    lastSender = lastMessage?.let { if (it.side == "buyer") ChatSide.BUYER else ChatSide.MERCHANT },
)

/**
 * [merchantId] is the store the thread belongs to (the counterpart for a buyer, the user's own
 * store for a merchant); [buyerKey] identifies the buyer end of the thread.
 */
fun MessageDto.toChatMessage(merchantId: String, buyerKey: String, isRead: Boolean) = ChatMessage(
    id = id.toString(),
    merchantId = merchantId,
    fromBuyer = senderSide == "buyer",
    text = body,
    sentAt = createdAt.toInstant(),
    listingId = listingId,
    isRead = isRead,
    buyer = buyerKey,
    conversationId = conversationId,
)

// ---- Admin

fun AdminMerchantDto.toAdminMerchant() = AdminMerchant(
    id, name, status.toEnum(MerchantStatus.PENDING), address, ownerName, ownerEmail, createdAt.toInstant(),
)

fun AdminComplaintDto.toAdminComplaint() = AdminComplaint(
    id = id,
    orderCode = orderCode,
    merchantName = merchantName,
    buyerName = buyerName,
    reason = reason.toEnum(ComplaintReason.OTHER),
    detail = detail,
    status = status.toEnum(ComplaintStatus.SUBMITTED),
    resolutionNote = resolutionNote,
    photoUrls = photoUrls,
    createdAt = createdAt.toInstant(),
)

fun AdminVoucherDto.toAdminVoucher() = AdminVoucher(
    code = code,
    title = title,
    description = description,
    percentOff = percentOff,
    flatOff = flatOff,
    maxDiscount = maxDiscount,
    minSpend = minSpend,
    appliesToTier = appliesToTier?.toEnum(ListingTier.HUMAN),
    expiresAt = expiresAt.toInstant(),
    quota = quota,
    usedCount = usedCount,
    terms = terms,
    accentHex = accentHex,
    isActive = isActive,
)
