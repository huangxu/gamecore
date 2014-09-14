/*    */ package com.youxigu.gs.core;
/*    */ 
/*    */ import java.util.concurrent.ThreadFactory;
/*    */ 
/*    */ public class DaemonThreadFactory
/*    */   implements ThreadFactory
/*    */ {
/*  8 */   public static DaemonThreadFactory Singleton = new DaemonThreadFactory();
/*    */ 
/*    */   public Thread newThread(Runnable r)
/*    */   {
/* 12 */     Thread t = new Thread(r);
/* 13 */     t.setDaemon(true);
/* 14 */     return t;
/*    */   }
/*    */ }

/* Location:           C:\Users\yyyyyy\Desktop\youxigu\jjjjjjjjj.jar
 * Qualified Name:     com.youxigu.gs.core.DaemonThreadFactory
 * JD-Core Version:    0.6.0
 */