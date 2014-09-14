/*     */ package com.youxigu.gs.core;
/*     */ 
/*     */ import com.youxigu.gs.message.Message;
/*     */ import com.youxigu.gs.pool.IObject;
/*     */ import com.youxigu.gs.util.ByteUtil;
/*     */ import com.youxigu.gs.util.NetUtil;
/*     */ import java.net.InetAddress;
/*     */ import java.net.Socket;
/*     */ import java.nio.ByteBuffer;
/*     */ import java.nio.channels.SocketChannel;
/*     */ import java.util.concurrent.ConcurrentLinkedQueue;
/*     */ import java.util.concurrent.locks.ReentrantLock;
/*     */ import org.slf4j.Logger;
/*     */ import org.slf4j.LoggerFactory;
/*     */ 
/*     */ public final class Connection
/*     */   implements Runnable, IObject
/*     */ {
/*  18 */   private static final Logger logger = LoggerFactory.getLogger(Connection.class);
/*  19 */   private static final Logger commLogger = LoggerFactory.getLogger("Connection.DATAFLOW");
/*     */   private static final int HEADER_SIZE = 2;
/*     */   private static final int HEADER_SIZE_32 = 4;
/*     */   private static final int READ_STATUS_HEADER = 1;
/*     */   private static final int READ_STATUS_MESSAGE = 2;
/*     */   private static final int READ_STATUS_IGNORE = 3;
/*     */   private static final int READ_STATUS_HTTP = 4;
/*  28 */   private static int SESSION_NO = 1;
/*     */ 
/*  30 */   private int processorId = 0;
/*     */   private Object object;
/*     */   protected Gate gate;
/*  36 */   protected int sessionId = SESSION_NO++;
/*     */   private SocketChannel channel;
/*  40 */   private boolean moreGumToChew = false;
/*     */ 
/*  42 */   protected int messagesReceived = 0;
/*     */ 
/*  44 */   private int message_size = 0;
/*     */ 
/*  46 */   private int bytesReaded = 0;
/*     */ 
/*  48 */   private ByteBuffer readBuffer = null;
/*     */ 
/*  50 */   private String httpData = new String();
/*     */ 
/*  52 */   private boolean httpRecOver = false;
/*     */ 
/*  54 */   private int readStatus = 1;
/*     */ 
/*  56 */   private boolean headerInSize = false;
/*     */ 
/*  58 */   private int decryptTimes = 0;
/*     */ 
/*  72 */   protected ConcurrentLinkedQueue<Message> messageInQueue = new ConcurrentLinkedQueue();
/*     */ 
/*  75 */   private Message actualWrittingMessage = null;
/*     */ 
/*  77 */   private ByteBuffer writeBuffer = null;
/*     */ 
/*  79 */   protected ConcurrentLinkedQueue<Message> messageOutQueue = new ConcurrentLinkedQueue();
/*     */ 
/*  82 */   private ReentrantLock readLock = new ReentrantLock();
/*  83 */   private ReentrantLock writeLock = new ReentrantLock();
/*  84 */   private ReentrantLock processLock = new ReentrantLock();
/*     */ 
/*  87 */   protected volatile boolean connected = false;
/*     */ 
/*  89 */   private boolean disconnectWhenDone = false;
/*     */ 
/*  91 */   private volatile boolean isKilled = false;
/*     */ 
/*  93 */   private String clientAddr = null;
/*     */ 
/*  95 */   private int clientPort = 0;
/*     */ 
/* 121 */   private Runnable writer = new WriteWorker();
/*     */ 
/*     */   public int getDecryptTimes()
/*     */   {
/*  61 */     return this.decryptTimes;
/*     */   }
/*     */ 
/*     */   public void incDecryptTimes() {
/*  65 */     this.decryptTimes += 1;
/*     */   }
/*     */ 
/*     */   public void setDecryptTimes(int decryptTimes) {
/*  69 */     this.decryptTimes = decryptTimes;
/*     */   }
/*     */ 
/*     */   public int getProcessorId()
/*     */   {
/*  98 */     return this.processorId;
/*     */   }
/*     */ 
/*     */   public void setProcessorId(int processorId) {
/* 102 */     this.processorId = processorId;
/*     */   }
/*     */ 
/*     */   public Object getObject() {
/* 106 */     return this.object;
/*     */   }
/*     */ 
/*     */   public void setObject(Object object) {
/* 110 */     this.object = object;
/*     */   }
/*     */ 
/*     */   public String getClientAddr() {
/* 114 */     return this.clientAddr;
/*     */   }
/*     */ 
/*     */   public int getClientPort() {
/* 118 */     return this.clientPort;
/*     */   }
/*     */ 
/*     */   public Runnable getWriter()
/*     */   {
/* 124 */     return this.writer;
/*     */   }
/*     */ 
/*     */   public void init(int readBufferSize, int writeBufferSize, boolean headerInSize) {
/* 128 */     if (this.readBuffer == null)
/* 129 */       this.readBuffer = ByteBuffer.allocate(readBufferSize);
/* 130 */     if (this.writeBuffer == null) {
/* 131 */       this.writeBuffer = ByteBuffer.allocate(writeBufferSize);
/* 132 */       this.writeBuffer.flip();
/*     */     }
/* 134 */     this.headerInSize = headerInSize;
/*     */   }
/*     */ 
/*     */   public void setup(Gate gate, SocketChannel channel) {
/* 138 */     this.gate = gate;
/* 139 */     this.channel = channel;
/* 140 */     if (channel != null) {
/* 141 */       this.clientAddr = channel.socket().getInetAddress().getHostAddress();
/* 142 */       this.clientPort = channel.socket().getPort();
/* 143 */       this.connected = true;
/* 144 */       commLogger.info("#" + this.sessionId + " CONNECTED");
/*     */     }
/*     */   }
/*     */ 
/*     */   public int getSessionID()
/*     */   {
/* 155 */     return this.sessionId;
/*     */   }
/*     */ 
/*     */   public void run() {
/* 159 */     this.readLock.lock();
/*     */     try {
/* 161 */       readData();
/*     */     } finally {
/* 163 */       this.readLock.unlock();
/*     */     }
/*     */   }
/*     */ 
/*     */   boolean readData()
/*     */   {
/* 174 */     if ((!this.connected) || (this.isKilled)) {
/* 175 */       return false;
/*     */     }
/*     */     try
/*     */     {
/* 179 */       aux = this.channel.read(this.readBuffer);
/*     */     }
/*     */     catch (Exception e)
/*     */     {
/*     */       int aux;
/* 181 */       logger.warn("Can't read data in " + this + ", disconnecting user... " + e.getClass().getName() + ": " + e.getMessage());
/* 182 */       ioError();
/* 183 */       return false;
/*     */     }
/*     */     int aux;
/* 185 */     if (aux == -1) {
/* 186 */       logger.info("Disconnection detected on " + this + " (-1 reveived from channel.read)");
/* 187 */       ioError();
/* 188 */       return false;
/*     */     }
/* 190 */     if (aux == 0) {
/* 191 */       return true;
/*     */     }
/*     */ 
/* 194 */     this.bytesReaded += aux;
/*     */ 
/* 196 */     if (logger.isDebugEnabled()) {
/* 197 */       commLogger.debug("#" + this.sessionId + " INPUT " + aux + " bytes");
/*     */     }
/*     */ 
/* 200 */     if (Configuration.getInstance().getReadHeaderSize() == 4)
/* 201 */       chew32();
/*     */     else {
/* 203 */       chew();
/*     */     }
/*     */ 
/* 206 */     return true;
/*     */   }
/*     */ 
/*     */   void readHttpData() {
/* 210 */     this.readBuffer.flip();
/* 211 */     String data = new String();
/*     */     while (true) {
/* 213 */       byte[] c = new byte[1];
/* 214 */       this.readBuffer.get(c[0]);
/* 215 */       if (c[0] == -1) break;
/* 216 */       data = data + c[0];
/*     */     }
/*     */ 
/* 221 */     logger.info("message:" + data);
/*     */   }
/*     */ 
/*     */   boolean writeData()
/*     */   {
/* 232 */     if (!this.connected) {
/* 233 */       return false;
/*     */     }
/*     */ 
/* 236 */     if (this.writeBuffer.hasRemaining()) {
/* 237 */       this.writeBuffer.compact();
/* 238 */       this.writeBuffer.flip();
/* 239 */       return writeChannel();
/*     */     }
/*     */ 
/* 242 */     this.actualWrittingMessage = null;
/*     */ 
/* 244 */     if ((this.messageOutQueue.isEmpty()) || (!this.connected)) {
/* 245 */       this.gate.notifyNoMoreDataToSend(this);
/* 246 */       if (this.disconnectWhenDone) {
/* 247 */         disconnectChannel();
/* 248 */         this.disconnectWhenDone = false;
/*     */       }
/* 250 */       return true;
/*     */     }
/*     */ 
/* 253 */     if (this.messageOutQueue.size() > 100) {
/* 254 */       logger.error("#" + this.sessionId + ", Outqueue size=" + this.messageOutQueue.size());
/*     */     }
/*     */ 
/* 257 */     while ((this.actualWrittingMessage = (Message)this.messageOutQueue.poll()) != null) {
/* 258 */       byte[] msgToSend = this.actualWrittingMessage.getData();
/* 259 */       this.writeBuffer.clear();
/* 260 */       while (msgToSend.length > this.writeBuffer.remaining()) {
/* 261 */         logger.info("Resizing write buffer to " + (this.writeBuffer.capacity() + 1024) + "...");
/* 262 */         this.writeBuffer.clear();
/* 263 */         this.writeBuffer = ByteBuffer.allocate(this.writeBuffer.capacity() + 1024);
/*     */       }
/* 265 */       this.writeBuffer.put(msgToSend);
/* 266 */       this.writeBuffer.flip();
/*     */ 
/* 268 */       if (!writeChannel()) {
/* 269 */         return false;
/*     */       }
/*     */     }
/* 272 */     return true;
/*     */   }
/*     */ 
/*     */   private boolean writeChannel()
/*     */   {
/*     */     try
/*     */     {
/* 294 */       if ((this.connected) && (this.writeBuffer.hasRemaining()) && (this.channel.isConnected())) {
/* 295 */         this.channel.write(this.writeBuffer);
/* 296 */         return !this.writeBuffer.hasRemaining();
/*     */       }
/* 298 */       return false;
/*     */     } catch (Exception e) {
/* 300 */       logger.info("Error writting data in " + this + ", disconnecting user... ", e.getMessage());
/* 301 */       if (!this.isKilled)
/* 302 */         ioError();
/*     */       else
/* 304 */         disconnectChannel(); 
/*     */     }
/* 305 */     return false;
/*     */   }
/*     */ 
/*     */   private void ioError()
/*     */   {
/* 329 */     this.connected = false;
/* 330 */     addToInputQueue(new Message(this, 32));
/*     */   }
/*     */ 
/*     */   protected void disconnectChannel() {
/* 334 */     this.connected = false;
/* 335 */     if (this.channel != null) {
/*     */       try {
/* 337 */         this.gate.notifyChannelDisconnectionFrom(this);
/* 338 */         commLogger.info("#" + this.sessionId + " DISCONNECTED");
/* 339 */         this.channel.close();
/*     */       }
/*     */       catch (Throwable localThrowable) {
/*     */       }
/* 343 */       if (this.actualWrittingMessage != null)
/*     */         try {
/* 345 */           this.messageOutQueue.add(this.actualWrittingMessage);
/* 346 */           this.actualWrittingMessage = null;
/*     */         }
/*     */         catch (Exception localException) {
/*     */         }
/* 350 */       this.readBuffer.clear();
/* 351 */       this.writeBuffer.clear();
/* 352 */       this.gate.removeConnection(this);
/*     */     }
/*     */   }
/*     */ 
/*     */   public boolean isConnected()
/*     */   {
/* 362 */     return this.connected;
/*     */   }
/*     */ 
/*     */   private void chew()
/*     */   {
/* 369 */     this.moreGumToChew = true;
/* 370 */     while (this.moreGumToChew)
/* 371 */       switch (this.readStatus) {
/*     */       case 1:
/* 373 */         chewReadHeader();
/* 374 */         break;
/*     */       case 2:
/* 377 */         chewReadData();
/* 378 */         break;
/*     */       case 3:
/* 381 */         this.readBuffer.clear();
/* 382 */         this.moreGumToChew = false;
/*     */ 
/* 384 */         logger.error("READ_STATUS_IGNORE on " + this + " data length = " + this.message_size);
/* 385 */         ioError();
/* 386 */         break;
/*     */       case 4:
/* 388 */         chewReadHttp();
/*     */       }
/*     */   }
/*     */ 
/*     */   private void chew32()
/*     */   {
/* 398 */     this.moreGumToChew = true;
/* 399 */     while (this.moreGumToChew)
/* 400 */       switch (this.readStatus) {
/*     */       case 1:
/* 402 */         chewReadHeader32();
/* 403 */         break;
/*     */       case 2:
/* 406 */         chewReadData32();
/* 407 */         break;
/*     */       case 3:
/* 410 */         this.readBuffer.clear();
/* 411 */         this.moreGumToChew = false;
/* 412 */         logger.error("READ_STATUS_IGNORE on " + this + " data length < 2 or message_size illegal !");
/* 413 */         ioError();
/* 414 */         break;
/*     */       case 4:
/* 416 */         chewReadHttp();
/*     */       }
/*     */   }
/*     */ 
/*     */   private void chewReadData()
/*     */   {
/* 427 */     if (this.bytesReaded >= this.message_size) {
/* 428 */       byte[] data = new byte[this.message_size];
/* 429 */       this.readBuffer.flip();
/* 430 */       this.readBuffer.get(data);
/* 431 */       if (logger.isDebugEnabled()) {
/* 432 */         commLogger.debug("#" + this.sessionId + " MESSAGE RECEIVED - " + this.message_size + " bytes message length");
/*     */       }
/* 434 */       if (data.length < 2) {
/* 435 */         this.readStatus = 3;
/*     */       } else {
/* 437 */         Message message = new Message(this, data);
/* 438 */         addInputMessage(message);
/* 439 */         this.readBuffer.compact();
/* 440 */         this.bytesReaded -= this.message_size;
/* 441 */         this.readStatus = 1;
/*     */       }
/*     */     } else {
/* 444 */       this.moreGumToChew = false;
/*     */     }
/*     */   }
/*     */ 
/*     */   private void chewReadData32()
/*     */   {
/* 453 */     if (this.bytesReaded >= this.message_size) {
/* 454 */       byte[] data = new byte[this.message_size];
/* 455 */       this.readBuffer.flip();
/* 456 */       this.readBuffer.get(data);
/* 457 */       commLogger.debug("#" + this.sessionId + " MESSAGE RECEIVED - " + this.message_size + " bytes message length");
/* 458 */       if (data.length < 2) {
/* 459 */         this.readStatus = 3;
/*     */       } else {
/* 461 */         Message message = new Message(this, data);
/* 462 */         addInputMessage(message);
/* 463 */         this.readBuffer.compact();
/* 464 */         this.bytesReaded -= this.message_size;
/* 465 */         this.readStatus = 1;
/*     */       }
/*     */     } else {
/* 468 */       this.moreGumToChew = false;
/*     */     }
/*     */   }
/*     */ 
/*     */   private void chewReadHttp()
/*     */   {
/* 474 */     if (!this.httpRecOver) {
/* 475 */       this.readBuffer.flip();
/* 476 */       String msg = new String();
/* 477 */       while (this.readBuffer.remaining() > 0) {
/* 478 */         byte[] c = new byte[1];
/* 479 */         this.readBuffer.get(c);
/* 480 */         if ((c[0] != 13) || (c[0] == 10) || (c[0] == -1)) {
/* 481 */           msg = msg + (char)c[0];
/*     */         } else {
/* 483 */           this.httpRecOver = true;
/* 484 */           break;
/*     */         }
/*     */       }
/* 487 */       this.httpData += msg;
/* 488 */       if (this.httpRecOver) {
/* 489 */         ByteBuffer buf = ByteBuffer.allocate(6 + this.httpData.length());
/* 490 */         buf.putInt(34);
/* 491 */         buf.putShort((short)this.httpData.getBytes().length);
/* 492 */         buf.put(this.httpData.getBytes());
/* 493 */         logger.info("httpData=" + this.httpData);
/* 494 */         Message message = new Message(this, buf.array());
/* 495 */         addInputMessage(message);
/* 496 */         this.httpData = "";
/*     */       } else {
/* 498 */         this.moreGumToChew = false;
/*     */       }
/*     */     }
/* 501 */     this.readStatus = 4;
/* 502 */     if (this.httpRecOver) {
/* 503 */       logger.info("#session id:" + this.sessionId + " ip:" + getClientAddr() + " process pay");
/*     */ 
/* 506 */       this.writeBuffer.clear();
/* 507 */       this.writeBuffer.put(Configuration.getInstance().getResponseData().getBytes());
/* 508 */       this.writeBuffer.flip();
/* 509 */       writeChannel();
/* 510 */       this.readStatus = 3;
/*     */     }
/*     */   }
/*     */ 
/*     */   private void chewReadHeader() {
/* 515 */     if (this.bytesReaded >= 2) {
/* 516 */       this.readBuffer.flip();
/* 517 */       if (this.headerInSize)
/* 518 */         this.message_size = (this.readBuffer.getShort() - 2);
/*     */       else {
/* 520 */         this.message_size = this.readBuffer.getShort();
/*     */       }
/* 522 */       this.readBuffer.compact();
/*     */ 
/* 525 */       if ((this.message_size == 18244) && (this.message_size > this.readBuffer.capacity())) {
/* 526 */         this.readBuffer.flip();
/* 527 */         this.message_size = this.readBuffer.get();
/* 528 */         this.bytesReaded -= 2;
/* 529 */         this.bytesReaded -= 1;
/* 530 */         this.readStatus = 2;
/*     */ 
/* 532 */         if (this.bytesReaded >= this.message_size) {
/* 533 */           byte[] data = new byte[this.message_size];
/* 534 */           this.readBuffer.get(data);
/* 535 */           if (data.length < 2) {
/* 536 */             this.readStatus = 3;
/*     */           } else {
/* 538 */             this.readBuffer.compact();
/* 539 */             this.bytesReaded -= this.message_size;
/* 540 */             this.readStatus = 1;
/*     */           }
/*     */         } else {
/* 543 */           this.moreGumToChew = false;
/*     */         }
/* 545 */         logger.info("#session id:" + this.sessionId + " ip:" + getClientAddr() + " parase TGW");
/* 546 */         return;
/*     */       }
/*     */ 
/* 549 */       if ((this.message_size > this.readBuffer.capacity()) || (this.message_size < 2))
/*     */       {
/* 552 */         if (this.message_size == 15472) {
/* 553 */           this.writeBuffer.clear();
/* 554 */           this.writeBuffer.put(Configuration.getInstance().getCrossDomain().getBytes());
/* 555 */           this.writeBuffer.flip();
/* 556 */           logger.info("#session id:" + this.sessionId + " ip:" + getClientAddr() + " send XML_CONFIG");
/* 557 */           writeChannel();
/* 558 */           this.readStatus = 3;
/*     */         }
/*     */ 
/* 562 */         if (this.message_size == 18245)
/*     */         {
/* 564 */           if (Configuration.getInstance().isHttpServer())
/*     */           {
/* 566 */             chewReadHttp();
/*     */           }
/*     */           else {
/* 569 */             sendRawData(Configuration.getInstance().getRelocateUrl());
/* 570 */             disconnectWhenAllSent();
/* 571 */             this.moreGumToChew = false;
/*     */ 
/* 579 */             logger.info("#session id:" + this.sessionId + " ip:" + getClientAddr() + " send CDN");
/*     */           }
/*     */         }
/*     */         else
/* 583 */           this.readStatus = 3;
/*     */       }
/*     */       else {
/* 586 */         if (logger.isDebugEnabled()) {
/* 587 */           commLogger.debug("#" + this.sessionId + " HEADER IN - " + 
/* 588 */             this.message_size + " bytes message length");
/*     */         }
/* 590 */         this.bytesReaded -= 2;
/* 591 */         this.readStatus = 2;
/* 592 */         chewReadData();
/*     */       }
/*     */     } else {
/* 595 */       this.moreGumToChew = false;
/*     */     }
/*     */   }
/*     */ 
/*     */   private void chewReadHeader32()
/*     */   {
/* 603 */     if (this.bytesReaded >= 4) {
/* 604 */       this.readBuffer.flip();
/* 605 */       if (this.headerInSize)
/* 606 */         this.message_size = (this.readBuffer.getInt() - 4);
/*     */       else {
/* 608 */         this.message_size = this.readBuffer.getInt();
/*     */       }
/* 610 */       this.readBuffer.compact();
/*     */ 
/* 612 */       if ((this.message_size > this.readBuffer.capacity()) || (this.message_size < 2))
/*     */       {
/* 614 */         if (this.message_size == 1195725852) {
/* 615 */           if (Configuration.getInstance().isHttpServer())
/*     */           {
/* 617 */             chewReadHttp();
/*     */           }
/*     */         }
/* 620 */         else this.readStatus = 3; 
/*     */       }
/*     */       else
/*     */       {
/* 623 */         if (logger.isDebugEnabled()) {
/* 624 */           commLogger.debug("#" + this.sessionId + " HEADER IN - " + 
/* 625 */             this.message_size + " bytes message length");
/*     */         }
/* 627 */         this.bytesReaded -= 4;
/* 628 */         this.readStatus = 2;
/* 629 */         chewReadData32();
/*     */       }
/*     */     } else {
/* 632 */       this.moreGumToChew = false;
/*     */     }
/*     */   }
/*     */ 
/*     */   private void addInputMessage(Message message)
/*     */   {
/* 645 */     this.messagesReceived += 1;
/* 646 */     if ((!this.connected) && (commLogger.isInfoEnabled())) {
/* 647 */       byte[] bytes = message.getData();
/* 648 */       commLogger.debug("#" + this.sessionId + " RCV t " + " [" + 
/* 649 */         message.getData().length + "] " + 
/* 650 */         ByteUtil.byteArrayToHexString(bytes, true) + " '" + 
/* 651 */         ByteUtil.byteArrayToPrintableString(bytes) + '\'');
/*     */     }
/*     */ 
/* 654 */     addToInputQueue(message);
/*     */   }
/*     */ 
/*     */   public void pushMessage(Message message) {
/* 658 */     this.messagesReceived += 1;
/* 659 */     if ((!this.connected) && (commLogger.isInfoEnabled())) {
/* 660 */       byte[] bytes = message.getData();
/* 661 */       commLogger.debug("#" + this.sessionId + " RCV t " + " [" + 
/* 662 */         message.getData().length + "] " + 
/* 663 */         ByteUtil.byteArrayToHexString(bytes, true) + " '" + 
/* 664 */         ByteUtil.byteArrayToPrintableString(bytes) + '\'');
/*     */     }
/*     */ 
/* 671 */     message.setPushed(true);
/* 672 */     addToInputQueue(message);
/*     */   }
/*     */ 
/*     */   private void addToInputQueue(Message message)
/*     */   {
/* 684 */     this.messageInQueue.add(message);
/* 685 */     if (this.messageInQueue.size() > 100) {
/* 686 */       logger.error("#" + this.sessionId + ", " + "InQueue size=" + this.messageInQueue.size());
/*     */     }
/* 688 */     this.gate.notifyMessagesPendingToProcess(this);
/*     */   }
/*     */ 
/*     */   void addToOutputQueue(Message message)
/*     */   {
/* 699 */     this.messageOutQueue.add(message);
/* 700 */     this.gate.notifyDataToSend(this);
/*     */   }
/*     */ 
/*     */   private void sendMessage(Message message)
/*     */   {
/* 712 */     this.gate.addUpMessage(message);
/*     */   }
/*     */ 
/*     */   public void sendMessage(short type, byte[] data) {
/* 716 */     byte[] msgData = new byte[data.length + Configuration.getInstance().getWriteHeaderSize() + 2];
/* 717 */     MessageFilter filter = Configuration.getInstance().getMessageFilter();
/* 718 */     if (Configuration.getInstance().getWriteHeaderSize() == 2) {
/* 719 */       ByteUtil.setShort(msgData, 0, data.length + 2);
/* 720 */       ByteUtil.setShort(msgData, 2, type);
/* 721 */       System.arraycopy(data, 0, msgData, 4, data.length);
/* 722 */       if (filter != null)
/* 723 */         msgData = filter.encrypt(msgData, 2);
/*     */     } else {
/* 725 */       ByteUtil.setInt(msgData, 0, data.length + 2);
/* 726 */       ByteUtil.setShort(msgData, Configuration.getInstance().getWriteHeaderSize(), type);
/* 727 */       System.arraycopy(data, 0, msgData, Configuration.getInstance().getWriteHeaderSize() + 2, data.length);
/* 728 */       if (filter != null) {
/* 729 */         msgData = filter.encrypt(msgData, 4);
/*     */       }
/*     */     }
/* 732 */     Message bm = new Message(this, msgData);
/* 733 */     if (isConnected())
/* 734 */       sendMessage(bm);
/*     */   }
/*     */ 
/*     */   public void sendRawData(byte[] data)
/*     */   {
/* 739 */     Message bm = new Message(this, data);
/* 740 */     if (isConnected())
/* 741 */       sendMessage(bm);
/*     */   }
/*     */ 
/*     */   void processInputMessages()
/*     */   {
/* 748 */     Message message = null;
/* 749 */     while ((message = (Message)this.messageInQueue.poll()) != null)
/* 750 */       processInputMessage(message);
/*     */   }
/*     */ 
/*     */   private void processInputMessage(Message message)
/*     */   {
/* 755 */     this.processLock.lock();
/*     */     try {
/* 757 */       this.gate.addDownMessage(message);
/*     */     } finally {
/* 759 */       this.processLock.unlock();
/*     */     }
/*     */   }
/*     */ 
/*     */   public String toString()
/*     */   {
/* 766 */     StringBuffer sb = new StringBuffer();
/* 767 */     sb.append("Connection [#");
/* 768 */     sb.append(this.sessionId);
/*     */ 
/* 770 */     if ((this.channel != null) && (this.channel.socket() != null) && 
/* 771 */       (this.channel.socket().getInetAddress() != null)) {
/* 772 */       sb.append('@').append(NetUtil.inetAddressToIPString(this.channel.socket().getInetAddress()));
/*     */     }
/* 774 */     sb.append("]");
/* 775 */     return sb.toString();
/*     */   }
/*     */ 
/*     */   void disconnectWhenAllSent()
/*     */   {
/* 784 */     if (!this.connected) {
/* 785 */       disconnectChannel();
/*     */     } else {
/* 787 */       this.disconnectWhenDone = true;
/* 788 */       this.gate.notifyDataToSend(this);
/*     */     }
/*     */   }
/*     */ 
/*     */   public void killConnection() {
/* 793 */     this.isKilled = true;
/* 794 */     Message bm = new Message(this, 31);
/* 795 */     sendMessage(bm);
/* 796 */     setObject(null);
/*     */   }
/*     */ 
/*     */   public Gate getGate() {
/* 800 */     return this.gate;
/*     */   }
/*     */ 
/*     */   public void reset()
/*     */   {
/* 805 */     this.processorId = 0;
/*     */ 
/* 807 */     this.isKilled = false;
/*     */ 
/* 809 */     this.object = null;
/*     */ 
/* 811 */     this.channel = null;
/*     */ 
/* 813 */     this.moreGumToChew = false;
/*     */ 
/* 815 */     this.messagesReceived = 0;
/*     */ 
/* 817 */     this.message_size = 0;
/*     */ 
/* 819 */     this.bytesReaded = 0;
/*     */ 
/* 821 */     this.readBuffer.clear();
/*     */ 
/* 823 */     this.writeBuffer.clear();
/* 824 */     this.writeBuffer.flip();
/*     */ 
/* 826 */     this.readStatus = 1;
/*     */ 
/* 828 */     this.actualWrittingMessage = null;
/*     */ 
/* 830 */     this.messageInQueue.clear();
/*     */ 
/* 832 */     this.messageOutQueue.clear();
/*     */ 
/* 834 */     this.connected = false;
/*     */ 
/* 836 */     this.disconnectWhenDone = false;
/*     */ 
/* 838 */     this.clientAddr = null;
/*     */ 
/* 840 */     this.clientPort = 0;
/*     */ 
/* 842 */     this.httpRecOver = false;
/* 843 */     this.httpData = "";
/*     */ 
/* 845 */     this.decryptTimes = 0;
/*     */   }
/*     */ 
/*     */   public int hashCode()
/*     */   {
/* 850 */     return this.sessionId;
/*     */   }
/*     */ 
/*     */   public boolean equals(Object that)
/*     */   {
/* 855 */     if (this == that) {
/* 856 */       return true;
/*     */     }
/* 858 */     if ((that != null) && (that.getClass().equals(Connection.class))) {
/* 859 */       return ((Connection)that).getSessionID() == 
/* 860 */         getSessionID();
/*     */     }
/* 862 */     return false;
/*     */   }
/*     */ 
/*     */   final class WriteWorker
/*     */     implements Runnable
/*     */   {
/*     */     WriteWorker()
/*     */     {
/*     */     }
/*     */ 
/*     */     public void run()
/*     */     {
/* 278 */       Connection.this.writeLock.lock();
/*     */       try {
/* 280 */         Connection.this.writeData();
/*     */       } finally {
/* 282 */         Connection.this.writeLock.unlock();
/*     */       }
/*     */     }
/*     */   }
/*     */ }

/* Location:           C:\Users\yyyyyy\Desktop\youxigu\jjjjjjjjj.jar
 * Qualified Name:     com.youxigu.gs.core.Connection
 * JD-Core Version:    0.6.0
 */