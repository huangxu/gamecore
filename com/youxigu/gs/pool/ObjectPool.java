/*    */ package com.youxigu.gs.pool;
/*    */ 
/*    */ import java.util.HashMap;
/*    */ import java.util.LinkedList;
/*    */ import java.util.Queue;
/*    */ import java.util.concurrent.locks.ReentrantLock;
/*    */ 
/*    */ public final class ObjectPool<T extends IObject>
/*    */ {
/* 15 */   private HashMap<T, T> objects = null;
/*    */ 
/* 17 */   private Queue<T> unUsedObjects = null;
/*    */ 
/* 19 */   private final ReentrantLock lockObj = new ReentrantLock();
/*    */ 
/*    */   public T borrow()
/*    */     throws Exception
/*    */   {
/* 28 */     this.lockObj.lock();
/*    */     try {
/* 30 */       IObject obj = (IObject)this.unUsedObjects.poll();
/* 31 */       if (obj != null) {
/* 32 */         this.objects.put(obj, obj);
/* 33 */         IObject localIObject1 = obj;
/*    */         return localIObject1;
/*    */       }
/* 35 */       throw new Exception("Not enought Object in Pool , unUsedSize=" + this.unUsedObjects.size() + ", usedSize=" + this.objects.size());
/*    */     }
/*    */     finally {
/* 38 */       this.lockObj.unlock();
/* 39 */     }throw localObject;
/*    */   }
/*    */ 
/*    */   public void back(T obj)
/*    */     throws Exception
/*    */   {
/* 49 */     obj.reset();
/* 50 */     this.lockObj.lock();
/*    */     try {
/* 52 */       IObject removed = (IObject)this.objects.remove(obj);
/* 53 */       if (removed != null)
/* 54 */         this.unUsedObjects.add(obj);
/*    */       else
/* 56 */         throw new Exception("No object in use pool , unUsedSize=" + this.unUsedObjects.size() + ", usedSize=" + this.objects.size());
/*    */     } finally {
/* 58 */       this.lockObj.unlock(); } this.lockObj.unlock();
/*    */   }
/*    */ 
/*    */   public boolean addObject(T obj)
/*    */   {
/* 68 */     this.lockObj.lock();
/*    */     try {
/* 70 */       if (obj != null) {
/* 71 */         boolean bool = this.unUsedObjects.add(obj);
/*    */         return bool;
/*    */       }
/*    */       return false;
/*    */     } finally {
/* 75 */       this.lockObj.unlock();
/* 76 */     }throw localObject;
/*    */   }
/*    */ 
/*    */   public void initPoolSize(int size)
/*    */   {
/* 84 */     if (this.objects == null) {
/* 85 */       this.objects = new HashMap(size);
/* 86 */       this.unUsedObjects = new LinkedList();
/*    */     }
/*    */   }
/*    */ 
/*    */   public int getUnusedSize() {
/* 91 */     return this.unUsedObjects.size();
/*    */   }
/*    */ 
/*    */   public int getUsedSize() {
/* 95 */     return this.objects.size();
/*    */   }
/*    */ }

/* Location:           C:\Users\yyyyyy\Desktop\youxigu\jjjjjjjjj.jar
 * Qualified Name:     com.youxigu.gs.pool.ObjectPool
 * JD-Core Version:    0.6.0
 */