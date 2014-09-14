/*     */ package com.youxigu.gs.core;
/*     */ 
/*     */ import com.youxigu.gs.message.Message;
/*     */ import com.youxigu.gs.tool.SafeSubscriptionManager;
/*     */ import java.io.IOException;
/*     */ import java.net.InetAddress;
/*     */ import java.net.InetSocketAddress;
/*     */ import java.net.ServerSocket;
/*     */ import java.net.Socket;
/*     */ import java.nio.ByteBuffer;
/*     */ import java.nio.channels.CancelledKeyException;
/*     */ import java.nio.channels.ClosedChannelException;
/*     */ import java.nio.channels.SelectionKey;
/*     */ import java.nio.channels.Selector;
/*     */ import java.nio.channels.ServerSocketChannel;
/*     */ import java.nio.channels.SocketChannel;
/*     */ import java.util.Collection;
/*     */ import java.util.Iterator;
/*     */ import java.util.Map;
/*     */ import java.util.Set;
/*     */ import java.util.concurrent.ArrayBlockingQueue;
/*     */ import java.util.concurrent.ConcurrentHashMap;
/*     */ import java.util.concurrent.ThreadPoolExecutor;
/*     */ import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
/*     */ import java.util.concurrent.TimeUnit;
/*     */ import org.slf4j.Logger;
/*     */ import org.slf4j.LoggerFactory;
/*     */ 
/*     */ public final class Gate
/*     */   implements Runnable
/*     */ {
/*  27 */   private static final Logger logger = LoggerFactory.getLogger(Gate.class);
/*     */   private Thread thread;
/*     */   private Server server;
/*     */   private String name;
/*     */   private Selector gateSelector;
/*  36 */   private boolean exitFlag = false;
/*     */   private ServerSocketChannel serverChannel;
/*  40 */   private static Map<Integer, Connection> clients = new ConcurrentHashMap();
/*     */   private MessageProcessor[] processors;
/*  44 */   private ConcurrentHashMap<Integer, Connection>[] connectionsWithMessagesToProcess = null;
/*     */   private static int messageProcessorWorkers;
/*     */   private int threadpoolMin;
/*     */   private int threadpoolMax;
/*     */   private int threadpoolArraySize;
/*     */   private SafeSubscriptionManager<Connection> nioRead;
/*     */   ThreadPoolExecutor threadPool;
/* 331 */   private static final ByteBuffer OVERFLOW = ByteBuffer.allocate(6).putShort(4).putShort(9473).putShort(999);
/*     */ 
/* 477 */   private static boolean running = false;
/*     */ 
/* 492 */   private boolean failed = false;
/*     */ 
/*     */   static {
/* 495 */     OVERFLOW.flip();
/*     */   }
/*     */ 
/*     */   public static int getMessageProcessorWorkers()
/*     */   {
/*  49 */     return messageProcessorWorkers;
/*     */   }
/*     */ 
/*     */   public Gate(Server server, String name)
/*     */   {
/*  61 */     ConnectionPool.getInstance().initConnectionPool(Configuration.getInstance().getConnectionPoolSize(), 
/*  62 */       Configuration.getInstance().getReadBufferSize(), Configuration.getInstance().getWriteBufferSize(), 
/*  63 */       Configuration.getInstance().isHeaderInSize());
/*  64 */     this.name = name;
/*  65 */     this.server = server;
/*  66 */     reload();
/*     */   }
/*     */ 
/*     */   private void reload() {
/*  70 */     int numOfCPU = Runtime.getRuntime().availableProcessors();
/*  71 */     messageProcessorWorkers = 1;
/*  72 */     this.threadpoolMin = 1;
/*  73 */     this.threadpoolMax = 1;
/*  74 */     if (numOfCPU == 4) {
/*  75 */       messageProcessorWorkers = 3;
/*  76 */       this.threadpoolMin = 2;
/*  77 */       this.threadpoolMax = 6;
/*  78 */     } else if (numOfCPU == 8) {
/*  79 */       messageProcessorWorkers = 4;
/*  80 */       this.threadpoolMin = 4;
/*  81 */       this.threadpoolMax = 8;
/*  82 */     } else if (numOfCPU == 2) {
/*  83 */       messageProcessorWorkers = 2;
/*  84 */       this.threadpoolMin = 1;
/*  85 */       this.threadpoolMax = 1;
/*     */     }
/*  87 */     this.threadpoolArraySize = 200;
/*     */   }
/*     */ 
/*     */   public boolean init() {
/*  91 */     this.failed = false;
/*  92 */     this.threadPool = 
/*  95 */       new ThreadPoolExecutor(this.threadpoolMin, this.threadpoolMax, 3L, 
/*  93 */       TimeUnit.SECONDS, 
/*  94 */       new ArrayBlockingQueue(this.threadpoolArraySize), 
/*  95 */       new ThreadPoolExecutor.CallerRunsPolicy());
/*  96 */     this.threadPool.setThreadFactory(DaemonThreadFactory.Singleton);
/*     */     try
/*     */     {
/*  99 */       if (!running)
/*     */       {
/* 109 */         if (!startMessageProcessors()) {
/* 110 */           logger.error("Can't start message processors for gate " + 
/* 111 */             this.name);
/* 112 */           this.failed = true;
/* 113 */           end();
/*     */         }
/*     */ 
/* 116 */         openServerChannel();
/* 117 */         startAccept();
/*     */ 
/* 119 */         this.thread = new Thread(this, "Gate " + this.name + " accept queue thread");
/* 120 */         this.thread.start();
/*     */         do {
/*     */           try {
/* 123 */             Thread.sleep(50L);
/*     */           }
/*     */           catch (InterruptedException localInterruptedException)
/*     */           {
/*     */           }
/* 121 */           if (this.failed) break; 
/* 121 */         }while (!running);
/*     */       }
/*     */ 
/*     */     }
/*     */     catch (Exception e)
/*     */     {
/* 129 */       logger.error("Can't start gate " + this.name + " on " + (
/* 130 */         Configuration.getInstance().getClientBindAddress() == null ? "" : Configuration.getInstance().getClientBindAddress().toString()) + ":" + 
/* 131 */         Configuration.getInstance().getClientPort(), e);
/* 132 */       this.failed = true;
/*     */     }
/* 134 */     return !this.failed;
/*     */   }
/*     */ 
/*     */   private boolean startMessageProcessors()
/*     */   {
/* 139 */     this.processors = new MessageProcessor[messageProcessorWorkers];
/* 140 */     this.connectionsWithMessagesToProcess = new ConcurrentHashMap[this.processors.length];
/* 141 */     for (int i = 0; i < this.processors.length; i++) {
/* 142 */       this.processors[i] = new MessageProcessor(this);
/* 143 */       this.connectionsWithMessagesToProcess[i] = new ConcurrentHashMap(Configuration.getInstance().getConnectionPoolSize() * 2);
/*     */     }
/*     */ 
/* 146 */     for (int i = 0; i < this.processors.length; i++) {
/* 147 */       if ((this.processors[i].isRunning()) || 
/* 148 */         (this.processors[i].init())) continue;
/* 149 */       logger.error("Could not start message processors for gate " + this.name);
/* 150 */       if (this.processors[i].isRunning()) {
/* 151 */         this.processors[i].end();
/*     */       }
/*     */     }
/*     */ 
/* 155 */     return true;
/*     */   }
/*     */ 
/*     */   public void end() {
/* 159 */     if (running)
/*     */     {
/*     */       try {
/* 162 */         closeServerChannel();
/*     */       }
/*     */       catch (IOException localIOException) {
/*     */       }
/*     */       try {
/* 167 */         closeAllConnections();
/*     */ 
/* 169 */         this.threadPool.shutdownNow();
/*     */         try {
/* 171 */           if (!this.threadPool.awaitTermination(10L, TimeUnit.SECONDS))
/* 172 */             logger.error("gate threadPool not shutdown!!!");
/*     */         }
/*     */         catch (InterruptedException e) {
/* 175 */           logger.error("Gate threadPool Interuppted", e);
/*     */         }
/*     */ 
/* 178 */         if (this.processors != null)
/* 179 */           for (int i = 0; i < this.processors.length; i++)
/* 180 */             if (this.processors[i].isRunning())
/* 181 */               this.processors[i].end();
/*     */       }
/*     */       catch (Exception e)
/*     */       {
/* 185 */         logger.error("Gate end has exception", e);
/*     */       } finally {
/* 187 */         this.exitFlag = true;
/* 188 */         this.gateSelector.wakeup();
/*     */ 
/* 190 */         running = false;
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */   private void closeAllConnections() {
/* 196 */     int size = clients.size();
/* 197 */     if (size > 0) {
/* 198 */       logger.info("Disconnecting " + size + " connections...");
/* 199 */       for (Connection c : clients.values())
/* 200 */         c.disconnectChannel();
/*     */     }
/*     */   }
/*     */ 
/*     */   private void openServerChannel() throws IOException
/*     */   {
/* 206 */     this.serverChannel = ServerSocketChannel.open();
/* 207 */     InetSocketAddress iddr = null;
/* 208 */     if (Configuration.getInstance().getClientBindAddress() == null)
/* 209 */       iddr = new InetSocketAddress(Configuration.getInstance().getClientPort());
/*     */     else {
/* 211 */       iddr = new InetSocketAddress(Configuration.getInstance().getClientBindAddress(), Configuration.getInstance().getClientPort());
/*     */     }
/* 213 */     this.serverChannel.socket().bind(iddr);
/* 214 */     this.serverChannel.configureBlocking(false);
/*     */   }
/*     */ 
/*     */   private void startAccept() throws IOException {
/* 218 */     this.gateSelector = Selector.open();
/* 219 */     this.serverChannel.register(this.gateSelector, 16);
/* 220 */     this.nioRead = new SafeSubscriptionManager(this.gateSelector);
/*     */   }
/*     */ 
/*     */   private void closeServerChannel() throws IOException {
/* 224 */     if (this.serverChannel != null) {
/* 225 */       this.serverChannel.register(this.gateSelector, 0);
/* 226 */       this.serverChannel.close();
/*     */     }
/*     */   }
/*     */ 
/*     */   public void run() {
/* 231 */     if (this.gateSelector == null) {
/* 232 */       logger.error("Gate '" + this.name + 
/* 233 */         "' cant start: selector is not defined (null)");
/* 234 */       this.failed = true;
/* 235 */       return;
/*     */     }
/*     */ 
/* 238 */     running = true;
/* 239 */     this.exitFlag = false;
/* 240 */     logger.info("Gate '" + this.name + "' listenning on " + (
/* 241 */       Configuration.getInstance().getClientBindAddress() != null ? Configuration.getInstance().getClientBindAddress().getHostName().trim() : "") + ':' + Configuration.getInstance().getClientPort());
/* 242 */     while (!this.exitFlag) {
/*     */       try {
/* 244 */         Set readyKeys = this.nioRead.waitForOperations();
/* 245 */         if (readyKeys != null) {
/* 246 */           Iterator i = readyKeys.iterator();
/* 247 */           while (i.hasNext()) {
/* 248 */             SelectionKey key = (SelectionKey)i.next();
/* 249 */             i.remove();
/*     */ 
/* 251 */             if ((key.isValid()) && (key.isAcceptable()))
/*     */             {
/* 253 */               accept(key);
/*     */             }
/*     */ 
/* 256 */             if ((key.isValid()) && (key.isWritable()))
/*     */             {
/* 258 */               write(key);
/*     */             }
/*     */ 
/* 261 */             if ((!key.isValid()) || (!key.isReadable()))
/*     */               continue;
/* 263 */             read(key);
/*     */           }
/*     */         }
/*     */       }
/*     */       catch (IOException ioe) {
/* 268 */         logger.warn("Gate " + this.name + " IOError", ioe);
/* 269 */         this.failed = true;
/*     */       } catch (CancelledKeyException localCancelledKeyException) {
/*     */       }
/*     */       catch (Exception e) {
/* 273 */         e.printStackTrace();
/* 274 */         logger.error("Gate " + this.name + ", unexpected exception thrown", e);
/* 275 */         this.failed = true;
/*     */       }
/*     */     }
/*     */ 
/* 279 */     logger.info("Gate '" + this.name + "' closed");
/* 280 */     running = false;
/*     */   }
/*     */ 
/*     */   public void read(SelectionKey key) {
/* 284 */     Connection connection = (Connection)key.attachment();
/* 285 */     this.threadPool.execute(connection);
/*     */   }
/*     */ 
/*     */   public void write(SelectionKey key) {
/* 289 */     Connection connection = (Connection)key.attachment();
/* 290 */     this.threadPool.execute(connection.getWriter());
/*     */   }
/*     */ 
/*     */   public Connection getConnectionsWithMessagesForProcessId(int processorID)
/*     */   {
/* 322 */     Iterator it = this.connectionsWithMessagesToProcess[processorID].values().iterator();
/* 323 */     if (it.hasNext()) {
/* 324 */       Connection c = (Connection)it.next();
/* 325 */       return (Connection)this.connectionsWithMessagesToProcess[processorID].remove(Integer.valueOf(c.getSessionID()));
/*     */     }
/* 327 */     return null;
/*     */   }
/*     */ 
/*     */   public void accept(SelectionKey key)
/*     */     throws IOException, ClosedChannelException
/*     */   {
/* 337 */     ServerSocketChannel ssChannel = (ServerSocketChannel)key.channel();
/* 338 */     SocketChannel clientChannel = ssChannel.accept();
/* 339 */     if (clientChannel == null) {
/* 340 */       return;
/*     */     }
/* 342 */     clientChannel.configureBlocking(false);
/* 343 */     clientChannel.socket().setTcpNoDelay(true);
/*     */ 
/* 345 */     Connection connection = ConnectionPool.getInstance().getConnection();
/* 346 */     if (connection == null) {
/* 347 */       logger.error("Connection pool is empty!");
/* 348 */       OVERFLOW.rewind();
/* 349 */       clientChannel.write(OVERFLOW);
/* 350 */       clientChannel.close();
/* 351 */       return;
/*     */     }
/* 353 */     connection.setup(this, clientChannel);
/* 354 */     this.nioRead.addConnection(connection, clientChannel, 1);
/* 355 */     logger.debug("Gate " + this.name + ": New connection from " + clientChannel.socket().getInetAddress() + " -> #" + connection.getSessionID());
/* 356 */     registerConnection(connection);
/* 357 */     connection.pushMessage(new Message(connection, 33));
/* 358 */     notifyMessagesPendingToProcess(connection);
/*     */   }
/*     */ 
/*     */   public void doAccept(SelectionKey key) throws IOException, ClosedChannelException {
/* 362 */     ServerSocketChannel ssChannel = (ServerSocketChannel)key.channel();
/* 363 */     SocketChannel clientChannel = ssChannel.accept();
/* 364 */     if (clientChannel == null) {
/* 365 */       return;
/*     */     }
/* 367 */     clientChannel.configureBlocking(false);
/* 368 */     clientChannel.socket().setTcpNoDelay(true);
/*     */ 
/* 370 */     Connection connection = ConnectionPool.getInstance().getConnection();
/* 371 */     if (connection == null) {
/* 372 */       OVERFLOW.rewind();
/* 373 */       clientChannel.write(OVERFLOW);
/* 374 */       clientChannel.close();
/* 375 */       return;
/*     */     }
/* 377 */     connection.setup(this, clientChannel);
/* 378 */     this.nioRead.addConnection(connection, clientChannel, 1);
/* 379 */     logger.debug("Gate " + this.name + ": New connection from " + clientChannel.socket().getInetAddress() + " -> #" + connection.getSessionID());
/* 380 */     registerConnection(connection);
/* 381 */     connection.pushMessage(new Message(connection, 33));
/* 382 */     notifyMessagesPendingToProcess(connection);
/*     */   }
/*     */ 
/*     */   public void registerConnection(Connection connection) {
/* 386 */     clients.put(Integer.valueOf(connection.getSessionID()), connection);
/*     */   }
/*     */ 
/*     */   public void removeConnection(Connection con) {
/* 390 */     this.connectionsWithMessagesToProcess[con.getProcessorId()].remove(Integer.valueOf(con.getSessionID()));
/* 391 */     clients.remove(Integer.valueOf(con.getSessionID()));
/* 392 */     ConnectionPool.getInstance().back(con);
/*     */   }
/*     */ 
/*     */   public String getName() {
/* 396 */     return this.name;
/*     */   }
/*     */ 
/*     */   private void decodeMessage(Message message)
/*     */   {
/* 404 */     MessageFilter filter = Configuration.getInstance().getMessageFilter();
/* 405 */     if ((filter != null) && (message.getMsgType() == 0))
/* 406 */       message.setData(filter.decrypt(message));
/*     */   }
/*     */ 
/*     */   public void processUp(Message message) {
/* 410 */     Connection connection = message.getConnection();
/* 411 */     if (message.getMsgType() == 31) {
/* 412 */       logger.info(connection.getSessionID() + " force to disconnect!");
/* 413 */       connection.disconnectWhenAllSent();
/* 414 */       return;
/*     */     }
/* 416 */     connection.addToOutputQueue(message);
/*     */   }
/*     */ 
/*     */   public void processDown(Message message) {
/* 420 */     Service service = this.server.getService();
/*     */     try {
/* 422 */       if (!message.isPushed()) {
/* 423 */         decodeMessage(message);
/*     */       }
/* 425 */       service.processDown(message);
/* 426 */       if (message.getMsgType() == 32)
/* 427 */         message.getConnection().disconnectChannel();
/*     */     } catch (Exception e) {
/* 429 */       logger.error("Error processing message in service " + 
/* 430 */         service.getClass().getName() + 
/* 431 */         ". THIS IS A FATAL ERROR FOR THIS SERVICE! Message is: " + 
/* 432 */         message, e);
/*     */     }
/*     */   }
/*     */ 
/*     */   public void addDownMessage(Message message) {
/* 437 */     processDown(message);
/*     */   }
/*     */ 
/*     */   public Server getServer() {
/* 441 */     return this.server;
/*     */   }
/*     */ 
/*     */   public static Connection getConnectionBySessionId(int session_id) {
/* 445 */     Connection c = (Connection)clients.get(Integer.valueOf(session_id));
/* 446 */     return c;
/*     */   }
/*     */ 
/*     */   public void addUpMessage(Message message) {
/* 450 */     processUp(message);
/*     */   }
/*     */ 
/*     */   public void notifyChannelDisconnectionFrom(Connection connection) {
/* 454 */     this.nioRead.removeConnection(connection);
/*     */   }
/*     */ 
/*     */   public void notifyMessagesPendingToProcess(Connection connection) {
/* 458 */     this.connectionsWithMessagesToProcess[connection.getProcessorId()].put(Integer.valueOf(connection.getSessionID()), connection);
/*     */   }
/*     */ 
/*     */   public void notifyDataToSend(Connection connection) {
/* 462 */     if (connection.isConnected())
/* 463 */       this.nioRead.addOperations(connection, 4);
/*     */   }
/*     */ 
/*     */   public void notifyNoMoreDataToSend(Connection connection)
/*     */   {
/* 468 */     if (connection.isConnected())
/* 469 */       this.nioRead.removeOperations(connection, 4);
/*     */   }
/*     */ 
/*     */   public boolean testEndCondition(int conditionID)
/*     */   {
/* 474 */     return !running;
/*     */   }
/*     */ 
/*     */   public String toString()
/*     */   {
/* 481 */     return "Gate['" + this.name + "']";
/*     */   }
/*     */ 
/*     */   public static boolean isRunning() {
/* 485 */     return running;
/*     */   }
/*     */ 
/*     */   public boolean haveFailed() {
/* 489 */     return this.failed;
/*     */   }
/*     */ 
/*     */   final class AcceptWorker
/*     */     implements Runnable
/*     */   {
/*     */     private SelectionKey key;
/*     */     private Gate gate;
/*     */ 
/*     */     public AcceptWorker(Gate gate, SelectionKey key)
/*     */     {
/* 299 */       this.gate = gate;
/* 300 */       this.key = key;
/*     */     }
/*     */ 
/*     */     public void run()
/*     */     {
/*     */       try {
/* 306 */         this.gate.doAccept(this.key);
/*     */       } catch (ClosedChannelException e) {
/* 308 */         Gate.logger.error("Gate " + Gate.this.name + " ClosedChannelException " + e);
/* 309 */         Gate.this.failed = true;
/*     */       } catch (IOException e) {
/* 311 */         Gate.logger.error("Gate " + Gate.this.name + " IOException", e);
/* 312 */         Gate.this.failed = true;
/*     */       } catch (Exception e) {
/* 314 */         e.printStackTrace();
/* 315 */         Gate.logger.error("Gate " + Gate.this.name + ", unexpected exception thrown", e);
/* 316 */         Gate.this.failed = true;
/*     */       }
/*     */     }
/*     */   }
/*     */ }

/* Location:           C:\Users\yyyyyy\Desktop\youxigu\jjjjjjjjj.jar
 * Qualified Name:     com.youxigu.gs.core.Gate
 * JD-Core Version:    0.6.0
 */