package com.nsgcc;
import java.lang.reflect.*;
/** Explicit host layout selected by structural contracts, independent of framework brand. */
public final class HostLayout {
 private static boolean alternate;
 public static void configure(ClassLoader loader)throws Exception{
  Class<?> w=loader.loadClass("com.qtrun.sys.Workspace");
  try{Field singleton=w.getDeclaredField("j");if(singleton.getType()==w){alternate=false;return;}}catch(NoSuchFieldException ignored){}
  Field singleton=w.getDeclaredField("k");if(singleton.getType()!=w||w.getDeclaredField("d").getType()!=loader.loadClass("com.qtrun.sys.DataSource"))throw new NoSuchFieldException("Unknown Workspace layout");
  Class<?> signaling=loader.loadClass("w80");if(signaling.getDeclaredMethod("E",android.view.LayoutInflater.class,android.view.ViewGroup.class,android.os.Bundle.class).getReturnType()!=android.view.View.class)throw new NoSuchMethodException("Unknown signaling view");
  alternate=true;
 }
 public static String name(String logical){
  if(!alternate)return logical;
  switch(logical){case "y80":return "w80";case "ts":return "rs";case "zg0":return "ug0";case "jd0":return "id0";case "b10":return "y00";case "ab0":return "za0";case "bb0":return "ab0";case "ob0":return "nb0";case "pb0":return "ob0";default:return logical;}
 }
 public static String field(Class<?> owner,String logical){
  if(!alternate)return logical;String type=owner.getName();
  if(type.equals("com.qtrun.sys.Workspace")){switch(logical){case "j":return "k";case "c":return "d";case "a":return "b";case "g":return "h";default:return logical;}}
  if(type.equals("w80")){switch(logical){case "d0":return "e0";case "Z":return "a0";default:return logical;}}
  if(type.equals("ns")){if(logical.equals("g"))return "h";if(logical.equals("G"))return "H";}
  if(type.equals("sp")){if(logical.equals("a"))return "b";if(logical.equals("b"))return "c";}
  if(type.equals("x80")&&logical.equals("a"))return "b";
  if(type.equals("z30")){if(logical.equals("u"))return "v";if(logical.equals("o"))return "p";if(logical.equals("p"))return "q";}
  return logical;
 }
 public static String pagerMethod(){return alternate?"a":"g";}
 public static String titleMethod(){return alternate?"e0":"f0";}
 public static String labelMethod(){return alternate?"d0":"e0";}
 public static String description(){return alternate?"4.8.12 alternate layout (w80/Workspace.k)":"legacy layout (y80/Workspace.j)";}
}
