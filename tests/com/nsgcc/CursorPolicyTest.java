package com.nsgcc;

public final class CursorPolicyTest {
 private static void expect(boolean testing,boolean locked,long selected,long latest,long wanted){
  long got=CursorPolicy.select(testing,locked,selected,latest);
  if(got!=wanted)throw new AssertionError("Expected "+wanted+", received "+got);
 }
 public static void main(String[] args){
  expect(true,false,90,100,100);
  expect(true,true,100,110,100);
  expect(true,true,20,120,20);
  expect(true,true,15,130,15);
  expect(true,false,15,140,140);
  expect(false,false,20,140,20);
  expect(false,true,20,140,20);
  expect(true,true,-1,140,-1);
  System.out.println("Cursor policy transitions passed");
 }
}
