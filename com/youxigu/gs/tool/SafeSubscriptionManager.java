/*     */ package com.youxigu.gs.tool;
/*     */ 
/*     */ import com.youxigu.gs.core.Connection;
/*     */ import java.io.IOException;
/*     */ import java.nio.channels.CancelledKeyException;
/*     */ import java.nio.channels.ClosedChannelException;
/*     */ import java.nio.channels.ClosedSelectorException;
/*     */ import java.nio.channels.SelectableChannel;
/*     */ import java.nio.channels.SelectionKey;
/*     */ import java.nio.channels.Selector;
/*     */ import java.nio.channels.ServerSocketChannel;
/*     */ import java.nio.channels.SocketChannel;
/*     */ import java.nio.channels.spi.AbstractSelectableChannel;
/*     */ import java.util.Collection;
/*     */ import java.util.Iterator;
/*     */ import java.util.Set;
/*     */ import java.util.concurrent.ConcurrentHashMap;
/*     */ import java.util.concurrent.ConcurrentLinkedQueue;
/*     */ import org.slf4j.Logger;
/*     */ import org.slf4j.LoggerFactory;
/*     */ 
/*     */ public final class SafeSubscriptionManager<T>
/*     */ {
/*  24 */   private static final Logger logger = LoggerFactory.getLogger(SafeSubscriptionManager.class);
/*     */   private Selector selector;
/*     */   private ConcurrentHashMap<Integer, SafeSubscriptionManager<T>.Subscription<T>> subscriptions;
/*     */   private ConcurrentLinkedQueue<SafeSubscriptionManager<T>.Subscription<T>> newSubscriptions;
/*     */   private ConcurrentLinkedQueue<SafeSubscriptionManager<T>.Subscription<T>> cancelledSubscriptions;
/*     */ 
/*     */   public SafeSubscriptionManager()
/*     */     throws IOException
/*     */   {
/*  33 */     this.selector = Selector.open();
/*  34 */     this.subscriptions = new ConcurrentHashMap();
/*  35 */     this.newSubscriptions = new ConcurrentLinkedQueue();
/*  36 */     this.cancelledSubscriptions = new ConcurrentLinkedQueue();
/*     */   }
/*     */ 
/*     */   public SafeSubscriptionManager(Selector selector) {
/*  40 */     this.selector = selector;
/*  41 */     this.subscriptions = new ConcurrentHashMap();
/*  42 */     this.newSubscriptions = new ConcurrentLinkedQueue();
/*  43 */     this.cancelledSubscriptions = new ConcurrentLinkedQueue();
/*     */   }
/*     */ 
/*     */   public Set<SelectionKey> waitForOperations() {
/*     */     try {
/*  48 */       updateOperations();
/*  49 */       this.selector.select();
/*  50 */       return this.selector.selectedKeys();
/*     */     } catch (IOException e) {
/*  52 */       logger.error("Cant select ", e);
/*     */     } catch (ClosedSelectorException e) {
/*  54 */       logger.error("selector closed!", e);
/*     */     }
/*     */ 
/*  57 */     return null;
/*     */   }
/*     */ 
/*     */   private void updateOperations() {
/*  61 */     Iterator it = this.subscriptions.values().iterator();
/*  62 */     while (it.hasNext()) {
/*  63 */       Subscription s = (Subscription)it.next();
/*  64 */       if (!s.changed) continue;
/*     */       try {
/*  66 */         s.updateInterestedOps();
/*     */       }
/*     */       catch (CancelledKeyException e)
/*     */       {
/*  71 */         removeConnection(s.attachment);
/*     */       }
/*     */ 
/*     */     }
/*     */ 
/*  76 */     Subscription subs = null;
/*  77 */     while ((subs = (Subscription)this.newSubscriptions.poll()) != null) {
/*     */       try {
/*  79 */         subs.registerWithInterestedOps();
/*  80 */         int sid = ((Connection)subs.attachment).getSessionID();
/*  81 */         this.subscriptions.put(Integer.valueOf(sid), subs);
/*     */       }
/*     */       catch (ClosedChannelException e)
/*     */       {
/*  86 */         logger.warn("Connection " + subs.attachment + 
/*  87 */           " cancelled before having time to register in the selector. The connection will be ignored.");
/*     */       } catch (NullPointerException e) {
/*  89 */         if (subs.channel == null)
/*  90 */           logger.error("Channel went null while updating operations. I'll ignore it.");
/*     */         else {
/*  92 */           logger.error("Cant update operations", e);
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/*  97 */     while ((subs = (Subscription)this.cancelledSubscriptions.poll()) != null) {
/*  98 */       SelectionKey key = subs.channel.keyFor(this.selector);
/*  99 */       if (key != null) {
/* 100 */         key.cancel();
/*     */         try {
/* 102 */           if (key.channel().isOpen())
/* 103 */             key.channel().close();
/*     */         }
/*     */         catch (NullPointerException e) {
/* 106 */           logger.warn("", e);
/*     */         } catch (IOException e) {
/* 108 */           logger.warn("Can't close channel for " + key.attachment());
/*     */         }
/*     */       }
/* 111 */       int sid = ((Connection)subs.attachment).getSessionID();
/* 112 */       this.subscriptions.remove(Integer.valueOf(sid));
/*     */     }
/*     */   }
/*     */ 
/*     */   public void addConnection(T object, SocketChannel channel, int ops) {
/* 117 */     Subscription s = new Subscription(object, channel);
/* 118 */     s.set(ops);
/*     */ 
/* 120 */     this.newSubscriptions.add(s);
/*     */   }
/*     */ 
/*     */   public void removeConnection(T object)
/*     */   {
/* 137 */     int sid = ((Connection)object).getSessionID();
/* 138 */     Subscription s = (Subscription)this.subscriptions.get(Integer.valueOf(sid));
/* 139 */     if (s != null)
/*     */     {
/* 141 */       this.cancelledSubscriptions.add(s);
/*     */     }
/*     */   }
/*     */ 
/*     */   public void addOperations(T object, int operation)
/*     */   {
/* 148 */     int sid = ((Connection)object).getSessionID();
/* 149 */     Subscription subscription = (Subscription)this.subscriptions.get(Integer.valueOf(sid));
/* 150 */     if (subscription == null) {
/* 151 */       logger.warn("Trying to register operation " + operation + " in " + object + 
/* 152 */         " but the object is not registered!");
/* 153 */       return;
/*     */     }
/* 155 */     subscription.add(operation);
/*     */   }
/*     */ 
/*     */   public void removeOperations(T object, int operations)
/*     */   {
/* 169 */     int sid = ((Connection)object).getSessionID();
/* 170 */     Subscription s = (Subscription)this.subscriptions.get(Integer.valueOf(sid));
/* 171 */     if (s == null) {
/* 172 */       logger.warn("Trying to unregister operation(s) " + operations + " in " + object + 
/* 173 */         " but the object is not registered!");
/* 174 */       return;
/*     */     }
/* 176 */     s.remove(operations);
/*     */   }
/*     */ 
/*     */   public void wakeUp() {
/* 180 */     if (this.selector != null)
/* 181 */       this.selector.wakeup();
/*     */   }
/*     */ 
/*     */   public Selector getSelector()
/*     */   {
/* 270 */     return this.selector;
/*     */   }
/*     */ 
/*     */   public final class Subscription<K>
/*     */   {
/* 186 */     public int currentOps = 0;
/* 187 */     public int nextOps = 0;
/*     */     public K attachment;
/*     */     public AbstractSelectableChannel channel;
/* 192 */     public boolean server = false;
/*     */     public boolean changed;
/*     */ 
/*     */     public Subscription(SocketChannel attachment)
/*     */     {
/* 196 */       this.attachment = attachment;
/* 197 */       this.channel = channel;
/*     */     }
/*     */ 
/*     */     public void registerWithInterestedOps() throws ClosedChannelException {
/* 201 */       this.channel.register(SafeSubscriptionManager.this.selector, update(), this.attachment);
/*     */     }
/*     */ 
/*     */     public void updateInterestedOps() {
/* 205 */       SelectionKey sk = this.channel.keyFor(SafeSubscriptionManager.this.selector);
/* 206 */       if (sk != null)
/* 207 */         sk.interestOps(update());
/*     */     }
/*     */ 
/*     */     public Subscription(ServerSocketChannel attachment) {
/* 211 */       this.attachment = attachment;
/* 212 */       this.channel = channel;
/* 213 */       this.server = true;
/*     */     }
/*     */ 
/*     */     public void add(int operations) {
/* 217 */       this.nextOps |= operations;
/* 218 */       if (this.nextOps != this.currentOps)
/* 219 */         changed();
/*     */     }
/*     */ 
/*     */     public void remove(int operations)
/*     */     {
/* 224 */       this.nextOps &= (operations ^ 0xFFFFFFFF);
/* 225 */       if (this.nextOps != this.currentOps)
/* 226 */         changed();
/*     */     }
/*     */ 
/*     */     public void set(int ops)
/*     */     {
/* 231 */       if (this.currentOps != ops) {
/* 232 */         this.nextOps = ops;
/* 233 */         changed();
/*     */       }
/*     */     }
/*     */ 
/*     */     public int update() {
/* 238 */       this.changed = false;
/* 239 */       this.currentOps = this.nextOps;
/*     */ 
/* 243 */       return this.currentOps;
/*     */     }
/*     */ 
/*     */     private void changed() {
/* 247 */       this.changed = true;
/* 248 */       SafeSubscriptionManager.this.selector.wakeup();
/*     */     }
/*     */ 
/*     */     public String getFlags() {
/* 252 */       StringBuffer sb = new StringBuffer();
/* 253 */       if ((this.currentOps & 0x10) != 0) {
/* 254 */         sb.append("ACCEPT ");
/*     */       }
/* 256 */       if ((this.currentOps & 0x8) != 0) {
/* 257 */         sb.append("CONNECT ");
/*     */       }
/* 259 */       if ((this.currentOps & 0x1) != 0) {
/* 260 */         sb.append("READ ");
/*     */       }
/* 262 */       if ((this.currentOps & 0x4) != 0) {
/* 263 */         sb.append("WRITE ");
/*     */       }
/* 265 */       return sb.toString();
/*     */     }
/*     */   }
/*     */ }

/* Location:           C:\Users\yyyyyy\Desktop\youxigu\jjjjjjjjj.jar
 * Qualified Name:     com.youxigu.gs.tool.SafeSubscriptionManager
 * JD-Core Version:    0.6.0
 */