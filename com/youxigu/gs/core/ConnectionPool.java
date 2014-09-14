/*    */ package com.youxigu.gs.core;
/*    */ 
/*    */ import com.youxigu.gs.pool.ObjectPool;
/*    */ 
/*    */ public final class ConnectionPool
/*    */ {
/* 12 */   private static final ConnectionPool instance = new ConnectionPool();
/*    */ 
/* 14 */   private ObjectPool<Connection> pool = null;
/*    */ 
/*    */   public void initConnectionPool(int size, int readBufferSize, int writeBufferSize, boolean headerInSize)
/*    */   {
/* 23 */     if (this.pool == null) {
/* 24 */       this.pool = new ObjectPool();
/* 25 */       this.pool.initPoolSize(size);
/* 26 */       for (int i = 0; i < size; i++) {
/* 27 */         Connection con = new Connection();
/* 28 */         con.init(readBufferSize, writeBufferSize, headerInSize);
/* 29 */         this.pool.addObject(con);
/*    */       }
/*    */     }
/*    */   }
/*    */ 
/*    */   public Connection getConnection() {
/*    */     try {
/* 36 */       return (Connection)this.pool.borrow();
/*    */     } catch (Exception e) {
/* 38 */       e.printStackTrace();
/*    */     }
/* 40 */     return null;
/*    */   }
/*    */ 
/*    */   public void back(Connection con) {
/*    */     try {
/* 45 */       this.pool.back(con);
/*    */     } catch (Exception e) {
/* 47 */       e.printStackTrace();
/*    */     }
/*    */   }
/*    */ 
/*    */   public static ConnectionPool getInstance() {
/* 52 */     return instance;
/*    */   }
/*    */ }

/* Location:           C:\Users\yyyyyy\Desktop\youxigu\jjjjjjjjj.jar
 * Qualified Name:     com.youxigu.gs.core.ConnectionPool
 * JD-Core Version:    0.6.0
 */