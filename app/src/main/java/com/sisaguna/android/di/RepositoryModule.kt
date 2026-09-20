package com.sisaguna.android.di

import com.sisaguna.android.data.repository.FakeListingRepository
import com.sisaguna.android.data.repository.ListingRepository
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
}
