package com.msmobile.visitas.di

import android.content.Context
import android.location.Geocoder
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.msmobile.visitas.VisitasDatabase
import com.msmobile.visitas.conversation.ConversationDao
import com.msmobile.visitas.conversation.ConversationRepository
import com.msmobile.visitas.fieldservice.FieldServiceDao
import com.msmobile.visitas.fieldservice.FieldServiceRepository
import com.msmobile.visitas.householder.HouseholderDao
import com.msmobile.visitas.householder.HouseholderRepository
import com.msmobile.visitas.preference.PreferenceDao
import com.msmobile.visitas.preference.PreferenceRepository
import com.msmobile.visitas.serialization.LocalDateTimeAdapter
import com.msmobile.visitas.serialization.SerializationFactory
import com.msmobile.visitas.serialization.UUIDAdapter
import com.msmobile.visitas.summary.SummaryDao
import com.msmobile.visitas.summary.SummaryRepository
import com.msmobile.visitas.util.AddressProvider
import com.msmobile.visitas.util.BackupHandler
import com.msmobile.visitas.util.CalendarEventManager
import com.msmobile.visitas.util.DateTimeProvider
import com.msmobile.visitas.util.DefaultLogger
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.EncryptionHandler
import com.msmobile.visitas.util.IdProvider
import com.msmobile.visitas.util.LocaleProvider
import com.msmobile.visitas.util.Logger
import com.msmobile.visitas.util.NetworkStatusTracker
import com.msmobile.visitas.util.PermissionChecker
import com.msmobile.visitas.util.TimerManager
import com.msmobile.visitas.util.UserLocationProvider
import com.msmobile.visitas.visit.VisitDao
import com.msmobile.visitas.visit.VisitHouseholderDao
import com.msmobile.visitas.visit.VisitHouseholderRepository
import com.msmobile.visitas.visit.VisitRepository
import com.msmobile.visitas.visit.VisitTimeValidator
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class ApplicationModule {
    @Singleton
    @Provides
    fun moshi(): Moshi {
        return Moshi.Builder()
            .add(LocalDateTimeAdapter())
            .add(UUIDAdapter())
            .add(KotlinJsonAdapterFactory())
            .add(SerializationFactory)
            .build()
    }

    @Singleton
    @Provides
    fun dispatcherProvider(): DispatcherProvider {
        return DispatcherProvider()
    }

    @Singleton
    @Provides
    fun visitRepository(visitDao: VisitDao): VisitRepository {
        return VisitRepository(visitDao = visitDao)
    }

    @Singleton
    @Provides
    fun visitHouseholderRepository(visitHouseholderDao: VisitHouseholderDao): VisitHouseholderRepository {
        return VisitHouseholderRepository(visitHouseholderDao = visitHouseholderDao)
    }

    @Singleton
    @Provides
    fun householderRepository(householderDao: HouseholderDao): HouseholderRepository {
        return HouseholderRepository(householderDao = householderDao)
    }

    @Singleton
    @Provides
    fun conversationRepository(conversationDao: ConversationDao): ConversationRepository {
        return ConversationRepository(conversationDao = conversationDao)
    }

    @Singleton
    @Provides
    fun summaryRepository(summaryDao: SummaryDao): SummaryRepository {
        return SummaryRepository(summaryDao = summaryDao)
    }

    @Singleton
    @Provides
    fun preferenceRepository(preferenceDao: PreferenceDao): PreferenceRepository {
        return PreferenceRepository(preferenceDao = preferenceDao)
    }

    @Singleton
    @Provides
    fun fieldServiceRepository(fieldServiceDao: FieldServiceDao): FieldServiceRepository {
        return FieldServiceRepository(fieldServiceDao = fieldServiceDao)
    }

    @Singleton
    @Provides
    fun geocoder(@ApplicationContext appContext: Context): Geocoder {
        return Geocoder(appContext)
    }

    @Singleton
    @Provides
    fun fusedLocationProviderClient(@ApplicationContext appContext: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(appContext)
    }

    @Singleton
    @Provides
    fun addressProvider(
        geocoder: Geocoder,
        locationProviderClient: FusedLocationProviderClient
    ): AddressProvider {
        return AddressProvider(
            geocoder = geocoder,
            locationProviderClient = locationProviderClient,
            looper = Looper.getMainLooper()
        )
    }

    @Singleton
    @Provides
    fun timer(): TimerManager {
        return TimerManager()
    }

    @Singleton
    @Provides
    fun logger(): Logger {
        return DefaultLogger
    }

    @Singleton
    @Provides
    fun networkStatusTracker(@ApplicationContext context: Context): NetworkStatusTracker {
        return NetworkStatusTracker(context)
    }

    @Singleton
    @Provides
    fun userLocationProvider(@ApplicationContext context: Context): UserLocationProvider {
        return UserLocationProvider(context)
    }

    @Singleton
    @Provides
    fun permissionChecker(@ApplicationContext context: Context): PermissionChecker {
        return PermissionChecker(context)
    }

    @Singleton
    @Provides
    fun calendarEventManager(
        @ApplicationContext context: Context,
        permissionChecker: PermissionChecker
    ): CalendarEventManager {
        return CalendarEventManager(context, permissionChecker)
    }

    @Singleton
    @Provides
    fun uuidProvider(): IdProvider {
        return IdProvider()
    }

    @Provides
    @Singleton
    fun provideEncryptionHandler(): EncryptionHandler {
        return EncryptionHandler()
    }

    @Provides
    @Singleton
    fun provideLocaleProvider(): LocaleProvider {
        return LocaleProvider()
    }

    @Provides
    @Singleton
    fun provideDateTimeProvider(): DateTimeProvider {
        return DateTimeProvider()
    }

    @Provides
    @Singleton
    fun provideVisitTimeValidator(): VisitTimeValidator {
        return VisitTimeValidator
    }

    @Provides
    @Singleton
    fun provideBackupHandler(
        @ApplicationContext context: Context,
        encryptionHandler: EncryptionHandler,
        database: VisitasDatabase,
        dispatcherProvider: DispatcherProvider,
        localeProvider: LocaleProvider,
        dateTimeProvider: DateTimeProvider,
        logger: Logger,
    ): BackupHandler {
        return BackupHandler(
            context = context,
            logger = logger,
            encryptionHandler = encryptionHandler,
            database = database,
            dispatcherProvider = dispatcherProvider,
            localeProvider = localeProvider,
            dateTimeProvider = dateTimeProvider
        )
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): okhttp3.OkHttpClient {
        return okhttp3.OkHttpClient.Builder()
            .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: okhttp3.OkHttpClient, moshi: Moshi): retrofit2.Retrofit {
        return retrofit2.Retrofit.Builder()
            .baseUrl("https://router.project-osrm.org/")
            .client(okHttpClient)
            .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideOsrmService(retrofit: retrofit2.Retrofit): com.msmobile.visitas.routing.OsrmService {
        return retrofit.create(com.msmobile.visitas.routing.OsrmService::class.java)
    }

    @Provides
    @Singleton
    fun provideOsrmRoutingProvider(
        osrmService: com.msmobile.visitas.routing.OsrmService,
        dispatcherProvider: DispatcherProvider,
        logger: Logger
    ): com.msmobile.visitas.routing.OsrmRoutingProvider {
        return com.msmobile.visitas.routing.OsrmRoutingProvider(osrmService, dispatcherProvider, logger)
    }

    @Singleton
    @Provides
    fun database(@ApplicationContext context: Context): VisitasDatabase {
        return VisitasDatabase.build(context)
    }

    @Singleton
    @Provides
    fun conversationDao(roomDatabase: VisitasDatabase): ConversationDao {
        return roomDatabase.conversationDao()
    }

    @Singleton
    @Provides
    fun householderDao(roomDatabase: VisitasDatabase): HouseholderDao {
        return roomDatabase.householderDao()
    }

    @Singleton
    @Provides
    fun summaryDao(roomDatabase: VisitasDatabase): SummaryDao {
        return roomDatabase.summaryDao()
    }

    @Singleton
    @Provides
    fun fieldServiceDao(roomDatabase: VisitasDatabase): FieldServiceDao {
        return roomDatabase.fieldServiceDao()
    }

    @Singleton
    @Provides
    fun visitDao(roomDatabase: VisitasDatabase): VisitDao {
        return roomDatabase.visitDao()
    }

    @Singleton
    @Provides
    fun visitHouseholderDao(roomDatabase: VisitasDatabase): VisitHouseholderDao {
        return roomDatabase.visitHouseholderDao()
    }

    @Singleton
    @Provides
    fun preferenceDao(roomDatabase: VisitasDatabase): PreferenceDao {
        return roomDatabase.preferenceDao()
    }
}