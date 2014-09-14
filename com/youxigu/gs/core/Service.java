package com.youxigu.gs.core;

import com.youxigu.gs.message.Message;

public abstract interface Service
{
  public abstract String getServiceName();

  public abstract void processDown(Message paramMessage);

  public abstract boolean isRunning();

  public abstract boolean haveFailed();

  public abstract boolean init();

  public abstract void end();
}

/* Location:           C:\Users\yyyyyy\Desktop\youxigu\jjjjjjjjj.jar
 * Qualified Name:     com.youxigu.gs.core.Service
 * JD-Core Version:    0.6.0
 */