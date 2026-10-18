package com.nsgcc;
import java.util.*;
public class CoreTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args){
  RelayLogIndex.BlerBucket bler=new RelayLogIndex.BlerBucket();bler.add(0);bler.add(10);bler.add(20);check(bler.percent()==1.0,"BLER averages modem x10 values");bler.add(1001);check(bler.percent()==null,"invalid BLER remains unknown");
  RrcCarrierFallback.Cell cell=new RrcCarrierFallback.Cell();cell.pointA=2070000;cell.ulScs=120;cell.ulPrbs=66;cell.ulOffset=0;check(cell.uplinkGridCenter(false)==null,"UL Point A inheritance requires verified TDD");check(cell.uplinkGridCenter(true)==2070792L,"UL grid center uses actual carrier parameters");
  RelayLogIndex index=new RelayLogIndex();RelayLogIndex.UplinkBucket b=new RelayLogIndex.UplinkBucket();b.add(1000000,0,2000000);b.add(1000000,2,1000000);b.add(1000000,5,500000);index.uplinkBuckets.put(1L,b);
  check(index.matchUplinkWindow(1100000,2f,new Object[]{1f,0.5f}).equals(Arrays.asList(0,2,5)),"two SCCs match exact ordered native vector");
  check(index.matchUplinkWindow(1100000,2f,new Object[]{0.5f,1f}).isEmpty(),"wrong order rejected");check(index.matchUplinkWindow(900000,2f,new Object[]{1f,0.5f}).isEmpty(),"future window rejected");check(index.matchUplinkWindow(4000000,2f,new Object[]{1f,0.5f}).isEmpty(),"stale window rejected");
  RelayLogIndex.UplinkBucket collision=new RelayLogIndex.UplinkBucket();collision.add(1000000,0,2000000);collision.add(1000000,4,1000000);collision.add(1000000,5,500000);index.uplinkBuckets.put(2L,collision);check(index.matchUplinkWindow(1100000,2f,new Object[]{1f,0.5f}).isEmpty(),"ambiguous vectors rejected");
  System.out.println("PASS: BLER, verified TDD inheritance, exact uplink vectors and ambiguity/time guards");
 }
}
