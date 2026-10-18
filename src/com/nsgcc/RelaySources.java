package com.nsgcc;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

final class RelaySources {
 static final class Job {
  volatile RelayLogIndex index;volatile String status="Building the Relay time index";volatile boolean cancelled;volatile boolean autoRrcStarted;volatile boolean live;volatile long lastRrcId=-1;final RrcDecodePlan rrcPlan=new RrcDecodePlan();final java.util.Set<Long> configIds=new java.util.HashSet<>();volatile boolean needsRrc=true;volatile int rrcRetries;volatile int rrcDecoded,rrcFound,rrcDeferred,lastRrcLines;volatile String uplinkStatus="UL labels have not been evaluated";volatile String displayStatus="No NSA display match result yet";volatile String autoRrcStatus="Waiting for automatic RRC decoding";
  InputStream input;Future<?> future;
  void cancel(){cancelled=true;if(future!=null)future.cancel(true);try{if(input!=null)input.close();}catch(Exception ignored){}}
 }
 volatile String moduleApk;
 final Map<Object,Job> jobs=Collections.synchronizedMap(new WeakHashMap<>());
 final ExecutorService executor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"NSG-relay-index");t.setDaemon(true);return t;});
 void clear(){synchronized(jobs){for(Job j:jobs.values())j.cancel();jobs.clear();}}
 Job get(Object source){return jobs.get(source);}
 void remove(Object source){Job j=jobs.remove(source);if(j!=null)j.cancel();}
 void loadLive(Object source,long startedMs,BiConsumer<Object,Job> ready){
  Job j=new Job();j.live=true;j.status="Testing: locating the NSG raw log";jobs.put(source,j);
  java.lang.ref.WeakReference<Object> ref=new java.lang.ref.WeakReference<>(source);
  j.future=executor.submit(()->{
   try{
    File selected=null;String diagnostics="";InputStream rootInput=null;boolean rootAccess=false;
    for(int attempt=0;attempt<100&&!j.cancelled;attempt++){
     File[] files=new File("/proc/self/fd").listFiles();
     Set<String> seen=new HashSet<>();int logs=0,readable=0,compressed=0,mismatch=0;List<String> errors=new ArrayList<>();
     List<File> matches=new ArrayList<>();List<File> denied=new ArrayList<>();
     if(files!=null)for(File fd:files)try{
      // Android's canonical-path cache can leave /proc/self/fd unresolved. Read the symlink explicitly.
      String path=android.system.Os.readlink(fd.toString());
      if(!path.startsWith("/")||!(path.endsWith(".log")||path.endsWith(".log.gz"))||!seen.add(path))continue;
      if(!android.system.OsConstants.S_ISREG(android.system.Os.stat(path).st_mode))continue;
      logs++;File f=new File(path);
      try(InputStream probe=new FileInputStream(f)){
       byte[] sig=probe.readNBytes(6);readable++;
       if(sig.length>=2&&(sig[0]&255)==31&&(sig[1]&255)==139){compressed++;continue;}
       if(sig.length<6||sig[0]!=33||sig[1]!=78||sig[2]!=83||sig[3]!=71)continue;
       byte[] end="</root>".getBytes(java.nio.charset.StandardCharsets.US_ASCII);int found=0,bytes=0,ch;
       while(found<end.length&&(ch=probe.read())>=0&&bytes++<1048576)found=ch==(end[found]&255)?found+1:ch==60?1:0;
       if(found!=end.length)continue;byte[] initial=probe.readNBytes(15);
       if(initial.length!=15||initial[0]!=10||initial[6]!=8)continue;
       long base=RelayLogIndex.u32(initial,7)|(RelayLogIndex.u32(initial,11)<<32);
       if(Math.abs(base/1000L-startedMs)>10000L){mismatch++;continue;}
       matches.add(f);
      }catch(Exception e){if(e instanceof FileNotFoundException&&String.valueOf(e.getMessage()).contains("EACCES"))denied.add(f);if(errors.size()<4)errors.add(f.getName()+": "+e.getClass().getSimpleName()+" "+e.getMessage());}
     }catch(Exception ignored){}
     diagnostics="File search: FDs "+(files==null?-1:files.length)+"; raw/compressed logs "+logs+"; readable "+readable+"; gzip "+compressed+"; source time mismatch "+mismatch;
     if(!errors.isEmpty())diagnostics+="\nRead errors: "+String.join("; ",errors);
     if(matches.size()>1)throw new IOException("Multiple NSG logs match the current source time; selection refused\n"+diagnostics);
     if(matches.size()==1){selected=matches.get(0);break;}
     if(!denied.isEmpty()){
      for(File candidate:denied){
       RootLogInput root=new RootLogInput(moduleApk,candidate,startedMs);j.input=root;
       try{byte[] sig=root.readNBytes(6);if(sig.length!=6)throw new IOException("Root reader returned incomplete signature");rootInput=new SequenceInputStream(new ByteArrayInputStream(sig),root);selected=candidate;rootAccess=true;break;}
       catch(IOException error){root.close();j.input=null;if(errors.size()<4)errors.add(error.getMessage());}
      }
      if(selected!=null)break;
     }
     j.status="Testing: waiting for the current raw log (up to 10 seconds)\n"+diagnostics;
     if(attempt%20==0){Object target=ref.get();if(target!=null)ready.accept(target,j);}
     Thread.sleep(100);
    }
    if(selected==null)throw new IOException("No uncompressed NSG log matches the current source. Enable raw log recording; if already enabled, share the search results below.\n"+diagnostics);
    j.status="Testing: raw log found: "+selected.getName()+(rootAccess?" (root read-only)":"")+"; waiting for data";Object attached=ref.get();if(attached!=null)ready.accept(attached,j);
    InputStream input=rootInput!=null?rootInput:new GrowingLogInput(selected,()->j.cancelled);j.input=input;
    final boolean viaRoot=rootAccess;
    RelayLogIndex.follow(input,index->{
     // Do not bind a leftover logger belonging to another test/session.
     if(Math.abs(index.baseUs/1000L-startedMs)>10000L){j.cancelled=true;j.status="The raw log start time does not match the first timestamp of the current source; native rows retained";return;}
     if(!j.cancelled){j.index=index;j.status=index.status+"\nSource: "+(viaRoot?"root read-only log; ":"direct read-only log; ")+"matched to the first timestamp of the current Testing session; "+j.autoRrcStatus;Object target=ref.get();if(target!=null)ready.accept(target,j);}
    });
   }catch(Throwable e){if(!j.cancelled){j.status="Testing live index unavailable: "+e.getMessage();Object target=ref.get();if(target!=null)ready.accept(target,j);}}
   finally{try{if(j.input!=null)j.input.close();}catch(Exception ignored){}}
  });
 }
 void load(Object source,InputStream input,String error,BiConsumer<Object,Job> ready){
  Job j=new Job();j.input=input;jobs.put(source,j);
  if(input==null){j.status="Relay unavailable: "+error;return;}
  java.lang.ref.WeakReference<Object> ref=new java.lang.ref.WeakReference<>(source);
  j.future=executor.submit(()->{
   try{RelayLogIndex index=RelayLogIndex.read(input);if(!j.cancelled){j.index=index;j.status=index.status;}}
   catch(Throwable e){if(!j.cancelled)j.status="Relay index unavailable: "+e.getMessage();}
   finally{try{input.close();}catch(Exception ignored){}}
   Object target=ref.get();if(!j.cancelled&&target!=null)ready.accept(target,j);
  });
 }
}
