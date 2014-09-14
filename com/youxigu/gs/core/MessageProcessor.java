/*    */ package com.youxigu.gs.core;
/*    */ 
/*    */ import org.slf4j.Logger;
/*    */ import org.slf4j.LoggerFactory;
/*    */ 
/*    */ public final class MessageProcessor
/*    */   implements Runnable
/*    */ {
/*  7 */   private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);
/*  8 */   private static int processorIDCount = 0;
/*    */ 
/* 10 */   private int processorID = processorIDCount++;
/* 11 */   private boolean starting = false;
/* 12 */   private boolean running = false;
/* 13 */   private boolean exitFlag = false;
/*    */   private Gate gate;
/*    */   private Thread thread;
/*    */ 
/*    */   public MessageProcessor(Gate gate)
/*    */   {
/* 20 */     this.gate = gate;
/*    */   }
/*    */ 
/*    */   public boolean isRunning() {
/* 24 */     return this.running;
/*    */   }
/*    */ 
/*    */   public boolean init() {
/* 28 */     if (((this.running ? 0 : 1) & (this.starting ? 0 : 1)) != 0) {
/* 29 */       this.starting = true;
/* 30 */       this.exitFlag = false;
/* 31 */       this.thread = new Thread(this, "Message processor " + this.gate.getName() + "#" + this.processorID);
/* 32 */       this.thread.setDaemon(true);
/* 33 */       this.thread.start();
/*    */     }
/* 35 */     return true;
/*    */   }
/*    */ 
/*    */   public void end() {
/* 39 */     this.exitFlag = true;
/* 40 */     wakeUp();
/*    */   }
/*    */ 
/*    */   public void run() {
/* 44 */     this.running = true;
/* 45 */     this.starting = false;
/* 46 */     while (!this.exitFlag) {
/*    */       try {
/* 48 */         processMessages();
/*    */         try {
/* 50 */           Thread.sleep(0L, 10);
/*    */         } catch (InterruptedException localInterruptedException) {
/*    */         }
/*    */       } catch (Throwable e) {
/* 54 */         logger.error("Error processing messages for gate " + this.gate.getName(), e);
/* 55 */         end();
/*    */       }
/*    */     }
/* 58 */     this.running = false;
/*    */   }
/*    */ 
/*    */   private void processMessages() {
/* 62 */     Connection conn = null;
/* 63 */     while ((conn = this.gate.getConnectionsWithMessagesForProcessId(this.processorID)) != null)
/* 64 */       conn.processInputMessages();
/*    */   }
/*    */ 
/*    */   private synchronized void wakeUp()
/*    */   {
/* 69 */     notify();
/*    */   }
/*    */ }

/* Location:           C:\Users\yyyyyy\Desktop\youxigu\jjjjjjjjj.jar
 * Qualified Name:     com.youxigu.gs.core.MessageProcessor
 * JD-Core Version:    0.6.0
 */