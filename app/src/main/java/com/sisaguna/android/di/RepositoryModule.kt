package com.sisaguna.android.di

import com.sisaguna.android.data.repository.AddressRepository
import com.sisaguna.android.data.repository.CartRepository
import com.sisaguna.android.data.repository.FakeAddressRepository
import com.sisaguna.android.data.repository.FakeListingRepository
import com.sisaguna.android.data.repository.FakeOrderRepository
import com.sisaguna.android.data.repository.FakePaymentMethodRepository
import com.sisaguna.android.data.repository.InMemoryCartRepository
import com.sisaguna.android.data.repository.OrderRepository
import com.sisaguna.android.data.repository.PaymentMethodRepository
import com.sisaguna.android.data.repository.FakeNotificationRepository
import com.sisaguna.android.data.repository.FakeProfileRepository
import com.sisaguna.android.data.repository.FakeSavedMerchantRepository
import com.sisaguna.android.data.repository.ListingRepository
import com.sisaguna.android.data.repository.NotificationRepository
import com.sisaguna.android.data.repository.ProfileRepository
import com.sisaguna.android.data.repository.FakeVoucherRepository
import com.sisaguna.android.data.repository.VoucherRepository
import com.sisaguna.android.data.repository.SavedMerchantRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // [Guessing] Swap this binding for SupabaseListingRepository once the Supabase project
    // decision in ANDROID_CLAUDE.md is confirmed.
    @Binds
    abstract fun bindListingRepository(impl: FakeListingRepository): ListingRepository

    @Binds
    abstract fun bindSavedMerchantRepository(impl: FakeSavedMerchantRepository): SavedMerchantRepository

    @Binds
    abstract fun bindProfileRepository(impl: FakeProfileRepository): ProfileRepository

    @Binds
    abstract fun bindNotificationRepository(impl: FakeNotificationRepository): NotificationRepository

    @Binds
    abstract fun bindCartRepository(impl: InMemoryCartRepository): CartRepository

    @Binds
    abstract fun bindOrderRepository(impl: FakeOrderRepository): OrderRepository

    @Binds
    abstract fun bindAddressRepository(impl: FakeAddressRepository): AddressRepository

    @Binds
    abstract fun bindPaymentMethodRepository(impl: FakePaymentMethodRepository): PaymentMethodRepository

    @Binds
    abstract fun bindVoucherRepository(impl: FakeVoucherRepository): VoucherRepository
}
