package com.nsgcc;
import java.io.*;
import java.util.function.BooleanSupplier;
/** Independently opens an existing regular logger file. Never reads a device/socket. */
final class GrowingLogInput extends InputStream {
 private final FileInputStream input;private final BooleanSupplier cancelled;
 GrowingLogInput(File file,BooleanSupplier cancelled)throws IOException{input=new FileInputStream(file);this.cancelled=cancelled;}
 public int read()throws IOException{byte[] b=new byte[1];return read(b,0,1)<0?-1:b[0]&255;}
 public int read(byte[] b,int off,int len)throws IOException{
  if(len==0)return 0;
  while(!cancelled.getAsBoolean()&&!Thread.currentThread().isInterrupted()){
   int n=input.read(b,off,len);if(n>=0)return n;
   try{Thread.sleep(100);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new InterruptedIOException("Live log cancelled");}
  }
  throw new InterruptedIOException("Live log cancelled");
 }
 public void close()throws IOException{input.close();}
}
