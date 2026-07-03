package com.bestfriends.beachbingo.core.di

import com.bestfriends.beachbingo.core.data.repository.AuthRepository
import com.bestfriends.beachbingo.core.data.repository.AuthRepositoryImpl
import com.bestfriends.beachbingo.core.data.repository.GameRepository
import com.bestfriends.beachbingo.core.data.repository.GameRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindGameRepository(impl: GameRepositoryImpl): GameRepository
}
