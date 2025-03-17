package org.dark.customenderchest.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.dark.customenderchest.CustomEnderChest;

public class DatabaseHandler {
   private final Connection connection;
   private final CustomEnderChest plugin;

   public DatabaseHandler(CustomEnderChest plugin) {
      this.plugin = plugin;
      this.connection = this.initializeConnection();
      this.createTables();
   }

   private Connection initializeConnection() {
      try {
         return this.plugin.getConfig().getBoolean("database.mysql", false) ? this.connectMySQL() : this.connectSQLite();
      } catch (SQLException var2) {
         this.plugin.getLogger().severe("Failed to initialize database connection: " + var2.getMessage());
         throw new RuntimeException("Database connection failed", var2);
      }
   }

   private Connection connectMySQL() throws SQLException {
      String host = this.plugin.getConfig().getString("database.host", "localhost");
      int port = this.plugin.getConfig().getInt("database.port", 3306);
      String database = this.plugin.getConfig().getString("database.database", "minecraft");
      String username = this.plugin.getConfig().getString("database.username", "root");
      String password = this.plugin.getConfig().getString("database.password", "");
      String url = String.format("jdbc:mysql://%s:%d/%s", host, port, database);
      return DriverManager.getConnection(url, username, password);
   }

   private Connection connectSQLite() throws SQLException {
      try {
         Class.forName("org.sqlite.JDBC");
         String url = "jdbc:sqlite:" + this.plugin.getDataFolder() + "/enderchest.db";
         return DriverManager.getConnection(url);
      } catch (ClassNotFoundException var2) {
         throw new SQLException("SQLite JDBC driver not found", var2);
      }
   }

   private void createTables() {
      try {
         Statement stmt = this.connection.createStatement();

         try {
            stmt.execute("CREATE TABLE IF NOT EXISTS inventories (uuid VARCHAR(36) PRIMARY KEY, player_name VARCHAR(16), inventory BLOB, migrated BOOLEAN DEFAULT 0, last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE IF NOT EXISTS last_access (uuid VARCHAR(36) PRIMARY KEY, last_access TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
         } catch (Throwable var5) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }
            }

            throw var5;
         }

