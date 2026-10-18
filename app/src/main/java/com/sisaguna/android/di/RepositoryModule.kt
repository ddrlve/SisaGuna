package com.sisaguna.android.di

import com.sisaguna.android.data.repository.AddressRepository
import com.sisaguna.android.data.repository.CartRepository
import com.sisaguna.android.data.push.FirebaseTokenSource
import com.sisaguna.android.data.push.PushTokenSource
import com.sisaguna.android.data.repository.AdminRepository
import com.sisaguna.android.data.repository.ApiAddressRepository
import com.sisaguna.android.data.repository.ApiAdminRepository
import com.sisaguna.android.data.repository.ApiAuthRepository
import com.sisaguna.android.data.repository.ApiChatRepository
import com.sisaguna.android.data.repository.ApiIncomingOrderRepository
import com.sisaguna.android.data.repository.ChatRepository
import com.sisaguna.android.data.repository.ApiListingRepository
import com.sisaguna.android.data.repository.ApiNotificationRepository
import com.sisaguna.android.data.repository.ApiOrderRepository
import com.sisaguna.android.data.repository.ApiReviewRepository
import com.sisaguna.android.data.repository.ReviewRepository
import com.sisaguna.android.data.repository.ApiPaymentMethodRepository
import com.sisaguna.android.data.repository.ApiVoucherRepository
import com.sisaguna.android.data.repository.IncomingOrderRepository
import com.sisaguna.android.data.repository.ApiMediaRepository
import com.sisaguna.android.data.repository.ApiProfileRepository
import com.sisaguna.android.data.repository.ApiSavedMerchantRepository
import com.sisaguna.android.data.repository.AuthRepository
import com.sisaguna.android.data.repository.MediaRepository
import com.sisaguna.android.data.repository.InMemoryCartRepository
import com.sisaguna.android.data.repository.OrderRepository
import com.sisaguna.android.data.repository.PaymentMethodRepository
import com.sisaguna.android.data.repository.ListingRepository
import com.sisaguna.android.data.repository.NotificationRepository
import com.sisaguna.android.data.repository.ProfileRepository
import com.sisaguna.android.data.repository.VoucherRepository
import com.sisaguna.android.data.repository.SavedMerchantRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindPushTokenSource(impl: FirebaseTokenSource): PushTokenSource

    @Binds
    abstract fun bindAdminRepository(impl: ApiAdminRepository): AdminRepository

    @Binds
    abstract fun bindAuthRepository(impl: ApiAuthRepository): AuthRepository

    @Binds
    abstract fun bindMediaRepository(impl: ApiMediaRepository): MediaRepository

    @Binds
    abstract fun bindListingRepository(impl: ApiListingRepository): ListingRepository

    @Binds
    abstract fun bindSavedMerchantRepository(impl: ApiSavedMerchantRepository): SavedMerchantRepository

    @Binds
    abstract fun bindProfileRepository(impl: ApiProfileRepository): ProfileRepository

    @Binds
    abstract fun bindNotificationRepository(impl: ApiNotificationRepository): NotificationRepository

    @Binds
    abstract fun bindCartRepository(impl: InMemoryCartRepository): CartRepository

    @Binds
    abstract fun bindOrderRepository(impl: ApiOrderRepository): OrderRepository

    @Binds
    abstract fun bindAddressRepository(impl: ApiAddressRepository): AddressRepository

    @Binds
    abstract fun bindPaymentMethodRepository(impl: ApiPaymentMethodRepository): PaymentMethodRepository

    @Binds
    abstract fun bindVoucherRepository(impl: ApiVoucherRepository): VoucherRepository

    @Binds
    abstract fun bindReviewRepository(impl: ApiReviewRepository): ReviewRepository

    @Binds
    abstract fun bindChatRepository(impl: ApiChatRepository): ChatRepository

    @Binds
    abstract fun bindIncomingOrderRepository(impl: ApiIncomingOrderRepository): IncomingOrderRepository
}
