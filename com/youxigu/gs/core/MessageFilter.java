package com.youxigu.gs.core;

import com.youxigu.gs.message.Message;

public abstract interface MessageFilter
{
  public abstract byte[] encrypt(byte[] paramArrayOfByte);

  public abstract byte[] encrypt(byte[] paramArrayOfByte, int paramInt);

  public abstract byte[] decrypt(Message paramMessage);
}

/* Location:           C:\Users\yyyyyy\Desktop\youxigu\jjjjjjjjj.jar
 * Qualified Name:     com.youxigu.gs.core.MessageFilter
 * JD-Core Version:    0.6.0
 */