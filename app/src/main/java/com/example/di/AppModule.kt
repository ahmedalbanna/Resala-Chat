package com.example.di

import android.content.Context
import com.example.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        // Phase 1: delegate to AppDatabase.getDatabase so seeding callback is centralized
        // and INSTANCE singleton is shared between Hilt and legacy callers.
        return AppDatabase.getDatabase(context.applicationContext)
    }

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideChatDao(db: AppDatabase): ChatDao = db.chatDao()

    @Provides
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    @Singleton
    fun provideChatRepository(
        db: AppDatabase,
        userDao: UserDao,
        chatDao: ChatDao,
        messageDao: MessageDao
    ): ChatRepository = ChatRepository(db, userDao, chatDao, messageDao)

    @Provides
    @Singleton
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
