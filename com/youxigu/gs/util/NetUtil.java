/*    */ package com.youxigu.gs.util;
/*    */ 
/*    */ import java.net.InetAddress;
/*    */ 
/*    */ public final class NetUtil
/*    */ {
/*    */   public static final String inetAddressToIPString(InetAddress inet)
/*    */   {
/*  8 */     StringBuffer sb = new StringBuffer();
/*  9 */     sb.append(inet.getAddress()[0] & 0xFF);
/* 10 */     sb.append('.');
/* 11 */     sb.append(inet.getAddress()[1] & 0xFF);
/* 12 */     sb.append('.');
/* 13 */     sb.append(inet.getAddress()[2] & 0xFF);
/* 14 */     sb.append('.');
/* 15 */     sb.append(inet.getAddress()[3] & 0xFF);
/* 16 */     return sb.toString();
/*    */   }
/*    */ 
/*    */   public static final boolean pingHost(String host, int timeout)
/*    */   {
/*    */     try {
/* 22 */       InetAddress hostAddr = InetAddress.getByName(host);
/* 23 */       return hostAddr.isReachable(timeout);
/*    */     } catch (Exception e) {
/* 25 */       e.printStackTrace();
/*    */     }
/*    */ 
/* 28 */     return false;
/*    */   }
/*    */ 
/*    */   public static final boolean pingHost(String host) {
/* 32 */     return pingHost(host, 10000);
/*    */   }
/*    */ }

/* Location:           C:\Users\yyyyyy\Desktop\youxigu\jjjjjjjjj.jar
 * Qualified Name:     com.youxigu.gs.util.NetUtil
 * JD-Core Version:    0.6.0
 */