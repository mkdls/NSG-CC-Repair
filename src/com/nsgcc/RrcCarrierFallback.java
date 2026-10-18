package com.nsgcc;

import java.util.*;
import java.util.regex.*;

/** A single decoded configuration, never an assertion about activated carriers. */
final class RrcCarrierFallback {
 static final class Cell {
  int index, pci, pointA, scs, prbs, offset,ulBandwidthMHz,ulPrbs,ulScs,ulOffset;Integer ulPointA; Integer ssb;
  Long uplinkGridCenter(boolean verifiedTdd){if(ulPrbs<=0||ulScs!=120||ulOffset<0)return null;Integer a=ulPointA!=null?ulPointA:verifiedTdd?pointA:null;return a==null?null:a+((long)ulOffset*12+ulPrbs*6)*2;}
  Long gridCenter(){return scs==120?pointA+((long)offset*12+prbs*6)*2:null;}
  String render(){
   // FR2 global raster is 60 kHz. Only use the verified 120 kHz layout.
   String center=scs==120?Long.toString(pointA+((long)offset*12+prbs*6)*2):"Not derived";
   return "Cell "+index+" | PCI "+pci+"\nSSB: "+(ssb==null?"No own SSB configured":ssb)+
    "\nPoint A: "+pointA+" | Grid center ARFCN: "+center+
    "\nSCS: "+scs+" kHz | "+prbs+" PRB"+(scs==120&&prbs==66?" (100 MHz)":"")+"\n";
  }
 }
 static Integer number(String s,String name){
  Matcher m=Pattern.compile("(?m)^\\s*"+Pattern.quote(name)+"\\s*:\\s*(\\d+)\\s*$").matcher(s);
  return m.find()?Integer.valueOf(m.group(1)):null;
 }
 static List<Cell> parse(String text){
  List<Cell> cells=new ArrayList<>();
  if(text==null||!text.contains("secondaryCellGroup")||!text.contains("cellGroupId : 1"))return cells;
  Matcher m=Pattern.compile("(?m)^([ ]*)(servCellIndex|sCellIndex)\\s*:\\s*(\\d+)\\s*$").matcher(text);
  List<Integer> starts=new ArrayList<>(),ends=new ArrayList<>(),ids=new ArrayList<>();
  while(m.find()){starts.add(m.start());ends.add(m.end());ids.add(Integer.parseInt(m.group(3)));}
  Set<Integer> seen=new HashSet<>();
  for(int i=0;i<starts.size();i++){
   String block=text.substring(ends.get(i),i+1<starts.size()?starts.get(i+1):text.length());
   int up=block.indexOf("uplinkConfigCommon");int ulBandwidth=0,ulPrbCount=0,ulScsValue=0,ulOffsetValue=-1;Integer ulPoint=null;
   if(up>=0){String ul=block.substring(up);int endUl=ul.indexOf("initialUplinkBWP");if(endUl>=0)ul=ul.substring(0,endUl);
    Integer ulPrbs=number(ul,"carrierBandwidth");Matcher ulSpacing=Pattern.compile("subcarrierSpacing\\s*:\\s*kHz(\\d+)").matcher(ul);
    if(ulPrbs!=null&&ulSpacing.find()){ulPrbCount=ulPrbs;ulScsValue=Integer.parseInt(ulSpacing.group(1));Integer offsetValue=number(ul,"offsetToCarrier");ulOffsetValue=offsetValue==null?-1:offsetValue;ulPoint=number(ul,"absoluteFrequencyPointA");if(ulPrbs==66&&ulScsValue==120)ulBandwidth=100;}
    block=block.substring(0,up);
   }
   Integer p=number(block,"absoluteFrequencyPointA"),pci=number(block,"physCellId"),bw=number(block,"carrierBandwidth"),off=number(block,"offsetToCarrier");
   Matcher spacing=Pattern.compile("subcarrierSpacing\\s*:\\s*kHz(\\d+)").matcher(block);
   if(p==null||p<2016667||p>3279165||pci==null||pci>1007||bw==null||bw<1||bw>275||off==null||!spacing.find())continue;
   Cell c=new Cell();c.index=ids.get(i);c.pci=pci;c.pointA=p;c.prbs=bw;c.offset=off;c.scs=Integer.parseInt(spacing.group(1));c.ssb=number(block,"absoluteFrequencySSB");c.ulBandwidthMHz=ulBandwidth;c.ulPrbs=ulPrbCount;c.ulScs=ulScsValue;c.ulOffset=ulOffsetValue;c.ulPointA=ulPoint;
   if(c.index<0||c.index>31||!seen.add(c.index))continue;
   cells.add(c);
  }
  return cells;
 }
 static String render(String text){
  List<Cell> cells=parse(text);if(cells.isEmpty())return null;
  StringBuilder out=new StringBuilder("Complete configurations parsed from this RRC: "+cells.size()+" FR2 carriers\nConfiguration only; does not imply activation or scheduling.\nThroughput/BLER have not been mapped in this configuration summary; no values are inferred.\n\n");
  for(Cell cell:cells)out.append(cell.render()).append('\n');return out.toString();
 }
}
