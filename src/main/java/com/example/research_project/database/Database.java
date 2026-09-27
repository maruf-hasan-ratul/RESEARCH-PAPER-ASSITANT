package com.example.research_project.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database.java - Manages the SQLite database connection and table setup.
 *
 * ================================================================
 * RESPONSIBILITIES
 * ================================================================
 * 1. Build the JDBC connection URL pointing to our .db file
 * 2. Create the "database/" folder if it doesn't exist
 * 3. Open a Connection to the SQLite file (auto-created by SQLite)
 * 4. Run CREATE TABLE IF NOT EXISTS for every table we need
 * 5. Enable SQLite foreign key enforcement (off by default in SQLite)
 *
 * ================================================================
 * HOW DAO CLASSES USE THIS
 * ================================================================
 * Every DAO method that needs to talk to the database calls:
 *
 *   try (Connection conn = Database.getConnection()) {
 *       // use conn here
 *   }
 *
 * The try-with-resources block automatically closes the connection
 * when done, even if an exception occurs. This is important because
 * SQLite locks the file while a connection is open.
 *
 * ================================================================
 * WHY NOT KEEP ONE SHARED CONNECTION OPEN?
 * ================================================================
 * SQLite supports one writer at a time. Keeping a single connection
 * open across the whole app lifetime can cause locking issues when
 * multiple DAOs try to write at the same time.
 * Opening a short-lived connection per operation is safer and
 * still very fast with SQLite.
 *
 * ================================================================
 * DATABASE SCHEMA
 * ================================================================
 *
 *  papers
 *  -------
 *  id          INTEGER PK AUTOINCREMENT
 *  title       TEXT NOT NULL
 *  authors     TEXT
 *  year        INTEGER
 *  abstract    TEXT
 *  methodology TEXT
 *  findings    TEXT
 *  category    TEXT
 *  source      TEXT
 *  file_path   TEXT
 *  created_at  TEXT
 *
 *  keywords (one-to-many with papers)
 *  --------
 *  id          INTEGER PK AUTOINCREMENT
 *  paper_id    INTEGER NOT NULL  FK -> papers(id) ON DELETE CASCADE
 *  keyword     TEXT NOT NULL
 *
 *  notes (one-to-many with papers)
 *  -----
 *  id          INTEGER PK AUTOINCREMENT
 *  paper_id    INTEGER NOT NULL  FK -> papers(id) ON DELETE CASCADE
 *  note        TEXT NOT NULL
 *  created_at  TEXT
 */
public class Database {

    // ----------------------------------------------------------------
    // The JDBC URL tells Java where the SQLite database file lives.
    // "jdbc:sqlite:" is the protocol.
    // "database/research_assistant.db" is the file path (relative to
    // where the app is launched from - usually the project root).
    // SQLite creates this file automatically if it does not exist.
    // ----------------------------------------------------------------
    private static final String DB_URL = "jdbc:sqlite:database/research_assistant.db";

    // ----------------------------------------------------------------
    // SQL: enable foreign key constraints.
    // SQLite ignores foreign keys by default - we must turn them on
    // each time we open a connection.
    // This ensures that deleting a paper also deletes its keywords/notes.
    // ----------------------------------------------------------------
    private static final String ENABLE_FOREIGN_KEYS = "PRAGMA foreign_keys = ON";

    // ----------------------------------------------------------------
    // SQL: CREATE TABLE IF NOT EXISTS
    // "IF NOT EXISTS" is crucial - it means running this a second time
    // will NOT wipe existing data. Safe to call every time the app starts.
    // ----------------------------------------------------------------
    private static final String CREATE_PAPERS_TABLE =
            "CREATE TABLE IF NOT EXISTS papers (" +
                    "    id             INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "    title          TEXT    NOT NULL," +
                    "    authors        TEXT," +
                    "    year           INTEGER," +
                    "    abstract       TEXT," +
                    "    methodology    TEXT," +
                    "    findings       TEXT," +
                    "    category       TEXT," +
                    "    source         TEXT," +
                    "    file_path      TEXT," +
                    "    created_at     TEXT," +
                    "    favorite       INTEGER DEFAULT 0," +
                    "    reading_status TEXT    DEFAULT 'UNREAD'" +
                    ")";

