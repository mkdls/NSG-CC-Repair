package com.nsgcc;
import java.lang.reflect.*;
public class HostMethodsTest {
 public static class A{} public static class B{} public static class C{}
 public static class Renamed {private String q(A a,B b,C c){return "view";} private String wrong(C c,B b,A a){return "wrong";}}
 public static class Child extends Renamed {}
 public static class Ambiguous {public String q(A a,B b,C c){return "q";} public String r(A a,B b,C c){return "r";}}
 public static class Original extends Ambiguous {public String E(A a,B b,C c){return "E";}}
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception {
  for(String name:new String[]{"com.nsgcc.HostMethods"}){
   Method resolve=Class.forName(name).getMethod("resolve",Class.class,String.class,Class.class,Class[].class);
   Class<?>[] params={A.class,B.class,C.class};
   Method m=(Method)resolve.invoke(null,Renamed.class,"E",String.class,params);check(m.getName().equals("q")&&m.invoke(new Renamed(),new A(),new B(),new C()).equals("view"),"renamed exact method is callable");
   m=(Method)resolve.invoke(null,Child.class,"E",String.class,params);check(m.getDeclaringClass()==Renamed.class,"inheritance walk");
   m=(Method)resolve.invoke(null,Original.class,"E",String.class,params);check(m.getDeclaringClass()==Original.class,"preferred audited name wins");
   for(Class<?> result:new Class<?>[]{String.class,Integer.class})try{resolve.invoke(null,Ambiguous.class,"E",result,params);throw new AssertionError("guessed ambiguous/missing signature");}catch(InvocationTargetException expected){check(expected.getCause() instanceof NoSuchMethodException,"explicit refusal");}
  }
  System.out.println("PASS: both resolvers; renamed/private view, exact ordered parameters, hierarchy, preferred mapping and ambiguous/wrong-return rejection.");
 }
}
