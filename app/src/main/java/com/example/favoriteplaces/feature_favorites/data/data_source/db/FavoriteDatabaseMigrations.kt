package com.example.favoriteplaces.feature_favorites.data.data_source.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object FavoriteDatabaseMigrations {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `Favorite` ADD COLUMN `placeType` TEXT DEFAULT NULL"
            )
        }
    }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `Favorite` ADD COLUMN `phoneNumber` TEXT DEFAULT NULL"
            )
        }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `Favorite` ADD COLUMN `googlePrimaryType` TEXT DEFAULT NULL"
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
