package com.nsgcc;
import android.app.*;import android.content.*;import android.os.*;import android.net.Uri;
import android.view.*;import android.widget.*;import android.util.Log;
import java.io.*;import java.lang.reflect.*;import java.util.*;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;
public final class CcModule extends XposedModule {
 private static final String TAG="NSGCC";
 private final Handler ui=new Handler(Looper.getMainLooper());
 private ClassLoader loader;private Method property,detail,first,last,seek,ended,next,key,value;private Constructor<?> iterator;
 private static final class RrcView {Object source;int sim;long id;String text;List<RrcCarrierFallback.Cell> cells;}
 private final LinkedList<RrcView> rrcViews=new LinkedList<>();
 private static Field field(Class<?> c,String n)throws Exception{return ReflectionFields.find(c,n);}
 private static Object read(Object o,String n)throws Exception{return field(o.getClass(),n).get(o);}
 private Object prop(Object s,String n,int sim)throws Exception{return property.invoke(s,"Common::"+n,sim);}
 private Object iter(Object p)throws Exception{return p==null?null:iterator.newInstance(p);}
 private static String str(Object o){return o==null?null:o.toString();}
 private void event(String name,Object... values){Log.i(TAG,name+" "+Arrays.toString(values));}
 @Override public void onPackageReady(PackageReadyParam param){
  if(!"com.qtrun.QuickTest".equals(param.getPackageName()))return;loader=param.getClassLoader();
  try{
   Class<?> ds=loader.loadClass("com.qtrun.sys.DataSource"),p=loader.loadClass("com.qtrun.sys.Property"),it=loader.loadClass("com.qtrun.sys.Property$Iterator");
   property=ds.getMethod("getProperty",String.class,int.class);detail=ds.getMethod("printDetail",long.class,int.class);iterator=it.getConstructor(p);
   first=it.getMethod("forward");last=it.getMethod("reverse");seek=it.getMethod("reverse",long.class);ended=it.getMethod("end");next=it.getMethod("next");key=it.getMethod("key");value=it.getMethod("value");
   installRelaySourceCapture(ds);installNsaReferenceRows(ds);installStatusButton();
   // Cache only details NSG actually returned; no authorization or decoder-state changes.
   Method nativeDetail=ds.getDeclaredMethod("printDetail",long.class,long.class,int.class);nativeDetail.setAccessible(true);
   hook(nativeDetail).intercept(chain->{Object result=chain.proceed();try{
    String text=(String)result;String rendered=RrcCarrierFallback.render(text);
    if(rendered!=null){Object ws=field(loader.loadClass("com.qtrun.sys.Workspace"),"j").get(null);
     if(read(ws,"c")==chain.getThisObject())rememberRrc(chain.getThisObject(),((Number)read(ws,"a")).intValue(),((Number)chain.getArg(1)).longValue(),text);
    }
   }catch(Throwable e){Log.w(TAG,"RRC observation",e);}return result;});
  }catch(Throwable e){Log.e(TAG,"CC module setup",e);nsaReferenceStatus="CC module initialization failed: "+e;}
 }
 private void installStatusButton()throws Exception{
  hook(loader.loadClass("y80").getMethod("E",LayoutInflater.class,ViewGroup.class,Bundle.class)).intercept(chain->{
   View original=(View)chain.proceed();if(original==null)return null;
   Context ctx=original.getContext();LinearLayout box=new LinearLayout(ctx);box.setOrientation(LinearLayout.VERTICAL);
   Button button=new Button(ctx);button.setText("CC repair status · 1.1.2");button.setOnClickListener(v->{
    String message="CC Repair 1.1.2\nEnabled: "+nsaReferenceEnabled+"\n"+relayStatusForUser();
    TextView text=new TextView(ctx);text.setText(message);text.setTextIsSelectable(true);text.setPadding(24,16,24,16);ScrollView scroll=new ScrollView(ctx);scroll.addView(text);
    new AlertDialog.Builder(ctx).setTitle("CC repair status").setView(scroll).setPositiveButton("Close",null).setNeutralButton("Copy",(d,w)->{((ClipboardManager)ctx.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("CC status",message));}).show();
   });box.addView(button,new LinearLayout.LayoutParams(-1,-2));box.addView(original,new LinearLayout.LayoutParams(-1,0,1));return box;
  });
 }
 private final RelaySources relaySources=new RelaySources();
 private void installRelaySourceCapture(Class<?> ds){
  relaySources.moduleApk=getModuleApplicationInfo().sourceDir;
  try {
   hook(loader.loadClass("com.qtrun.sys.TestService").getDeclaredMethod("openFile",android.os.ParcelFileDescriptor.class,String.class)).intercept(chain->{
    InputStream input=null;String error=null;
    try{
     android.os.ParcelFileDescriptor fd=(android.os.ParcelFileDescriptor)chain.getArg(0);
     if(fd!=null&&android.system.OsConstants.S_ISREG(android.system.Os.fstat(fd.getFileDescriptor()).st_mode))
      input=new FileInputStream("/proc/self/fd/"+fd.getFd()); // independent open; dup would share NSG's file offset
     else {
      // t2.call passes the selected content URI (with display-name fragment) while
      // NSG consumes its own socket pair. Reopen the provider, never read that socket.
      String origin=(String)chain.getArg(1);Uri uri=Uri.parse(origin).buildUpon().fragment(null).build();
      if(!"content".equals(uri.getScheme())&&!"file".equals(uri.getScheme()))throw new IOException("Unsupported log source");
      Context context=(Context)read(chain.getThisObject(),"u");input=context.getContentResolver().openInputStream(uri);
      if(input==null)error="The file provider returned no input stream";
     }
    }catch(Throwable e){error=e.toString();}
    Object source;
    try{source=chain.proceed();}catch(Throwable e){if(input!=null)try{input.close();}catch(Exception ignored){}throw e;}
    if(source==null){if(input!=null)try{input.close();}catch(Exception ignored){}return null;}
    if(input==null)Log.w(TAG,"Relay source unavailable: "+error);
    relaySources.load(source,input,error,(target,job)->{
     event("relay_index_result","summary",job.status);Log.i(TAG,"Relay index: "+job.status);
     ui.post(()->{try{
      Object ws=field(loader.loadClass("com.qtrun.sys.Workspace"),"j").get(null);
      if(read(ws,"c")==target){if(job.index!=null&&job.index.applicable)beginAutomaticRrc(target,job);nsaReferenceStatus=job.status;long cursor=((Number)read(ws,"g")).longValue();if(cursor>=0)ws.getClass().getMethod("e",long.class,Object.class).invoke(ws,cursor,null);}
     }catch(Throwable e){event("relay_refresh_error","error",e.toString());}});
    });return source;
   });
   hook(loader.loadClass("com.qtrun.sys.Workspace").getMethod("c",ds)).intercept(chain->{
    Object result=chain.proceed();Object source=read(chain.getThisObject(),"c");
    if(source!=null)ui.postDelayed(()->attachTestingSource(source),1000);
    return result;
   });
   hook(loader.loadClass("com.qtrun.sys.TestService").getDeclaredMethod("stopTest")).intercept(chain->{
    synchronized(relaySources.jobs){java.util.Iterator<RelaySources.Job> jobs=relaySources.jobs.values().iterator();while(jobs.hasNext()){RelaySources.Job j=jobs.next();if(j.live){j.cancel();jobs.remove();}}}
    return chain.proceed();
   });
   hook(loader.loadClass("com.qtrun.sys.Workspace").getMethod("b")).intercept(chain->{
    Object old=read(chain.getThisObject(),"c");if(old!=null)relaySources.remove(old);synchronized(rrcViews){rrcViews.clear();}nsaReferenceStates.clear();nsaReferenceStatus="Source changed; waiting for the new log index";return chain.proceed();
   });event("relay_source_hooks_installed");
  }catch(Throwable e){nsaReferenceStatus="Relay source hook failed: "+e;event("relay_source_hooks_unavailable","error",e.toString());}
 }
 private boolean testingActive(){
  try{Object service=loader.loadClass("com.qtrun.sys.TestService").getMethod("o").invoke(null);return Boolean.TRUE.equals(read(service,"o"))&&!Boolean.TRUE.equals(read(service,"p"));}catch(Throwable e){return false;}
 }
 private boolean cursorLocked(){
  try{Object service=loader.loadClass("com.qtrun.sys.TestService").getMethod("o").invoke(null);return Boolean.TRUE.equals(service.getClass().getMethod("x").invoke(service));}
  catch(Throwable e){return true;}
 }
 private void attachTestingSource(Object source){
  try{
   Object ws=field(loader.loadClass("com.qtrun.sys.Workspace"),"j").get(null);
   if(read(ws,"c")!=source||!testingActive()||relaySources.get(source)!=null)return;
   Object timestamps=property.invoke(source,"Common::Timestamp",1);
   if(timestamps==null){ui.postDelayed(()->attachTestingSource(source),1000);return;}
   Object firstTime=iter(timestamps);first.invoke(firstTime);
   if((boolean)ended.invoke(firstTime)){ui.postDelayed(()->attachTestingSource(source),1000);return;}
   Object date=value.invoke(firstTime);if(!(date instanceof Date))return;
   long sourceStart=((Date)date).getTime();
   relaySources.loadLive(source,sourceStart,(target,job)->ui.post(()->{
    try{
     Object active=field(loader.loadClass("com.qtrun.sys.Workspace"),"j").get(null);
     if(job.cancelled||read(active,"c")!=target)return;
     if(job.index==null)nsaReferenceStatus=job.status;
     if(job.index!=null&&job.index.applicable)beginAutomaticRrc(target,job);
     // Acquisition stays live; display refresh must honor NSG's history lock.
     if(testingActive()){
      boolean locked=cursorLocked();long latest=-1;
      if(!locked){Object timestamp=property.invoke(target,"Common::Timestamp",((Number)read(active,"a")).intValue());
       if(timestamp!=null){Object tail=iter(timestamp);last.invoke(tail);if(!(boolean)ended.invoke(tail))latest=((Number)key.invoke(tail)).longValue();}}
      long refresh=CursorPolicy.select(true,locked,((Number)read(active,"g")).longValue(),latest);
      if(refresh>=0)active.getClass().getMethod("a",long.class,short.class,boolean.class,Object.class).invoke(active,refresh,((Number)read(active,"a")).shortValue(),false,null);
     }
    }catch(Throwable e){event("relay_live_refresh_error","error",e.toString());}
   }));
   event("relay_live_attached","sourceStartMs",sourceStart);
  }catch(Throwable e){nsaReferenceStatus="Testing attachment failed: "+e;event("relay_live_attach_error","error",e.toString());}
 }
 // Read message titles first, then decode only the highest-priority SCG candidates.
 private void rememberRrc(Object source,int sim,long id,String text){
  String rendered=RrcCarrierFallback.render(text);if(rendered==null)return;
  RrcView v=new RrcView();v.source=source;v.sim=sim;v.id=id;v.text=rendered;v.cells=RrcCarrierFallback.parse(text);
  synchronized(rrcViews){rrcViews.removeIf(old->old.source==source&&old.sim==sim&&old.id==id);rrcViews.add(v);while(rrcViews.size()>16)rrcViews.removeFirst();}
  RelaySources.Job job=relaySources.get(source);if(job!=null){if(job.configIds.add(id))job.rrcFound++;job.needsRrc=false;job.rrcPlan.completed();job.autoRrcStatus="Complete RRC cached; automatic detail decoding paused. Calls: "+job.rrcPlan.calls+"/"+RrcDecodePlan.MAX_CALLS;}
 }
 private void beginAutomaticRrc(Object source,RelaySources.Job job){
  if(job.autoRrcStarted||job.cancelled||!job.needsRrc)return;
  if(job.rrcPlan.calls>=RrcDecodePlan.MAX_CALLS){job.autoRrcStatus="Automatic RRC budget reached (4 calls). Open the complete SCG manually; successful results will be cached.";return;}
  job.autoRrcStarted=true;job.autoRrcStatus="Scanning titles only; prioritizing nested NR RRC + radio bearer configuration";
  ui.postDelayed(new Runnable(){
   Object rows;int scanned;
   public void run(){
    try{
     Object ws=field(loader.loadClass("com.qtrun.sys.Workspace"),"j").get(null);
     if(job.cancelled||relaySources.get(source)!=job||read(ws,"c")!=source){job.autoRrcStarted=false;return;}
     if(!job.needsRrc){job.autoRrcStarted=false;return;}
     if(((Number)read(ws,"a")).intValue()!=1){ui.postDelayed(this,1000);return;}
     if(rows==null){Object titles=prop(source,"Message::Message_Title",1);if(titles==null)throw new IOException("No signaling message titles available");rows=iter(titles);if(job.lastRrcId>=0)rows.getClass().getMethod("forward",long.class).invoke(rows,job.lastRrcId+1);else first.invoke(rows);}
     int slice=0;
     while(!(boolean)ended.invoke(rows)&&scanned<200000&&slice++<500){
      long id=((Number)key.invoke(rows)).longValue();String title=str(value.invoke(rows));next.invoke(rows);scanned++;job.lastRrcId=id;job.rrcPlan.offer(id,title);
     }
     if(!(boolean)ended.invoke(rows)&&scanned<200000){ui.postDelayed(this,50);return;}
     RrcDecodePlan.Candidate candidate=job.rrcPlan.take();
     if(candidate!=null){
      String text=(String)detail.invoke(source,candidate.id,0);job.rrcDecoded++;if(candidate.retry)job.rrcRetries++;
      int lines=text==null?0:1;if(text!=null)for(int i=0;i<text.length();i++)if(text.charAt(i)=='\n')lines++;job.lastRrcLines=lines;
      rememberRrc(source,1,candidate.id,text);
      if(job.needsRrc&&text!=null&&text.contains("Decode on next scan")){job.rrcDeferred++;job.rrcPlan.deferred(candidate);}
      if(job.needsRrc){job.autoRrcStatus="Targeted RRC calls: "+job.rrcPlan.calls+"/4; complete configurations: "+job.rrcFound+"; latest: "+lines+" lines";ui.postDelayed(this,500);return;}
     }
     job.autoRrcStarted=false;
     if(job.needsRrc){job.autoRrcStatus="No complete SCG yet; targeted calls "+job.rrcPlan.calls+"/4. Generic reconfiguration messages were not decoded.";if(job.live)ui.postDelayed(()->beginAutomaticRrc(source,job),3000);}
     long cursor=((Number)read(ws,"g")).longValue();if(cursor>=0)ws.getClass().getMethod("e",long.class,Object.class).invoke(ws,cursor,null);
    }catch(Throwable e){job.autoRrcStarted=false;job.autoRrcStatus="Targeted RRC decoding failed: "+e.getClass().getSimpleName();}
   }
  },500);
 }
 private long replayTime(Object source,int sim,long record)throws Exception{
  Object p=property.invoke(source,"Common::Timestamp",sim);if(p==null)return -1;
  Object it=iterator.newInstance(p);seek.invoke(it,record);if((boolean)ended.invoke(it))return -1;
  Object date=value.invoke(it);return date instanceof Date?((Date)date).getTime()*1000L:-1;
 }
 private volatile boolean nsaReferenceEnabled=true;
 private volatile String nsaReferenceStatus="The downlink carrier page has not been opened";
 private static final class NsaReferenceState {
  Object source;int sim;long cursor,workspaceCursor;java.util.List<RelayLogIndex.Row> rows=java.util.Collections.emptyList();
 }
 private final java.util.Map<Object,NsaReferenceState> nsaReferenceStates=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
 private static final class UplinkState {Object source;int sim;long cursor;List<RelayLogIndex.Row> rows;}
 private final Map<Object,UplinkState> uplinkStates=Collections.synchronizedMap(new WeakHashMap<>());
 private boolean isNsaUplink(Object adapter){try{return read(adapter,"e").getClass().getName().equals("bb0");}catch(Throwable e){return false;}}
 private Integer numberAt(Object source,int sim,long cursor,String name)throws Exception{
  Object p=property.invoke(source,name,sim);if(p==null)return null;Object it=iter(p);seek.invoke(it,cursor);if((boolean)ended.invoke(it))return null;
  Object v=value.invoke(it);if(v instanceof Object[]){Object[] a=(Object[])v;v=a.length==0?null:a[0];}return v instanceof Number?((Number)v).intValue():null;
 }
 private String uplinkColumns(Object group,Object source,int sim,long cursor){
  StringBuilder out=new StringBuilder();
  try{
   List<?> specs=(List<?>)read(group,"a"),columns=(List<?>)read(group,"b");
   int shift=columns.size()-specs.size();
   for(int i=0;i<specs.size();i++){
    if(i>0)out.append("; ");String name=String.valueOf(read(specs.get(i),"a"));int sep=name.lastIndexOf("::");out.append(sep<0?name:name.substring(sep+2));
    int at=i+shift;Object values=at>=0&&at<columns.size()?columns.get(at):null;
    out.append("=").append(values instanceof Object[]?Arrays.toString((Object[])values):String.valueOf(values));
    Object p=property.invoke(source,name,sim);if(p==null)continue;Object it=iter(p);seek.invoke(it,cursor);if((boolean)ended.invoke(it))continue;
    long record=((Number)key.invoke(it)).longValue(),sampleTime=replayTime(source,sim,record),cursorTime=replayTime(source,sim,cursor);
    if(sampleTime>=0&&cursorTime>=0)out.append(" (age ").append((cursorTime-sampleTime)/1000).append(" ms)");
   }
  }catch(Throwable e){out.append(" [diagnostic ").append(e.getClass().getSimpleName()).append("]");}
  return out.toString();
 }
 private static final class UlSample {Object[] values;long record,time;}
 private UlSample uplinkSample(Object group,Object source,int sim,long cursor)throws Exception{
  List<?> specs=(List<?>)read(group,"a"),columns=(List<?>)read(group,"b");int shift=columns.size()-specs.size();
  for(int i=0;i<specs.size();i++){
   String name=String.valueOf(read(specs.get(i),"a"));if(!name.endsWith("Physical_Throughput_UL"))continue;
   int at=i+shift;if(at<0||at>=columns.size()||!(columns.get(at) instanceof Object[]))return null;
   Object p=property.invoke(source,name,sim);if(p==null)return null;Object it=iter(p);seek.invoke(it,cursor);if((boolean)ended.invoke(it))return null;
   UlSample sample=new UlSample();sample.values=(Object[])columns.get(at);sample.record=((Number)key.invoke(it)).longValue();sample.time=replayTime(source,sim,sample.record);return sample;
  }return null;
 }
 private void prepareUplink(Object adapter,Object source,int sim,long cursor){
  uplinkStates.remove(adapter);
  try{
   RelaySources.Job job=relaySources.get(source);if(job==null||job.index==null||!job.index.applicable||sim!=1)return;
   long time=replayTime(source,sim,cursor);Integer ssb=numberAt(source,sim,cursor,"NR5G::Serving_Cell::NR_ARFCN_SSB"),pci=numberAt(source,sim,cursor,"NR5G::Serving_Cell::NR_PCI");if(time<0||ssb==null||pci==null)return;
   RrcView config=null;synchronized(rrcViews){for(RrcView v:rrcViews)if(v.source==source&&v.sim==sim&&v.id<=cursor&&(config==null||v.id>config.id))config=v;}
   List<RelayLogIndex.Row> identities=job.index.uplinkReferences(time,config==null?Collections.emptyList():config.cells,config==null?job.index.baseUs:replayTime(source,sim,config.id),ssb,pci);
   List<Integer> order=job.index.uplinkOrder(time);Object groups=read(adapter,"d");
   int pcellCount=((Number)read(java.lang.reflect.Array.get(groups,0),"c")).intValue(),scellCount=((Number)read(java.lang.reflect.Array.get(groups,1),"c")).intValue();
   if(pcellCount>1){job.uplinkStatus="UL labels retained: unexpected primary row count";return;}
   List<RelayLogIndex.Row> matched=new ArrayList<>();int repaired=0;
   if(pcellCount==1){RelayLogIndex.Row primary=null;for(RelayLogIndex.Row r:identities)if(r.cc==0)primary=r;matched.add(primary);if(primary!=null)repaired++;}
   List<Integer> scells=new ArrayList<>();for(int cc:order)if(cc!=0)scells.add(cc);
   int observedScells=scells.size();String matching=scells.size()==scellCount?"full observed CC order":"unresolved";
   UlSample primarySample=uplinkSample(java.lang.reflect.Array.get(groups,0),source,sim,cursor),secondarySample=uplinkSample(java.lang.reflect.Array.get(groups,1),source,sim,cursor);
   if(primarySample!=null&&secondarySample!=null&&primarySample.record==secondarySample.record&&primarySample.values.length==1&&primarySample.values[0] instanceof Number&&secondarySample.values.length==scellCount){
    List<Integer> exact=job.index.matchUplinkWindow(secondarySample.time,(Number)primarySample.values[0],secondarySample.values);
    if(!exact.isEmpty()){scells=new ArrayList<>(exact);scells.remove(Integer.valueOf(0));matching="exact native PUSCH throughput window";}
   }
   for(int i=0;i<scellCount;i++){
    RelayLogIndex.Row row=null;
    if(scells.size()==scellCount)for(RelayLogIndex.Row r:identities)if(r.cc==scells.get(i))row=r;
    matched.add(row);if(row!=null)repaired++;
   }
   UplinkState state=new UplinkState();state.source=source;state.sim=sim;state.cursor=cursor;state.rows=matched;uplinkStates.put(adapter,state);
   job.uplinkStatus="UL labels matched: "+repaired+"/"+matched.size()+"; native P/S rows "+pcellCount+"/"+scellCount+"; observed SCell CCs "+observedScells+". Recent PUSCH CCs (1s/3s/5s): "+job.index.recentUplinkActivity(time,1000000L)+" / "+job.index.recentUplinkActivity(time,3000000L)+" / "+job.index.recentUplinkActivity(time,5000000L)+". UL RRC references are independent of DL sample freshness.";
   job.uplinkStatus+="\nNative UL SCell columns: "+uplinkColumns(java.lang.reflect.Array.get(groups,1),source,sim,cursor);
   job.uplinkStatus+="\nUL identity matching: "+matching+"; SCell order "+scells+"; native PCell throughput "+(primarySample==null?"unavailable":Arrays.toString(primarySample.values))+"; same sample record "+(primarySample!=null&&secondarySample!=null&&primarySample.record==secondarySample.record);
  }catch(Throwable e){RelaySources.Job j=relaySources.get(source);if(j!=null)j.uplinkStatus="UL label matching failed: "+e.getClass().getSimpleName();}
 }
 private View labelUplink(Object adapter,int position,View view){
  if(view!=null&&"nsgcc-ul-reference".equals(view.getTag())){view.setOnLongClickListener(null);view.setLongClickable(false);view.setContentDescription(null);view.setTag(null);}
  UplinkState state=uplinkStates.get(adapter);if(view==null||state==null||position<0||position>=state.rows.size())return view;
  try{
   Object ws=field(loader.loadClass("com.qtrun.sys.Workspace"),"j").get(null);if(read(ws,"c")!=state.source||((Number)read(ws,"a")).intValue()!=state.sim)return view;
   RelayLogIndex.Row row=state.rows.get(position);if(row==null)return view;Context ctx=view.getContext();int id=ctx.getResources().getIdentifier("tv_bandwidth","id","com.qtrun.QuickTest");TextView bandwidth=view.findViewById(id);
   if(bandwidth!=null){String nativeText=bandwidth.getText().toString();int slash=nativeText.indexOf('/');String width=slash>=0?nativeText.substring(slash+1):"-";
    if(row.config!=null&&row.config.ulBandwidthMHz>0)width=row.config.ulBandwidthMHz+" M";bandwidth.setText(row.band+"/"+width);}
   if(row.uplinkGrid!=null){TextView cell=view.findViewById(ctx.getResources().getIdentifier("tv_cell","id","com.qtrun.QuickTest"));if(cell!=null)cell.setText(row.uplinkGrid+"*");}
   view.setTag("nsgcc-ul-reference");
   view.setOnLongClickListener(v->{new AlertDialog.Builder(ctx).setTitle("Uplink carrier · CC "+row.cc).setMessage("Cell "+row.cell+" · n"+row.band+"\n"+(row.uplinkGrid==null?"UL frequency reference unavailable.":"* "+row.uplinkGrid+" is the configured UL grid center derived from RRC, not an SSB measurement. The verified TDD profile uses the DL Point A when the UL Point A is absent.")+"\nThroughput and BLER remain native values.").setPositiveButton("Close",null).show();return true;});
   view.setContentDescription("Uplink CC "+row.cc+"; "+(row.primary?"primary carrier":"Cell "+row.cell)+"; band n"+row.band+"; native throughput and BLER");
  }catch(Throwable e){Log.w(TAG,"UL row labels",e);}return view;
 }
 private boolean isNsaDownlink(Object adapter) {
  try{return read(adapter,"e").getClass().getName().equals("ab0");}catch(Throwable e){return false;}
 }
 private java.util.List<RelayLogIndex.Row> nsaRows(Object adapter) {
  if(!nsaReferenceEnabled)return java.util.Collections.emptyList();
  try {
   NsaReferenceState state=nsaReferenceStates.get(adapter);if(state==null)return java.util.Collections.emptyList();
   Object ws=field(loader.loadClass("com.qtrun.sys.Workspace"),"j").get(null);
   if(read(ws,"c")!=state.source||((Number)read(ws,"a")).intValue()!=state.sim||((Number)read(ws,"g")).longValue()!=state.workspaceCursor)return java.util.Collections.emptyList();
   return state.rows;
  }catch(Throwable e){return java.util.Collections.emptyList();}
 }
 private void installNsaReferenceRows(Class<?> ds) {
  try {
   Class<?> adapter=loader.loadClass("b10");
   hook(adapter.getMethod("e",long.class,ds,short.class)).intercept(chain->{
    Object result=chain.proceed(),owner=chain.getThisObject();
    if(isNsaUplink(owner)){boolean hadLabels=uplinkStates.containsKey(owner);prepareUplink(owner,chain.getArg(1),((Number)chain.getArg(2)).intValue(),((Number)chain.getArg(0)).longValue());return hadLabels||uplinkStates.containsKey(owner)?true:result;}
    if(!isNsaDownlink(owner))return result;
    NsaReferenceState state=new NsaReferenceState();state.cursor=((Number)chain.getArg(0)).longValue();state.source=chain.getArg(1);state.sim=((Number)chain.getArg(2)).intValue();
    try {Object ws=field(loader.loadClass("com.qtrun.sys.Workspace"),"j").get(null);state.workspaceCursor=((Number)read(ws,"g")).longValue();state.sim=((Number)read(ws,"a")).intValue();}
    catch(Throwable e){nsaReferenceStatus="Workspace read failed: "+e;}
    nsaReferenceStates.put(owner,state);
    if(!nsaReferenceEnabled)return result;
    try {
     RelaySources.Job job=relaySources.get(state.source);
     if(job==null){if(testingActive())attachTestingSource(state.source);nsaReferenceStatus="No index for the current source. Testing is reconnecting to the raw log; reload the file for Replay.";return result;}
     if(job.index==null){nsaReferenceStatus=job.status;return result;}
     if(!job.index.applicable){nsaReferenceStatus=job.status;return result;}
     beginAutomaticRrc(state.source,job);
     if(state.sim!=1){nsaReferenceStatus="Only module 1 has been verified; native rows are retained for this SIM";return result;}
     RrcView best=null;synchronized(rrcViews){for(RrcView v:rrcViews)if(v.source==state.source&&v.sim==state.sim&&v.id<=state.cursor&&(best==null||v.id>best.id))best=v;}
     // RRC adds grid frequencies; reliable direct identities can still show missing CC without it.
     Object group=java.lang.reflect.Array.get(read(owner,"d"),0);java.util.List<?> columns=(java.util.List<?>)read(group,"b");
     Object[] ssbs=(Object[])columns.get(1),pcis=(Object[])columns.get(2);
     if(ssbs==null||pcis==null||ssbs.length==0||pcis.length==0||!(ssbs[0] instanceof Number)||!(pcis[0] instanceof Number)){nsaReferenceStatus="No primary carrier identity available at the cursor";return result;}
     if(best!=null&&best.cells.stream().noneMatch(c->c.ssb!=null&&c.ssb.equals(((Number)ssbs[0]).intValue())&&c.pci==((Number)pcis[0]).intValue())){job.needsRrc=true;job.rrcPlan.complete=false;beginAutomaticRrc(state.source,job);}
     long time=replayTime(state.source,state.sim,state.cursor),configTime=best==null?job.index.baseUs:replayTime(state.source,state.sim,best.id);
     if(time<0||configTime<0){nsaReferenceStatus="Cannot convert the cursor to a log timestamp";return result;}
     state.rows=job.index.at(time,best==null?java.util.Collections.emptyList():best.cells,configTime,((Number)ssbs[0]).intValue(),((Number)pcis[0]).intValue());
     job.displayStatus="NSA match: "+state.rows.size()+" rows; primary carrier: "+ssbs[0]+"/"+pcis[0]+"; RRC "+(best==null?"unavailable; frequencies remain unknown for SSB-less rows":Long.toString(best.id))+"\n"+job.index.snapshotStatus(time);
     nsaReferenceStatus=job.status+"\nTime="+new Date(time/1000)+"; displaying "+state.rows.size()+" Relay carriers\nThput/BLER use the previous complete second; BLER follows the native calculation\n"+(state.rows.isEmpty()?"Mapping/state/RRC validation failed; native rows retained":"Queried at the cursor; backward seeking does not reuse future mappings");
     Log.i(TAG,"Relay display: "+nsaReferenceStatus);
     event("relay_display","cursor",state.cursor,"timeUs",time,"rows",state.rows.size(),"configId",best==null?-1:best.id);
    }catch(Throwable e){nsaReferenceStatus="Relay display match failed: "+e;event("relay_display_error","error",e.toString());}
    return state.rows.isEmpty()?result:true;
   });
   hook(adapter.getMethod("getCount")).intercept(chain->{java.util.List<RelayLogIndex.Row> rows=nsaRows(chain.getThisObject());return rows.isEmpty()?chain.proceed():rows.size();});
   hook(adapter.getMethod("getView",int.class,View.class,ViewGroup.class)).intercept(chain->{
    Object owner=chain.getThisObject();if(isNsaUplink(owner))return labelUplink(owner,((Number)chain.getArg(0)).intValue(),(View)chain.proceed());java.util.List<RelayLogIndex.Row> rows=nsaRows(owner);
    int position=((Number)chain.getArg(0)).intValue();
    if(position>=0&&position<rows.size())return RelayCarrierRows.view((ViewGroup)chain.getArg(2),chain.getThisObject(),rows.get(position));
    View recycled=(View)chain.getArg(1);
    if(recycled!=null&&(RelayCarrierRows.TAG.equals(recycled.getTag())))return chain.proceed(new Object[]{chain.getArg(0),null,chain.getArg(2)});
    return chain.proceed();
   });
   event("nsa_reference_hooks_installed");Log.i(TAG,"NSA reference hooks installed");
  }catch(Throwable e){nsaReferenceStatus="Hook failed: "+e;Log.e(TAG,nsaReferenceStatus,e);event("nsa_reference_hooks_unavailable","error",e.toString());}
 }

 private String relayStatusForUser(){
  try{Object ws=field(loader.loadClass("com.qtrun.sys.Workspace"),"j").get(null);RelaySources.Job j=relaySources.get(read(ws,"c"));
   if(j!=null)return j.status+"\n"+j.autoRrcStatus+"\nRRC totals: candidates "+j.rrcDecoded+"; complete configurations "+j.rrcFound+"; deferred decoding "+j.rrcDeferred+"; latest: "+j.lastRrcLines+" lines; normal retries "+j.rrcRetries+" attempts\n"+j.displayStatus+"\n"+j.uplinkStatus+"\nLast display status: "+nsaReferenceStatus;
  }catch(Throwable ignored){}return nsaReferenceStatus;
 }

}
