package com.youxigu.gs.core;

import com.youxigu.gs.message.Message;
import java.nio.ByteBuffer;

public abstract interface Handler
{
  public abstract void execute(Object paramObject, Message paramMessage, ByteBuffer paramByteBuffer);
}

/* Location:           C:\Users\yyyyyy\Desktop\youxigu\jjjjjjjjj.jar
 * Qualified Name:     com.youxigu.gs.core.Handler
 * JD-Core Version:    0.6.0
 */