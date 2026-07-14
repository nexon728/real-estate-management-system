# CS241 – Real Estate DBMS · Deliverable 3

## Project Structure

```
RealEstateAdmin/
├── lib/                         ← place mysql-connector-j-*.jar here
├── src/main/java/realestate/
│   ├── DBConnection.java        ← JDBC connection helper
│   ├── QueryRunner.java         ← all SQL queries (a–f + 6 bonus)
│   └── AdminMenu.java           ← interactive CLI (main entry point)
├── compile_and_run.sh           ← Linux / macOS build & run
├── compile_and_run.bat          ← Windows build & run
└── README.md
```

## Prerequisites

| Requirement | Version |
|---|---|
| JDK | 11 or higher |
| MySQL Server | 8.0+ |
| MySQL Connector/J | 8.0+ |

## Setup Steps

### 1 — Load the database
```bash
mysql -u root -p < CS241_Deliverable_2_Schema_Data_Final.sql
```

### 2 — Download MySQL Connector/J
- https://dev.mysql.com/downloads/connector/j/
- Choose **Platform Independent → ZIP Archive**
- Extract and copy `mysql-connector-j-*.jar` into the `lib/` folder

### 3 — Configure credentials (if needed)
Edit `src/main/java/realestate/DBConnection.java`:
```java
private static final String USER     = "root";
private static final String PASSWORD = "";   // your MySQL password
```

### 4 — Compile and run

**Linux / macOS**
```bash
bash compile_and_run.sh
```

**Windows**
```
compile_and_run.bat
```

**Manual (any OS)**
```bash
# Compile
javac -cp "lib/mysql-connector-j-*.jar" -d out \
      src/main/java/realestate/*.java

# Run (Linux/Mac uses :, Windows uses ;)
java -cp "lib/mysql-connector-j-*.jar:out" realestate.AdminMenu
```

## Available Queries

### Required (CS241 Deliverable 3)
| Key | Query |
|-----|-------|
| `a` | Houses built after 2023 available for rent |
| `b` | Average selling price of properties sold in 2018 |
| `c` | Active rent on GS Road — ≥2 bedrooms, rent < Rs.15,000 |
| `d` | Top-selling agent in 2023 |
| `e` | Average days on market per agent (2018 sales) |
| `f` | Most expensive house (sale) & house with highest rent |
| `all` | Run all required queries a–f in sequence |

### Additional / Bonus
| Key | Query |
|-----|-------|
| `1` | Houses in Guwahati priced Rs.20L–Rs.60L |
| `2` | All currently active listings |
| `3` | Full transaction history |
| `4` | Agent performance summary |
| `5` | Current property ownership |
| `6` | Active leases & rent details |

| `0` | Exit |
