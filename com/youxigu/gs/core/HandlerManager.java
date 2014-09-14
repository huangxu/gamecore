/*    */ package com.youxigu.gs.core;
/*    */ 
/*    */ import java.util.HashMap;
/*    */ import java.util.Map;
/*    */ 
/*    */ public final class HandlerManager
/*    */ {
/* 14 */   public Handler[] Handlers = null;
/*    */ 
/* 21 */   private Map<Short, Handler> restricHandlers = new HashMap();
/*    */ 
/* 54 */   public static final HandlerManager Singleton = new HandlerManager();
/*    */ 
/*    */   public void setupHandler(int size)
/*    */   {
/* 17 */     if (this.Handlers == null)
/* 18 */       this.Handlers = new Handler[size];
/*    */   }
/*    */ 
/*    */   public boolean openHandler(short type)
/*    */   {
/* 28 */     Handler handler = (Handler)this.restricHandlers.get(Short.valueOf(type));
/* 29 */     if (handler != null) {
/* 30 */       this.Handlers[type] = handler;
/* 31 */       this.restricHandlers.remove(Short.valueOf(type));
/* 32 */       return true;
/*    */     }
/* 34 */     return false;
/*    */   }
/*    */ 
/*    */   public boolean closeHandler(short type)
/*    */   {
/* 42 */     Handler handler = this.Handlers[type];
/* 43 */     if (handler != null) {
/* 44 */       this.restricHandlers.put(Short.valueOf(type), handler);
/* 45 */       this.Handlers[type] = null;
/* 46 */       return true;
/*    */     }
/* 48 */     return false;
/*    */   }
/*    */ }

/* Location:           C:\Users\yyyyyy\Desktop\youxigu\jjjjjjjjj.jar
 * Qualified Name:     com.youxigu.gs.core.HandlerManager
 * JD-Core Version:    0.6.0
 */