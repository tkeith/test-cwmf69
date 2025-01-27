package com.delayedmessaging.android.di

import com.delayedmessaging.android.ui.viewmodel.AuthViewModel
import com.delayedmessaging.android.ui.viewmodel.MessageListViewModel
import com.delayedmessaging.android.ui.viewmodel.ComposeMessageViewModel
import dagger.Module // version: 2.44
import dagger.Provides // version: 2.44
import dagger.hilt.InstallIn // version: 2.44
import dagger.hilt.android.components.ViewModelComponent // version: 2.44
import dagger.hilt.android.scopes.ViewModelScoped // version: 2.44

/**
 * Dagger Hilt module providing dependency injection bindings for all ViewModels
 * in the Delayed Messaging application. Ensures proper scoping and lifecycle management
 * for the MVVM architecture.
 */
@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {

    /**
     * Provides a scoped instance of AuthViewModel for handling user authentication operations.
     * Ensures single instance per ViewModel scope with proper dependency injection.
     *
     * @param loginUseCase Use case for handling login operations
     * @param authRepository Repository for authentication state management
     * @return Scoped instance of AuthViewModel
     */
    @Provides
    @ViewModelScoped
    fun provideAuthViewModel(
        loginUseCase: LoginUseCase,
        authRepository: AuthRepository
    ): AuthViewModel {
        return AuthViewModel(loginUseCase, authRepository)
    }

    /**
     * Provides a scoped instance of MessageListViewModel for managing message list operations.
     * Ensures single instance per ViewModel scope with proper dependency injection.
     *
     * @param messageRepository Repository for message operations
     * @return Scoped instance of MessageListViewModel
     */
    @Provides
    @ViewModelScoped
    fun provideMessageListViewModel(
        messageRepository: MessageRepository
    ): MessageListViewModel {
        return MessageListViewModel(messageRepository)
    }

    /**
     * Provides a scoped instance of ComposeMessageViewModel for handling message composition.
     * Ensures single instance per ViewModel scope with proper dependency injection.
     *
     * @param messageRepository Repository for message operations
     * @param userRepository Repository for user operations
     * @return Scoped instance of ComposeMessageViewModel
     */
    @Provides
    @ViewModelScoped
    fun provideComposeMessageViewModel(
        messageRepository: MessageRepository,
        userRepository: UserRepository
    ): ComposeMessageViewModel {
        return ComposeMessageViewModel(messageRepository, userRepository)
    }
}