package com.nsgcc;
import android.content.Context;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.util.*;

final class RelayCarrierRows {
 static final String TAG="nsg-relay-carrier-row";
 private static TextView text(Context ctx,String value,float size,int gravity){
  TextView t=new TextView(ctx);t.setText(value);t.setTextSize(size);t.setTextColor(Color.rgb(0,207,220));t.setSingleLine(true);t.setGravity(gravity|Gravity.CENTER_VERTICAL);t.setEllipsize(android.text.TextUtils.TruncateAt.END);return t;
 }
 private static String time(long us){return new java.text.SimpleDateFormat("HH:mm:ss.SSS",Locale.US).format(new Date(us/1000));}
 static View view(ViewGroup parent,Object adapter,RelayLogIndex.Row r){
  Context ctx=parent.getContext();
  View row=nativeRow(parent,adapter,r);
  if(row==null)row=fallback(ctx,r);
  bind(ctx,row,r);return row;
 }
 static View nativeRow(ViewGroup parent,Object adapter,RelayLogIndex.Row r){
  try{
   Context ctx=parent.getContext();String pkg="com.qtrun.QuickTest";
   View row=LayoutInflater.from(ctx).inflate(ctx.getResources().getIdentifier("nr_ca_list_row","layout",pkg),parent,false);
   String freq=r.ssb!=null?r.ssb.toString():r.grid!=null?r.grid+"*":"CC"+r.cc;
   String[] ids={"tv_type","tv_cell","tv_bandwidth","tv_throughput"};
   String[] values={"N",freq+"/"+r.pci,r.band+"/"+(r.bandwidthMHz>0?r.bandwidthMHz+" M":"—"),r.mbps==null?"—":String.format(Locale.US,"%.1f",r.mbps)};
   for(int i=0;i<ids.length;i++){TextView t=row.findViewById(ctx.getResources().getIdentifier(ids[i],"id",pkg));t.setText(values[i]);t.setTextColor(ctx.getResources().getColor(android.R.color.holo_blue_bright));}
   TextView bler=row.findViewById(ctx.getResources().getIdentifier("tv_bler","id",pkg));
   Object group=java.lang.reflect.Array.get(ReflectionFields.find(adapter.getClass(),"d").get(adapter),r.primary?0:1);
   Object model=group.getClass().getMethod("c",int.class).invoke(group,6);
   java.lang.reflect.Method formatter=null;
   for(java.lang.reflect.Method m:adapter.getClass().getMethods())if(m.getName().equals("h")&&java.lang.reflect.Modifier.isStatic(m.getModifiers())&&m.getParameterCount()==3&&m.getParameterTypes()[2]==TextView.class)formatter=m;
   if(formatter==null)throw new NoSuchMethodException("native BLER formatter");
   formatter.invoke(null,r.bler,model,bler);if(r.bler==null)bler.setText("—");return row;
  }catch(Throwable e){android.util.Log.w("NSGCC","Native carrier row unavailable",e);return null;}
 }
 static View fallback(Context ctx,RelayLogIndex.Row r){
  LinearLayout row=new LinearLayout(ctx);row.setTag(TAG);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
  float d=ctx.getResources().getDisplayMetrics().density;int p=(int)(1*d);row.setPadding(p,p,p,p);row.setMinimumHeight((int)(22*d));
  String freq=r.ssb!=null?r.ssb.toString():r.grid!=null?r.grid+"*":"CC"+r.cc+"·C"+r.cell;
  String throughput=r.mbps==null?"—":String.format(Locale.US,"%.1f",r.mbps);
  String bler=r.bler==null?"—":String.format(Locale.US,"%.1f %%",r.bler);
  row.addView(text(ctx,"N",10,Gravity.START),new LinearLayout.LayoutParams(0,-2,8));
  row.addView(text(ctx,freq+"/"+r.pci,13,Gravity.START),new LinearLayout.LayoutParams(0,-2,35));
  row.addView(text(ctx,r.band+"/"+(r.bandwidthMHz>0?r.bandwidthMHz+" M":"—"),13,Gravity.START),new LinearLayout.LayoutParams(0,-2,25));
  row.addView(text(ctx,throughput,13,Gravity.CENTER_HORIZONTAL),new LinearLayout.LayoutParams(0,-2,16));
  row.addView(text(ctx,bler,13,Gravity.CENTER_HORIZONTAL),new LinearLayout.LayoutParams(0,-2,16));
  return row;
 }
 static void bind(Context ctx,View row,RelayLogIndex.Row r){
  row.setTag(TAG);row.setAlpha(r.active?1f:0.6f);
  String freq=r.ssb!=null?r.ssb.toString():r.grid!=null?r.grid+"*":"CC"+r.cc;
  String throughput=r.mbps==null?"—":String.format(Locale.US,"%.1f",r.mbps);
  String bler=r.bler==null?"—":String.format(Locale.US,"%.1f %%",r.bler);
  row.setContentDescription("Relay carrier "+(r.primary?"Primary carrier":"Cell "+r.cell)+", CC "+r.cc+", "+freq+", downlink "+throughput+" Mbps, BLER "+bler+", "+(r.active?"Active":"Inactive; identity retained")+", previous complete second; tap for details");
  row.setOnClickListener(v->{
   String message="PHY CC "+r.cc+" → "+(r.primary?"Relay primary carrier (raw SCell Index 0)":"SCell "+r.cell)+"\nPCI "+r.pci+" · n"+r.band+
    "\nState: "+(r.active?"Active":"Inactive; identity retained")+(r.mappingFresh?"":"; previous mapping; current measurements unavailable")+"\nMapping time: "+time(r.mappingTime)+"\nSCell state time: "+time(r.stateTime)+
    "\n\n¹ Thput: "+throughput+" Mbps\nFixed one-second window: "+time(r.windowStart)+"–"+time(r.windowStart+1000000L)+
    "\nSum of raw per-CC throughput; the averaging window may differ from native NR Downlink.\n\nBLER: "+bler+" ("+r.blerSamples+" records)\nWithin the same window: sum of modem BLER x10 values / sample count / 10, verified against the native calculation. This is not the CRC failure ratio. No sample or uncertain mapping is shown as —."+
    (r.ssb==null?"\n\nNo own SSB configured.":"")+(r.grid!=null&&r.ssb==null?"\n* "+r.grid+" is an RRC-derived grid center, not an SSB frequency.":"")+
    (r.config!=null?"\nGrid frequency comes from decoded RRC. Bandwidth comes from the verified Relay format. Later undecoded RRC changes may invalidate the grid reference.":"\nNo matching RRC; the grid frequency remains unknown. Bandwidth is taken from the verified Relay format.");
   new android.app.AlertDialog.Builder(ctx).setTitle("Carrier mapping · CC "+r.cc).setMessage(message).setPositiveButton("Close",null).show();
  });
 }
}
