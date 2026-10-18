package com.nsgcc;
import java.io.*;
final class RootLogInput extends InputStream {
 private final Process process;private final InputStream input;private final StringBuilder errors=new StringBuilder();
 private static String quote(String s){return "'"+s.replace("'","'\\''")+"'";}
 RootLogInput(String moduleApk,File file,long sourceTime)throws IOException{
  if(moduleApk==null)throw new IOException("Module APK path unavailable");
  String command="CLASSPATH="+quote(moduleApk)+" /system/bin/app_process /system/bin com.nsgcc.RootLogReader "+quote(file.toString())+" "+sourceTime;
  process=new ProcessBuilder("su","-c",command).start();input=process.getInputStream();
  Thread stderr=new Thread(()->{try(Reader r=new InputStreamReader(process.getErrorStream())){char[] b=new char[256];int n;while((n=r.read(b))>=0)synchronized(errors){if(errors.length()<4096)errors.append(b,0,Math.min(n,4096-errors.length()));}}catch(Exception ignored){}},"NSG-log-reader-errors");stderr.setDaemon(true);stderr.start();
 }
 public int read()throws IOException{byte[] b=new byte[1];return read(b,0,1)<0?-1:b[0]&255;}
 public int read(byte[] b,int off,int len)throws IOException{
  int n=input.read(b,off,len);if(n<0){try{process.waitFor(300,java.util.concurrent.TimeUnit.MILLISECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}synchronized(errors){throw new IOException("Root log reader stopped: "+errors);}}return n;
 }
 public void close(){try{process.getOutputStream().close();}catch(Exception ignored){}try{input.close();}catch(Exception ignored){}process.destroy();}
}
