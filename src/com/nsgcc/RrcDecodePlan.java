package com.nsgcc;
import java.util.*;
/** Per-source budget for ordinary detail calls; never changes decoder or license state. */
final class RrcDecodePlan {
 static final int MAX_CALLS=4;
 static final class Candidate {final long id;final int rank;final boolean retry;Candidate(long id,int rank,boolean retry){this.id=id;this.rank=rank;this.retry=retry;}}
 private final Map<Long,Candidate> pending=new HashMap<>();private final Set<Long> attempted=new HashSet<>(),retried=new HashSet<>();
 int calls;boolean complete;
 static int rank(String title){
  if(title==null)return 0;
  if(title.contains("RRC Reconfiguration (PDU)"))return title.contains("Radio Bearer Config (PDU)")?3:2;
  return title.trim().equals("RRC Reconfiguration")?1:0;
 }
 void offer(long id,String title){int rank=rank(title);if(rank>0&&!attempted.contains(id))pending.putIfAbsent(id,new Candidate(id,rank,false));}
 Candidate take(){
  if(complete||calls>=MAX_CALLS)return null;
  Candidate c=pending.values().stream().min(Comparator.<Candidate>comparingInt(v->-v.rank).thenComparingLong(v->v.id)).orElse(null);
  if(c!=null){pending.remove(c.id);attempted.add(c.id);calls++;}return c;
 }
 void deferred(Candidate c){if(!c.retry&&retried.add(c.id)&&calls<MAX_CALLS)pending.put(c.id,new Candidate(c.id,4,true));}
 void completed(){complete=true;pending.clear();}
}
