package com.bettertick.di

import com.bettertick.data.firebase.DataSeeder
import com.bettertick.data.firebase.FirestoreProvider
import com.bettertick.data.firebase.SyncManager
import com.bettertick.data.repository.AuthRepository
import com.bettertick.data.repository.DiaryRepository
import com.bettertick.data.repository.FocusRepository
import com.bettertick.data.repository.HabitRepository
import com.bettertick.data.repository.ListRepository
import com.bettertick.data.repository.MatrixRepository
import com.bettertick.data.repository.TabBarRepository
import com.bettertick.data.repository.TagRepository
import com.bettertick.data.repository.TaskRepository
import com.bettertick.data.repository.UserRepository
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirestoreProvider(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): FirestoreProvider = FirestoreProvider(auth, firestore)

    @Provides
    @Singleton
    fun provideDataSeeder(
        firestoreProvider: FirestoreProvider
    ): DataSeeder = DataSeeder(firestoreProvider)

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestoreProvider: FirestoreProvider,
        dataSeeder: DataSeeder
    ): AuthRepository = AuthRepository(auth, firestoreProvider, dataSeeder)

    @Provides
    @Singleton
    fun provideSyncManager(
        firestoreProvider: FirestoreProvider
    ): SyncManager = SyncManager(firestoreProvider)

    @Provides
    @Singleton
    fun provideTaskRepository(
        firestoreProvider: FirestoreProvider,
        @ApplicationContext appContext: Context
    ): TaskRepository = TaskRepository(firestoreProvider, appContext)

    @Provides
    @Singleton
    fun provideListRepository(
        firestoreProvider: FirestoreProvider
    ): ListRepository = ListRepository(firestoreProvider)

    @Provides
    @Singleton
    fun provideHabitRepository(
        firestoreProvider: FirestoreProvider
    ): HabitRepository = HabitRepository(firestoreProvider)

    @Provides
    @Singleton
    fun provideFocusRepository(
        firestoreProvider: FirestoreProvider
    ): FocusRepository = FocusRepository(firestoreProvider)

    @Provides
    @Singleton
    fun provideTagRepository(
        firestoreProvider: FirestoreProvider
    ): TagRepository = TagRepository(firestoreProvider)

    @Provides
    @Singleton
    fun provideUserRepository(
        firestoreProvider: FirestoreProvider
    ): UserRepository = UserRepository(firestoreProvider)

    @Provides
    @Singleton
    fun provideMatrixRepository(
        firestoreProvider: FirestoreProvider
    ): MatrixRepository = MatrixRepository(firestoreProvider)

    @Provides
    @Singleton
    fun provideTabBarRepository(
        firestoreProvider: FirestoreProvider
    ): TabBarRepository = TabBarRepository(firestoreProvider)

    @Provides
    @Singleton
    fun provideDiaryRepository(
        firestoreProvider: FirestoreProvider
    ): DiaryRepository = DiaryRepository(firestoreProvider)
}
