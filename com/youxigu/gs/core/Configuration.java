/*     */ package com.youxigu.gs.core;
/*     */ 
/*     */ import com.youxigu.gs.util.ByteUtil;
/*     */ import com.youxigu.gs.util.CfgToolbox;
/*     */ import java.io.File;
/*     */ import java.io.FileInputStream;
/*     */ import java.io.FileNotFoundException;
/*     */ import java.io.IOException;
/*     */ import java.net.InetAddress;
/*     */ import java.util.Enumeration;
/*     */ import java.util.Properties;
/*     */ import org.apache.log4j.xml.DOMConfigurator;
/*     */ 
/*     */ public final class Configuration
/*     */ {
/*  18 */   private String prefix = "";
/*     */   private String serverIP;
/*  26 */   private InetAddress clientBindAddress = null;
/*     */   private static File filePath;
/*     */   private static File filePath2;
/*     */   private String service;
/*     */   private static Properties properties;
/*     */   private int readBufferSize;
/*     */   private int writeBufferSize;
/*     */   private int connectionPoolSize;
/*     */   private int writeHeaderSize;
/*     */   private int readHeaderSize;
/*  46 */   private String confPath = "";
/*     */ 
/*  48 */   private boolean headerInSize = false;
/*     */ 
/*  50 */   private boolean httpServer = true;
/*     */   private byte[] relocateUrl;
/*  84 */   private String responseData = "HTTP/1.1 200 OK\r\nContent-type: text/html\r\nContent-Length: 28\r\n\r\n{\r\n\"ret\":0,\r\n\"msg\":\"OK\"\r\n}\r\n";
/*     */ 
/*  90 */   private String crossDomain = "<?xml version=\"1.0\"?><cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"*\" /></cross-domain-policy>";
/*     */   private MessageFilter messageFilter;
/* 357 */   private static final Configuration instance = new Configuration();
/*     */ 
/*     */   public String getPrefix()
/*     */   {
/*  21 */     return this.prefix;
/*     */   }
/*     */ 
/*     */   public boolean isHttpServer()
/*     */   {
/*  75 */     return this.httpServer;
/*     */   }
/*     */ 
/*     */   public byte[] getRelocateUrl()
/*     */   {
/*  81 */     return this.relocateUrl;
/*     */   }
/*     */ 
/*     */   public String getResponseData()
/*     */   {
/*  87 */     return this.responseData;
/*     */   }
/*     */ 
/*     */   public String getCrossDomain()
/*     */   {
/*  94 */     return this.crossDomain;
/*     */   }
/*     */ 
/*     */   public void setCrossDomain(String crossDomain) {
/*  98 */     this.crossDomain = crossDomain;
/*     */   }
/*     */ 
/*     */   public boolean isHeaderInSize() {
/* 102 */     return this.headerInSize;
/*     */   }
/*     */ 
/*     */   public String getConfPath() {
/* 106 */     return this.confPath;
/*     */   }
/*     */ 
/*     */   public void setConfPath(String xmlPath) {
/* 110 */     this.confPath = xmlPath;
/*     */   }
/*     */ 
/*     */   public int getReadHeaderSize() {
/* 114 */     return this.readHeaderSize;
/*     */   }
/*     */ 
/*     */   public void setReadHeaderSize(int readHeaderSize) {
/* 118 */     this.readHeaderSize = readHeaderSize;
/*     */   }
/*     */ 
/*     */   public int getWriteHeaderSize() {
/* 122 */     return this.writeHeaderSize;
/*     */   }
/*     */ 
/*     */   public int getConnectionPoolSize() {
/* 126 */     return this.connectionPoolSize;
/*     */   }
/*     */ 
/*     */   public int getReadBufferSize() {
/* 130 */     return this.readBufferSize;
/*     */   }
/*     */ 
/*     */   public int getWriteBufferSize() {
/* 134 */     return this.writeBufferSize;
/*     */   }
/*     */ 
/*     */   public MessageFilter getMessageFilter()
/*     */   {
/* 143 */     return this.messageFilter;
/*     */   }
/*     */ 
/*     */   public void setMessageFilter(MessageFilter messageFilter) {
/* 147 */     this.messageFilter = messageFilter;
/*     */   }
/*     */ 
/*     */   public void setup(File directory, File directory2, String prefix) throws IOException {
/* 151 */     filePath = directory;
/* 152 */     if (directory2 != null)
/* 153 */       filePath2 = directory2;
/*     */     else {
/* 155 */       filePath2 = filePath;
/*     */     }
/* 157 */     this.prefix = prefix;
/* 158 */     properties = CfgToolbox.loadAllProperties(directory, directory2);
/*     */ 
/* 160 */     this.service = properties.getProperty(prefix + "services");
/* 161 */     this.readBufferSize = Integer.parseInt(properties.getProperty(prefix + "readBufferSize").trim());
/* 162 */     this.writeBufferSize = Integer.parseInt(properties.getProperty(prefix + "writeBufferSize").trim());
/* 163 */     this.connectionPoolSize = Integer.parseInt(properties.getProperty(prefix + "connectionPoolSize").trim());
/* 164 */     if (properties.getProperty(prefix + "writeHeaderSize") != null)
/* 165 */       this.writeHeaderSize = Integer.parseInt(properties.getProperty(prefix + "writeHeaderSize").trim());
/*     */     else {
/* 167 */       this.writeHeaderSize = 2;
/*     */     }
/* 169 */     if (properties.getProperty(prefix + "readHeaderSize") != null)
/* 170 */       this.readHeaderSize = Integer.parseInt(properties.getProperty(prefix + "readHeaderSize").trim());
/*     */     else {
/* 172 */       this.readHeaderSize = 2;
/*     */     }
/* 174 */     this.confPath = properties.getProperty(prefix + "path");
/* 175 */     if ((this.confPath == null) || (this.confPath.isEmpty())) {
/* 176 */       this.confPath = ".";
/*     */     }
/*     */ 
/* 179 */     String insize = properties.getProperty(prefix + "headerInsize");
/* 180 */     if ((insize == null) || (insize.isEmpty()))
/* 181 */       this.headerInSize = false;
/*     */     else {
/* 183 */       this.headerInSize = Boolean.parseBoolean(insize.trim());
/*     */     }
/*     */ 
/* 186 */     String http = properties.getProperty(prefix + "isHttpServer");
/* 187 */     if ((http == null) || (http.isEmpty()))
/* 188 */       this.httpServer = false;
/*     */     else {
/* 190 */       this.httpServer = Boolean.parseBoolean(http.trim());
/*     */     }
/*     */ 
/* 193 */     String messageFilterConf = properties.getProperty(prefix + "messageFilter");
/* 194 */     if (messageFilterConf != null) {
/*     */       try {
/* 196 */         this.messageFilter = ((MessageFilter)Class.forName(messageFilterConf.trim()).newInstance());
/*     */       } catch (InstantiationException e) {
/* 198 */         e.printStackTrace();
/*     */       } catch (IllegalAccessException e) {
/* 200 */         e.printStackTrace();
/*     */       } catch (ClassNotFoundException e) {
/* 202 */         e.printStackTrace();
/*     */       }
/*     */     }
/*     */ 
/* 206 */     String xml = properties.getProperty("crossDomain");
/* 207 */     if (xml != null)
/* 208 */       this.crossDomain = xml.trim();
/*     */     String urlFilePath;
/*     */     String urlFilePath;
/* 216 */     if (directory2 != null)
/* 217 */       urlFilePath = directory2.getPath();
/*     */     else {
/* 219 */       urlFilePath = directory.getPath();
/*     */     }
/* 221 */     this.relocateUrl = loadUrlRelocate(urlFilePath);
/*     */ 
/* 223 */     loadSlf4jConfig();
/*     */   }
/*     */ 
/*     */   private byte[] loadUrlRelocate(String path) {
/* 227 */     byte[] data = null;
/*     */     try {
/* 229 */       FileInputStream stream = new FileInputStream(path + "/relocate.txt");
/* 230 */       data = ByteUtil.readAllData(stream);
/*     */     } catch (FileNotFoundException e) {
/* 232 */       e.printStackTrace();
/*     */     } catch (IOException e) {
/* 234 */       e.printStackTrace();
/*     */     }
/* 236 */     return data;
/*     */   }
/*     */ 
/*     */   private void loadSlf4jConfig() {
/* 240 */     String filePathStr = filePath2.getPath();
/* 241 */     if (!filePathStr.endsWith("/")) {
/* 242 */       filePathStr = filePathStr + "/";
/*     */     }
/* 244 */     String slf4jPath = filePathStr + this.prefix + "log4j.xml";
/*     */     try {
/* 246 */       DOMConfigurator.configure(slf4jPath);
/*     */     } catch (Exception e) {
/* 248 */       e.printStackTrace();
/*     */     }
/*     */   }
/*     */ 
/*     */   public static File getFilePath()
/*     */   {
/* 263 */     return filePath;
/*     */   }
/*     */ 
/*     */   public static File getFilePath2() {
/* 267 */     return filePath2;
/*     */   }
/*     */ 
/*     */   public int getClientPort()
/*     */   {
/* 286 */     return Integer.parseInt(properties.getProperty(this.prefix + "client_port").trim());
/*     */   }
/*     */ 
/*     */   public InetAddress getClientBindAddress() {
/* 290 */     return this.clientBindAddress;
/*     */   }
/*     */ 
/*     */   public void setClientBindAddress(InetAddress clientBindAddress) {
/* 294 */     this.clientBindAddress = clientBindAddress;
/*     */   }
/*     */ 
/*     */   public String getServerIP() {
/* 298 */     return this.serverIP;
/*     */   }
/*     */ 
/*     */   public void setServerIP(String serverIP) {
/* 302 */     this.serverIP = serverIP;
/*     */   }
/*     */ 
/*     */   public String getServiceName() {
/* 306 */     return this.service;
/*     */   }
/*     */ 
/*     */   public Properties getLowercaseConfigurationFor(String prefix) {
/* 310 */     return getConfigurationForSection(prefix, properties, false);
/*     */   }
/*     */ 
/*     */   public Properties getUppercaseConfigurationFor(String prefix) {
/* 314 */     return getConfigurationForSection(prefix, properties, false);
/*     */   }
/*     */ 
/*     */   public Properties getConfigurationFor(String prefix) {
/* 318 */     return getConfigurationForSection(prefix, properties);
/*     */   }
/*     */ 
/*     */   public static Properties getConfigurationForSection(String prefix, Properties properties) {
/* 322 */     prefix = prefix.toLowerCase() + "-";
/* 323 */     Properties result = new Properties();
/* 324 */     Enumeration en = properties.keys();
/* 325 */     while (en.hasMoreElements()) {
/* 326 */       String key = (String)en.nextElement();
/* 327 */       if (key.startsWith(prefix)) {
/* 328 */         result.setProperty(key.substring(prefix.length()), properties.getProperty(key));
/*     */       }
/*     */     }
/* 331 */     return result;
/*     */   }
/*     */ 
/*     */   public static final Properties getConfigurationForSection(String prefix, Properties properties, boolean upperCase) {
/* 335 */     prefix = prefix.toLowerCase() + "_";
/* 336 */     Properties result = new Properties();
/* 337 */     Enumeration en = properties.keys();
/* 338 */     while (en.hasMoreElements()) {
/* 339 */       String key = (String)en.nextElement();
/* 340 */       String value = properties.getProperty(key);
/* 341 */       if (upperCase)
/* 342 */         key = key.toUpperCase();
/*     */       else {
/* 344 */         key = key.toLowerCase();
/*     */       }
/* 346 */       if (key.startsWith(prefix)) {
/* 347 */         result.setProperty(key.substring(prefix.length()), value);
/*     */       }
/*     */     }
/* 350 */     return result;
/*     */   }
/*     */ 
/*     */   public static Properties getProperties() {
/* 354 */     return properties;
/*     */   }
/*     */ 
/*     */   public static Configuration getInstance()
/*     */   {
/* 360 */     return instance;
/*     */   }
/*     */ }

/* Location:           C:\Users\yyyyyy\Desktop\youxigu\jjjjjjjjj.jar
 * Qualified Name:     com.youxigu.gs.core.Configuration
 * JD-Core Version:    0.6.0
 */