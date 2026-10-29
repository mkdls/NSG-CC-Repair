package com.nsgcc;

/** Models the audited y80.b branch: origin != this selects the cursor's row again. */
public final class PassiveRefreshTest {
 static class Signaling {
  int top=12,offset=-17,selectionCalls,updates;
  void receive(long cursor,Object origin){updates++;if(origin!=this){selectionCalls++;top=(int)cursor;offset=0;}}
  void patched(long cursor,Object origin){receive(cursor,PassiveRefresh.signalingOrigin(origin,this));}
 }
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[] args){
  Signaling before=new Signaling();long selected=99;
  for(int i=0;i<30;i++)before.receive(CursorPolicy.select(true,true,selected,100+i),null);
  check(before.top==99&&before.selectionCalls==30,"locked cursor alone reproduces the native scroll-to-bottom problem");
  Signaling fixed=new Signaling();
  for(int i=0;i<30;i++)fixed.patched(CursorPolicy.select(true,true,selected,100+i),PassiveRefresh.liveOrigin(true));
  check(fixed.top==12&&fixed.offset==-17&&fixed.selectionCalls==0&&fixed.updates==30,"locked passive updates keep exact viewport while native data callbacks still run");
  fixed.patched(7,null);check(fixed.top==7&&fixed.selectionCalls==1,"manual time-axis seek still locates the requested message");
  fixed.top=21;fixed.offset=-9;fixed.patched(99,PassiveRefresh.ORIGIN);check(fixed.top==21&&fixed.offset==-9,"index/RRC completion is passive in replay too");
  fixed.patched(CursorPolicy.select(true,false,7,150),PassiveRefresh.liveOrigin(false));check(fixed.top==150,"unlock resumes native live following");
  Object user=new Object(),otherModule=new Object(){public boolean equals(Object value){return true;}};
  check(PassiveRefresh.signalingOrigin(user,fixed)==user&&PassiveRefresh.signalingOrigin(otherModule,fixed)==otherModule,"only the private marker identity changes origin; external actions are untouched");
  check(PassiveRefresh.signalingOrigin(null,fixed)==null&&PassiveRefresh.signalingOrigin(fixed,fixed)==fixed,"native null and signaling self-origin retain original semantics");
  Signaling second=new Signaling();check(PassiveRefresh.signalingOrigin(PassiveRefresh.ORIGIN,second)==second,"each signaling instance receives its own self-origin");
  System.out.println("PASS: locked scroll reproduction, exact viewport preservation, data update delivery, replay completion, manual seek and unlocked live following.");
 }
}
