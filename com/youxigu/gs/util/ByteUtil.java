/*     */ package com.youxigu.gs.util;
/*     */ 
/*     */ import java.io.ByteArrayOutputStream;
/*     */ import java.io.DataInputStream;
/*     */ import java.io.DataOutputStream;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.UnsupportedEncodingException;
/*     */ import java.nio.ByteBuffer;
/*     */ 
/*     */ public final class ByteUtil
/*     */ {
/*     */   public static final void setUbyte(byte[] data, int position, int value)
/*     */   {
/*  14 */     data[position] = (byte)(value & 0xFF);
/*     */   }
/*     */ 
/*     */   public static final int getUbyte(byte[] data, int position) {
/*  18 */     return data[position] & 0xFF;
/*     */   }
/*     */ 
/*     */   public static final void setShort(byte[] data, int position, int value) {
/*  22 */     data[position] = (byte)((value & 0xFF00) >> 8);
/*  23 */     data[(position + 1)] = (byte)(value & 0xFF);
/*     */   }
/*     */ 
/*     */   public static final short getShort(byte[] data, int position) {
/*  27 */     return (short)(((data[position] & 0xFF) << 8) + (data[(position + 1)] & 0xFF));
/*     */   }
/*     */ 
/*     */   public static final void setInt(byte[] data, int position, int value) {
/*  31 */     data[position] = (byte)((value & 0xFF000000) >> 24);
/*  32 */     data[(position + 1)] = (byte)((value & 0xFF0000) >> 16);
/*  33 */     data[(position + 2)] = (byte)((value & 0xFF00) >> 8);
/*  34 */     data[(position + 3)] = (byte)(value & 0xFF);
/*     */   }
/*     */ 
/*     */   public static final int getInt(byte[] data, int position) {
/*  38 */     return (data[position] << 24 & 0xFF000000) + (data[(position + 1)] << 16 & 0xFF0000) + (
/*  39 */       data[(position + 2)] << 8 & 0xFF00) + (data[(position + 3)] & 0xFF);
/*     */   }
/*     */ 
/*     */   public static final void readAllData(InputStream in, byte[] buffer) throws IOException {
/*  43 */     int toRead = buffer.length;
/*  44 */     int readed = 0;
/*  45 */     while (readed < toRead)
/*  46 */       readed += in.read(buffer, readed, toRead - readed);
/*     */   }
/*     */ 
/*     */   public static final byte[] readAllData(InputStream in) throws IOException
/*     */   {
/*  51 */     ByteArrayOutputStream baos = new ByteArrayOutputStream();
/*  52 */     byte[] buffer = new byte[1024];
/*  53 */     int readed = 0;
/*  54 */     while ((readed = in.read(buffer)) != -1) {
/*  55 */       baos.write(buffer, 0, readed);
/*     */     }
/*  57 */     return baos.toByteArray();
/*     */   }
/*     */ 
/*     */   public static final String byteArrayToHexString(byte[] b) {
/*  61 */     return byteArrayToHexString(b, false);
/*     */   }
/*     */ 
/*     */   public static final String byteArrayToHexString(byte[] b, boolean spaced) {
/*  65 */     StringBuffer sb = new StringBuffer(b.length * 2);
/*  66 */     for (int i = 0; i < b.length; i++) {
/*  67 */       int v = b[i] & 0xFF;
/*  68 */       if (v < 16) {
/*  69 */         sb.append('0');
/*     */       }
/*  71 */       sb.append(Integer.toHexString(v));
/*  72 */       if (spaced) {
/*  73 */         sb.append(' ');
/*     */       }
/*     */     }
/*  76 */     return sb.toString().toUpperCase();
/*     */   }
/*     */ 
/*     */   public static final String getHexString(byte[] inByArr, int start, int len) {
/*  80 */     StringBuilder sb = new StringBuilder();
/*  81 */     for (int i = start; i < start + len; i++) {
/*  82 */       sb.append(Integer.toHexString(inByArr[i] & 0xFF)).append(' ');
/*     */     }
/*  84 */     return sb.toString();
/*     */   }
/*     */ 
/*     */   public static final String byteArrayToPrintableString(byte[] b) {
/*  88 */     StringBuffer sb = new StringBuffer(b.length);
/*  89 */     for (int i = 0; i < b.length; i++) {
/*  90 */       int v = b[i] & 0xFF;
/*  91 */       if ((v < 32) || (v > 127))
/*  92 */         sb.append(' ');
/*     */       else {
/*  94 */         sb.append((char)v);
/*     */       }
/*     */     }
/*     */ 
/*  98 */     return sb.toString();
/*     */   }
/*     */ 
/*     */   public static final byte[] hexStringToByteArray(String s) throws NumberFormatException {
/* 102 */     byte[] b = new byte[s.length() / 2];
/* 103 */     for (int i = 0; i < b.length; i++) {
/* 104 */       int index = i * 2;
/* 105 */       int v = Integer.parseInt(s.substring(index, index + 2), 16);
/* 106 */       b[i] = (byte)v;
/*     */     }
/* 108 */     return b;
/*     */   }
/*     */ 
/*     */   public static final String intArrayToString(int[] array) {
/* 112 */     if (array.length == 0) {
/* 113 */       return "[]";
/*     */     }
/* 115 */     StringBuffer sb = new StringBuffer("[");
/* 116 */     sb.append(array[0]);
/* 117 */     for (int i = 1; i < array.length; i++) {
/* 118 */       sb.append(", ");
/* 119 */       sb.append(array[i]);
/*     */     }
/* 121 */     sb.append(']');
/* 122 */     return sb.toString();
/*     */   }
/*     */ 
/*     */   public static final byte[] getUTFBytes(String string) {
/*     */     try {
/* 127 */       return string.getBytes("UTF-8"); } catch (UnsupportedEncodingException e) {
/*     */     }
/* 129 */     return string.getBytes();
/*     */   }
/*     */ 
/*     */   public static final String getUTFString(byte[] data)
/*     */   {
/* 134 */     return getUTFString(data, 0, data.length);
/*     */   }
/*     */ 
/*     */   public static final String getUTFString(byte[] data, int offset, int length) {
/*     */     try {
/* 139 */       return new String(data, offset, length, "UTF-8"); } catch (UnsupportedEncodingException e) {
/*     */     }
/* 141 */     return new String(data);
/*     */   }
/*     */ 
/*     */   public static final byte[] getBytes(byte[] source, int init)
/*     */   {
/* 146 */     int end = source.length;
/* 147 */     return getBytes(source, init, end);
/*     */   }
/*     */ 
/*     */   public static final byte[] getBytes(byte[] source, int init, int end) {
/* 151 */     byte[] result = new byte[end - init];
/* 152 */     System.arraycopy(source, init, result, 0, result.length);
/* 153 */     return result;
/*     */   }
/*     */ 
/*     */   public static final void writeUbyteUTFString(ByteBuffer buffer, String str)
/*     */   {
/* 173 */     if (str == null) {
/* 174 */       buffer.put(0);
/*     */     } else {
/* 176 */       int currentPosition = buffer.position();
/* 177 */       int length = str.length();
/* 178 */       if (length > 255) {
/* 179 */         length = 255;
/*     */       }
/* 181 */       buffer.put((byte)length);
/*     */       try {
/* 183 */         buffer.put(str.toUpperCase().getBytes("UTF-8"));
/*     */       } catch (UnsupportedEncodingException e) {
/* 185 */         buffer.put(currentPosition, 0);
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */   public static final int writeUbyteUTFString(DataOutputStream dos, String string, int maxLength) throws IOException {
/* 191 */     if (maxLength > 255) {
/* 192 */       maxLength = 255;
/*     */     }
/* 194 */     byte[] stringBytes = getUTFBytes(string);
/* 195 */     int toWrite = maxLength;
/* 196 */     if (stringBytes.length <= maxLength) {
/* 197 */       toWrite = stringBytes.length;
/*     */     }
/* 199 */     dos.write(toWrite);
/* 200 */     dos.write(stringBytes, 0, toWrite);
/* 201 */     return toWrite;
/*     */   }
/*     */ 
/*     */   public static final String readUShortUTFString(DataInputStream dis) throws IOException {
/* 205 */     byte[] buff = new byte[dis.readShort() & 0xFFFF];
/* 206 */     int len = dis.read(buff);
/* 207 */     return getUTFString(buff, 0, len);
/*     */   }
/*     */ 
/*     */   public static final int writeUShortUTFString(DataOutputStream dos, String string)
/*     */     throws IOException
/*     */   {
/* 217 */     return writeUShortUTFString(dos, string, 65536);
/*     */   }
/*     */ 
/*     */   public static final int writeUShortUTFString(DataOutputStream dos, String string, int maxLength) throws IOException {
/* 221 */     if (maxLength > 65536) {
/* 222 */       maxLength = 65536;
/*     */     }
/* 224 */     int toWrite = maxLength;
/* 225 */     byte[] stringBytes = getUTFBytes(string);
/* 226 */     if (stringBytes.length <= maxLength) {
/* 227 */       toWrite = stringBytes.length;
/*     */     }
/* 229 */     dos.writeShort(toWrite);
/* 230 */     dos.write(stringBytes, 0, toWrite);
/*     */ 
/* 232 */     return toWrite;
/*     */   }
/*     */ 
/*     */   public static final String getStackTrace(Throwable t) {
/* 236 */     StackTraceElement[] elements = t.getStackTrace();
/* 237 */     StringBuffer sb = new StringBuffer(t.getClass().getName() + " : " + t.getMessage());
/* 238 */     for (int i = 0; i < elements.length; i++) {
/* 239 */       StackTraceElement e = elements[i];
/* 240 */       sb.append("\n\t");
/* 241 */       sb.append(e.getClassName());
/* 242 */       sb.append("::");
/* 243 */       sb.append(e.getMethodName());
/* 244 */       sb.append(" [");
/* 245 */       sb.append(e.getLineNumber());
/* 246 */       sb.append(']');
/*     */     }
/* 248 */     return sb.toString();
/*     */   }
/*     */ 
/*     */   public static final boolean[] toBooleanArray(String string) {
/* 252 */     boolean[] result = new boolean[string.length()];
/* 253 */     for (int i = 0; i < string.length(); i++) {
/* 254 */       result[i] = (string.charAt(i) != '0' ? 1 : false);
/*     */     }
/* 256 */     return result;
/*     */   }
/*     */ 
/*     */   public static final boolean[] toBooleanArray(String string, int numberOfFlags) {
/* 260 */     boolean[] result = new boolean[numberOfFlags];
/* 261 */     int i = 0;
/*     */     do {
/* 263 */       result[i] = (string.charAt(i) != '0' ? 1 : false);
/*     */ 
/* 262 */       i++; if (i >= string.length()) break; 
/* 262 */     }while (i < numberOfFlags);
/*     */ 
/* 265 */     for (; i < numberOfFlags; i++) {
/* 266 */       result[i] = false;
/*     */     }
/* 268 */     return result;
/*     */   }
/*     */ 
/*     */   public static final int fromBinaryStringToInt(String string) {
/* 272 */     boolean[] binary = toBooleanArray(string);
/* 273 */     int result = 0;
/* 274 */     for (int i = 0; i < binary.length; i++) {
/* 275 */       result = (result << 1) + (binary[i] != 0 ? 1 : 0);
/*     */     }
/* 277 */     return result;
/*     */   }
/*     */ 
/*     */   public static final long getLong(byte[] data, int position) {
/* 281 */     return (data[position] << 56) + (
/* 282 */       (data[(position + 1)] & 0xFF) << 48) + (
/* 283 */       (data[(position + 2)] & 0xFF) << 40) + (
/* 284 */       (data[(position + 3)] & 0xFF) << 32) + (
/* 285 */       (data[(position + 4)] & 0xFF) << 24) + (
/* 286 */       (data[(position + 5)] & 0xFF) << 16) + (
/* 287 */       (data[(position + 6)] & 0xFF) << 8) + (
/* 288 */       (data[(position + 7)] & 0xFF) << 0);
/*     */   }
/*     */ 
/*     */   public static final void setLong(byte[] data, int position, long value) {
/* 292 */     data[(0 + position)] = (byte)(int)(value >> 56);
/* 293 */     data[(1 + position)] = (byte)(int)(value >> 48);
/* 294 */     data[(2 + position)] = (byte)(int)(value >> 40);
/* 295 */     data[(3 + position)] = (byte)(int)(value >> 32);
/* 296 */     data[(4 + position)] = (byte)(int)(value >> 24);
/* 297 */     data[(5 + position)] = (byte)(int)(value >> 16);
/* 298 */     data[(6 + position)] = (byte)(int)(value >> 8);
/* 299 */     data[(7 + position)] = (byte)(int)(value >> 0);
/*     */   }
/*     */ }

/* Location:           C:\Users\yyyyyy\Desktop\youxigu\jjjjjjjjj.jar
 * Qualified Name:     com.youxigu.gs.util.ByteUtil
 * JD-Core Version:    0.6.0
 */