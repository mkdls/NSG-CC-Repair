package com.nsgcc;
public class RrcDecodePlanTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args){
  RrcDecodePlan p=new RrcDecodePlan();
  for(int i=0;i<70;i++)p.offer(i,"RRC Connection Reconfiguration");
  check(p.take()==null&&p.calls==0,"generic titles consume no detail calls");
  p.offer(20,"RRC Connection Reconfiguration//RRC Reconfiguration (PDU)");
  p.offer(10,"RRC Connection Reconfiguration//RRC Reconfiguration (PDU);Radio Bearer Config (PDU)");
  RrcDecodePlan.Candidate first=p.take();check(first.id==10,"full nested configuration prioritized");
  p.deferred(first);RrcDecodePlan.Candidate retry=p.take();check(retry.id==10&&retry.retry,"deferred candidate retried before other candidates");p.deferred(retry);
  check(p.take().id==20,"same message retried only once");p.offer(30,"RRC Reconfiguration");check(p.take().id==30,"fourth call allowed");p.offer(40,"RRC Reconfiguration");check(p.take()==null&&p.calls==4,"per-source four-call cap enforced");
  RrcDecodePlan complete=new RrcDecodePlan();complete.offer(1,"RRC Reconfiguration");complete.completed();check(complete.take()==null&&complete.calls==0,"manual success pauses automatic detail decoding");
  System.out.println("PASS: title prioritization, zero generic decodes, one deferred retry, four-call cap, manual-completion pause");
 }
}
