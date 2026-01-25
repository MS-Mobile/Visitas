package com.msmobile.visitas

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.msmobile.visitas.conversation.Conversation
import com.msmobile.visitas.conversation.ConversationDao
import com.msmobile.visitas.fieldservice.FieldService
import com.msmobile.visitas.fieldservice.FieldServiceDao
import com.msmobile.visitas.householder.Householder
import com.msmobile.visitas.householder.HouseholderDao
import com.msmobile.visitas.migration.MIGRATION_1_2
import com.msmobile.visitas.migration.MIGRATION_2_3
import com.msmobile.visitas.migration.MIGRATION_3_4
import com.msmobile.visitas.migration.MIGRATION_4_5
import com.msmobile.visitas.migration.MIGRATION_5_6
import com.msmobile.visitas.preference.Preference
import com.msmobile.visitas.preference.PreferenceDao
import com.msmobile.visitas.preference.PreferenceTypeConverters
import com.msmobile.visitas.summary.SummaryDao
import com.msmobile.visitas.util.RoomLocalDateTimeConverter
import com.msmobile.visitas.util.RoomUUIDConverter
import com.msmobile.visitas.visit.Visit
import com.msmobile.visitas.visit.VisitDao
import com.msmobile.visitas.visit.VisitHouseholder
import com.msmobile.visitas.visit.VisitHouseholderDao
import java.io.File

@Database(
    entities = [
        Conversation::class,
        Householder::class,
        FieldService::class,
        Visit::class,
        Preference::class
    ],
    views = [
        VisitHouseholder::class
    ],
    version = 6
)
@TypeConverters(RoomUUIDConverter::class, RoomLocalDateTimeConverter::class, PreferenceTypeConverters::class)
abstract class VisitasDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun householderDao(): HouseholderDao
    abstract fun summaryDao(): SummaryDao
    abstract fun fieldServiceDao(): FieldServiceDao
    abstract fun visitDao(): VisitDao
    abstract fun visitHouseholderDao(): VisitHouseholderDao
    abstract fun preferenceDao(): PreferenceDao

    companion object {
        const val DATABASE_NAME = "visitas"

        private val MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6
        )

        fun build(context: Context): VisitasDatabase {
            return Room.databaseBuilder(context, VisitasDatabase::class.java, DATABASE_NAME)
                .addMigrations(*MIGRATIONS)
                .build()
        }

        fun buildFromFile(context: Context, file: File): VisitasDatabase {
            return Room.databaseBuilder(context, VisitasDatabase::class.java, file.absolutePath)
                .createFromFile(file)
                .addMigrations(*MIGRATIONS)
                .build()
        }
    }
}