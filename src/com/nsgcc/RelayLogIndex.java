package com.nsgcc;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Read-only index for the verified Exynos ICD9 archive subset. No native writes. */
final class RelayLogIndex {
 static final long LEASE_US=2500000L;
 static final class Mapping {long time;int cc,cell,group,stack;}
 static final class Identity {int cell,pci,band,ssb,status;}
 static final class Cells {long time;List<Identity> values=new ArrayList<>();}
 static final class Traffic {long time,value;int cc;}
 static final class BlerBucket {
  float sumX10;int count;boolean valid=true;
  void add(int value){count++;if(value<0||value>1000)valid=false;else sumX10+=value;}
  Double percent(){return valid&&count>0?(double)(sumX10/(float)(10L*count)):null;}
 }
 static final class Row {
  int cc,cell,pci,band,bandwidthMHz;long mappingTime,stateTime,windowStart;Integer ssb;Long grid,uplinkGrid;
  Double mbps,bler;int blerSamples;boolean primary,active,mappingFresh;RrcCarrierFallback.Cell config;
 }
 final Map<Integer,TreeMap<Long,Mapping>> mappings=new TreeMap<>();
 final TreeMap<Long,Cells> states=new TreeMap<>();
 final Map<Integer,TreeMap<Long,Long>> buckets=new TreeMap<>();
 final Map<Integer,TreeMap<Long,BlerBucket>> blerBuckets=new TreeMap<>();
 final Map<Integer,TreeMap<Long,Long>> uplinkActivity=new TreeMap<>();
 final Map<Integer,Long> uplinkFirstSeen=new TreeMap<>();
 static final class UplinkBucket {
  long first=Long.MAX_VALUE,last;final TreeMap<Integer,Long> bits=new TreeMap<>();
  void add(long time,int cc,long value){first=Math.min(first,time);last=Math.max(last,time);bits.merge(cc,value,Long::sum);}
 }
 final Map<Long,UplinkBucket> uplinkBuckets=new TreeMap<>();
 synchronized List<Integer> matchUplinkWindow(long sampleTime,Number primary,Object[] secondary){
  if(primary==null||secondary==null)return Collections.emptyList();
  List<Integer> match=null;int matches=0;
  for(UplinkBucket bucket:uplinkBuckets.values()){
   // Common::Timestamp is a Date (milliseconds), while archive times use microseconds.
   if(bucket.last>sampleTime+999L||sampleTime-bucket.last>2000000L||bucket.bits.size()!=secondary.length+1||!bucket.bits.containsKey(0))continue;
   List<Integer> order=new ArrayList<>(bucket.bits.keySet());boolean valid=true;
   for(int i=0;i<order.size();i++){
    Object observed=i==0?primary:secondary[i-1];long bits=bucket.bits.get(order.get(i));
    // Native sums TBSize in a signed 32-bit integer, then converts bits/1e6 to float.
    if(!(observed instanceof Number)||bits>Integer.MAX_VALUE||Float.floatToIntBits((float)(bits/1000000.0))!=Float.floatToIntBits(((Number)observed).floatValue())){valid=false;break;}
   }
   if(valid){match=order;matches++;}
  }
  return matches==1?match:Collections.emptyList();
 }
 final List<Traffic> traffic=new ArrayList<>();
 boolean live;java.util.function.Consumer<RelayLogIndex> updates;long notifiedNs,prunedUs;
 long baseUs,lastUs,packets;int relayCount;String status,libVersion;boolean applicable;
 static long u32(byte[] b,int p){return ((long)b[p]&255)|((long)(b[p+1]&255)<<8)|((long)(b[p+2]&255)<<16)|((long)(b[p+3]&255)<<24);}
 static int u16(byte[] b,int p){return (b[p]&255)|((b[p+1]&255)<<8);}
 static final class Window {
  final InputStream in;final byte[] b=new byte[131072];int start,end;boolean eof;
  Window(InputStream in){this.in=in;}
  boolean need(int n)throws IOException{
   if(n>b.length)throw new IOException("archive record too large");
   if(end-start>=n)return true;
   if(start>0){System.arraycopy(b,start,b,0,end-start);end-=start;start=0;}
   while(end<n&&!eof){int k=in.read(b,end,b.length-end);if(k<0)eof=true;else if(k>0)end+=k;}
   return end-start>=n;
  }
  int at(int p){return b[start+p]&255;}
  void skip(int n){start+=n;}
  long var(int[] p,int limit)throws IOException{
   long n=0;for(int i=0;i<limit;i++){if(!need(p[0]+1))throw new EOFException();int x=at(p[0]++);n|=(long)(x&127)<<(7*i);if(x<128)return n;}throw new IOException("invalid varint");
  }
 }
 static RelayLogIndex read(InputStream original)throws IOException{
  BufferedInputStream buffered=new BufferedInputStream(original,131072);buffered.mark(2);int a=buffered.read(),b=buffered.read();buffered.reset();
  InputStream in=a==31&&b==139?new GZIPInputStream(buffered,131072):buffered;
  RelayLogIndex out=new RelayLogIndex();
  try(InputStream close=in){out.parse(in);}return out;
 }
 static void follow(InputStream in,java.util.function.Consumer<RelayLogIndex> updates)throws IOException{
  RelayLogIndex out=new RelayLogIndex();out.live=true;out.updates=updates;
  try(InputStream close=in){out.parse(in);}
 }
 private void publish(){
  applicable=relayCount>0&&!states.isEmpty()&&!traffic.isEmpty();
  status=(live?"Testing live log; ":"")+"ICD9 / "+libVersion+"; Relay "+relayCount+"; SCell states "+states.size()+"; per-CC throughput "+traffic.size();
  if(!applicable)status+="\nThe n257/100 MHz/120 kHz mapping and state required for repair are unavailable; native rows retained. Android 17 mmWave still requires field verification.";
 }
 private void prune(){
  if(!live||lastUs-prunedUs<10000000L)return;prunedUs=lastUs;long cutoff=lastUs-120000000L;
  for(TreeMap<Long,Mapping> h:mappings.values()){Map.Entry<Long,Mapping> keep=h.floorEntry(cutoff);h.headMap(cutoff,false).clear();if(keep!=null)h.put(keep.getKey(),keep.getValue());}
  states.headMap(cutoff,false).clear();
  for(TreeMap<Long,Long> h:uplinkActivity.values())h.headMap(cutoff/100000L,false).clear();
  uplinkBuckets.values().removeIf(bucket->bucket.last<cutoff);
  for(TreeMap<Long,Long> h:buckets.values())h.headMap(cutoff/1000000L,false).clear();
  for(TreeMap<Long,BlerBucket> h:blerBuckets.values())h.headMap(cutoff/1000000L,false).clear();
  traffic.removeIf(t->t.time<cutoff);
 }
 private void parse(InputStream in)throws IOException{
  byte[] signature=in.readNBytes(6);if(signature.length!=6||signature[0]!='!'||signature[1]!='N'||signature[2]!='S'||signature[3]!='G')throw new IOException("Unsupported NSG log");
  ByteArrayOutputStream header=new ByteArrayOutputStream();byte[] tail="</root>".getBytes(StandardCharsets.US_ASCII);int matched=0;
  while(matched<tail.length){int c=in.read();if(c<0||header.size()>1048576)throw new IOException("Incomplete log header");header.write(c);matched=c==tail[matched]?matched+1:(c==tail[0]?1:0);}
  String xml=header.toString(StandardCharsets.UTF_8);
  java.util.regex.Matcher version=java.util.regex.Pattern.compile("<libVersion>\\s*([^<]+?)\\s*</libVersion>").matcher(xml);
  libVersion=version.find()?version.group(1):"Unknown";
  if(!xml.contains("<type>ModuleExynos</type>")||!xml.matches("(?s).*<icdVersion>\\s*9\\s*</icdVersion>.*")||!(libVersion.equals("1700.24.79")||libVersion.equals("1700.26.36")))throw new IOException("Unverified Shannon version: "+libVersion+"; accepted profiles: ICD9 / 1700.24.79 and 1700.26.36");
  byte[] time=in.readNBytes(15);byte[] prefix={10,0,0,0,0,0,8};if(time.length!=15)throw new EOFException();
  for(int i=0;i<7;i++)if(time[i]!=prefix[i])throw new IOException("Unsupported initial timestamp record");
  baseUs=u32(time,7)|(u32(time,11)<<32);if(baseUs<1577836800000000L||baseUs>4102444800000000L)throw new IOException("Invalid log timestamp");
  Window w=new Window(in);long scanned=0;long started=System.nanoTime();
  while(w.need(2)){
   if((++scanned&65535)==0&&(Thread.currentThread().isInterrupted()||(!live&&System.nanoTime()-started>120000000000L)))throw new IOException("Log indexing cancelled or timed out");
   if(w.at(0)!=1||w.at(1)!=0){w.skip(1);continue;}
   int[] p={2};long delta,size;int s;
   try{
    delta=w.var(p,9);if(!w.need(p[0]+2)||w.at(p[0])!=24||w.at(p[0]+1)!=0){w.skip(1);continue;}p[0]+=2;
    size=w.var(p,5);s=p[0];
    if(delta<0||delta>604800000000L||size<15||size>65535||!w.need(s+(int)size)){w.skip(1);continue;}
   }catch(EOFException e){break;}catch(IOException e){w.skip(1);continue;}
   if(w.at(s)!=132||w.at(s+1)!=3||w.at(s+4)!=161){w.skip(1);continue;}
   long t=baseUs+delta;int tag=w.at(s+6),flags=w.at(s+5),payload=s+15,n=(int)size-15;int command=(int)u32(w.b,w.start+s+11);
   synchronized(this){
   packets++;lastUs=Math.max(lastUs,t);
   // Accept the verified single-stack SCG format only. Header bits are not SIM ids.
   if(tag==242&&flags==1&&command==27&&n==85){
    int q=w.start+payload;int cc=w.b[q]&255,group=w.b[q+1]&255,stack=w.b[q+2]&255,cell=u16(w.b,q+3);
    if(cc<16&&group==1&&stack==0&&cell<32&&u16(w.b,q+5)==14&&(w.b[q+7]&255)==3&&(w.b[q+8]&255)==1){
     Mapping m=new Mapping();m.time=t;m.cc=cc;m.cell=cell;m.group=group;m.stack=stack;mappings.computeIfAbsent(cc,k->new TreeMap<>()).put(t,m);relayCount++;
    }
   }else if(tag==241&&flags==33&&command==35&&n>=4){
    int q=w.start+payload,count=(int)u32(w.b,q);
    // ICD9 carries seven fixed 180-byte slots, count says how many are valid.
    if(count>=0&&count<=7&&n==1264){
     Cells cells=new Cells();cells.time=t;Set<Integer> seen=new HashSet<>();boolean valid=true;
     for(int i=0;i<count;i++){
      int z=q+4+i*180;Identity id=new Identity();id.cell=u16(w.b,z);id.ssb=(int)u32(w.b,z+2);id.pci=(int)u32(w.b,z+10);id.band=(int)u32(w.b,z+14);id.status=w.b[z+18]&255;
      if(id.cell>31||id.pci>1007||id.band!=257||id.status>1||!seen.add(id.cell)){valid=false;break;}cells.values.add(id);
     }
     if(valid)states.put(t,cells);
    }
   }else if(tag==241&&flags==1&&command==11&&n==32){
    // PhyNrTxPuschStatus.Time is a 32-bit PhyNrTimeUL; its low 4 bits identify CC.
    int cc=w.b[w.start+payload]&15;uplinkFirstSeen.merge(cc,t,Math::min);uplinkActivity.computeIfAbsent(cc,k->new TreeMap<>()).merge(t/100000L,t,Math::min);
    // SDM's 32 kHz clock defines native TickTime seconds; wall-clock seconds differ.
    long second=u32(w.b,w.start+s+7)/32768L;
    uplinkBuckets.computeIfAbsent(second,k->new UplinkBucket()).add(t,cc,u32(w.b,w.start+payload+24));
   }else if(tag==241&&flags==1&&command==6&&n==48){
    // Matches native 0x3062f58: mean of modem-provided BLER x10, not a CRC ratio.
    int q=w.start+payload,cc=w.b[q]&15,raw=(int)u32(w.b,q+44);long second=t/1000000L;
    blerBuckets.computeIfAbsent(cc,k->new TreeMap<>()).computeIfAbsent(second,k->new BlerBucket()).add(raw);
   }else if(tag==241&&flags==1&&command==7&&n==12){
    int q=w.start+payload;Traffic tr=new Traffic();tr.time=t;tr.cc=w.b[q]&15;tr.value=u32(w.b,q+8);traffic.add(tr);
    long second=t/1000000L;buckets.computeIfAbsent(tr.cc,k->new TreeMap<>()).merge(second,tr.value,Long::sum);
   }
   if(relayCount>1000000||states.size()>1000000||traffic.size()>1000000)throw new IOException("Index size limit exceeded");
   w.skip(s+(int)size);prune();
   }
   if(live&&System.nanoTime()-notifiedNs>500000000L){synchronized(this){publish();}notifiedNs=System.nanoTime();updates.accept(this);}
  }
  synchronized(this){publish();}
 }
 synchronized List<Row> uplinkReferences(long cursor,List<RrcCarrierFallback.Cell> config,long configTime,int primarySsb,int primaryPci){
  if(config.isEmpty())return at(cursor,config,configTime,primarySsb,primaryPci);
  List<Row> result=new ArrayList<>();if(cursor<baseUs||cursor>lastUs+LEASE_US||configTime>cursor)return result;
  RrcCarrierFallback.Cell primary=null;for(RrcCarrierFallback.Cell c:config)if(c.ssb!=null&&c.ssb==primarySsb&&c.pci==primaryPci)primary=c;
  if(primary==null)return result;
  Map.Entry<Long,Cells> latest=states.floorEntry(cursor);
  for(int cc:uplinkOrder(cursor)){
   TreeMap<Long,Mapping> history=mappings.get(cc);Map.Entry<Long,Mapping> mapping=history==null?null:history.floorEntry(cursor);
   RrcCarrierFallback.Cell cell=null;
   if(cc==0)cell=primary;
   else if(mapping!=null){for(RrcCarrierFallback.Cell c:config)if(c.index==mapping.getValue().cell)cell=c;}
   if(cell==null||cell.ulBandwidthMHz<=0)continue;
   if(cc!=0&&latest!=null&&latest.getKey()>=configTime){
    Identity observed=null;for(Identity id:latest.getValue().values)if(id.cell==cell.index)observed=id;
    if(observed==null||observed.pci!=cell.pci||observed.band!=257||!Objects.equals(observed.ssb>0?observed.ssb:null,cell.ssb))continue;
   }
   Row row=new Row();row.cc=cc;row.cell=cc==0?0:cell.index;row.primary=cc==0;row.pci=cell.pci;row.band=257;row.config=cell;row.uplinkGrid=mapping==null?null:cell.uplinkGridCenter(true);row.mappingTime=mapping==null?0:mapping.getKey();row.stateTime=latest==null?0:latest.getKey();result.add(row);
  }
  return result;
 }
 synchronized List<Integer> recentUplinkActivity(long cursor,long windowUs){
  List<Integer> result=new ArrayList<>();for(Map.Entry<Integer,TreeMap<Long,Long>> e:uplinkActivity.entrySet()){
   Map.Entry<Long,Long> t=e.getValue().floorEntry(cursor/100000L);if(t!=null&&t.getValue()>cursor)t=e.getValue().lowerEntry(t.getKey());
   if(t!=null&&cursor-t.getValue()<=windowUs)result.add(e.getKey());
  }return result;
 }
 synchronized List<Integer> uplinkOrder(long cursor){
  List<Integer> result=new ArrayList<>();for(Map.Entry<Integer,Long> e:uplinkFirstSeen.entrySet())if(e.getValue()<=cursor)result.add(e.getKey());return result;
 }
 synchronized String snapshotStatus(long cursor){
  Map.Entry<Long,Cells> state=states.floorEntry(cursor);
  return "Cursor ahead of indexed data by "+((cursor-lastUs)/1000L)+" ms; SCell state age at cursor: "+(state==null?"None":((cursor-state.getKey())/1000L)+" ms")+"; mapped CCs "+mappings.size();
 }
 private boolean observedIdentity(Mapping mapping,Row row,long cursor){
  Map.Entry<Long,Cells> sample=states.floorEntry(mapping.time);
  if(sample!=null&&mapping.time-sample.getKey()<=LEASE_US&&sameIdentity(sample.getValue(),row))return true;
  sample=states.ceilingEntry(mapping.time);
  return sample!=null&&sample.getKey()<=cursor&&sample.getKey()-mapping.time<=LEASE_US&&sameIdentity(sample.getValue(),row);
 }
 private static boolean sameIdentity(Cells cells,Row row){
  for(Identity id:cells.values)if(id.cell==row.cell&&id.pci==row.pci&&id.band==row.band&&Objects.equals(id.ssb>0?id.ssb:null,row.ssb))return true;
  return false;
 }
 synchronized List<Row> at(long cursor,List<RrcCarrierFallback.Cell> config,long configTime,int primarySsb,int primaryPci){
  if(cursor<baseUs||cursor>lastUs+LEASE_US||configTime>cursor)return Collections.emptyList();
  Map.Entry<Long,Cells> current=states.floorEntry(cursor);if(current==null||cursor-current.getKey()>LEASE_US||current.getKey()<configTime)return Collections.emptyList();
  RrcCarrierFallback.Cell primary=null;for(RrcCarrierFallback.Cell c:config)if(c.ssb!=null&&c.ssb==primarySsb&&c.pci==primaryPci)primary=c;
  // Direct Relay + current SCell identities can establish rows before RRC is decoded.
  // Never invent an SSB or grid frequency for an SSB-less cell.
  if(primary==null&&config.isEmpty()&&primarySsb>=2054167&&primarySsb<=2104166&&primaryPci>=0&&primaryPci<=1007){
   primary=new RrcCarrierFallback.Cell();primary.index=-1;primary.pci=primaryPci;primary.ssb=primarySsb;
  }
  if(primary==null)return Collections.emptyList();
  List<Row> out=new ArrayList<>();Set<Integer> seenCells=new HashSet<>();long window=cursor/1000000L-1;
  for(TreeMap<Long,Mapping> history:mappings.values()){
   Map.Entry<Long,Mapping> entry=history.floorEntry(cursor);if(entry==null)continue;Mapping m=entry.getValue();
   if(m.cc!=0&&m.cell==0)continue;
   Row r=new Row();r.bandwidthMHz=100;r.cc=m.cc;r.cell=m.cell;r.mappingTime=m.time;r.primary=m.cc==0&&m.cell==0;r.mappingFresh=cursor-m.time<=LEASE_US;r.active=r.primary;r.windowStart=window*1000000L;
   RrcCarrierFallback.Cell match=null;
   if(r.primary){match=primary;r.pci=primaryPci;r.ssb=primarySsb;r.band=257;r.stateTime=current.getKey();}
   else {
    Identity id=null;for(Identity v:current.getValue().values)if(v.cell==m.cell)id=v;
    if(id==null)continue;r.active=id.status==1;
    r.pci=id.pci;r.band=id.band;r.ssb=id.ssb>0?id.ssb:null;r.stateTime=current.getKey();
    for(RrcCarrierFallback.Cell c:config)if(c.index==id.cell&&c.pci==id.pci&&Objects.equals(c.ssb,r.ssb))match=c;
   }
   if(!r.primary&&(!r.active||!r.mappingFresh)&&match==null&&!observedIdentity(m,r,cursor))continue;
   // If an index was reused with a different current identity, do not carry the old frequency.
   if(match!=null&&match.scs==120&&match.prbs==66){r.config=match;r.grid=match.gridCenter();}
   if(!seenCells.add(r.primary?-1:r.cell))return Collections.emptyList();
   TreeMap<Long,Long> values=buckets.get(r.cc);Long bits=values==null?null:values.get(window);
   // Never attribute a completed bucket across a known mapping change or before first mapping.
   Map.Entry<Long,Mapping> before=history.floorEntry(r.windowStart);
   boolean stable=r.mappingFresh&&before!=null&&before.getValue().cell==m.cell;
   for(Mapping x:history.subMap(r.windowStart,true,(window+1)*1000000L,false).values())if(x.cell!=m.cell)stable=false;
   if(bits!=null&&stable&&r.windowStart>=baseUs)r.mbps=bits/1000000.0;
   TreeMap<Long,BlerBucket> blers=blerBuckets.get(r.cc);BlerBucket bler=blers==null?null:blers.get(window);
   if(bler!=null&&stable&&r.windowStart>=baseUs){r.bler=bler.percent();r.blerSamples=bler.count;}
   out.add(r);
  }
  if(out.stream().noneMatch(r->r.primary))return Collections.emptyList();
  out.sort(Comparator.comparingInt(r->r.primary?-1:r.cell));return out;
 }
}
