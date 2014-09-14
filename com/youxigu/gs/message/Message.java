/*    */ package com.youxigu.gs.message;
/*    */ 
/*    */ import com.youxigu.gs.core.Connection;
/*    */ 
/*    */ public final class Message
/*    */ {
/*    */   public static final byte GAME_MESSAGE = 0;
/*    */   public static final byte S2S_MESSAGE = 1;
/*    */   public static final byte TYPE_GATE_KILL_CONNECTION = 31;
/*    */   public static final byte USER_DISCONNECTED = 32;
/*    */   public static final byte USER_CONNECTED = 33;
/*    */   public static final byte HTTP_REQUEST = 34;
/* 19 */   private Connection connection = null;
/*    */ 
/* 21 */   private byte[] data = null;
/*    */ 
/* 23 */   private byte msgType = 0;
/*    */ 
/* 25 */   private boolean pushed = false;
/*    */ 
/*    */   public boolean isPushed() {
/* 28 */     return this.pushed;
/*    */   }
/*    */ 
/*    */   public void setPushed(boolean pushed) {
/* 32 */     this.pushed = pushed;
/*    */   }
/*    */ 
/*    */   public byte getMsgType() {
/* 36 */     return this.msgType;
/*    */   }
/*    */ 
/*    */   public Message(Connection connection, byte type) {
/* 40 */     this.connection = connection;
/* 41 */     this.msgType = type;
/*    */   }
/*    */ 
/*    */   public Message(Connection connection, byte[] data) {
/* 45 */     this.connection = connection;
/* 46 */     this.data = data;
/*    */   }
/*    */ 
/*    */   public byte[] getData() {
/* 50 */     return this.data;
/*    */   }
/*    */ 
/*    */   public void setData(byte[] data) {
/* 54 */     this.data = data;
/*    */   }
/*    */ 
/*    */   public Connection getConnection() {
/* 58 */     return this.connection;
/*    */   }
/*    */ }

/* Location:           C:\Users\yyyyyy\Desktop\youxigu\jjjjjjjjj.jar
 * Qualified Name:     com.youxigu.gs.message.Message
 * JD-Core Version:    0.6.0
 */