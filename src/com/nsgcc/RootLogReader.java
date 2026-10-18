package com.nsgcc;
import java.io.*;
/** Root subprocess: one validated regular NSG log, independent read offset, no writes. */
public final class RootLogReader {
 public static void main(String[] args){
  try{
   if(args.length!=2)throw new IOException("Expected log path and source time");
   File file=new File(args[0]).getCanonicalFile();String path=file.toString();
   if(!(path.startsWith("/storage/")||path.startsWith("/mnt/user/")||path.startsWith("/data/media/"))||!path.endsWith(".log")||!android.system.OsConstants.S_ISREG(android.system.Os.stat(path).st_mode))throw new IOException("Not an allowed regular log file");
   long expected=Long.parseLong(args[1]);
   try(FileInputStream input=new FileInputStream(file)){
    ByteArrayOutputStream prefix=new ByteArrayOutputStream();byte[] sig=input.readNBytes(6);
    if(sig.length!=6||sig[0]!=33||sig[1]!=78||sig[2]!=83||sig[3]!=71)throw new IOException("Not an NSG raw log");prefix.write(sig);
    byte[] end="</root>".getBytes(java.nio.charset.StandardCharsets.US_ASCII);int matched=0,ch;
    while(matched<end.length){ch=input.read();if(ch<0||prefix.size()>1048576)throw new IOException("Incomplete NSG header");prefix.write(ch);matched=ch==(end[matched]&255)?matched+1:ch==60?1:0;}
    byte[] initial=input.readNBytes(15);if(initial.length!=15||initial[0]!=10||initial[6]!=8)throw new IOException("Invalid NSG timestamp record");
    long base=RelayLogIndex.u32(initial,7)|(RelayLogIndex.u32(initial,11)<<32);
    if(Math.abs(base/1000L-expected)>10000L)throw new IOException("Log does not match current Testing source time");prefix.write(initial);
    java.util.concurrent.atomic.AtomicBoolean running=new java.util.concurrent.atomic.AtomicBoolean(true);
    Thread stop=new Thread(()->{try{while(System.in.read()>=0){} }catch(Exception ignored){}finally{running.set(false);}},"NSG-log-owner");stop.setDaemon(true);stop.start();
    System.out.write(prefix.toByteArray());System.out.flush();byte[] bytes=new byte[65536];
    while(running.get()){int n=input.read(bytes);if(n>0){System.out.write(bytes,0,n);System.out.flush();if(System.out.checkError())break;}else Thread.sleep(100);}
   }
  }catch(Throwable e){System.err.println("Root log reader: "+e.getClass().getSimpleName()+": "+e.getMessage());System.exit(1);}
 }
}