         if (stmt != null) {
            stmt.close();
         }

      } catch (SQLException var6) {
         this.plugin.getLogger().severe("Failed to create tables: " + var6.getMessage());
         throw new RuntimeException("Failed to initialize database tables", var6);
      }
   }

   private void migrateLegacyEnderChest(UUID uuid) {
      if (uuid != null) {
         String migrationCheckQuery = "SELECT migrated FROM inventories WHERE uuid = ?";
         String updateMigrationQuery = "UPDATE inventories SET migrated = 1 WHERE uuid = ?";
         String insertMigrationQuery = "INSERT INTO inventories (uuid, player_name, inventory, migrated, last_modified) VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP)";

         try {
            boolean needsMigration = true;
            PreparedStatement checkStmt = this.connection.prepareStatement(migrationCheckQuery);

            label237: {
               try {
                  ResultSet rs;
                  label239: {
                     checkStmt.setString(1, uuid.toString());
                     rs = checkStmt.executeQuery();

                     try {
                        if (rs.next() && rs.getBoolean("migrated")) {
                           break label239;
                        }
                     } catch (Throwable var31) {
                        if (rs != null) {
                           try {
                              rs.close();
                           } catch (Throwable var23) {
                              var31.addSuppressed(var23);
                           }
                        }

                        throw var31;
                     }

                     if (rs != null) {
                        rs.close();
                     }
                     break label237;
                  }

                  if (rs != null) {
                     rs.close();
                  }
               } catch (Throwable var32) {
                  if (checkStmt != null) {
                     try {
                        checkStmt.close();
                     } catch (Throwable var22) {
                        var32.addSuppressed(var22);
                     }
                  }

                  throw var32;
               }

               if (checkStmt != null) {
                  checkStmt.close();
               }

               return;
            }

            if (checkStmt != null) {
               checkStmt.close();
            }

            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
               return;
            }

            boolean hasExistingData = false;
            PreparedStatement checkDataStmt = this.connection.prepareStatement("SELECT 1 FROM inventories WHERE uuid = ?");

            try {
               checkDataStmt.setString(1, uuid.toString());
               ResultSet rs = checkDataStmt.executeQuery();

               try {
                  hasExistingData = rs.next();
               } catch (Throwable var29) {
                  if (rs != null) {
                     try {
                        rs.close();
                     } catch (Throwable var21) {
                        var29.addSuppressed(var21);
                     }
                  }

                  throw var29;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var30) {
               if (checkDataStmt != null) {
                  try {
                     checkDataStmt.close();
                  } catch (Throwable var20) {
                     var30.addSuppressed(var20);
                  }
               }

               throw var30;
            }

            if (checkDataStmt != null) {
               checkDataStmt.close();
            }

            ItemStack[] legacyContents = player.getEnderChest().getContents();
            if (legacyContents == null || legacyContents.length == 0) {
               if (hasExistingData) {
                  PreparedStatement updateStmt = this.connection.prepareStatement(updateMigrationQuery);

                  try {
                     updateStmt.setString(1, uuid.toString());
                     updateStmt.executeUpdate();
                  } catch (Throwable var28) {
                     if (updateStmt != null) {
                        try {
                           updateStmt.close();
                        } catch (Throwable var19) {
                           var28.addSuppressed(var19);
                        }
                     }

                     throw var28;
                  }

                  if (updateStmt != null) {
                     updateStmt.close();
                  }
               }

               return;
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            try {
               BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos);

               try {
                  oos.writeObject(legacyContents);
                  byte[] serializedInventory = bos.toByteArray();
                  PreparedStatement updateStmt;
                  if (hasExistingData) {
                     updateStmt = this.connection.prepareStatement(updateMigrationQuery);

                     try {
                        updateStmt.setString(1, uuid.toString());
                        updateStmt.executeUpdate();
                     } catch (Throwable var25) {
                        if (updateStmt != null) {
                           try {
                              updateStmt.close();
                           } catch (Throwable var18) {
                              var25.addSuppressed(var18);
                           }
                        }

                        throw var25;
                     }

                     if (updateStmt != null) {
                        updateStmt.close();
                     }
                  } else {
                     updateStmt = this.connection.prepareStatement(insertMigrationQuery);

                     try {
                        updateStmt.setString(1, uuid.toString());
                        updateStmt.setString(2, player.getName());
                        updateStmt.setBytes(3, serializedInventory);
                        updateStmt.executeUpdate();
                     } catch (Throwable var24) {
                        if (updateStmt != null) {
                           try {
                              updateStmt.close();
                           } catch (Throwable var17) {
                              var24.addSuppressed(var17);
                           }
                        }

                        throw var24;
                     }

                     if (updateStmt != null) {
                        updateStmt.close();
                     }
                  }

                  player.getEnderChest().clear();
               } catch (Throwable var26) {
                  try {
                     oos.close();
                  } catch (Throwable var16) {
                     var26.addSuppressed(var16);
                  }

                  throw var26;
               }

               oos.close();
            } catch (Throwable var27) {
               try {
                  bos.close();
               } catch (Throwable var15) {
                  var27.addSuppressed(var15);
               }

               throw var27;
            }

            bos.close();
         } catch (IOException | SQLException var33) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to migrate legacy EnderChest for " + uuid, var33);
         }

      }
   }

   public synchronized void saveInventory(UUID uuid, ItemStack[] inventory) {
      if (uuid == null) return;

      String query = "INSERT INTO inventories (uuid, inventory, last_modified) VALUES (?, ?, CURRENT_TIMESTAMP) " +
              "ON CONFLICT(uuid) DO UPDATE SET inventory = excluded.inventory, last_modified = CURRENT_TIMESTAMP";

      try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
           BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos)) {

         oos.writeObject(inventory);
         byte[] serializedInventory = bos.toByteArray();

         try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            stmt.setBytes(2, serializedInventory);
            stmt.executeUpdate();
         }

         updateLastAccess(uuid);
      } catch (IOException | SQLException e) {
         plugin.getLogger().log(Level.SEVERE, "Failed to save EnderChest inventory for " + uuid, e);
      }
   }


   public synchronized ItemStack[] loadInventory(UUID uuid) {
      if (uuid == null) {
         return new ItemStack[54];
      } else {
         if (this.plugin.getConfig().getBoolean("database.migrate-legacy-enderchest", false)) {
            this.migrateLegacyEnderChest(uuid);
         }

         String query = "SELECT inventory FROM inventories WHERE uuid = ?";

         try {
            PreparedStatement stmt = this.connection.prepareStatement(query);

            ItemStack[] var8;
            label129: {
               try {
                  stmt.setString(1, uuid.toString());
                  ResultSet rs = stmt.executeQuery();

                  label118: {
                     try {
                        if (!rs.next()) {
                           break label118;
                        }

                        byte[] serializedInventory = rs.getBytes("inventory");
                        this.updateLastAccess(uuid);
                        ByteArrayInputStream bis = new ByteArrayInputStream(serializedInventory);

                        try {
                           BukkitObjectInputStream ois = new BukkitObjectInputStream(bis);

                           try {
                              var8 = (ItemStack[])ois.readObject();
                           } catch (Throwable var14) {
                              try {
                                 ois.close();
                              } catch (Throwable var13) {
                                 var14.addSuppressed(var13);
                              }

                              throw var14;
                           }

                           ois.close();
                        } catch (Throwable var15) {
                           try {
                              bis.close();
                           } catch (Throwable var12) {
                              var15.addSuppressed(var12);
                           }

                           throw var15;
                        }

                        bis.close();
                     } catch (Throwable var16) {
                        if (rs != null) {
                           try {
                              rs.close();
                           } catch (Throwable var11) {
                              var16.addSuppressed(var11);
                           }
                        }

                        throw var16;
                     }

                     if (rs != null) {
                        rs.close();
                     }
                     break label129;
                  }

                  if (rs != null) {
                     rs.close();
                  }
               } catch (Throwable var17) {
                  if (stmt != null) {
                     try {
                        stmt.close();
                     } catch (Throwable var10) {
                        var17.addSuppressed(var10);
                     }
                  }

                  throw var17;
               }

               if (stmt != null) {
                  stmt.close();
               }

               return new ItemStack[54];
            }

            if (stmt != null) {
               stmt.close();
            }

            return var8;
         } catch (IOException | ClassNotFoundException | SQLException var18) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to load inventory for " + uuid, var18);
            return new ItemStack[54];
         }
      }
   }

   public synchronized void deleteInventory(UUID uuid) {
      if (uuid != null) {
         String query = "DELETE FROM inventories WHERE uuid = ?";
         String lastAccessQuery = "DELETE FROM last_access WHERE uuid = ?";

         try {
            PreparedStatement stmt = this.connection.prepareStatement(query);

            try {
               PreparedStatement lastAccessStmt = this.connection.prepareStatement(lastAccessQuery);

               try {
                  this.connection.setAutoCommit(false);

                  try {
                     stmt.setString(1, uuid.toString());
                     lastAccessStmt.setString(1, uuid.toString());
                     stmt.executeUpdate();
                     lastAccessStmt.executeUpdate();
                     this.connection.commit();
                  } catch (SQLException var17) {
                     this.connection.rollback();
                     throw var17;
                  } finally {
                     this.connection.setAutoCommit(true);
                  }
               } catch (Throwable var19) {
                  if (lastAccessStmt != null) {
                     try {
                        lastAccessStmt.close();
                     } catch (Throwable var16) {
                        var19.addSuppressed(var16);
                     }
                  }

                  throw var19;
               }

               if (lastAccessStmt != null) {
                  lastAccessStmt.close();
               }
            } catch (Throwable var20) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var15) {
                     var20.addSuppressed(var15);
                  }
               }

               throw var20;
            }

            if (stmt != null) {
               stmt.close();
            }

         } catch (SQLException var21) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to delete inventory for " + uuid, var21);
            throw new RuntimeException("Failed to delete inventory", var21);
         }
      }
   }

   private void updateLastAccess(UUID uuid) {
      String query = "INSERT OR REPLACE INTO last_access (uuid, last_access) VALUES (?, CURRENT_TIMESTAMP)";

      try {
         PreparedStatement stmt = this.connection.prepareStatement(query);

         try {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
         } catch (Throwable var7) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (stmt != null) {
            stmt.close();
         }
      } catch (SQLException var8) {
         this.plugin.getLogger().log(Level.SEVERE, "Failed to update last access for " + uuid, var8);
      }

   }

   public UUID hasInventory(String playerName) {
      String query = "SELECT uuid FROM inventories WHERE player_name = ?";

      try {
         PreparedStatement stmt = this.connection.prepareStatement(query);

         label78: {
            UUID var5;
            try {
               stmt.setString(1, playerName);
               ResultSet rs = stmt.executeQuery();

               label80: {
                  try {
                     if (rs.next()) {
                        var5 = UUID.fromString(rs.getString("uuid"));
                        break label80;
                     }
                  } catch (Throwable var9) {
                     if (rs != null) {
                        try {
                           rs.close();
                        } catch (Throwable var8) {
                           var9.addSuppressed(var8);
                        }
                     }

                     throw var9;
                  }

                  if (rs != null) {
                     rs.close();
                  }
                  break label78;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var10) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var7) {
                     var10.addSuppressed(var7);
                  }
               }

               throw var10;
            }

            if (stmt != null) {
               stmt.close();
            }

            return var5;
         }

         if (stmt != null) {
            stmt.close();
         }
      } catch (SQLException var11) {
         this.plugin.getLogger().log(Level.SEVERE, "Failed to check inventory existence for " + playerName, var11);
      }

      return null;
   }

   public Set<String> getStoredPlayerNames() {
      Set<String> playerNames = new HashSet();
      String query = "SELECT player_name FROM inventories WHERE player_name IS NOT NULL";

      try {
         Statement stmt = this.connection.createStatement();

         try {
            ResultSet rs = stmt.executeQuery(query);

            try {
               while(rs.next()) {
                  String playerName = rs.getString("player_name");
                  if (playerName != null && !playerName.isEmpty()) {
                     playerNames.add(playerName);
                  }
               }
            } catch (Throwable var9) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var10) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (stmt != null) {
            stmt.close();
         }
      } catch (SQLException var11) {
         this.plugin.getLogger().log(Level.SEVERE, "Failed to get stored player names", var11);
      }

      return playerNames;
   }

   public void closeConnection() {
      try {
         if (this.connection != null && !this.connection.isClosed()) {
            this.connection.close();
         }
      } catch (SQLException var2) {
         this.plugin.getLogger().log(Level.SEVERE, "Failed to close database connection", var2);
      }

   }
}
