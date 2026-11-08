package store.oatec.playertv.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import store.oatec.playertv.model.MediaEntry
import store.oatec.playertv.model.MediaKind

/**
 * On-device catalogue storage. Writes are batched and reads are paged so UI
 * callers do not need to materialize a provider's complete catalogue in RAM.
 */
class MediaCatalogStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE media_catalog (
                source_id TEXT NOT NULL,
                section TEXT NOT NULL,
                item_id TEXT NOT NULL,
                title TEXT NOT NULL,
                category TEXT NOT NULL,
                stream_url TEXT NOT NULL,
                logo_url TEXT NOT NULL,
                kind TEXT NOT NULL,
                description TEXT NOT NULL,
                season TEXT NOT NULL,
                episode_number TEXT NOT NULL,
                genre TEXT NOT NULL,
                rating TEXT NOT NULL,
                year TEXT NOT NULL,
                release_date TEXT NOT NULL,
                director TEXT NOT NULL,
                cast TEXT NOT NULL,
                PRIMARY KEY(source_id, section, item_id)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX media_catalog_source_section_title ON media_catalog(source_id, section, title COLLATE NOCASE)")
        db.execSQL("CREATE INDEX media_catalog_source_section_category ON media_catalog(source_id, section, category COLLATE NOCASE)")
        db.execSQL(
            """
            CREATE TABLE catalog_categories (
                source_id TEXT NOT NULL,
                section TEXT NOT NULL,
                category_id TEXT NOT NULL,
                category_name TEXT NOT NULL,
                loaded INTEGER NOT NULL DEFAULT 0,
                item_count INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(source_id, section, category_id)
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Keep existing catalogues when upgrading. Version 2 introduced this
        // table; older installations simply create it if it is absent.
        if (oldVersion < 2) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS media_catalog (
                    source_id TEXT NOT NULL, section TEXT NOT NULL, item_id TEXT NOT NULL,
                    title TEXT NOT NULL, category TEXT NOT NULL, stream_url TEXT NOT NULL,
                    logo_url TEXT NOT NULL, kind TEXT NOT NULL, description TEXT NOT NULL,
                    season TEXT NOT NULL, episode_number TEXT NOT NULL, genre TEXT NOT NULL,
                    rating TEXT NOT NULL, year TEXT NOT NULL, release_date TEXT NOT NULL,
                    director TEXT NOT NULL, cast TEXT NOT NULL,
                    PRIMARY KEY(source_id, section, item_id)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS media_catalog_source_section_title ON media_catalog(source_id, section, title COLLATE NOCASE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS media_catalog_source_section_category ON media_catalog(source_id, section, category COLLATE NOCASE)")
        }
        if (oldVersion < 3) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS catalog_categories (
                    source_id TEXT NOT NULL, section TEXT NOT NULL, category_id TEXT NOT NULL,
                    category_name TEXT NOT NULL, loaded INTEGER NOT NULL DEFAULT 0,
                    item_count INTEGER NOT NULL DEFAULT 0, updated_at INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(source_id, section, category_id)
                )
                """.trimIndent(),
            )
        }
    }

    /**
     * Starts an atomic catalogue replacement. Feed bounded batches while parsing
     * the provider response, then call commit; closing without commit rolls back.
     */
    fun beginImport(sourceId: String, section: String): ImportSession {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            db.delete(TABLE, "source_id=? AND section=?", arrayOf(sourceId, section))
            ImportSession(db, sourceId, section)
        } catch (error: Throwable) {
            db.endTransaction()
            throw error
        }
    }

    /** Replace only one provider category; other category rows remain intact. */
    fun beginCategoryImport(sourceId: String, section: String, category: SourceCategory): ImportSession {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            db.delete(TABLE, "source_id=? AND section=? AND category=?", arrayOf(sourceId, section, category.name))
            ImportSession(db, sourceId, section, category.id, category.name)
        } catch (error: Throwable) {
            db.endTransaction()
            throw error
        }
    }

    /** Save the provider category directory without downloading any media items. */
    fun saveCategories(sourceId: String, section: String, categories: List<SourceCategory>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (category in categories) {
                val previousName = db.query(
                    "catalog_categories", arrayOf("category_name"),
                    "source_id=? AND section=? AND category_id=?",
                    arrayOf(sourceId, section, category.id), null, null, null, "1",
                ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                if (previousName != null && previousName != category.name) {
                    db.delete(TABLE, "source_id=? AND section=? AND category=?", arrayOf(sourceId, section, previousName))
                }
                val values = ContentValues().apply {
                    put("source_id", sourceId)
                    put("section", section)
                    put("category_id", category.id)
                    put("category_name", category.name)
                }
                db.insertWithOnConflict("catalog_categories", null, values, SQLiteDatabase.CONFLICT_IGNORE)
                val update = ContentValues().apply {
                    put("category_name", category.name)
                    if (previousName != null && previousName != category.name) {
                        put("loaded", 0)
                        put("item_count", 0)
                    }
                }
                db.update("catalog_categories", update, "source_id=? AND section=? AND category_id=?", arrayOf(sourceId, section, category.id))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun categoryList(sourceId: String, section: String): List<SourceCategory> {
        val result = mutableListOf<SourceCategory>()
        readableDatabase.query(
            "catalog_categories",
            arrayOf("category_id", "category_name"),
            "source_id=? AND section=?",
            arrayOf(sourceId, section),
            null, null, "category_name COLLATE NOCASE ASC",
        ).use { cursor ->
            while (cursor.moveToNext()) result += SourceCategory(cursor.getString(0), cursor.getString(1))
        }
        return result
    }

    fun isCategoryCached(sourceId: String, section: String, categoryId: String): Boolean =
        readableDatabase.query(
            "catalog_categories",
            arrayOf("loaded"),
            "source_id=? AND section=? AND category_id=?",
            arrayOf(sourceId, section, categoryId),
            null, null, null, "1",
        ).use { cursor -> cursor.moveToFirst() && cursor.getInt(0) == 1 }

    inner class ImportSession internal constructor(
        private val db: SQLiteDatabase,
        private val sourceId: String,
        private val section: String,
        private val categoryId: String? = null,
        private val categoryName: String? = null,
    ) : AutoCloseable {
        private var finished = false

        fun insertBatch(entries: List<MediaEntry>) {
            check(!finished) { "Catalogue import is already closed." }
            if (entries.isEmpty()) return
            for (entry in entries) {
                val values = ContentValues().apply {
                    put("source_id", sourceId)
                    put("section", section)
                    put("item_id", entry.id)
                    put("title", entry.title)
                    put("category", entry.category)
                    put("stream_url", entry.streamUrl)
                    put("logo_url", entry.logoUrl)
                    put("kind", entry.kind.name)
                    put("description", entry.description)
                    put("season", entry.season)
                    put("episode_number", entry.episodeNumber)
                    put("genre", entry.genre)
                    put("rating", entry.rating)
                    put("year", entry.year)
                    put("release_date", entry.releaseDate)
                    put("director", entry.director)
                    put("cast", entry.cast)
                }
                db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
        }

        fun commit() {
            check(!finished) { "Catalogue import is already closed." }
            if (!categoryId.isNullOrBlank() && !categoryName.isNullOrBlank()) {
                val count = db.rawQuery(
                    "SELECT COUNT(*) FROM $TABLE WHERE source_id=? AND section=? AND category=?",
                    arrayOf(sourceId, section, categoryName),
                ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
                val values = ContentValues().apply {
                    put("source_id", sourceId)
                    put("section", section)
                    put("category_id", categoryId)
                    put("category_name", categoryName)
                    put("loaded", 1)
                    put("item_count", count)
                    put("updated_at", System.currentTimeMillis())
                }
                db.insertWithOnConflict("catalog_categories", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
            finished = true
            db.endTransaction()
        }

        override fun close() {
            if (!finished) {
                finished = true
                db.endTransaction()
            }
        }
    }

    /** Replace a section atomically; call appendBatch repeatedly inside this transaction. */
    fun replaceSection(sourceId: String, section: String, batches: Sequence<List<MediaEntry>>) {
        beginImport(sourceId, section).use { session ->
            batches.forEach(session::insertBatch)
            session.commit()
        }
    }

    /**
     * Append entries without deleting previously stored rows. Use beginImport()
     * when the caller intends to replace a whole section atomically.
     */
    fun insertBatch(sourceId: String, section: String, entries: List<MediaEntry>) {
        if (entries.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (entry in entries) {
                val values = ContentValues().apply {
                    put("source_id", sourceId)
                    put("section", section)
                    put("item_id", entry.id)
                    put("title", entry.title)
                    put("category", entry.category)
                    put("stream_url", entry.streamUrl)
                    put("logo_url", entry.logoUrl)
                    put("kind", entry.kind.name)
                    put("description", entry.description)
                    put("season", entry.season)
                    put("episode_number", entry.episodeNumber)
                    put("genre", entry.genre)
                    put("rating", entry.rating)
                    put("year", entry.year)
                    put("release_date", entry.releaseDate)
                    put("director", entry.director)
                    put("cast", entry.cast)
                }
                db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun page(
        sourceId: String,
        section: String,
        limit: Int = DEFAULT_PAGE_SIZE,
        offset: Int = 0,
        category: String? = null,
        search: String = "",
    ): List<MediaEntry> {
        require(limit in 1..MAX_PAGE_SIZE) { "Page size must be between 1 and $MAX_PAGE_SIZE." }
        val where = StringBuilder("source_id=? AND section=?")
        val args = mutableListOf(sourceId, section)
        if (!category.isNullOrBlank() && category != "All") {
            where.append(" AND category=?")
            args += category
        }
        if (search.isNotBlank()) {
            val pattern = "%${escapeLike(search.trim())}%"
            where.append(" AND (title LIKE ? ESCAPE '\\' OR category LIKE ? ESCAPE '\\')")
            args += pattern
            args += pattern
        }
        val results = ArrayList<MediaEntry>(limit)
        readableDatabase.query(
            TABLE,
            null,
            where.toString(),
            args.toTypedArray(),
            null,
            null,
            "title COLLATE NOCASE ASC",
            "$limit OFFSET ${offset.coerceAtLeast(0)}",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                results += MediaEntry(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("item_id")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                    streamUrl = cursor.getString(cursor.getColumnIndexOrThrow("stream_url")),
                    logoUrl = cursor.getString(cursor.getColumnIndexOrThrow("logo_url")),
                    kind = runCatching { MediaKind.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("kind"))) }.getOrDefault(MediaKind.LIVE),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    season = cursor.getString(cursor.getColumnIndexOrThrow("season")),
                    episodeNumber = cursor.getString(cursor.getColumnIndexOrThrow("episode_number")),
                    genre = cursor.getString(cursor.getColumnIndexOrThrow("genre")),
                    rating = cursor.getString(cursor.getColumnIndexOrThrow("rating")),
                    year = cursor.getString(cursor.getColumnIndexOrThrow("year")),
                    releaseDate = cursor.getString(cursor.getColumnIndexOrThrow("release_date")),
                    director = cursor.getString(cursor.getColumnIndexOrThrow("director")),
                    cast = cursor.getString(cursor.getColumnIndexOrThrow("cast")),
                )
            }
        }
        return results
    }

    /** Lightweight ordered IDs for global receiver numbering without loading full media rows. */
    fun allIds(sourceId: String, section: String): List<String> {
        val result = ArrayList<String>()
        readableDatabase.query(
            TABLE,
            arrayOf("item_id"),
            "source_id=? AND section=?",
            arrayOf(sourceId, section),
            null,
            null,
            "rowid ASC",
        ).use { cursor ->
            while (cursor.moveToNext()) result += cursor.getString(0)
        }
        return result
    }

    fun count(sourceId: String, section: String, search: String = "", category: String? = null): Long {
        val where = StringBuilder("source_id=? AND section=?")
        val args = mutableListOf(sourceId, section)
        if (!category.isNullOrBlank() && category != "All") {
            where.append(" AND category=?")
            args += category
        }
        if (search.isNotBlank()) {
            val pattern = "%${escapeLike(search.trim())}%"
            where.append(" AND (title LIKE ? ESCAPE '\\' OR category LIKE ? ESCAPE '\\')")
            args += pattern
            args += pattern
        }
        return readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE WHERE ${where}",
            args.toTypedArray(),
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L }
    }

    fun categories(sourceId: String, section: String): List<String> {
        val result = mutableListOf<String>()
        readableDatabase.rawQuery(
            "SELECT DISTINCT category FROM $TABLE WHERE source_id=? AND section=? ORDER BY category COLLATE NOCASE",
            arrayOf(sourceId, section),
        ).use { cursor -> while (cursor.moveToNext()) result += cursor.getString(0) }
        return result
    }

    private fun escapeLike(value: String): String =
        value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private companion object {
        const val DATABASE_NAME = "player_catalog.db"
        const val DATABASE_VERSION = 3
        const val TABLE = "media_catalog"
        const val DEFAULT_PAGE_SIZE = 100
        const val MAX_PAGE_SIZE = 500
    }
}
