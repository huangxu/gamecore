/*     */ package com.youxigu.gs.core;
/*     */ 
/*     */ import java.net.InetAddress;
/*     */ import java.net.InetSocketAddress;
/*     */ import java.net.ServerSocket;
/*     */ import java.net.UnknownHostException;
/*     */ import java.nio.channels.ServerSocketChannel;
/*     */ import org.slf4j.Logger;
/*     */ import org.slf4j.LoggerFactory;
/*     */ 
/*     */ public final class Server
/*     */ {
/*  16 */   private static final Logger logger = LoggerFactory.getLogger(Server.class);
/*     */   private static final String VERSION = "1.0";
/*     */   public static final String ENCODING = "UTF-8";
/*     */   private InetAddress localHost;
/*     */   private Gate clientGate;
/*     */   public static final int STATUS_DEFAULT = 0;
/*     */   public static final int STATUS_STARTING = 1;
/*     */   public static final int STATUS_RUNNING = 2;
/*     */   public static final int STATUS_STOPPING = 3;
/*     */   public static final int STATUS_STOPPED = 4;
/*  30 */   private int status = 0;
/*     */ 
/*  32 */   protected boolean running = false;
/*  33 */   protected boolean failed = false;
/*  34 */   protected boolean finished = false;
/*     */ 
/*  36 */   protected Service service = null;
/*     */ 
/*     */   public void start()
/*     */   {
/*  42 */     logger.info("Running Youxigu game server, control thread started");
/*  43 */     this.finished = false;
/*     */     try {
/*  45 */       runInternal();
/*     */     } catch (Exception e) {
/*  47 */       logger.error("Error executing Youxigu game server", e);
/*     */ 
/*  49 */       stopServer();
/*     */     }
/*  51 */     this.finished = true;
/*  52 */     this.status = 4;
/*  53 */     logger.info("Youxigu game server control thread stopped");
/*     */   }
/*     */ 
/*     */   private void runInternal() {
/*  57 */     boolean failed = !startServer();
/*  58 */     if (failed) {
/*  59 */       stopServer();
/*  60 */       return;
/*     */     }
/*     */ 
/*  63 */     this.running = true;
/*  64 */     this.status = 2;
/*  65 */     while (this.running) {
/*     */       try {
/*  67 */         synchronized (this) {
/*  68 */           wait();
/*     */         }
/*     */       } catch (InterruptedException e) {
/*  71 */         e.printStackTrace();
/*     */       }
/*     */     }
/*     */ 
/*  75 */     stopServer();
/*     */   }
/*     */ 
/*     */   private boolean startServer() {
/*  79 */     logger.info("Starting up Youxigu game server...");
/*  80 */     logger.info("Step #1: Acquiring configuration for this server...");
/*     */ 
/*  86 */     if (!acquireServerConfiguration()) {
/*  87 */       return false;
/*     */     }
/*     */ 
/*  90 */     logger.info("Step 2: Checking port...");
/*  91 */     if (!isPortAvaliable()) {
/*  92 */       return false;
/*     */     }
/*  94 */     logger.info("Step #3: Preparing services...");
/*  95 */     if (!instanceServices()) {
/*  96 */       return false;
/*     */     }
/*     */ 
/*  99 */     if (!checkServices()) {
/* 100 */       return false;
/*     */     }
/*     */ 
/* 103 */     logger.info("Step #4: Starting services...");
/* 104 */     if (!startService()) {
/* 105 */       return false;
/*     */     }
/*     */ 
/* 108 */     logger.info("Step #5: Opening gates...");
/* 109 */     prepareGates();
/*     */ 
/* 111 */     return openGates();
/*     */   }
/*     */ 
/*     */   private boolean isPortAvaliable()
/*     */   {
/* 118 */     boolean bRet = false;
/* 119 */     ServerSocketChannel serverChannel = null;
/*     */     try {
/* 121 */       serverChannel = ServerSocketChannel.open();
/* 122 */       InetSocketAddress iddr = null;
/* 123 */       if (Configuration.getInstance().getClientBindAddress() == null)
/* 124 */         iddr = new InetSocketAddress(Configuration.getInstance()
/* 125 */           .getClientPort());
/*     */       else {
/* 127 */         iddr = new InetSocketAddress(Configuration.getInstance()
/* 128 */           .getClientBindAddress(), Configuration.getInstance()
/* 129 */           .getClientPort());
/*     */       }
/* 131 */       serverChannel.socket().bind(iddr);
/* 132 */       serverChannel.configureBlocking(false);
/*     */ 
/* 134 */       if (serverChannel != null) {
/* 135 */         serverChannel.close();
/*     */       }
/* 137 */       bRet = true;
/*     */     } catch (Exception e) {
/* 139 */       bRet = false;
/* 140 */       logger.error("", e);
/*     */       try
/*     */       {
/* 143 */         if (serverChannel != null)
/* 144 */           serverChannel.close();
/*     */       }
/*     */       catch (Exception e) {
/* 147 */         logger.error("", e);
/*     */       }
/*     */     }
/*     */     finally
/*     */     {
/*     */       try
/*     */       {
/* 143 */         if (serverChannel != null)
/* 144 */           serverChannel.close();
/*     */       }
/*     */       catch (Exception e) {
/* 147 */         logger.error("", e);
/*     */       }
/*     */     }
/* 150 */     return bRet;
/*     */   }
/*     */ 
/*     */   private boolean checkServices() {
/* 154 */     return this.service != null;
/*     */   }
/*     */ 
/*     */   private boolean startService() {
/* 158 */     if (this.service == null) {
/* 159 */       logger.warn("There aren't services defined for this server!");
/* 160 */       return false;
/*     */     }
/*     */     try
/*     */     {
/* 164 */       if ((!this.service.isRunning()) && 
/* 165 */         (!this.service.init())) {
/* 166 */         logger.error("Could not start services. Status is: " + this.service.isRunning());
/* 167 */         if (this.service.isRunning())
/* 168 */           this.service.end();
/* 169 */         return false;
/*     */       }
/*     */     }
/*     */     catch (Exception e) {
/* 173 */       e.printStackTrace(System.err);
/* 174 */       logger.error("Could not start services. Status is: " + this.service.isRunning(), e);
/* 175 */       return false;
/*     */     }
/*     */ 
/* 178 */     return true;
/*     */   }
/*     */ 
/*     */   private void stopService() {
/* 182 */     if ((this.service != null) && (this.service.isRunning()))
/* 183 */       this.service.end();
/*     */   }
/*     */ 
/*     */   public Service getService()
/*     */   {
/* 188 */     return this.service;
/*     */   }
/*     */ 
/*     */   private boolean instanceServices() {
/* 192 */     String serviceName = Configuration.getInstance().getServiceName();
/* 193 */     if (serviceName == null) {
/* 194 */       logger.error("No services are defined for this server");
/* 195 */       return false;
/*     */     }
/*     */     try {
/* 198 */       Object o = Class.forName(serviceName).newInstance();
/* 199 */       if ((o instanceof Service)) {
/* 200 */         this.service = ((Service)o);
/*     */       } else {
/* 202 */         logger.error("Class " + this.service + 
/* 203 */           " is not a proper service (it does not implement 'ServiceInterface'");
/* 204 */         return false;
/*     */       }
/*     */     }
/*     */     catch (Exception e) {
/* 208 */       logger.warn("Can't instantiate " + this.service + ": " + e.getClass().getCanonicalName());
/* 209 */       if (logger.isDebugEnabled()) {
/* 210 */         logger.debug("Cant instantiate " + this.service, e);
/*     */       }
/*     */ 
/*     */     }
/*     */ 
/* 216 */     return true;
/*     */   }
/*     */ 
/*     */   private void stopServer() {
/* 220 */     logger.info("Shutting down Youxigu game server...");
/*     */ 
/* 222 */     logger.info("Step #1: Stopping services...");
/* 223 */     stopService();
/*     */ 
/* 225 */     logger.info("Step #2: Closing gates...");
/* 226 */     closeGates();
/*     */   }
/*     */ 
/*     */   public boolean acquireLocalhost() {
/*     */     try {
/* 231 */       this.localHost = InetAddress.getLocalHost();
/* 232 */       return true;
/*     */     } catch (UnknownHostException e) {
/* 234 */       logger.error("Can't get localhost address {}", e);
/* 235 */     }return false;
/*     */   }
/*     */ 
/*     */   public boolean acquireServerConfiguration()
/*     */   {
/* 240 */     if (Configuration.getInstance().getServerIP() == null)
/*     */     {
/* 242 */       Configuration.getInstance().setServerIP("127.0.0.1");
/*     */     }
/* 244 */     logger.info("Server IP is " + Configuration.getInstance().getServerIP());
/*     */ 
/* 246 */     return true;
/*     */   }
/*     */ 
/*     */   private void prepareGates() {
/* 250 */     this.clientGate = new Gate(this, "server");
/* 251 */     logger.info("Server's readBufferSize is " + Configuration.getInstance().getReadBufferSize());
/* 252 */     logger.info("Server's writeBufferSize is " + Configuration.getInstance().getWriteBufferSize());
/*     */   }
/*     */ 
/*     */   private boolean openGates() {
/* 256 */     return openClientGate();
/*     */   }
/*     */ 
/*     */   private void closeGates() {
/* 260 */     closeClientGate();
/*     */   }
/*     */ 
/*     */   public boolean openClientGate() {
/* 264 */     return prepareGate(this.clientGate);
/*     */   }
/*     */ 
/*     */   public void closeClientGate() {
/* 268 */     closeGate(this.clientGate);
/*     */   }
/*     */ 
/*     */   private boolean prepareGate(Gate gate) {
/* 272 */     if (Gate.isRunning()) {
/* 273 */       logger.warn("Can't open " + gate.getName() + " gate. It's already oppened.");
/* 274 */       return true;
/*     */     }
/*     */ 
/* 277 */     if (!gate.init()) {
/* 278 */       logger.error("Error opening " + gate.getName() + " gate");
/* 279 */       return false;
/*     */     }
/* 281 */     return Gate.isRunning();
/*     */   }
/*     */ 
/*     */   private void closeGate(Gate gate) {
/* 285 */     if (gate == null) {
/* 286 */       return;
/*     */     }
/* 288 */     if (!Gate.isRunning()) {
/* 289 */       logger.warn("Can't close " + gate.getName() + " gate. It isn't oppened.");
/* 290 */       return;
/*     */     }
/*     */ 
/* 293 */     gate.end();
/*     */   }
/*     */ 
/*     */   public boolean init() {
/* 297 */     this.status = 1;
/* 298 */     logger.info("Init Youxigu game server v1.0");
/* 299 */     Runtime.getRuntime().addShutdownHook(new Shutdown());
/* 300 */     start();
/* 301 */     return true;
/*     */   }
/*     */ 
/*     */   public void end() {
/* 305 */     if (this.status == 2) {
/* 306 */       logger.info("Ending Youxigu game server v1.0");
/* 307 */       this.status = 3;
/* 308 */       this.running = false;
/* 309 */       synchronized (this) {
/* 310 */         notifyAll();
/*     */       }
/*     */     }
/* 313 */     logger.warn("You can't stop a server that is not running!");
/*     */   }
/*     */ 
/*     */   public boolean haveFailed()
/*     */   {
/* 318 */     return this.failed;
/*     */   }
/*     */ 
/*     */   public boolean isRunning() {
/* 322 */     return this.running;
/*     */   }
/*     */ 
/*     */   public Gate getClientGate() {
/* 326 */     return this.clientGate;
/*     */   }
/*     */ 
/*     */   private void killServer() {
/* 330 */     logger.info("Starting kill server...");
/* 331 */     end();
/* 332 */     while (!this.finished)
/*     */       try {
/* 334 */         Thread.sleep(1000L);
/*     */       }
/*     */       catch (InterruptedException localInterruptedException) {
/*     */       }
/* 338 */     logger.info("Kill server done!");
/*     */   }
/*     */ 
/*     */   public int getStatus()
/*     */   {
/* 351 */     return this.status;
/*     */   }
/*     */ 
/*     */   public class Shutdown extends Thread
/*     */   {
/*     */     public Shutdown()
/*     */     {
/*     */     }
/*     */ 
/*     */     public void run()
/*     */     {
/* 345 */       Server.this.killServer();
/*     */     }
/*     */   }
/*     */ }

/* Location:           C:\Users\yyyyyy\Desktop\youxigu\jjjjjjjjj.jar
 * Qualified Name:     com.youxigu.gs.core.Server
 * JD-Core Version:    0.6.0
 */