    private static final String CREATE_KEYWORDS_TABLE =
            "CREATE TABLE IF NOT EXISTS keywords (" +
                    "    id       INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "    paper_id INTEGER NOT NULL," +
                    "    keyword  TEXT    NOT NULL," +
                    "    FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_NOTES_TABLE =
            "CREATE TABLE IF NOT EXISTS notes (" +
                    "    id         INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "    paper_id   INTEGER NOT NULL," +
                    "    note       TEXT    NOT NULL," +
                    "    created_at TEXT," +
                    "    FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE CASCADE" +
                    ")";

    // ----------------------------------------------------------------
    // Prevent anyone from creating a Database object.
    // All methods are static - you use them like:
    //   Database.getConnection()
    //   Database.initialise()
    // ----------------------------------------------------------------
    private Database() {}

    // ================================================================
    // PUBLIC API
    // ================================================================

    /**
     * Opens and returns a new Connection to the SQLite database.
     *
     * IMPORTANT: The caller is responsible for closing the connection.
     * Always use try-with-resources:
     *
     *   try (Connection conn = Database.getConnection()) {
     *       // work here
     *   }  // conn.close() is called automatically here
     *
     * @return  A live Connection to research_assistant.db
     * @throws  SQLException if the database file cannot be opened
     */
    public static Connection getConnection() throws SQLException {
        // Enable foreign keys on every new connection.
        // We do this by executing the PRAGMA right after connecting.
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(ENABLE_FOREIGN_KEYS);
        }
        return conn;
    }

    /**
     * Initialises the database.
     *
     * Call this ONCE when the application starts (in Main.java).
     *
     * What it does:
     *   1. Creates the "database/" folder if missing
     *   2. Opens a connection (SQLite creates the .db file if missing)
     *   3. Creates all three tables if they don't exist yet
     *   4. Prints a success or error message to the console
     */
    public static void initialise() {
        // Step 1: Make sure the "database/" folder exists.
        // If the folder is missing, DriverManager.getConnection() would fail.
        File dbFolder = new File("database");
        if (!dbFolder.exists()) {
            boolean created = dbFolder.mkdirs();
            if (created) {
                System.out.println("Database: created 'database/' folder.");
            }
        }

        // Step 2: Connect and create tables.
        // try-with-resources closes the connection automatically.
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create the three tables (safe to call even if they already exist)
            stmt.execute(CREATE_PAPERS_TABLE);
            stmt.execute(CREATE_KEYWORDS_TABLE);
            stmt.execute(CREATE_NOTES_TABLE);

            // Safely migrate existing databases if new columns are missing
            migrateSchema(conn);

            System.out.println("Database: initialised successfully.");
            System.out.println("Database: file -> database/research_assistant.db");

            // Seed initial sample papers if database is empty
            seedSampleDataIfEmpty(conn);

        } catch (SQLException e) {
            // Print a clear error message - the app cannot run without a database
            System.err.println("====================================================");
            System.err.println("DATABASE ERROR: Could not initialise the database.");
            System.err.println("Reason: " + e.getMessage());
            System.err.println("====================================================");
            // In Step 7 we will also show a JavaFX alert dialog here
        }
    }

    /**
     * Safely migrates existing database schemas without deleting data.
     * Adds 'favorite' and 'reading_status' columns if they do not exist.
     */
    private static void migrateSchema(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            boolean hasFavorite = false;
            boolean hasReadingStatus = false;

            try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(papers)")) {
                while (rs.next()) {
                    String colName = rs.getString("name");
                    if ("favorite".equalsIgnoreCase(colName)) {
                        hasFavorite = true;
                    } else if ("reading_status".equalsIgnoreCase(colName)) {
                        hasReadingStatus = true;
                    }
                }
            }

            if (!hasFavorite) {
                stmt.execute("ALTER TABLE papers ADD COLUMN favorite INTEGER DEFAULT 0");
                System.out.println("Database Migration: added 'favorite' column to papers table.");
            }

            if (!hasReadingStatus) {
                stmt.execute("ALTER TABLE papers ADD COLUMN reading_status TEXT DEFAULT 'UNREAD'");
                System.out.println("Database Migration: added 'reading_status' column to papers table.");
            }

            // Ensure any existing nulls are set to default values
            stmt.execute("UPDATE papers SET favorite = 0 WHERE favorite IS NULL");
            stmt.execute("UPDATE papers SET reading_status = 'UNREAD' WHERE reading_status IS NULL OR reading_status = ''");

        } catch (SQLException e) {
            System.err.println("Database Migration warning: " + e.getMessage());
        }
    }

    /**
     * Seeds initial sample research papers and keywords if the papers table is empty.
     */
    private static void seedSampleDataIfEmpty(Connection conn) {
        try (Statement checkStmt = conn.createStatement();
             var rs = checkStmt.executeQuery("SELECT COUNT(*) FROM papers")) {
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("Database: Seeding initial sample papers...");
                String insertPaperSql = "INSERT INTO papers (title, authors, year, abstract, methodology, findings, category, source, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, datetime('now', 'localtime'))";
                String insertKwSql = "INSERT INTO keywords (paper_id, keyword) VALUES (?, ?)";

                // Paper 1
                try (var ps = conn.prepareStatement(insertPaperSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, "Attention Is All You Need");
                    ps.setString(2, "Ashish Vaswani, Noam Shazeer, Niki Parmar, Jakob Uszkoreit");
                    ps.setInt(3, 2017);
                    ps.setString(4, "The dominant sequence transduction models are based on complex recurrent or convolutional neural networks. We propose the Transformer, based solely on attention mechanisms, dispensing with recurrence and convolutions entirely.");
                    ps.setString(5, "Transformer architecture relying entirely on multi-head self-attention without recurrence.");
                    ps.setString(6, "Achieved state-of-the-art translation quality and significantly faster training speed.");
                    ps.setString(7, "Artificial Intelligence");
                    ps.setString(8, "NeurIPS 2017");
                    ps.executeUpdate();
                    try (var keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            int pId = keys.getInt(1);
                            try (var kwPs = conn.prepareStatement(insertKwSql)) {
                                for (String kw : new String[]{"Transformer", "Attention", "Deep Learning", "Neural Network"}) {
                                    kwPs.setInt(1, pId);
                                    kwPs.setString(2, kw);
                                    kwPs.executeUpdate();
                                }
                            }
                        }
                    }
                }

                // Paper 2
                try (var ps = conn.prepareStatement(insertPaperSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, "Deep Residual Learning for Image Recognition");
                    ps.setString(2, "Kaiming He, Xiangyu Zhang, Shaoqing Ren, Jian Sun");
                    ps.setInt(3, 2016);
                    ps.setString(4, "Deeper neural networks are more difficult to train. We present a residual learning framework to ease training of networks that are substantially deeper, reformulating layers as learning residual functions.");
                    ps.setString(5, "Deep residual neural networks with identity shortcut connections.");
                    ps.setString(6, "Won 1st place in ILSVRC 2015 image classification with 3.57% top-5 error rate.");
                    ps.setString(7, "Computer Vision");
                    ps.setString(8, "CVPR 2016");
                    ps.executeUpdate();
                    try (var keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            int pId = keys.getInt(1);
                            try (var kwPs = conn.prepareStatement(insertKwSql)) {
                                for (String kw : new String[]{"ResNet", "Deep Learning", "Computer Vision", "Convolutional Neural Network"}) {
                                    kwPs.setInt(1, pId);
                                    kwPs.setString(2, kw);
                                    kwPs.executeUpdate();
                                }
                            }
                        }
                    }
                }

                // Paper 3
                try (var ps = conn.prepareStatement(insertPaperSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, "The Architecture of SQLite");
                    ps.setString(2, "D. Richard Hipp");
                    ps.setInt(3, 2020);
                    ps.setString(4, "SQLite is a C-language library implementing a small, fast, self-contained, high-reliability, full-featured SQL database engine used worldwide across billions of devices.");
                    ps.setString(5, "Serverless relational database engine using B-tree indexing and transactional ACID rollback journal.");
                    ps.setString(6, "Proven zero-configuration architecture with ultra-high reliability and cross-platform compatibility.");
                    ps.setString(7, "Software Engineering");
                    ps.setString(8, "ACM SIGMOD");
                    ps.executeUpdate();
                    try (var keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            int pId = keys.getInt(1);
                            try (var kwPs = conn.prepareStatement(insertKwSql)) {
                                for (String kw : new String[]{"SQLite", "Database", "Storage Engine", "ACID", "SQL"}) {
                                    kwPs.setInt(1, pId);
                                    kwPs.setString(2, kw);
                                    kwPs.executeUpdate();
                                }
                            }
                        }
                    }
                }
                System.out.println("Database: Seeded 3 sample papers successfully.");
            }
        } catch (SQLException e) {
            System.err.println("Database seed warning: " + e.getMessage());
        }
    }

    /**
     * Tests the connection to the database.
     * Returns true if the connection succeeds, false otherwise.
     * Useful for unit tests (Step 23).
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Database.testConnection() failed: " + e.getMessage());
            return false;
        }
    }
}