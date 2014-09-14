/*    */ package com.youxigu.gs.util;
/*    */ 
/*    */ import java.io.File;
/*    */ import java.io.FileInputStream;
/*    */ import java.io.FileNotFoundException;
/*    */ import java.io.FilenameFilter;
/*    */ import java.io.IOException;
/*    */ import java.util.Enumeration;
/*    */ import java.util.Properties;
/*    */ import java.util.StringTokenizer;
/*    */ 
/*    */ public final class CfgToolbox
/*    */ {
/*    */   public static final Properties loadAllProperties(File directory, File directory2)
/*    */     throws IOException
/*    */   {
/* 15 */     if (!directory.exists())
/* 16 */       throw new IllegalArgumentException(directory + " not found");
/* 17 */     if (!directory.isDirectory()) {
/* 18 */       throw new IllegalArgumentException("Expected a directory, received " + directory);
/*    */     }
/*    */ 
/* 21 */     if (directory2 != null) {
/* 22 */       if (!directory2.exists())
/* 23 */         throw new IllegalArgumentException(directory2 + " not found");
/* 24 */       if (!directory2.isDirectory()) {
/* 25 */         throw new IllegalArgumentException(
/* 26 */           "Expected a directory2, received " + directory2);
/*    */       }
/*    */     }
/*    */ 
/* 30 */     Properties result = new Properties();
/* 31 */     File[] files1 = directory.listFiles(new FilenameFilter() {
/*    */       public boolean accept(File dir, String name) {
/* 33 */         return name.toLowerCase().endsWith(".properties");
/*    */       }
/*    */     });
/* 36 */     for (int i = 0; i < files1.length; i++) {
/* 37 */       loadProperties(result, files1[i]);
/*    */     }
/*    */ 
/* 40 */     if (directory2 != null) {
/* 41 */       File[] files2 = directory2.listFiles(new FilenameFilter() {
/*    */         public boolean accept(File dir, String name) {
/* 43 */           return name.toLowerCase().endsWith(".properties");
/*    */         }
/*    */       });
/* 46 */       for (int i = 0; i < files2.length; i++) {
/* 47 */         loadProperties(result, files2[i]);
/*    */       }
/*    */     }
/*    */ 
/* 51 */     return result;
/*    */   }
/*    */ 
/*    */   public static final void loadProperties(Properties props, File file) throws FileNotFoundException, IOException {
/* 55 */     FileInputStream fis = new FileInputStream(file);
/* 56 */     props.load(fis);
/* 57 */     fis.close();
/*    */   }
/*    */ 
/*    */   public static final Properties propertiesToLowerCase(Properties props) {
/* 61 */     Properties result = new Properties();
/* 62 */     Enumeration en = props.keys();
/* 63 */     while (en.hasMoreElements()) {
/* 64 */       String key = (String)en.nextElement();
/* 65 */       String value = props.getProperty(key);
/* 66 */       result.setProperty(key.toLowerCase(), value);
/*    */     }
/* 68 */     return result;
/*    */   }
/*    */ 
/*    */   public static final String[] stringToArrayOfStrings(String data) {
/* 72 */     StringTokenizer st = new StringTokenizer(data, ", ");
/* 73 */     String[] result = new String[st.countTokens()];
/* 74 */     for (int i = 0; i < result.length; i++) {
/* 75 */       result[i] = st.nextToken();
/*    */     }
/* 77 */     return result;
/*    */   }
/*    */ }

/* Location:           C:\Users\yyyyyy\Desktop\youxigu\jjjjjjjjj.jar
 * Qualified Name:     com.youxigu.gs.util.CfgToolbox
 * JD-Core Version:    0.6.0
 */